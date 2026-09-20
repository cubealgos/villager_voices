package villager_voices.fabric.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;

import villager_voices.LineCatalogue;
import villager_voices.LineRef;
import villager_voices.LineSink;
import villager_voices.VillagerEventBus;
import villager_voices.VillagerReactionEvent;
import villager_voices.VillagerReactionSignal;
import villager_voices.fabric.VillagerVoicesFabric;
import villager_voices.fabric.events.CombatAndStateEvents;

/**
 * VV-5: each of the six combat/state events (docs/spec/domains/reaction.md §3,
 * {@code REACTION-REQ-001}) actually fires its native Fabric hook and reaches
 * {@link VillagerVoicesFabric#eventBus()} in a real world, against a real {@link Villager} or
 * {@link ZombieVillager} entity — no fakes below the bus (docs/spec/operations/testing.md). Each
 * test subscribes its own capturing consumer to {@link VillagerVoicesFabric#eventBus()}, filtered
 * to its own villager's id, rather than building a fresh bus of its own: a fresh bus would never
 * see events dispatched through the sources actually wired at server start (VV-8).
 */
public final class CombatAndStateGameTest {

    /** REACTION-REQ-001 (`hurt`): {@code LivingEntity.hurtServer} → {@code AFTER_DAMAGE}. */
    @GameTest
    public void hurtReachesTheBus(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(2, 1, 2));
        List<VillagerReactionSignal> captured = subscribe(villager.getUUID());

        villager.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1.0F);

        helper.assertTrue(hasEvent(captured, VillagerReactionEvent.HURT), "HURT reached the bus");
        helper.succeed();
    }

    /** REACTION-REQ-001 (`killed`): {@code LivingEntity.die} → {@code AFTER_DEATH}. */
    @GameTest
    public void killedReachesTheBus(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(2, 1, 2));
        List<VillagerReactionSignal> captured = subscribe(villager.getUUID());

        villager.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), villager.getMaxHealth() * 2.0F);

        helper.assertTrue(hasEvent(captured, VillagerReactionEvent.KILLED), "KILLED reached the bus");
        helper.succeed();
    }

    /**
     * REACTION-REQ-001 (`zombified`): {@code Villager.convertTo(ZOMBIE_VILLAGER, ...)} →
     * {@code MOB_CONVERSION}, direction proof #1 — the signal names the villager being converted
     * (the original), per the ticket, not the zombie villager it becomes.
     */
    @GameTest
    public void zombifiedNamesTheOriginalVillager(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(2, 1, 2));
        UUID originalId = villager.getUUID();
        List<VillagerReactionSignal> captured = subscribe(originalId);

        ZombieVillager converted = villager.convertTo(
                EntityTypes.ZOMBIE_VILLAGER, ConversionParams.single(villager, true, true), zv -> { });

        helper.assertTrue(converted != null, "the conversion produced a zombie villager");
        helper.assertTrue(hasEvent(captured, VillagerReactionEvent.ZOMBIFIED),
                "ZOMBIFIED reached the bus, naming the original villager, not the zombie villager it became");
        helper.succeed();
    }

    /**
     * REACTION-REQ-001 (`cured`): {@code ZombieVillager.convertTo(VILLAGER, ...)} →
     * {@code MOB_CONVERSION}, direction proof #2 — the signal names the resulting villager, per
     * the ticket, not the zombie villager it was cured from. Uses the public
     * {@code Mob.convertTo} directly rather than the private {@code ZombieVillager.finishConversion}
     * it wraps (jar-confirmed identical, see {@link CombatAndStateEvents}'s Javadoc and VV-5's
     * Findings): the real potion-and-apple cure timer is real-time-gated and too slow for a game
     * test, exactly the case the ticket names as the fallback.
     */
    @GameTest
    public void curedNamesTheResultingVillager(GameTestHelper helper) {
        ZombieVillager zombieVillager = helper.spawn(EntityTypes.ZOMBIE_VILLAGER, new BlockPos(2, 1, 2));
        List<VillagerReactionSignal> captured = new ArrayList<>();
        VillagerVoicesFabric.eventBus().subscribe(captured::add);

        Villager resulting = zombieVillager.convertTo(
                EntityTypes.VILLAGER, ConversionParams.single(zombieVillager, true, true), v -> { });

        helper.assertTrue(resulting != null, "the cure produced a resulting villager");
        helper.assertTrue(
                captured.stream().anyMatch(s -> s.event() == VillagerReactionEvent.CURED
                        && s.villagerId().equals(resulting.getUUID())),
                "CURED reached the bus, naming the resulting villager");
        helper.succeed();
    }

    /** REACTION-REQ-001 (`sleep`): {@code LivingEntity.startSleeping} → {@code START_SLEEPING}. */
    @GameTest
    public void sleepReachesTheBus(GameTestHelper helper) {
        BlockPos bedPos = new BlockPos(2, 1, 2);
        helper.setBlock(bedPos, Blocks.BED.red());
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(3, 1, 2));
        List<VillagerReactionSignal> captured = subscribe(villager.getUUID());

        villager.startSleeping(helper.absolutePos(bedPos));

        helper.assertTrue(hasEvent(captured, VillagerReactionEvent.SLEEP), "SLEEP reached the bus");
        helper.succeed();
    }

    /** REACTION-REQ-001 (`wake`): {@code LivingEntity.stopSleeping} → {@code STOP_SLEEPING}. */
    @GameTest
    public void wakeReachesTheBus(GameTestHelper helper) {
        BlockPos bedPos = new BlockPos(2, 1, 2);
        helper.setBlock(bedPos, Blocks.BED.red());
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(3, 1, 2));
        villager.startSleeping(helper.absolutePos(bedPos));
        List<VillagerReactionSignal> captured = subscribe(villager.getUUID());

        villager.stopSleeping();

        helper.assertTrue(hasEvent(captured, VillagerReactionEvent.WAKE), "WAKE reached the bus");
        helper.succeed();
    }

    /**
     * REACTION-REQ-009 end to end: "the system shall suppress every event except {@code sleep}
     * itself while the triggering villager is asleep." Proven against a dedicated, fully
     * configured local bus (its own fake {@link LineCatalogue}/{@link LineSink}/clock/roll,
     * docs/spec/operations/testing.md) rather than {@link VillagerVoicesFabric#eventBus()} — VV-8's
     * real bus is fully configured too by the time any game test runs (built at
     * {@code SERVER_STARTED}, before the game test server itself starts), but its real catalogue,
     * clock, and roll would make this test's outcome dependent on shared state and real time
     * instead of the deterministic fake catalogue (only {@code ZOMBIFIED} eligible) and fake clock
     * this assertion needs.
     *
     * <p>Registers {@link CombatAndStateEvents} a second time onto that local bus — harmless: a
     * Fabric API event accepts any number of listeners, and this test only reads its own local
     * bus's sink and subscriber lists.
     *
     * <p><b>Reading of the acceptance criterion, recorded per the ticket's "append findings"
     * instruction (also in VV-5's Findings):</b> the ticket text says the suppressed event must
     * "not reach the bus." Architecturally (VillagerReactionSignal's own Javadoc: "the loader
     * adapter reads LivingEntity.isSleeping()/its age the same tick it reports the signal") a
     * loader adapter always publishes an accurate signal; REACTION-REQ-009's suppression is
     * {@code ReactionRules}'s job inside {@code VillagerEventBus.react()}, downstream of
     * {@code publish}. This test proves both halves: the raw signal *does* reach the bus (a plain
     * subscriber below observes it, proving detection ran), but it never reaches the sink (proving
     * selection was suppressed) — the substantive behaviour REACTION-REQ-009 actually specifies.
     * Uses {@code zombified} rather than {@code hurt} as the suppressed second event: {@code
     * LivingEntity.hurtServer} itself calls {@code stopSleeping()} before {@code AFTER_DAMAGE}
     * fires (jar-confirmed by disassembly — taking damage always wakes a sleeping entity first),
     * so a sleeping villager's {@code HURT} signal would misleadingly carry {@code
     * villagerAsleep=false}, unable to exercise this rule at all. {@code Mob.convertTo} does not
     * touch sleeping state, so it stays {@code true} through the whole call.
     */
    @GameTest
    public void sleepingVillagerSuppressesZombifiedFromTheSink(GameTestHelper helper) {
        LineRef zombifiedLine = new LineRef(VillagerReactionEvent.ZOMBIFIED, "villager_voices:reaction.zombified.1");
        RecordingSink sink = new RecordingSink();
        AtomicLong clock = new AtomicLong();
        LineCatalogue catalogue = event -> event == VillagerReactionEvent.ZOMBIFIED ? List.of(zombifiedLine) : List.of();
        VillagerEventBus localBus = new VillagerEventBus(catalogue, sink, clock::get, bound -> 0);
        List<VillagerReactionSignal> rawSignals = new ArrayList<>();
        localBus.subscribe(rawSignals::add);
        CombatAndStateEvents.register(localBus);

        BlockPos bedPos = new BlockPos(2, 1, 2);
        helper.setBlock(bedPos, Blocks.BED.red());
        Villager sleepingVillager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(3, 1, 2));
        sleepingVillager.startSleeping(helper.absolutePos(bedPos));
        Villager awakeVillager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(5, 1, 2));

        clock.set(1000);
        sleepingVillager.convertTo(EntityTypes.ZOMBIE_VILLAGER, ConversionParams.single(sleepingVillager, true, true), zv -> { });
        clock.set(2000);
        awakeVillager.convertTo(EntityTypes.ZOMBIE_VILLAGER, ConversionParams.single(awakeVillager, true, true), zv -> { });

        helper.assertTrue(
                rawSignals.stream().anyMatch(s -> s.villagerId().equals(sleepingVillager.getUUID())),
                "the sleeping villager's ZOMBIFIED signal still reached the bus (detection is not suppressed)");
        helper.assertTrue(
                sink.shownFor(sleepingVillager.getUUID()).isEmpty(),
                "ZOMBIFIED did not reach the sink for the sleeping villager (REACTION-REQ-009)");
        helper.assertTrue(
                !sink.shownFor(awakeVillager.getUUID()).isEmpty(),
                "ZOMBIFIED reached the sink for the awake villager (control: the mechanism actually distinguishes)");
        helper.succeed();
    }

    private static List<VillagerReactionSignal> subscribe(UUID villagerId) {
        List<VillagerReactionSignal> captured = new ArrayList<>();
        VillagerVoicesFabric.eventBus().subscribe(signal -> {
            if (signal.villagerId().equals(villagerId)) {
                captured.add(signal);
            }
        });
        return captured;
    }

    private static boolean hasEvent(List<VillagerReactionSignal> signals, VillagerReactionEvent event) {
        return signals.stream().anyMatch(s -> s.event() == event);
    }

    private static final class RecordingSink implements LineSink {
        private final List<UUID> shown = new ArrayList<>();

        @Override
        public void show(UUID villagerId, Set<UUID> playerIds, LineRef line) {
            shown.add(villagerId);
        }

        List<UUID> shownFor(UUID villagerId) {
            return shown.stream().filter(villagerId::equals).toList();
        }
    }
}
