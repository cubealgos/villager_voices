package villager_voices.catalogue;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogueCodecTest {

    private static final String TRADE_COMPLETED_JSON = """
        {
          "lines": [
            { "subtitle": "Mrrgh -- traded! Nice.", "sound": "villager_voices:reaction.trade_completed.1" },
            { "subtitle": "Hmnh, good trade, that.", "sound": "villager_voices:reaction.trade_completed.2" },
            { "subtitle": "Ha! Emeralds for me.", "sound": "villager_voices:reaction.trade_completed.3" },
            { "subtitle": "Mmh-hmm, pleasure doing business.", "sound": "villager_voices:reaction.trade_completed.4" }
          ]
        }
        """;

    @Test
    void parsesFourLinesInFileOrder() {
        List<Line> lines = CatalogueCodec.parseEventFile("trade_completed", TRADE_COMPLETED_JSON, id -> true);
        assertEquals(4, lines.size());
        assertEquals(new Line("Mrrgh -- traded! Nice.", "villager_voices:reaction.trade_completed.1"), lines.get(0));
        assertEquals(new Line("Mmh-hmm, pleasure doing business.", "villager_voices:reaction.trade_completed.4"), lines.get(3));
    }

    @Test
    void rejectsAnEmptyLinesArray() {
        CatalogueLoadException ex = assertThrows(CatalogueLoadException.class,
            () -> CatalogueCodec.parseEventFile("hurt", "{\"lines\": []}", id -> true));
        assertTrue(ex.getMessage().contains("at least one line"), ex.getMessage());
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(CatalogueLoadException.class,
            () -> CatalogueCodec.parseEventFile("hurt", "not json", id -> true));
    }

    @Test
    void rejectsAMissingLinesArray() {
        assertThrows(CatalogueLoadException.class,
            () -> CatalogueCodec.parseEventFile("hurt", "{}", id -> true));
    }

    @Test
    void rejectsANonObjectTopLevel() {
        assertThrows(CatalogueLoadException.class,
            () -> CatalogueCodec.parseEventFile("hurt", "[]", id -> true));
    }

    @Test
    void rejectsABlankSubtitle() {
        assertThrows(CatalogueLoadException.class, () -> CatalogueCodec.parseEventFile("hurt",
            "{\"lines\": [{\"subtitle\": \"  \", \"sound\": \"villager_voices:reaction.hurt.1\"}]}", id -> true));
    }

    @Test
    void rejectsASoundIdWithTheWrongShape() {
        CatalogueLoadException ex = assertThrows(CatalogueLoadException.class, () -> CatalogueCodec.parseEventFile("hurt",
            "{\"lines\": [{\"subtitle\": \"Ow!\", \"sound\": \"villager_voices:hurt.1\"}]}", id -> true));
        assertTrue(ex.getMessage().contains("villager_voices:hurt.1"), ex.getMessage());
    }

    @Test
    void rejectsASoundIdNamingADifferentEvent() {
        CatalogueLoadException ex = assertThrows(CatalogueLoadException.class, () -> CatalogueCodec.parseEventFile("hurt",
            "{\"lines\": [{\"subtitle\": \"Ow!\", \"sound\": \"villager_voices:reaction.killed.1\"}]}", id -> true));
        assertTrue(ex.getMessage().contains("killed"), ex.getMessage());
    }

    @Test
    void rejectsASoundIdUnknownToTheInjectedRegistryCheck_namingTheId() {
        // REACTION-REQ-012 / REACTION-FAIL-003: rejected with a clear error naming the missing id,
        // not silently dropped or a crash. The real SoundEvent registry check is VV-8's territory;
        // this ticket can only exercise the rejection path with a fake one.
        CatalogueLoadException ex = assertThrows(CatalogueLoadException.class, () -> CatalogueCodec.parseEventFile("hurt",
            "{\"lines\": [{\"subtitle\": \"Ow!\", \"sound\": \"villager_voices:reaction.hurt.1\"}]}",
            id -> false));
        assertTrue(ex.getMessage().contains("villager_voices:reaction.hurt.1"), ex.getMessage());
        assertTrue(ex.getMessage().contains("not a registered SoundEvent"), ex.getMessage());
    }

    @Test
    void soundExistsIsConsultedPerId() {
        String json = "{\"lines\": ["
            + "{\"subtitle\": \"Ow!\", \"sound\": \"villager_voices:reaction.hurt.1\"},"
            + "{\"subtitle\": \"Ouch!\", \"sound\": \"villager_voices:reaction.hurt.2\"}]}";
        // Only .1 is "registered"; .2 must be the one named in the rejection.
        CatalogueLoadException ex = assertThrows(CatalogueLoadException.class, () -> CatalogueCodec.parseEventFile(
            "hurt", json, "villager_voices:reaction.hurt.1"::equals));
        assertTrue(ex.getMessage().contains("villager_voices:reaction.hurt.2"), ex.getMessage());
    }

    // VV-18: the optional "grunt" field (AUDIO-REQ-007, AUDIO-DEC-005).

    @Test
    void parsesALineWithAGrunt() {
        List<Line> lines = CatalogueCodec.parseEventFile("hurt",
            "{\"lines\": [{\"subtitle\": \"Watch it!\", \"sound\": \"villager_voices:reaction.hurt.1\", "
                + "\"grunt\": \"minecraft:entity.villager.hurt\"}]}",
            id -> true);
        assertEquals(1, lines.size());
        assertEquals("minecraft:entity.villager.hurt", lines.get(0).grunt());
    }

    @Test
    void parsesALineWithoutAGruntAsNull() {
        List<Line> lines = CatalogueCodec.parseEventFile("hurt",
            "{\"lines\": [{\"subtitle\": \"Watch it!\", \"sound\": \"villager_voices:reaction.hurt.1\"}]}",
            id -> true);
        assertEquals(1, lines.size());
        assertEquals(null, lines.get(0).grunt());
    }

    @Test
    void rejectsABlankGrunt() {
        CatalogueLoadException ex = assertThrows(CatalogueLoadException.class, () -> CatalogueCodec.parseEventFile("hurt",
            "{\"lines\": [{\"subtitle\": \"Watch it!\", \"sound\": \"villager_voices:reaction.hurt.1\", \"grunt\": \"  \"}]}",
            id -> true));
        assertTrue(ex.getMessage().contains("grunt"), ex.getMessage());
    }

    @Test
    void rejectsAMalformedGruntId() {
        CatalogueLoadException ex = assertThrows(CatalogueLoadException.class, () -> CatalogueCodec.parseEventFile("hurt",
            "{\"lines\": [{\"subtitle\": \"Watch it!\", \"sound\": \"villager_voices:reaction.hurt.1\", "
                + "\"grunt\": \"not a namespaced id\"}]}",
            id -> true));
        assertTrue(ex.getMessage().contains("not a namespaced id") || ex.getMessage().contains("grunt id"), ex.getMessage());
    }

    // VV-11 round six: the optional "spoken" field (AUDIO-DEC-006 amendment) -- the voice
    // pipeline's generator-only TTS input override for an expressively spelled subtitle.

    @Test
    void parsesALineWithASpokenOverride() {
        List<Line> lines = CatalogueCodec.parseEventFile("sleep",
            "{\"lines\": [{\"subtitle\": \"Zzz.\", \"sound\": \"villager_voices:reaction.sleep.3\", "
                + "\"spoken\": \"Shh.\"}]}",
            id -> true);
        assertEquals(1, lines.size());
        assertEquals("Shh.", lines.get(0).spoken());
        assertEquals("Zzz.", lines.get(0).text());
    }

    @Test
    void parsesALineWithoutASpokenOverrideAsNull() {
        List<Line> lines = CatalogueCodec.parseEventFile("hurt",
            "{\"lines\": [{\"subtitle\": \"Watch it!\", \"sound\": \"villager_voices:reaction.hurt.1\"}]}",
            id -> true);
        assertEquals(1, lines.size());
        assertEquals(null, lines.get(0).spoken());
    }

    @Test
    void rejectsABlankSpokenOverride() {
        CatalogueLoadException ex = assertThrows(CatalogueLoadException.class, () -> CatalogueCodec.parseEventFile("hurt",
            "{\"lines\": [{\"subtitle\": \"Watch it!\", \"sound\": \"villager_voices:reaction.hurt.1\", \"spoken\": \"  \"}]}",
            id -> true));
        assertTrue(ex.getMessage().contains("spoken"), ex.getMessage());
    }

    @Test
    void aGruntAndASpokenOverrideCoexist() {
        List<Line> lines = CatalogueCodec.parseEventFile("killed",
            "{\"lines\": [{\"subtitle\": \"Wha-- no!\", \"sound\": \"villager_voices:reaction.killed.3\", "
                + "\"grunt\": \"minecraft:entity.villager.hurt\", \"spoken\": \"What, no!\"}]}",
            id -> true);
        assertEquals(1, lines.size());
        assertEquals("minecraft:entity.villager.hurt", lines.get(0).grunt());
        assertEquals("What, no!", lines.get(0).spoken());
    }

    // VV-11 round ten: the optional "mood" field (LINES-DEC-002) -- the voice pipeline's
    // per-event emotion class, read only by tools/voices/render.py's generator.

    @Test
    void parsesALineWithAMood() {
        List<Line> lines = CatalogueCodec.parseEventFile("panic",
            "{\"lines\": [{\"subtitle\": \"Run!\", \"sound\": \"villager_voices:reaction.panic.1\", "
                + "\"mood\": \"alarmed\"}]}",
            id -> true);
        assertEquals(1, lines.size());
        assertEquals("alarmed", lines.get(0).mood());
    }

    @Test
    void parsesALineWithoutAMoodAsNull() {
        List<Line> lines = CatalogueCodec.parseEventFile("hurt",
            "{\"lines\": [{\"subtitle\": \"Watch it!\", \"sound\": \"villager_voices:reaction.hurt.1\"}]}",
            id -> true);
        assertEquals(1, lines.size());
        assertEquals(null, lines.get(0).mood());
    }

    @Test
    void rejectsABlankMood() {
        CatalogueLoadException ex = assertThrows(CatalogueLoadException.class, () -> CatalogueCodec.parseEventFile("hurt",
            "{\"lines\": [{\"subtitle\": \"Watch it!\", \"sound\": \"villager_voices:reaction.hurt.1\", \"mood\": \"  \"}]}",
            id -> true));
        assertTrue(ex.getMessage().contains("mood"), ex.getMessage());
    }

    @Test
    void rejectsAnUnknownMood() {
        CatalogueLoadException ex = assertThrows(CatalogueLoadException.class, () -> CatalogueCodec.parseEventFile("hurt",
            "{\"lines\": [{\"subtitle\": \"Watch it!\", \"sound\": \"villager_voices:reaction.hurt.1\", "
                + "\"mood\": \"furious\"}]}",
            id -> true));
        assertTrue(ex.getMessage().contains("furious"), ex.getMessage());
    }

    @Test
    void rejectsDisagreeingMoodsInTheSameFile() {
        String json = "{\"lines\": ["
            + "{\"subtitle\": \"Watch it!\", \"sound\": \"villager_voices:reaction.hurt.1\", \"mood\": \"hurt\"},"
            + "{\"subtitle\": \"Ow!\", \"sound\": \"villager_voices:reaction.hurt.2\", \"mood\": \"annoyed\"}]}";
        CatalogueLoadException ex = assertThrows(CatalogueLoadException.class,
            () -> CatalogueCodec.parseEventFile("hurt", json, id -> true));
        assertTrue(ex.getMessage().contains("disagrees"), ex.getMessage());
    }

    @Test
    void allowsSomeLinesToOmitMoodWhileOthersSetIt() {
        String json = "{\"lines\": ["
            + "{\"subtitle\": \"Watch it!\", \"sound\": \"villager_voices:reaction.hurt.1\", \"mood\": \"hurt\"},"
            + "{\"subtitle\": \"Ow!\", \"sound\": \"villager_voices:reaction.hurt.2\"}]}";
        List<Line> lines = CatalogueCodec.parseEventFile("hurt", json, id -> true);
        assertEquals("hurt", lines.get(0).mood());
        assertEquals(null, lines.get(1).mood());
    }

    @Test
    void aGruntASpokenOverrideAndAMoodCoexist() {
        List<Line> lines = CatalogueCodec.parseEventFile("killed",
            "{\"lines\": [{\"subtitle\": \"Wha-- no!\", \"sound\": \"villager_voices:reaction.killed.3\", "
                + "\"grunt\": \"minecraft:entity.villager.hurt\", \"spoken\": \"What, no!\", \"mood\": \"alarmed\"}]}",
            id -> true);
        assertEquals(1, lines.size());
        assertEquals("minecraft:entity.villager.hurt", lines.get(0).grunt());
        assertEquals("What, no!", lines.get(0).spoken());
        assertEquals("alarmed", lines.get(0).mood());
    }
}
