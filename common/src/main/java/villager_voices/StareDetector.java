package villager_voices;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Edge-triggered `player_staring` detection (docs/spec/domains/reaction.md §3
 * `player_staring` row, §7: "genuinely new design, no research precedent, first ticket" — the
 * three thresholds below are this ticket's own proposal, recorded in `VV-6`'s Findings, not a
 * value the spec already named). No vanilla hook exists for this event; a fabric-side poll
 * (`villager_voices.fabric.events.PolledEvents`) samples, once per server tick and per
 * player/villager pair, the dot product between the player's look vector and the unit vector from
 * the player's eyes to the villager's eyes, plus the distance between them, and hands both here.
 * Pure, no Minecraft import of its own (`ARCH-DEC-001`).
 *
 * <p>{@link #sample} returns {@code true} exactly once a player has kept the dot product at or
 * above {@link #DOT_THRESHOLD} and the distance at or below {@link #RANGE_BLOCKS} for
 * {@link #REQUIRED_TICKS} consecutive ticks — and not again for that player/villager pair until
 * the stare breaks (the dot drops below threshold, or the pair moves out of range) and a fresh run
 * reaches {@link #REQUIRED_TICKS} again, mirroring {@link PanicDetector}'s edge-triggered shape.
 */
public final class StareDetector {

    /**
     * {@code cos(15°)}: the player's look vector must point within ~15 degrees of the vector to
     * the villager's eyes — narrow enough to mean "looking at this villager specifically", not
     * merely "this villager is somewhere in view". Proposed default (`reaction.md` §7); no
     * research precedent to confirm against.
     */
    public static final double DOT_THRESHOLD = 0.9659258263; // cos(15 deg)

    /**
     * 8 blocks: close enough for the stare to plausibly be noticed, deliberately tighter than
     * `reaction.md` §3's 16-block sound-hearing default — hearing a villager and having it notice
     * a stare are different distances, and this event is about the latter. Proposed default.
     */
    public static final double RANGE_BLOCKS = 8.0;

    /**
     * 40 ticks (2s at 20 ticks/s): long enough that a passing camera swing across the villager
     * does not qualify, short enough to feel responsive. Proposed default.
     */
    public static final int REQUIRED_TICKS = 40;

    private final Map<PlayerVillagerKey, Integer> consecutiveTicks = new HashMap<>();

    /**
     * Samples one player/villager pair for the current tick.
     *
     * @param playerId       the staring player's UUID
     * @param villagerId     the stared-at villager's UUID
     * @param dot            this tick's dot product of the player's look vector and the unit
     *                       vector from the player's eyes to the villager's eyes (1.0 = dead
     *                       centre, 0.0 = perpendicular, negative = facing away)
     * @param distanceBlocks this tick's eye-to-eye distance, in blocks
     * @return {@code true} exactly on the tick the consecutive-qualifying-tick count first reaches
     *     {@link #REQUIRED_TICKS}; {@code false} on every other tick, including every tick after
     *     the first firing until the stare breaks and restarts
     */
    public boolean sample(UUID playerId, UUID villagerId, double dot, double distanceBlocks) {
        PlayerVillagerKey key = new PlayerVillagerKey(playerId, villagerId);
        if (dot < DOT_THRESHOLD || distanceBlocks > RANGE_BLOCKS) {
            consecutiveTicks.remove(key);
            return false;
        }
        int ticks = consecutiveTicks.merge(key, 1, Integer::sum);
        return ticks == REQUIRED_TICKS;
    }

    private record PlayerVillagerKey(UUID playerId, UUID villagerId) {
    }
}
