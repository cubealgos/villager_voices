package villager_voices.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** VV-1: the mod loads. Everything else follows (docs/spec/decisions/DEC-005-alpha-scope.md). */
public final class SmokeGameTest {
    @GameTest
    public void theModLoads(GameTestHelper helper) {
        helper.assertTrue(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("villager_voices"), "the mod is loaded");
        helper.succeed();
    }
}
