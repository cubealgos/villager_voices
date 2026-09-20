package villager_voices.fabric.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.Blocks;
import villager_voices.VillagerEventBus;
import villager_voices.VillagerReactionEvent;
import villager_voices.VillagerReactionSignal;
import villager_voices.fabric.events.PolledEvents;

/**
 * VV-6: `panic` fires exactly once per panic episode -- edge-detected, not once per tick while
 * panicking (docs/spec/domains/reaction.md §3 `panic` row, `PanicDetector`'s own contract,
 * `REACTION-FAIL-004`). Triggers panic the way the ticket names as the direct option: setting the
 * villager's brain activity to {@code PANIC} via {@code setActiveActivityIfPossible} and ticking,
 * rather than waiting on a spawned hostile's own AI to trigger it indirectly.
 *
 * <p>Filters captured signals down to this test's own villager UUID: {@link PolledEvents} polls
 * every loaded villager server-wide (by design -- {@code ARCH-DEC-003}), and Minecraft's own game
 * test framework runs several {@code @GameTest} methods concurrently in the same world at
 * different coordinate offsets, so an unfiltered capture list would also see the other panic test
 * method's villager.
 */
public final class PanicGameTest {

    @GameTest(maxTicks = 40)
    public void panicFiresOnceWhenBrainActivityBecomesPanic(GameTestHelper helper) {
        helper.setBlock(2, 0, 2, Blocks.STONE);
        Villager villager = helper.spawn(EntityTypes.VILLAGER, 2, 1, 2);
        UUID villagerId = villager.getUUID();

        VillagerEventBus bus = new VillagerEventBus();
        List<VillagerReactionSignal> captured = new ArrayList<>();
        bus.subscribe(captured::add);
        PolledEvents.register(bus);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(panicCount(captured, villagerId) == 0, "no panic signal before the brain activity changes");
            villager.getBrain().setActiveActivityIfPossible(Activity.PANIC);
        });

        helper.runAfterDelay(8, () -> {
            long panicSignals = panicCount(captured, villagerId);
            helper.assertTrue(panicSignals == 1, "panic should fire exactly once, fired " + panicSignals + " times");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void panicDoesNotFireAgainWhileStillPanicking(GameTestHelper helper) {
        helper.setBlock(2, 0, 2, Blocks.STONE);
        Villager villager = helper.spawn(EntityTypes.VILLAGER, 2, 1, 2);
        UUID villagerId = villager.getUUID();
        villager.getBrain().setActiveActivityIfPossible(Activity.PANIC);

        VillagerEventBus bus = new VillagerEventBus();
        List<VillagerReactionSignal> captured = new ArrayList<>();
        bus.subscribe(captured::add);
        PolledEvents.register(bus);

        helper.runAfterDelay(15, () -> {
            long panicSignals = panicCount(captured, villagerId);
            helper.assertTrue(panicSignals == 1,
                    "steady panic across 15 ticks should still fire exactly once, fired " + panicSignals + " times");
            helper.succeed();
        });
    }

    private static long panicCount(List<VillagerReactionSignal> captured, UUID villagerId) {
        return captured.stream()
                .filter(signal -> signal.event() == VillagerReactionEvent.PANIC && signal.villagerId().equals(villagerId))
                .count();
    }
}
