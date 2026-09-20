package villager_voices.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-villager talking-until-tick map (docs/spec/domains/compat.md {@code COMPAT-REQ-002}): a
 * villager is talking for exactly the half-open interval {@code [the tick startTalking was called,
 * untilTick)} of its currently-playing reaction line's sound, {@code false} otherwise. Pure Java —
 * zero Minecraft, Fabric, or NeoForge imports ({@code ARCH-DEC-001}) — deliberately the <em>same</em>
 * class on both sides of VV-12's sync, one identity scheme end to end (a villager's {@link UUID},
 * matching {@link villager_voices.LineSink#show}'s own convention): the fabric server module holds
 * one authoritative instance it marks when a line plays, a small sync payload carries each mark's
 * own {@code untilTick} to nearby clients, and each client holds its own instance the render-state
 * side channel queries during {@code extractRenderState} — never re-derived from anything else,
 * {@code COMPAT-DEC-001}.
 *
 * <p>Never persisted (docs/spec/contracts/data-contract.md "The talking-state flag is transient,
 * not persisted", {@code DATA-REQ-003}) — this class carries no I/O of its own, so that rule is
 * satisfied simply by never being written to disk by any caller, not by anything in here.
 *
 * <p>Not thread-safe, the same discipline as {@link villager_voices.display.DisplayQueue}: intended
 * to be driven from one thread only (the server thread server-side, the render thread client-side).
 * One map entry per distinct villager ever marked talking in this session — never additional per
 * call, since a repeat {@link #startTalking} for an already-known id simply overwrites its entry —
 * so unbounded growth is bounded by the number of distinct villagers ever talked to, judged
 * acceptable for the alpha's session lifetime (recorded in this ticket's Findings).
 */
public final class TalkingState {

    private final Map<UUID, Long> talkingUntilTick = new HashMap<>();

    /**
     * Marks {@code villagerId} talking through {@code untilTick}, exclusive — replaces any
     * still-pending mark for the same id rather than extending or stacking it.
     */
    public void startTalking(UUID villagerId, long untilTick) {
        talkingUntilTick.put(villagerId, untilTick);
    }

    /**
     * @return whether {@code villagerId} is talking at {@code now}: {@code true} exactly while
     *     {@code now} is strictly before the {@code untilTick} its most recent {@link #startTalking}
     *     call named; an id never marked, or one whose mark has expired, is {@code false}.
     */
    public boolean isTalking(UUID villagerId, long now) {
        Long untilTick = talkingUntilTick.get(villagerId);
        return untilTick != null && now < untilTick;
    }
}
