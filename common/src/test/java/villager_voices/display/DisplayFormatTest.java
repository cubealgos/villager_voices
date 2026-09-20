package villager_voices.display;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** {@code DISPLAY-REQ-004}: {@code "<profession>: <line>"}, falling back to {@code "Villager: <line>"}. */
final class DisplayFormatTest {

    @Test
    void formatsWithASpeakerLabel() {
        assertEquals("Farmer: Mrrgh -- traded! Nice.", DisplayFormat.format("Farmer", "Mrrgh -- traded! Nice."));
    }

    @Test
    void fallsBackToVillagerForANullLabel() {
        assertEquals("Villager: ...", DisplayFormat.format(null, "..."));
    }

    @Test
    void fallsBackToVillagerForABlankLabel() {
        assertEquals("Villager: ...", DisplayFormat.format("   ", "..."));
    }
}
