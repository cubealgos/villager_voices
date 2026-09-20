package villager_voices.catalogue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogueTest {

    private static String oneLine(String eventId, int n, String subtitle) {
        return "{\"lines\": [{\"subtitle\": \"" + subtitle + "\", \"sound\": \"villager_voices:reaction." + eventId + "." + n + "\"}]}";
    }

    @Test
    void linesForAnUnloadedEventIsEmpty() {
        Catalogue catalogue = Catalogue.builder().build();
        assertEquals(List.of(), catalogue.linesFor("hurt"));
        assertEquals(0, catalogue.events().size());
    }

    @Test
    void collectsLinesPerEvent() {
        Catalogue catalogue = Catalogue.builder()
            .load("hurt", oneLine("hurt", 1, "Ow!"), id -> true)
            .load("killed", oneLine("killed", 1, "Argh!"), id -> true)
            .build();

        assertEquals(Set.of("hurt", "killed"), catalogue.events());
        assertEquals("Ow!", catalogue.linesFor("hurt").get(0).text());
        assertEquals("Argh!", catalogue.linesFor("killed").get(0).text());
    }

    @Test
    void aLaterLoadForTheSameEventReplacesTheEarlierFileEntirely() {
        // REACTION-REQ-011: a datapack replacing one catalogue file is used in place of the
        // shipped default, decoded the same way -- a full file-level replacement, not a merge.
        Catalogue catalogue = Catalogue.builder()
            .load("hurt", oneLine("hurt", 1, "shipped default"), id -> true)
            .load("hurt", oneLine("hurt", 1, "datapack override"), id -> true)
            .build();

        List<Line> lines = catalogue.linesFor("hurt");
        assertEquals(1, lines.size(), "the override replaces, it does not append to, the earlier file");
        assertEquals("datapack override", lines.get(0).text());
    }

    @Test
    void aLaterLoadCanShrinkTheLineCount() {
        Catalogue catalogue = Catalogue.builder()
            .load("hurt", "{\"lines\": ["
                + "{\"subtitle\": \"a\", \"sound\": \"villager_voices:reaction.hurt.1\"},"
                + "{\"subtitle\": \"b\", \"sound\": \"villager_voices:reaction.hurt.2\"}]}", id -> true)
            .load("hurt", oneLine("hurt", 1, "override only"), id -> true)
            .build();

        List<Line> lines = catalogue.linesFor("hurt");
        assertEquals(1, lines.size());
        assertTrue(lines.stream().noneMatch(l -> l.text().equals("b")), "nothing from the earlier file survives an override");
    }
}
