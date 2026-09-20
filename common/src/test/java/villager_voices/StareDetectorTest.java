package villager_voices;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StareDetectorTest {

    private static final UUID PLAYER = UUID.randomUUID();
    private static final UUID OTHER_PLAYER = UUID.randomUUID();
    private static final UUID VILLAGER = UUID.randomUUID();
    private static final UUID OTHER_VILLAGER = UUID.randomUUID();
    private static final double ON_TARGET = 1.0;
    private static final double OFF_TARGET = 0.0;
    private static final double IN_RANGE = 3.0;
    private static final double OUT_OF_RANGE = StareDetector.RANGE_BLOCKS + 1.0;

    @Test
    void doesNotFireBeforeRequiredTicksElapse() {
        StareDetector detector = new StareDetector();
        for (int tick = 0; tick < StareDetector.REQUIRED_TICKS - 1; tick++) {
            assertFalse(detector.sample(PLAYER, VILLAGER, ON_TARGET, IN_RANGE), "tick " + tick);
        }
    }

    @Test
    void firesExactlyOnTheRequiredTick() {
        StareDetector detector = new StareDetector();
        boolean fired = false;
        for (int tick = 0; tick < StareDetector.REQUIRED_TICKS; tick++) {
            fired = detector.sample(PLAYER, VILLAGER, ON_TARGET, IN_RANGE);
        }
        assertTrue(fired);
    }

    @Test
    void doesNotFireAgainWhileTheStareContinuesUnbroken() {
        StareDetector detector = new StareDetector();
        for (int tick = 0; tick < StareDetector.REQUIRED_TICKS; tick++) {
            detector.sample(PLAYER, VILLAGER, ON_TARGET, IN_RANGE);
        }
        // Ten more ticks of the same unbroken stare: never fires again.
        for (int tick = 0; tick < 10; tick++) {
            assertFalse(detector.sample(PLAYER, VILLAGER, ON_TARGET, IN_RANGE));
        }
    }

    @Test
    void dotDroppingBelowThresholdBreaksTheStare() {
        StareDetector detector = new StareDetector();
        for (int tick = 0; tick < StareDetector.REQUIRED_TICKS - 1; tick++) {
            detector.sample(PLAYER, VILLAGER, ON_TARGET, IN_RANGE);
        }
        // One tick looking away resets the run -- the dot falls just under the threshold.
        assertFalse(detector.sample(PLAYER, VILLAGER, StareDetector.DOT_THRESHOLD - 0.01, IN_RANGE));
        assertFalse(detector.sample(PLAYER, VILLAGER, ON_TARGET, IN_RANGE));
    }

    @Test
    void leavingRangeBreaksTheStare() {
        StareDetector detector = new StareDetector();
        for (int tick = 0; tick < StareDetector.REQUIRED_TICKS - 1; tick++) {
            detector.sample(PLAYER, VILLAGER, ON_TARGET, IN_RANGE);
        }
        assertFalse(detector.sample(PLAYER, VILLAGER, ON_TARGET, OUT_OF_RANGE));
        assertFalse(detector.sample(PLAYER, VILLAGER, ON_TARGET, IN_RANGE));
    }

    @Test
    void aBrokenStareFiresAgainOnceRequiredTicksElapseAnew() {
        StareDetector detector = new StareDetector();
        for (int tick = 0; tick < StareDetector.REQUIRED_TICKS; tick++) {
            detector.sample(PLAYER, VILLAGER, ON_TARGET, IN_RANGE);
        }
        detector.sample(PLAYER, VILLAGER, OFF_TARGET, IN_RANGE); // breaks it

        boolean firedAgain = false;
        for (int tick = 0; tick < StareDetector.REQUIRED_TICKS; tick++) {
            firedAgain = detector.sample(PLAYER, VILLAGER, ON_TARGET, IN_RANGE);
        }
        assertTrue(firedAgain);
    }

    @Test
    void exactlyAtDotThresholdCounts() {
        StareDetector detector = new StareDetector();
        boolean fired = false;
        for (int tick = 0; tick < StareDetector.REQUIRED_TICKS; tick++) {
            fired = detector.sample(PLAYER, VILLAGER, StareDetector.DOT_THRESHOLD, IN_RANGE);
        }
        assertTrue(fired);
    }

    @Test
    void exactlyAtRangeBoundaryCounts() {
        StareDetector detector = new StareDetector();
        boolean fired = false;
        for (int tick = 0; tick < StareDetector.REQUIRED_TICKS; tick++) {
            fired = detector.sample(PLAYER, VILLAGER, ON_TARGET, StareDetector.RANGE_BLOCKS);
        }
        assertTrue(fired);
    }

    @Test
    void stareIsIndependentPerPlayerVillagerPair() {
        StareDetector detector = new StareDetector();
        for (int tick = 0; tick < StareDetector.REQUIRED_TICKS - 1; tick++) {
            detector.sample(PLAYER, VILLAGER, ON_TARGET, IN_RANGE);
        }
        // A different player starting fresh on the same villager does not inherit the first
        // player's near-complete run.
        assertFalse(detector.sample(OTHER_PLAYER, VILLAGER, ON_TARGET, IN_RANGE));
        // The same player staring at a different villager does not inherit it either.
        assertFalse(detector.sample(PLAYER, OTHER_VILLAGER, ON_TARGET, IN_RANGE));
        // The original pair's run is untouched by the two lookups above.
        assertTrue(detector.sample(PLAYER, VILLAGER, ON_TARGET, IN_RANGE));
    }
}
