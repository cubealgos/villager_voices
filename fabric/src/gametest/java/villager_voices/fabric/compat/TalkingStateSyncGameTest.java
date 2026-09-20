package villager_voices.fabric.compat;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import villager_voices.VillagerEventBus;
import villager_voices.VillagerReactionEvent;
import villager_voices.VillagerReactionSignal;
import villager_voices.config.Config;
import villager_voices.display.DisplayQueue;
import villager_voices.fabric.catalogue.FabricLineCatalogue;
import villager_voices.fabric.sound.FabricLineSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * VV-12, docs/spec/domains/compat.md {@code COMPAT-REQ-002}: a played reaction line marks the
 * villager talking server-side through {@code config.talkingDurationTicks()} and sends exactly one
 * {@link TalkingPayload} to the one player in range — a throwaway pipeline built by hand (the same
 * pattern {@code villager_voices.fabric.debug.DebugCommand} already uses for its own real-catalogue,
 * real-selection, forced-event testing), with {@link TalkingPayloadSender} substituted for a
 * capturing list instead of a real network send — {@code GameTestHelper}'s own mock players are not
 * real network-connected clients a sent payload could otherwise be observed arriving at.
 */
public final class TalkingStateSyncGameTest {

    @GameTest
    public void aPlayedLineMarksTheVillagerTalkingAndSendsExactlyOnePayloadToThePlayerInRange(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 1, 1));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(villager.getX(), villager.getY(), villager.getZ());

        Config config = Config.defaults();
        List<TalkingPayload> captured = new ArrayList<>();
        TalkingStateSync sync = new TalkingStateSync((p, payload) -> captured.add(payload));
        DisplayQueue queue = new DisplayQueue();
        MinecraftServer server = helper.getLevel().getServer();
        FabricLineSink sink = new FabricLineSink(server, config, queue, sync);

        VillagerEventBus bus = new VillagerEventBus(
                new FabricLineCatalogue(), sink, server::getTickCount, new Random()::nextInt);

        long now = server.getTickCount();
        VillagerReactionSignal signal = new VillagerReactionSignal(
                villager.getUUID(), VillagerReactionEvent.TRADE_COMPLETED, false, false, Set.of(player.getUUID()));
        bus.publish(signal);

        helper.assertTrue(sync.talkingState().isTalking(villager.getUUID(), now),
                "expected the villager marked talking as of the tick the line played");
        helper.assertTrue(!sync.talkingState().isTalking(villager.getUUID(), now + config.talkingDurationTicks()),
                "expected the mark to expire exactly config.talkingDurationTicks() later");

        helper.assertTrue(captured.size() == 1, "expected exactly one payload sent, found " + captured.size());
        TalkingPayload payload = captured.get(0);
        helper.assertTrue(payload.villagerId().equals(villager.getUUID()), "expected the payload keyed to the villager that talked");
        helper.assertTrue(payload.ticks() == config.talkingDurationTicks(), "expected the payload's own duration to match config.talkingDurationTicks()");

        helper.succeed();
    }
}
