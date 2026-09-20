package villager_voices.fabric.debug;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import villager_voices.display.DisplayQueue;
import villager_voices.fabric.VillagerVoicesFabric;

/**
 * {@code VV-13}'s three acceptance-criteria game tests for
 * {@code villager_voices.fabric.debug.DebugCommand}, run in the game test environment, which is
 * itself a development environment ({@code FabricLoader.isDevelopmentEnvironment()} is true under
 * {@code runGameTest}, exactly as it is under {@code runClient}), so the command is registered and
 * reachable here. Registration being gated on that same check -- the one line in {@code
 * VillagerVoicesFabric.onInitialize} -- is otherwise proven by code review, not a game test: there
 * is no development/non-development pair of environments a single test run can compare (the same
 * finding {@code create_villager_customers}' {@code VC-5} and {@code create_metered_motor}'s
 * {@code MM-8} already recorded for their own identically-gated debug commands).
 *
 * <p>Every test runs the real command through {@code MinecraftServer.getCommands()
 * .performPrefixedCommand}, against a {@link CommandSourceStack} built with a capturing
 * {@link CommandSource} so the feedback {@code DebugCommand} sends can be asserted on by
 * translation key rather than rendered text (robust to a dedicated server's lack of client-side
 * localisation). A {@code sendFailure} message -- brigadier's own path for a thrown {@code
 * CommandSyntaxException}, such as {@code eventOf}'s unknown-event-id error -- wraps the original
 * message as a sibling of an empty root component rather than carrying it as its own contents
 * ({@code CommandSourceStack#sendFailure}, confirmed by {@code javap -p -c} against
 * {@code minecraft-merged-deobf-26.2.jar}), so {@link CapturingSource#hasKey} walks siblings too,
 * not just the top-level message's own contents.
 */
public final class DebugCommandGameTest {

    /**
     * {@code trigger trade_completed} on a real villager: "a captured line through a test
     * subscriber" reads here as a real recipient -- a mock player placed exactly at the villager's
     * position, well within {@code ActionBarDisplay#PLACEHOLDER_HEARING_RADIUS_BLOCKS} -- actually
     * receiving a line on the mod's real {@link VillagerVoicesFabric#DISPLAY_QUEUE} (VV-7's own
     * queue, not a stand-in), proving the "real display" half of {@code VV-13}'s acceptance
     * criteria; the feedback key proves the command itself reported a selection, the "real
     * selection" half (VV-2's {@code LineSelector}, not a hardcoded test line).
     */
    @GameTest
    public void triggerTradeCompletedYieldsACapturedLineAndTheFeedbackKey(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));

        helper.runAfterDelay(2, () -> {
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            player.setPos(villager.getX(), villager.getY(), villager.getZ());

            CapturingSource capturing = new CapturingSource();
            CommandSourceStack source = sourceFor(helper, capturing, villager.position());
            helper.getLevel().getServer().getCommands().performPrefixedCommand(
                source, "villager_voices debug trigger trade_completed " + villager.getUUID());

            helper.assertTrue(
                capturing.hasKey("command.villager_voices.debug.trigger.selected"),
                "trigger reported a selected line: " + capturing.describe());

            DisplayQueue queue = VillagerVoicesFabric.DISPLAY_QUEUE;
            boolean captured = queue.pendingCount(player.getUUID()) > 0 || queue.current(player.getUUID()) != null;
            helper.assertTrue(captured, "the forced line reached the real display queue for the nearby player");
            helper.succeed();
        });
    }

    /** {@code VV-13}: {@code restock}'s default catalogue ships exactly 4 lines (VV-3). */
    @GameTest
    public void linesRestockPrintsFourLines(GameTestHelper helper) {
        CapturingSource capturing = new CapturingSource();
        CommandSourceStack source = sourceFor(helper, capturing, helper.absoluteVec(Vec3.ZERO));
        helper.getLevel().getServer().getCommands().performPrefixedCommand(source, "villager_voices debug lines restock");

        helper.assertTrue(
            capturing.countKey("command.villager_voices.debug.lines.line") == 4,
            "lines restock printed 4 lines: " + capturing.describe());
        helper.succeed();
    }

    /** {@code VV-13}: an unknown event id is a command-syntax error, not a silent no-op or crash. */
    @GameTest
    public void triggerWithAnUnknownEventFailsWithTheErrorKey(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));

        helper.runAfterDelay(2, () -> {
            CapturingSource capturing = new CapturingSource();
            CommandSourceStack source = sourceFor(helper, capturing, villager.position());
            helper.getLevel().getServer().getCommands().performPrefixedCommand(
                source, "villager_voices debug trigger not_a_real_event " + villager.getUUID());

            helper.assertTrue(
                capturing.hasKey("command.villager_voices.debug.unknown_event"),
                "an unknown event id fails with the error key: " + capturing.describe());
            helper.succeed();
        });
    }

    private static CommandSourceStack sourceFor(GameTestHelper helper, CapturingSource capturing, Vec3 pos) {
        return new CommandSourceStack(
            capturing, pos, Vec2.ZERO, helper.getLevel(), PermissionSet.ALL_PERMISSIONS, "DebugCommandGameTest",
            Component.literal("DebugCommandGameTest"), helper.getLevel().getServer(), null);
    }

    /**
     * A {@link CommandSource} that records every message sent to it instead of delivering it
     * anywhere. {@link #hasKey} and {@link #countKey} walk each message's sibling components too,
     * not just its own contents -- {@code sendFailure} wraps the original message as a sibling of an
     * empty root rather than carrying it directly (see this class's own Javadoc).
     */
    private static final class CapturingSource implements CommandSource {
        private final List<Component> messages = new ArrayList<>();

        @Override
        public void sendSystemMessage(Component component) {
            messages.add(component);
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }

        boolean hasKey(String key) {
            return messages.stream().anyMatch(m -> containsKey(m, key));
        }

        long countKey(String key) {
            return messages.stream().filter(m -> containsKey(m, key)).count();
        }

        private static boolean containsKey(Component component, String key) {
            if (component.getContents() instanceof TranslatableContents t && t.getKey().equals(key)) {
                return true;
            }
            for (Component sibling : component.getSiblings()) {
                if (containsKey(sibling, key)) {
                    return true;
                }
            }
            return false;
        }

        String describe() {
            return messages.stream()
                .map(m -> m.getContents() instanceof TranslatableContents t ? t.getKey() : m.toString())
                .toList()
                .toString();
        }
    }
}
