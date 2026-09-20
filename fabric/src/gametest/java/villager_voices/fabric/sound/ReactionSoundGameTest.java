package villager_voices.fabric.sound;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import villager_voices.VillagerReactionEvent;
import villager_voices.VillagerReactionSignal;
import villager_voices.display.DisplayLine;
import villager_voices.fabric.VillagerVoicesFabric;

import java.util.Set;

/**
 * VV-8: a forced signal, published through VV-2's own {@code VillagerEventBus} API (no mixin or
 * native event hook exists yet — VV-4/5/6's own territory), reaches the fabric {@code LineSink}
 * adapter wired in {@link VillagerVoicesFabric#onServerStarted}: it enqueues an action-bar line for
 * a player within hearing range ({@code DISPLAY-REQ-001}), and the line's sound id resolves to a
 * real, registered {@code SoundEvent} in {@link SoundRegistration} ({@code AUDIO-REQ-001}).
 */
public final class ReactionSoundGameTest {

    @GameTest
    public void aForcedSignalEnqueuesALineForAPlayerInRangeAndResolvesItsSoundInTheRegistry(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 1, 1));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(villager.getX(), villager.getY(), villager.getZ());

        VillagerReactionSignal signal = new VillagerReactionSignal(
            villager.getUUID(), VillagerReactionEvent.TRADE_COMPLETED, false, false, Set.of(player.getUUID()));

        VillagerVoicesFabric.eventBus().publish(signal);

        // DisplayQueue only advances a line from "pending" to "current" on its own #tick, driven
        // here by the real, globally-registered ActionBarDisplay against the game test's own real
        // server tick loop (docs/spec/domains/display.md: "the first line pushes immediately" --
        // i.e. on the very next tick after enqueue, not the same tick as the publish() call above).
        helper.runAtTickTime(1, () -> {
            DisplayLine shown = VillagerVoicesFabric.displayQueue().current(player.getUUID());
            helper.assertTrue(shown != null, "expected a line enqueued and advanced for the player in range");

            helper.assertTrue(shown.soundId().startsWith("villager_voices:reaction.trade_completed."),
                "expected a trade_completed sound id, found " + shown.soundId());
            helper.assertTrue(SoundRegistration.exists(shown.soundId()),
                "expected " + shown.soundId() + " to resolve to a registered SoundEvent (AUDIO-REQ-001)");
            helper.assertTrue(!shown.text().isBlank(), "expected non-blank subtitle text on the shown line");

            helper.succeed();
        });
    }
}
