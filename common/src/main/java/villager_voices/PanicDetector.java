package villager_voices;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Edge-triggered `panic` detection (docs/spec/domains/reaction.md §3 `panic` row,
 * `ARCH-DEC-003`): the mod has no push-based hook for a villager's
 * {@code Brain<Villager>.isActive(Activity.PANIC)} state, so a fabric-side poll samples it once
 * per server tick and hands the boolean here (`villager_voices.fabric.events.PolledEvents`). Pure,
 * no Minecraft import of its own (`ARCH-DEC-001`) — a per-villager memory of whether it was
 * panicking last tick is the only state it holds.
 *
 * <p>{@link #sample} returns {@code true} exactly on the tick a villager's panic state transitions
 * from not-panicking to panicking, and {@code false} on every other tick — including every tick
 * while panic continues, and every tick while calm — so a steady "still panicking" or
 * "never panicking" village produces no line, no new heap object, and no map resize once the
 * tracked villager set has stabilized (`REACTION-FAIL-004`, `ARCH-FAIL-004`).
 */
public final class PanicDetector {

    private final Set<UUID> panicking = new HashSet<>();

    /**
     * Samples one villager's panic state for the current tick.
     *
     * @param villagerId  the villager's persistent UUID, the per-villager memory key
     * @param isPanicking this tick's {@code Brain.isActive(Activity.PANIC)} reading
     * @return {@code true} exactly on the tick {@code isPanicking} first becomes {@code true} since
     *     the last time it was {@code false} (or since this villager was first sampled); otherwise
     *     {@code false}
     */
    public boolean sample(UUID villagerId, boolean isPanicking) {
        if (isPanicking) {
            // Set.add returns true only the first time an element is added -- the rising edge.
            return panicking.add(villagerId);
        }
        panicking.remove(villagerId);
        return false;
    }
}
