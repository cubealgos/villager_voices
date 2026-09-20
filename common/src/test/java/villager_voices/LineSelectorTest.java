package villager_voices;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineSelectorTest {

    private static final VillagerReactionEvent EVENT = VillagerReactionEvent.TRADE_COMPLETED;
    private static final LineRef A = new LineRef(EVENT, "1");
    private static final LineRef B = new LineRef(EVENT, "2");
    private static final LineRef C = new LineRef(EVENT, "3");
    private static final LineRef D = new LineRef(EVENT, "4");

    @Test
    void emptyEligibleYieldsNoSelection() {
        Optional<LineRef> selected = LineSelector.select(List.of(), Optional.empty(), bound -> 0);
        assertTrue(selected.isEmpty());
    }

    @Test
    void noLastPlayedPicksAmongTheFullEligibleList() {
        Optional<LineRef> selected = LineSelector.select(List.of(A, B), Optional.empty(), bound -> {
            assertEquals(2, bound);
            return 1;
        });
        assertEquals(Optional.of(B), selected);
    }

    @Test
    void excludesTheLineThatPlayedLast() {
        // REACTION-REQ-005: eligible [A, B, C, D], last played B -> candidates [A, C, D].
        Optional<LineRef> selected = LineSelector.select(List.of(A, B, C, D), Optional.of(B), bound -> {
            assertEquals(3, bound);
            return 0;
        });
        assertEquals(Optional.of(A), selected);
    }

    @Test
    void excludesTheLineThatPlayedLastRegardlessOfItsPosition() {
        Optional<LineRef> selected = LineSelector.select(List.of(A, B, C, D), Optional.of(B), bound -> 2);
        assertEquals(Optional.of(D), selected); // candidates [A, C, D], index 2 -> D
    }

    @Test
    void singleLineCatalogueRepeatsWhenExcludingItLeavesNothing() {
        // Decision recorded in LineSelector's own Javadoc: a single-line event may repeat rather
        // than produce no line at all.
        Optional<LineRef> selected = LineSelector.select(List.of(A), Optional.of(A), bound -> {
            assertEquals(1, bound);
            return 0;
        });
        assertEquals(Optional.of(A), selected);
    }

    @Test
    void lastPlayedNotPresentInEligibleIsHarmless() {
        // e.g. a datapack reload dropped the old line -- nothing to exclude, so no exclusion happens.
        Optional<LineRef> selected = LineSelector.select(List.of(A, B), Optional.of(C), bound -> {
            assertEquals(2, bound);
            return 0;
        });
        assertEquals(Optional.of(A), selected);
    }
}
