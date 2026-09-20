package villager_voices;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure cooldown, rate-limit, silence, and last-played-line bookkeeping for one
 * {@link VillagerEventBus} (docs/spec/domains/reaction.md §3, REACTION-REQ-005–010). Holds no
 * static state and touches no clock or random source of its own — every time-dependent method
 * takes {@code nowTicks} explicitly, so a unit test drives it with plain {@code long} values
 * instead of a real server tick loop (docs/spec/operations/testing.md).
 *
 * <p>The three windows below are this ticket's hardcoded defaults, taken from
 * docs/spec/domains/reaction.md §3's "Cooldowns and rate limit" table, which names them
 * "proposed", "not invented here", not yet confirmed by Kevin. VV-8 makes them
 * config-file-overridable (docs/spec/contracts/data-contract.md DATA-REQ-002) without changing
 * this class's logic at all — it will simply pass config-sourced values to the explicit
 * constructor below instead of relying on the defaults.
 */
public final class ReactionRules {

    /** Per-villager-per-event cooldown default: 60s at 20 ticks/s (REACTION-REQ-006). */
    public static final long DEFAULT_PER_EVENT_COOLDOWN_TICKS = 60 * 20L;
    /** Per-villager-global cooldown default: 5s at 20 ticks/s (REACTION-REQ-007). */
    public static final long DEFAULT_PER_VILLAGER_GLOBAL_COOLDOWN_TICKS = 5 * 20L;
    /** Per-player server-wide rate-limit window default: 1 line per 2s (REACTION-REQ-008). */
    public static final long DEFAULT_PER_PLAYER_RATE_LIMIT_TICKS = 2 * 20L;

    private final long perEventCooldownTicks;
    private final long perVillagerGlobalCooldownTicks;
    private final long perPlayerRateLimitTicks;

    private final Map<VillagerEventKey, Long> lastFiredPerEvent = new HashMap<>();
    private final Map<UUID, Long> lastFiredGlobal = new HashMap<>();
    private final Map<UUID, Long> lastLinePerPlayer = new HashMap<>();
    private final Map<VillagerEventKey, LineRef> lastPlayedLine = new HashMap<>();

    /** Uses the spec's proposed defaults (§3's "Cooldowns and rate limit" table). */
    public ReactionRules() {
        this(DEFAULT_PER_EVENT_COOLDOWN_TICKS, DEFAULT_PER_VILLAGER_GLOBAL_COOLDOWN_TICKS,
                DEFAULT_PER_PLAYER_RATE_LIMIT_TICKS);
    }

    public ReactionRules(long perEventCooldownTicks, long perVillagerGlobalCooldownTicks,
            long perPlayerRateLimitTicks) {
        this.perEventCooldownTicks = perEventCooldownTicks;
        this.perVillagerGlobalCooldownTicks = perVillagerGlobalCooldownTicks;
        this.perPlayerRateLimitTicks = perPlayerRateLimitTicks;
    }

    /**
     * Whether a just-detected {@code event} on {@code villagerId} should reach selection: not
     * silenced (REACTION-REQ-009, REACTION-REQ-010), not within its own per-event cooldown
     * (REACTION-REQ-006), and not within the villager's global cooldown (REACTION-REQ-007).
     * Records nothing by itself — call {@link #record} once a line is actually selected, so a
     * suppressed detection never starts a cooldown window of its own
     * (REACTION-FAIL-002's backstop reasoning: the global cooldown only backstops a *missed*
     * per-event edge case, it does not fire on every detection regardless of outcome).
     */
    public boolean permits(UUID villagerId, VillagerReactionEvent event, boolean villagerAsleep,
            boolean villagerBaby, long nowTicks) {
        if (isSilenced(event, villagerAsleep, villagerBaby)) {
            return false;
        }
        Long lastEvent = lastFiredPerEvent.get(new VillagerEventKey(villagerId, event));
        if (lastEvent != null && nowTicks - lastEvent < perEventCooldownTicks) {
            return false;
        }
        Long lastGlobal = lastFiredGlobal.get(villagerId);
        return lastGlobal == null || nowTicks - lastGlobal >= perVillagerGlobalCooldownTicks;
    }

    /**
     * Whether {@code event} is suppressed by the sleep/baby silence rules alone, independent of
     * any cooldown: asleep suppresses every event but {@code SLEEP} (REACTION-REQ-009); baby
     * suppresses every event but {@code BABY_GROWS} (REACTION-REQ-010) — the two combine, so a
     * sleeping baby's {@code SLEEP} event is still silenced by the baby rule.
     */
    public static boolean isSilenced(VillagerReactionEvent event, boolean villagerAsleep, boolean villagerBaby) {
        if (villagerAsleep && event != VillagerReactionEvent.SLEEP) {
            return true;
        }
        return villagerBaby && event != VillagerReactionEvent.BABY_GROWS;
    }

    /** The line that played last for {@code villagerId} on {@code event}, if any. */
    public Optional<LineRef> lastPlayed(UUID villagerId, VillagerReactionEvent event) {
        return Optional.ofNullable(lastPlayedLine.get(new VillagerEventKey(villagerId, event)));
    }

    /**
     * Records that {@code line} was selected for {@code villagerId}/{@code event} at
     * {@code nowTicks}: starts both cooldown windows and remembers the line for the next
     * no-immediate-repeat selection (REACTION-REQ-005).
     */
    public void record(UUID villagerId, VillagerReactionEvent event, LineRef line, long nowTicks) {
        VillagerEventKey key = new VillagerEventKey(villagerId, event);
        lastFiredPerEvent.put(key, nowTicks);
        lastFiredGlobal.put(villagerId, nowTicks);
        lastPlayedLine.put(key, line);
    }

    /**
     * Attempts to consume {@code playerId}'s server-wide rate-limit slot at {@code nowTicks}
     * (REACTION-REQ-008), independent of how many villagers are nearby: returns {@code true} and
     * starts a fresh window when the player's last line was at least
     * {@code perPlayerRateLimitTicks} ago (or never); returns {@code false} and leaves the window
     * untouched otherwise.
     */
    public boolean tryConsumePlayerRate(UUID playerId, long nowTicks) {
        Long last = lastLinePerPlayer.get(playerId);
        if (last != null && nowTicks - last < perPlayerRateLimitTicks) {
            return false;
        }
        lastLinePerPlayer.put(playerId, nowTicks);
        return true;
    }

    private record VillagerEventKey(UUID villagerId, VillagerReactionEvent event) {
    }
}
