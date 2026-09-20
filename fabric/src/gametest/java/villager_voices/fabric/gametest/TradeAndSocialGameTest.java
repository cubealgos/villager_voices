package villager_voices.fabric.gametest;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.phys.EntityHitResult;
import villager_voices.VillagerReactionEvent;
import villager_voices.VillagerReactionSignal;
import villager_voices.fabric.VillagerVoicesFabric;

/**
 * One game test per VV-4 event (docs/spec/operations/testing.md "Game tests" row,
 * TEST-REQ-003): a real {@link Villager} entity, a real trigger for the event's own hook, and the
 * result captured through {@link VillagerVoicesFabric#eventBus()}'s subscriber API (VV-1) rather
 * than the display, per this ticket's own instruction. Every test filters captured signals to the
 * specific villager id and event it triggered, since {@link VillagerVoicesFabric#eventBus()} is a
 * single server-lifetime bus shared by every game test in this run (VV-8: built once
 * {@code SERVER_STARTED} fires, before any game test runs).
 */
public final class TradeAndSocialGameTest {

    @GameTest
    public void tradeCompletedFiresOnNotifyTrade(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 1, 1));
        List<VillagerReactionSignal> captured = subscribe();

        MerchantOffer offer = new MerchantOffer(
                new ItemCost(Items.EMERALD), new ItemStack(Items.BREAD), 12, 1, 0.05f);
        villager.notifyTrade(offer);

        helper.assertTrue(
                signalFired(captured, villager, VillagerReactionEvent.TRADE_COMPLETED),
                "trade_completed reached the bus");
        helper.succeed();
    }

    @GameTest
    public void offerOpenedFiresOnInteract(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 1, 1));
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        List<VillagerReactionSignal> captured = subscribe();

        UseEntityCallback.EVENT
                .invoker()
                .interact(
                        player,
                        helper.getLevel(),
                        InteractionHand.MAIN_HAND,
                        villager,
                        new EntityHitResult(villager));

        helper.assertTrue(
                signalFired(captured, villager, VillagerReactionEvent.OFFER_OPENED),
                "offer_opened reached the bus");
        helper.succeed();
    }

    @GameTest
    public void levelUpFiresOnIncreasedLevel(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 1, 1));
        List<VillagerReactionSignal> captured = subscribe();

        VillagerData current = villager.getVillagerData();
        villager.setVillagerData(current.withLevel(current.level() + 1));

        helper.assertTrue(
                signalFired(captured, villager, VillagerReactionEvent.LEVEL_UP),
                "level_up reached the bus");
        helper.succeed();
    }

    @GameTest
    public void levelUpDoesNotFireWhenLevelIsUnchanged(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 1, 1));
        List<VillagerReactionSignal> captured = subscribe();

        villager.setVillagerData(villager.getVillagerData());

        helper.assertFalse(
                signalFired(captured, villager, VillagerReactionEvent.LEVEL_UP),
                "level_up must not fire when the level does not increase");
        helper.succeed();
    }

    @GameTest
    public void restockFiresOnRestock(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 1, 1));
        List<VillagerReactionSignal> captured = subscribe();

        villager.restock();

        helper.assertTrue(
                signalFired(captured, villager, VillagerReactionEvent.RESTOCK), "restock reached the bus");
        helper.succeed();
    }

    @GameTest
    public void raidBellFiresOnBellHit(GameTestHelper helper) {
        BlockPos bellPos = new BlockPos(1, 1, 1);
        helper.setBlock(bellPos, Blocks.BELL);
        BellBlockEntity bell = helper.getBlockEntity(bellPos, BellBlockEntity.class);
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(2, 1, 1));
        List<VillagerReactionSignal> captured = subscribe();

        bell.onHit(Direction.UP);

        helper.assertTrue(
                signalFired(captured, villager, VillagerReactionEvent.RAID_BELL),
                "raid_bell reached the bus for a villager near the bell");
        helper.succeed();
    }

    @GameTest
    public void breedingFiresOnSuccessfulBreedOffspring(GameTestHelper helper) {
        Villager parentA = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 1, 1));
        Villager parentB = helper.spawn(EntityTypes.VILLAGER, new BlockPos(2, 1, 1));
        List<VillagerReactionSignal> captured = subscribe();

        Villager child = parentA.getBreedOffspring(helper.getLevel(), parentB);

        helper.assertTrue(child != null, "getBreedOffspring produced a child");
        helper.assertTrue(
                signalFired(captured, parentA, VillagerReactionEvent.BREEDING),
                "breeding reached the bus for the first parent");
        helper.assertTrue(
                signalFired(captured, parentB, VillagerReactionEvent.BREEDING),
                "breeding reached the bus for the second parent");
        helper.succeed();
    }

    @GameTest
    public void babyGrowsFiresOnAgeBoundaryReached(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 1, 1));
        villager.setBaby(true);
        List<VillagerReactionSignal> captured = subscribe();

        // ageBoundaryReached() is protected; setAge crossing the baby(<0)/adult(>=0) boundary is
        // its one public trigger (confirmed by javap -c: AgeableMob.setAge calls
        // ageBoundaryReached() whenever old and new age fall on opposite sides of zero) — the
        // ticket's own "setting age" alternative to calling the hook directly.
        villager.setAge(0);

        helper.assertTrue(
                signalFired(captured, villager, VillagerReactionEvent.BABY_GROWS),
                "baby_grows reached the bus");
        helper.succeed();
    }

    @GameTest
    public void golemSummonedFiresOnIronGolemLoad(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 1, 1));
        List<VillagerReactionSignal> captured = subscribe();

        helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(2, 1, 1));

        helper.assertTrue(
                signalFired(captured, villager, VillagerReactionEvent.GOLEM_SUMMONED),
                "golem_summoned reached the bus for a nearby villager");
        helper.succeed();
    }

    /** Subscribes a fresh collector to the mod's own live bus (VV-1's subscriber API). */
    private static List<VillagerReactionSignal> subscribe() {
        List<VillagerReactionSignal> captured = new ArrayList<>();
        VillagerVoicesFabric.eventBus().subscribe(captured::add);
        return captured;
    }

    private static boolean signalFired(
            List<VillagerReactionSignal> captured, Villager villager, VillagerReactionEvent event) {
        return captured.stream()
                .anyMatch(signal -> signal.villagerId().equals(villager.getUUID()) && signal.event() == event);
    }
}
