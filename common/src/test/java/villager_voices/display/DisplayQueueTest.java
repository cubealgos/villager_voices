package villager_voices.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * docs/spec/operations/testing.md: "the per-player {@code DisplayQueue}'s hold-time/advance
 * logic", checked with a fake clock — plain {@code long} tick numbers passed to {@link
 * DisplayQueue#tick}, no wall-clock or Minecraft server dependency.
 */
final class DisplayQueueTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    private static DisplayLine line(String text) {
        return new DisplayLine("Farmer", text, "villager_voices:line_" + text);
    }

    @Test
    void firstLineForAPlayerShowsOnTheNextTick() {
        DisplayQueue queue = new DisplayQueue(30, 8);
        queue.enqueue(ALICE, line("hi"));

        Map<UUID, DisplayLine> pushed = queue.tick(0);

        assertEquals(line("hi"), pushed.get(ALICE));
        assertEquals(line("hi"), queue.current(ALICE));
    }

    @Test
    void aSecondLineWaitsForTheMinimumHoldTimeBeforeAdvancing() {
        DisplayQueue queue = new DisplayQueue(30, 8);
        queue.enqueue(ALICE, line("first"));
        queue.tick(0);
        queue.enqueue(ALICE, line("second"));

        assertTrue(queue.tick(10).isEmpty(), "still within the hold time");
        assertTrue(queue.tick(29).isEmpty(), "one tick short of the hold time");

        Map<UUID, DisplayLine> pushed = queue.tick(30);
        assertEquals(line("second"), pushed.get(ALICE));
        assertEquals(line("second"), queue.current(ALICE));
    }

    @Test
    void linesAdvanceInEnqueueOrder() {
        DisplayQueue queue = new DisplayQueue(10, 8);
        queue.enqueue(ALICE, line("one"));
        queue.enqueue(ALICE, line("two"));
        queue.enqueue(ALICE, line("three"));

        assertEquals(line("one"), queue.tick(0).get(ALICE));
        assertEquals(line("two"), queue.tick(10).get(ALICE));
        assertEquals(line("three"), queue.tick(20).get(ALICE));
    }

    @Test
    void aBurstBeyondTheCapDropsTheOldestStillPendingLineNotTheCurrentOne() {
        DisplayQueue queue = new DisplayQueue(1000, 2);
        queue.enqueue(ALICE, line("shown"));
        queue.tick(0);
        queue.enqueue(ALICE, line("a"));
        queue.enqueue(ALICE, line("b"));
        queue.enqueue(ALICE, line("c"));

        assertEquals(2, queue.pendingCount(ALICE), "cap of 2 holds, dropping the oldest pending line");
        assertEquals(line("shown"), queue.current(ALICE), "the currently shown line is never dropped by the cap");

        Map<UUID, DisplayLine> next = queue.tick(1000);
        assertEquals(line("b"), next.get(ALICE), "\"a\" was dropped as the oldest pending line");
    }

    @Test
    void eachPlayerHasItsOwnIndependentQueue() {
        DisplayQueue queue = new DisplayQueue(30, 8);
        queue.enqueue(ALICE, line("for-alice"));
        queue.enqueue(BOB, line("for-bob"));

        Map<UUID, DisplayLine> pushed = queue.tick(0);

        assertEquals(2, pushed.size());
        assertEquals(line("for-alice"), pushed.get(ALICE));
        assertEquals(line("for-bob"), pushed.get(BOB));
    }

    @Test
    void aSecondPlayersHoldTimeIsUnaffectedByAnotherPlayersAdvance() {
        DisplayQueue queue = new DisplayQueue(30, 8);
        queue.enqueue(ALICE, line("alice-first"));
        queue.tick(0);
        queue.enqueue(BOB, line("bob-first"));

        Map<UUID, DisplayLine> pushedAtTen = queue.tick(10);
        assertEquals(line("bob-first"), pushedAtTen.get(BOB), "Bob's first line shows immediately");
        assertTrue(pushedAtTen.containsKey(BOB));
        assertNull(pushedAtTen.get(ALICE), "Alice's hold time (30 ticks) has not elapsed yet");
    }

    @Test
    void aPlayerWithNothingQueuedIsAbsentFromTheResult() {
        DisplayQueue queue = new DisplayQueue(30, 8);

        assertTrue(queue.tick(0).isEmpty());
        assertNull(queue.current(ALICE));
        assertEquals(0, queue.pendingCount(ALICE));
    }

    @Test
    void constructorRejectsInvalidBounds() {
        assertThrows(IllegalArgumentException.class, () -> new DisplayQueue(-1, 8));
        assertThrows(IllegalArgumentException.class, () -> new DisplayQueue(30, 0));
    }
}
