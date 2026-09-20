package villager_voices.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import villager_voices.catalogue.Catalogue;
import villager_voices.catalogue.Line;
import villager_voices.fabric.catalogue.CatalogueReloadListener;

import java.util.List;

/**
 * VV-3: the 16-event line catalogue loads through Minecraft's own resource/datapack system, and a
 * datapack override -- this test module's own {@code fabric/src/gametest/resources}, loaded as the
 * {@code villager_voices_gametest} mod's data on top of {@code villager_voices}'s own -- replaces
 * one event's lines entirely (`REACTION-REQ-011`).
 */
public final class CatalogueGameTest {

    private static final List<String> EVENTS = List.of(
        "trade_completed", "offer_opened", "hurt", "killed", "zombified", "cured", "level_up",
        "restock", "sleep", "wake", "raid_bell", "golem_summoned", "panic", "player_staring",
        "breeding", "baby_grows");

    @GameTest
    public void allSixteenEventsLoadFourLinesEach(GameTestHelper helper) {
        Catalogue catalogue = CatalogueReloadListener.current();
        helper.assertTrue(catalogue.events().size() == 16,
            "expected 16 events loaded, found " + catalogue.events().size() + ": " + catalogue.events());
        for (String event : EVENTS) {
            List<Line> lines = catalogue.linesFor(event);
            helper.assertTrue(lines.size() == 4, event + ": expected 4 lines, found " + lines.size());
        }
        helper.succeed();
    }

    @GameTest
    public void theGametestDatapackOverridesRestocksLinesEntirely(GameTestHelper helper) {
        List<Line> restock = CatalogueReloadListener.current().linesFor("restock");
        helper.assertTrue(restock.size() == 4, "expected 4 overridden lines, found " + restock.size());
        for (Line line : restock) {
            helper.assertTrue(line.text().startsWith("[gametest override]"),
                "expected the gametest resources pack's override, found: " + line.text());
        }
        helper.succeed();
    }
}
