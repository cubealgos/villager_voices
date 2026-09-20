package villager_voices;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

/**
 * The selection rule (docs/spec/domains/reaction.md "Selection rule", REACTION-REQ-005): a random
 * pick among an event's eligible lines, excluding the one line that played last for that villager
 * on that event — unless excluding it would leave nothing to pick from, in which case the
 * exclusion is dropped and a repeat is allowed rather than the event producing no line at all. The
 * spec does not rule on this edge case explicitly; this is this ticket's decision, made because a
 * single-line catalogue (a datapack override, or any event before it has 4 lines) must still be
 * able to react at all, and REACTION-REQ-005 only ever asks to prefer a non-repeat, not to
 * guarantee one.
 *
 * <p>Pure: the caller supplies the roll, so a unit test drives it with a fixed source
 * (docs/spec/operations/testing.md) instead of {@link java.util.Random} directly.
 */
public final class LineSelector {

    private LineSelector() {
    }

    /**
     * @param eligible   the event's eligible lines (already catalogue-filtered by the caller); may
     *                   be empty
     * @param lastPlayed the line that played last for this villager on this event, if any
     * @param roll       a source of a uniformly distributed index in {@code [0, bound)} given
     *                   {@code bound}, e.g. {@code new Random()::nextInt}
     * @return the selected line, or empty when {@code eligible} is itself empty
     */
    public static Optional<LineRef> select(List<LineRef> eligible, Optional<LineRef> lastPlayed,
            IntUnaryOperator roll) {
        if (eligible.isEmpty()) {
            return Optional.empty();
        }

        List<LineRef> candidates = eligible;
        if (lastPlayed.isPresent()) {
            List<LineRef> withoutRepeat = new ArrayList<>(eligible);
            withoutRepeat.remove(lastPlayed.get());
            if (!withoutRepeat.isEmpty()) {
                candidates = withoutRepeat;
            }
        }

        int index = roll.applyAsInt(candidates.size());
        return Optional.of(candidates.get(index));
    }
}
