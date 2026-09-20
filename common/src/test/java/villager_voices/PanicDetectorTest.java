package villager_voices;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanicDetectorTest {

    private static final UUID VILLAGER = UUID.randomUUID();
    private static final UUID OTHER_VILLAGER = UUID.randomUUID();

    @Test
    void firstPanicTickFires() {
        PanicDetector detector = new PanicDetector();
        assertTrue(detector.sample(VILLAGER, true));
    }

    @Test
    void staysCalmNeverFires() {
        PanicDetector detector = new PanicDetector();
        for (int tick = 0; tick < 5; tick++) {
            assertFalse(detector.sample(VILLAGER, false));
        }
    }

    @Test
    void steadyPanicFiresOnlyOnce() {
        // Synthetic sequence: false, true, true, true, true -- one panic episode held for 4 ticks.
        PanicDetector detector = new PanicDetector();
        assertFalse(detector.sample(VILLAGER, false));
        assertTrue(detector.sample(VILLAGER, true));
        assertFalse(detector.sample(VILLAGER, true));
        assertFalse(detector.sample(VILLAGER, true));
        assertFalse(detector.sample(VILLAGER, true));
    }

    @Test
    void panicEndingThenRestartingFiresAgain() {
        // Synthetic sequence: true, true, false, true -- two separate episodes.
        PanicDetector detector = new PanicDetector();
        assertTrue(detector.sample(VILLAGER, true));
        assertFalse(detector.sample(VILLAGER, true));
        assertFalse(detector.sample(VILLAGER, false));
        assertTrue(detector.sample(VILLAGER, true));
    }

    @Test
    void oneRapidFlickerPerTickFiresEveryRisingEdge() {
        // Synthetic sequence: true, false, true, false, true -- panic re-triggers each tick it's
        // observed true again after a false, matching REACTION-FAIL-002's "two hooks fire in the
        // same tick" backstop reasoning applied to a poll: each edge is its own episode.
        PanicDetector detector = new PanicDetector();
        assertTrue(detector.sample(VILLAGER, true));
        assertFalse(detector.sample(VILLAGER, false));
        assertTrue(detector.sample(VILLAGER, true));
        assertFalse(detector.sample(VILLAGER, false));
        assertTrue(detector.sample(VILLAGER, true));
    }

    @Test
    void panicIsIndependentPerVillager() {
        PanicDetector detector = new PanicDetector();
        assertTrue(detector.sample(VILLAGER, true));
        // A second villager panicking for the first time still fires, unaffected by the first's
        // already-panicking state.
        assertTrue(detector.sample(OTHER_VILLAGER, true));
        // The first villager, still panicking, does not re-fire.
        assertFalse(detector.sample(VILLAGER, true));
    }
}
