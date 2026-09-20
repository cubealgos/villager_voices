package villager_voices.fabric.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import villager_voices.StareDetector;
import villager_voices.VillagerEventBus;
import villager_voices.VillagerReactionEvent;
import villager_voices.VillagerReactionSignal;
import villager_voices.fabric.events.PolledEvents;

/**
 * VV-6: `player_staring` fires once a player has kept a villager within
 * {@link StareDetector#DOT_THRESHOLD}/{@link StareDetector#RANGE_BLOCKS} for
 * {@link StareDetector#REQUIRED_TICKS} consecutive ticks, and not again while the same stare
 * continues unbroken (docs/spec/domains/reaction.md §3 `player_staring` row; the three thresholds
 * are VV-6's own proposal -- no research precedent -- recorded in the ticket's own Findings). A
 * mock server player is placed 3 blocks from the villager and made to look directly at its eyes,
 * matching the ticket's own test approach.
 *
 * <p>Filters captured signals down to this test's own villager UUID: {@link PolledEvents} polls
 * every loaded villager and player server-wide (by design -- {@code ARCH-DEC-003}), and
 * Minecraft's own game test framework runs several {@code @GameTest} methods concurrently in the
 * same world at different coordinate offsets, so an unfiltered capture list would also see the
 * other stare test method's villager/player pair.
 */
public final class StareGameTest {

    @GameTest(maxTicks = StareDetector.REQUIRED_TICKS + 30)
    public void playerStaringFiresAfterRequiredTicksAtCloseRange(GameTestHelper helper) {
        helper.setBlock(2, 0, 2, Blocks.STONE);
        helper.setBlock(2, 0, 5, Blocks.STONE);
        Villager villager = helper.spawn(EntityTypes.VILLAGER, 2, 1, 2);
        villager.setNoAi(true); // stays put -- the mock player's aim is set once, not re-tracked
        UUID villagerId = villager.getUUID();
        aimedMockPlayer(helper, villager, 2.5, 1.0, 5.5); // 3 blocks from the villager

        VillagerEventBus bus = new VillagerEventBus();
        List<VillagerReactionSignal> captured = new ArrayList<>();
        bus.subscribe(captured::add);
        PolledEvents.register(bus);

        helper.runAfterDelay(StareDetector.REQUIRED_TICKS - 5, () -> {
            helper.assertTrue(stareCount(captured, villagerId) == 0, "should not fire before the required tick count is reached");
        });

        helper.runAfterDelay(StareDetector.REQUIRED_TICKS + 5, () -> {
            long stares = stareCount(captured, villagerId);
            helper.assertTrue(stares == 1, "player_staring should fire exactly once, fired " + stares + " times");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = StareDetector.REQUIRED_TICKS + 30)
    public void playerStaringDoesNotFireBeyondRange(GameTestHelper helper) {
        // Diagonal placement within the default 8x8x8 empty structure: dx=6, dz=6, distance
        // sqrt(72) =~ 8.49 blocks -- beyond StareDetector.RANGE_BLOCKS (8.0) -- since no
        // axis-aligned pair fits both endpoints inside an 8-block-wide region and exceeds it.
        helper.setBlock(1, 0, 1, Blocks.STONE);
        helper.setBlock(7, 0, 7, Blocks.STONE);
        Villager villager = helper.spawn(EntityTypes.VILLAGER, 1, 1, 1);
        villager.setNoAi(true);
        UUID villagerId = villager.getUUID();
        aimedMockPlayer(helper, villager, 7.5, 1.0, 7.5);

        VillagerEventBus bus = new VillagerEventBus();
        List<VillagerReactionSignal> captured = new ArrayList<>();
        bus.subscribe(captured::add);
        PolledEvents.register(bus);

        helper.runAfterDelay(StareDetector.REQUIRED_TICKS + 5, () -> {
            helper.assertTrue(stareCount(captured, villagerId) == 0, "a player beyond range should never trigger player_staring");
            helper.succeed();
        });
    }

    private static ServerPlayer aimedMockPlayer(GameTestHelper helper, Villager villager, double x, double y, double z) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(x, y, z)));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, villager.getEyePosition());
        return player;
    }

    private static long stareCount(List<VillagerReactionSignal> captured, UUID villagerId) {
        return captured.stream()
                .filter(signal -> signal.event() == VillagerReactionEvent.PLAYER_STARING && signal.villagerId().equals(villagerId))
                .count();
    }
}
