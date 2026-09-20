package villager_voices.fabric.catalogue;

import org.junit.jupiter.api.Test;
import villager_voices.catalogue.CatalogueCodec;
import villager_voices.catalogue.Line;
import villager_voices.catalogue.MiniJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 16 shipped {@code data/villager_voices/reaction/<event>.json} files and the lang file agree,
 * off the disk, without Minecraft's own resource manager: `LINES-REQ-001` (64 lines, 4 per event,
 * across all 16 events) and `LINES-REQ-003` (every subtitle matches its lang key's text and its
 * sound id 1:1). This is `common`'s codec exercised against the real shipped resources, not the
 * loader shim itself -- {@code fabric/src/gametest}'s {@code CatalogueGameTest} covers loading them
 * through Minecraft's own resource system.
 */
class DefaultCatalogueResourcesTest {

    private static final Path CATALOGUE_DIR = Path.of("src/main/resources/data/villager_voices/reaction");
    private static final Path LANG_FILE = Path.of("src/main/resources/assets/villager_voices/lang/en_us.json");
    private static final List<String> EVENTS = List.of(
        "trade_completed", "offer_opened", "hurt", "killed", "zombified", "cured", "level_up",
        "restock", "sleep", "wake", "raid_bell", "golem_summoned", "panic", "player_staring",
        "breeding", "baby_grows");

    @Test
    void exactlySixteenEventFilesShip() throws IOException {
        try (Stream<Path> files = Files.list(CATALOGUE_DIR)) {
            List<String> names = files.map(p -> p.getFileName().toString()).sorted().toList();
            assertEquals(16, names.size(), "expected 16 event files, found " + names);
        }
    }

    @Test
    void everyEventShipsExactlyFourLinesSixtyFourTotal() throws IOException {
        int total = 0;
        for (String event : EVENTS) {
            List<Line> lines = parse(event);
            assertEquals(4, lines.size(), event + ": expected 4 lines, found " + lines.size());
            total += lines.size();
        }
        assertEquals(64, total);
    }

    @Test
    void everySoundIdIsUniqueAcrossTheWholeCatalogue() throws IOException {
        Set<String> seen = new HashSet<>();
        for (String event : EVENTS) {
            for (Line line : parse(event)) {
                assertTrue(seen.add(line.soundId()), "duplicate sound id " + line.soundId());
            }
        }
        assertEquals(64, seen.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void everyLineSubtitleMatchesItsLangKeyTextOneToOne() throws IOException {
        Map<String, Object> lang = (Map<String, Object>) MiniJson.parse(Files.readString(LANG_FILE));
        long subtitleKeyCount = lang.keySet().stream()
            .filter(key -> key.startsWith("subtitles.villager_voices.reaction."))
            .count();
        // The lang file also carries non-subtitle keys now (VV-13's `command.villager_voices.debug.*`
        // feedback strings) -- LINES-REQ-003 only ever promised the 64 subtitle keys line up 1:1 with
        // the catalogue, not that the whole file holds exactly 64 keys.
        assertEquals(64, subtitleKeyCount, "expected 64 subtitle lang keys, found " + subtitleKeyCount);

        List<String> mismatches = new ArrayList<>();
        for (String event : EVENTS) {
            List<Line> lines = parse(event);
            for (int i = 0; i < lines.size(); i++) {
                Line line = lines.get(i);
                String key = "subtitles.villager_voices.reaction." + event + "." + (i + 1);
                Object langText = lang.get(key);
                if (langText == null) {
                    mismatches.add(key + " is missing from the lang file");
                } else if (!langText.equals(line.text())) {
                    mismatches.add(key + ": lang says \"" + langText + "\", catalogue says \"" + line.text() + "\"");
                }
            }
        }
        assertTrue(mismatches.isEmpty(), String.join("; ", mismatches));
    }

    private static List<Line> parse(String eventId) throws IOException {
        Path file = CATALOGUE_DIR.resolve(eventId + ".json");
        String json = Files.readString(file);
        return CatalogueCodec.parseEventFile(eventId, json, soundId -> true);
    }
}
