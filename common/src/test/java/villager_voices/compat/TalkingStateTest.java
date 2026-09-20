package villager_voices.compat;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * docs/spec/operations/testing.md's unit layer, {@code COMPAT-REQ-002}: a villager is talking for
 * exactly the duration a line's sound plays, nothing before or after — pure, no fake clock needed
 * since {@code now} is just a caller-supplied {@code long} (VV-2's own {@code ReactionRules} test
 * pattern).
 */
final class TalkingStateTest {

    private final UUID villagerId = UUID.randomUUID();

    @Test
    void aVillagerNeverMarkedIsNeverTalking() {
        TalkingState state = new TalkingState();

        assertFalse(state.isTalking(villagerId, 0));
        assertFalse(state.isTalking(villagerId, 1000));
    }

    @Test
    void isTalkingThroughEveryTickStrictlyBeforeUntilTick() {
        TalkingState state = new TalkingState();

        state.startTalking(villagerId, 40);

        assertTrue(state.isTalking(villagerId, 0));
        assertTrue(state.isTalking(villagerId, 39));
    }

    @Test
    void isNotTalkingFromUntilTickOnward() {
        TalkingState state = new TalkingState();

        state.startTalking(villagerId, 40);

        assertFalse(state.isTalking(villagerId, 40));
        assertFalse(state.isTalking(villagerId, 41));
    }

    @Test
    void aRepeatStartTalkingReplacesRatherThanExtendsTheMark() {
        TalkingState state = new TalkingState();

        state.startTalking(villagerId, 40);
        state.startTalking(villagerId, 10);

        assertFalse(state.isTalking(villagerId, 10), "the second call's own untilTick replaces the first's");
        assertTrue(state.isTalking(villagerId, 9));
    }

    @Test
    void eachVillagerIdIsTrackedIndependently() {
        TalkingState state = new TalkingState();
        UUID otherVillagerId = UUID.randomUUID();

        state.startTalking(villagerId, 40);

        assertTrue(state.isTalking(villagerId, 0));
        assertFalse(state.isTalking(otherVillagerId, 0));
    }
}
