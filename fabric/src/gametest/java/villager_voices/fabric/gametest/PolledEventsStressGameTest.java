package villager_voices.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import villager_voices.VillagerEventBus;
import villager_voices.fabric.VillagerVoicesFabric;
import villager_voices.fabric.events.PolledEvents;

/**
 * VV-6 acceptance criterion: a large-village stress check -- many loaded villagers, none
 * panicking or stared at -- shows no measurable steady-state allocation from the poll
 * (docs/spec/domains/reaction.md `REACTION-FAIL-004`, docs/spec/04-architecture.md
 * `ARCH-FAIL-004`).
 *
 * <p>Measures wall-clock server-tick time across two phases of the same running test: first
 * {@link #MEASURED_TICKS} ticks with the poll registered but zero villagers loaded (baseline --
 * whatever else the server does per tick), then the same tick count with {@link #VILLAGER_COUNT}
 * idle villagers loaded (poll cost + the same baseline). The delta between the two isolates the
 * poll's own per-tick cost from the rest of the server's per-tick work, which stays roughly
 * constant across both phases of one test run. Logged either way -- VV-6's own Findings record one
 * measured run's numbers -- and asserted only against a generous bound: a game test's JIT warm-up
 * and scheduling jitter make a tight millisecond budget unreliable, so the assertion catches a
 * gross regression (e.g. an accidental full-entity-list scan instead of the bounded
 * {@code ServerLevel#getEntities(EntityType, Predicate)} walk), not a few microseconds of noise.
 */
public final class PolledEventsStressGameTest {

    private static final int VILLAGER_COUNT = 40;
    private static final int MEASURED_TICKS = 100;
    private static final int SETTLE_TICKS = 2;
    // Generous on purpose -- see the class Javadoc. The actual measured delta is far below this
    // (recorded in VV-6's own Findings).
    private static final long MAX_DELTA_NANOS_PER_TICK = 5_000_000L; // 5 ms/tick for 40 villagers

    @GameTest(maxTicks = 2 * (MEASURED_TICKS + SETTLE_TICKS) + 10)
    public void manyIdleVillagersAddLittlePerTickOverBaseline(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        VillagerEventBus bus = new VillagerEventBus();
        PolledEvents.register(bus);

        long[] baselineStart = new long[1];
        long[] baselineNanos = new long[1];
        helper.runAfterDelay(SETTLE_TICKS, () -> baselineStart[0] = System.nanoTime());
        helper.runAfterDelay(SETTLE_TICKS + MEASURED_TICKS, () -> {
            baselineNanos[0] = System.nanoTime() - baselineStart[0];
            for (int i = 0; i < VILLAGER_COUNT; i++) {
                helper.spawn(EntityTypes.VILLAGER, 1 + (i % 6), 1, 1 + (i / 6) % 6);
            }
        });

        int loadedDelay = SETTLE_TICKS + MEASURED_TICKS + SETTLE_TICKS;
        long[] loadedStart = new long[1];
        helper.runAfterDelay(loadedDelay, () -> loadedStart[0] = System.nanoTime());
        helper.runAfterDelay(loadedDelay + MEASURED_TICKS, () -> {
            long loadedNanos = System.nanoTime() - loadedStart[0];
            long baselinePerTick = baselineNanos[0] / MEASURED_TICKS;
            long loadedPerTick = loadedNanos / MEASURED_TICKS;
            long deltaPerTick = loadedPerTick - baselinePerTick;
            VillagerVoicesFabric.LOGGER.info(
                    "VV-6 poll stress: {} villagers -- baseline {} ns/tick, loaded {} ns/tick, delta {} ns/tick",
                    VILLAGER_COUNT, baselinePerTick, loadedPerTick, deltaPerTick);
            helper.assertTrue(deltaPerTick < MAX_DELTA_NANOS_PER_TICK,
                    "poll cost delta looked too high: " + deltaPerTick + " ns/tick for " + VILLAGER_COUNT + " villagers");
            helper.succeed();
        });
    }
}
