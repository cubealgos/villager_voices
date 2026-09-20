package villager_voices.fabric.display;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import villager_voices.display.DisplayLine;
import villager_voices.display.DisplayQueue;

/**
 * VV-7, docs/spec/operations/testing.md: "the per-player {@code DisplayQueue}'s hold-time/advance
 * logic" driven from a real server tick, against a real {@link ServerPlayer}.
 *
 * <p>Runs {@link ActionBarDisplay#onEndServerTick} directly against the game test's own real
 * {@code MinecraftServer} at chosen tick offsets, rather than through Fabric's global {@code
 * ServerTickEvents} bus — {@link ActionBarDisplay#register} is a one-line delegation to that bus
 * and registering onto it here would accumulate listeners across repeated game-test runs in the
 * same JVM, without adding coverage beyond calling the drain method itself.
 *
 * <p><b>Test seam, chosen over inspecting real overlay packets:</b> {@link GameTestHelper
 * #makeMockServerPlayerInLevel()} does wire the mock player to a real (embedded, in-process)
 * network {@code Connection}, so {@code ServerPlayer#sendOverlayMessage} would not throw — but
 * reading back what it sent needs reflection into {@code Connection}/Netty internals with no
 * public API. This test instead builds its {@link ActionBarDisplay} with the package-visible
 * {@code OverlayPusher} test seam, capturing exactly the {@code (player, formatted text)} pair
 * {@link ActionBarDisplay#onEndServerTick} would otherwise have handed to
 * {@code sendOverlayMessage} — the same call this ticket adds, minus the unobservable last hop
 * into networking.
 */
public final class DisplayQueueGameTest {

    private static final long MIN_HOLD_TICKS = 2;

    @GameTest
    public void aBurstOfTwoLinesDisplaysEachInOrderHeldForTheMinimumTime(GameTestHelper helper) {
        List<String> pushedText = new ArrayList<>();
        DisplayQueue queue = new DisplayQueue(MIN_HOLD_TICKS, DisplayQueue.DEFAULT_CAP_PER_PLAYER);
        ActionBarDisplay display = new ActionBarDisplay(queue, (player, text) -> pushedText.add(text));

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        DisplayLine first = new DisplayLine("Farmer", "line one", "villager_voices:line_one");
        DisplayLine second = new DisplayLine("Farmer", "line two", "villager_voices:line_two");
        queue.enqueue(player.getUUID(), first);
        queue.enqueue(player.getUUID(), second);

        // Tick 0: the queue is empty of any "currently shown" line, so the first line displays
        // immediately (DISPLAY-REQ-001).
        display.onEndServerTick(helper.getLevel().getServer());
        helper.assertTrue(pushedText.equals(List.of("Farmer: line one")),
            "the first line pushes immediately: " + pushedText);

        // One tick later: still within the minimum hold time, so the second line must wait
        // (DISPLAY-REQ-003) rather than overlap or drop the first (DISPLAY-FAIL-002).
        helper.runAtTickTime(1, () -> {
            display.onEndServerTick(helper.getLevel().getServer());
            helper.assertTrue(pushedText.size() == 1,
                "no advance before the minimum hold time: " + pushedText);
        });

        // Past the minimum hold time: the second line advances, in order, and nothing was
        // dropped.
        helper.runAtTickTime(MIN_HOLD_TICKS + 1, () -> {
            display.onEndServerTick(helper.getLevel().getServer());
            helper.assertTrue(pushedText.equals(List.of("Farmer: line one", "Farmer: line two")),
                "both lines pushed in order after the hold time: " + pushedText);
            helper.succeed();
        });
    }
}
