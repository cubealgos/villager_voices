package villager_voices.fabric.sound;

import org.junit.jupiter.api.Test;
import villager_voices.catalogue.CatalogueCodec;
import villager_voices.catalogue.Line;
import villager_voices.catalogue.MiniJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VV-8's generated cross-check ({@code AUDIO-REQ-001}): {@code assets/villager_voices/sounds.json}'s
 * 64 keys are exactly the 64 sound ids the 16 shipped catalogue files name (VV-3's own
 * {@link CatalogueCodec}, off the disk, the same technique
 * {@code villager_voices.fabric.catalogue.DefaultCatalogueResourcesTest} uses) — proof that
 * {@code sounds.json} was generated from the catalogue, never hand-typed, and never drifts from it.
 */
class SoundsJsonResourcesTest {

    private static final Path CATALOGUE_DIR = Path.of("src/main/resources/data/villager_voices/reaction");
    private static final Path SOUNDS_JSON = Path.of("src/main/resources/assets/villager_voices/sounds.json");
    private static final Path SOUNDS_DIR = Path.of("src/main/resources/assets/villager_voices/sounds");
    private static final List<String> EVENTS = List.of(
        "trade_completed", "offer_opened", "hurt", "killed", "zombified", "cured", "level_up",
        "restock", "sleep", "wake", "raid_bell", "golem_summoned", "panic", "player_staring",
        "breeding", "baby_grows");

    @SuppressWarnings("unchecked")
    @Test
    void soundsJsonHasExactlyTheCatalogueSSixtyFourSoundIdsAsKeys() throws IOException {
        Set<String> catalogueSoundIds = new LinkedHashSet<>();
        for (String event : EVENTS) {
            String json = Files.readString(CATALOGUE_DIR.resolve(event + ".json"));
            for (Line line : CatalogueCodec.parseEventFile(event, json, soundId -> true)) {
                catalogueSoundIds.add(line.soundId());
            }
        }
        assertEquals(64, catalogueSoundIds.size());

        Map<String, Object> sounds = (Map<String, Object>) MiniJson.parse(Files.readString(SOUNDS_JSON));
        assertEquals(64, sounds.size(), "expected 64 sounds.json entries, found " + sounds.size());

        Set<String> soundsJsonIds = new LinkedHashSet<>();
        for (String pathKey : sounds.keySet()) {
            soundsJsonIds.add("villager_voices:" + pathKey);
        }
        assertEquals(catalogueSoundIds, soundsJsonIds,
            "sounds.json's keys must equal the catalogue's own 64 sound ids exactly");
    }

    @SuppressWarnings("unchecked")
    @Test
    void everySoundsJsonEntryCarriesItsMatchingSubtitleKeyAndTheSharedPlaceholderFile() throws IOException {
        Map<String, Object> sounds = (Map<String, Object>) MiniJson.parse(Files.readString(SOUNDS_JSON));
        for (Map.Entry<String, Object> entry : sounds.entrySet()) {
            String pathKey = entry.getKey(); // e.g. "reaction.trade_completed.1"
            Map<String, Object> definition = (Map<String, Object>) entry.getValue();
            assertEquals("subtitles.villager_voices." + pathKey, definition.get("subtitle"),
                pathKey + ": subtitle key mismatch");

            List<Object> soundFiles = (List<Object>) definition.get("sounds");
            assertEquals(List.of("reaction/placeholder"), soundFiles,
                pathKey + ": every alpha entry shares the one placeholder file (AUDIO-DEC-001, this ticket's Findings)");
        }
    }

    @Test
    void thePlaceholderOggFileIsGenuinelyPresentAndNonEmpty() throws IOException {
        Path placeholder = SOUNDS_DIR.resolve("reaction").resolve("placeholder.ogg");
        assertTrue(Files.exists(placeholder), "missing " + placeholder + " (AUDIO-FAIL-001)");
        assertTrue(Files.size(placeholder) > 0, placeholder + " must not be an empty file (AUDIO-REQ-002)");
    }
}
