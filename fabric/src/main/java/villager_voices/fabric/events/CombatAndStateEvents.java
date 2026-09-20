package villager_voices.fabric.events;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import villager_voices.VillagerEventBus;
import villager_voices.VillagerReactionEvent;
import villager_voices.VillagerReactionSignal;

/**
 * Wires VV-5's six combat/state events (docs/spec/domains/reaction.md §3: {@code hurt},
 * {@code killed}, {@code zombified}, {@code cured}, {@code sleep}, {@code wake}) to native Fabric
 * API events, publishing a {@link VillagerReactionSignal} for each — no mixin, matching
 * `contracts/platform-matrix.md`'s mixin table, which lists none of these six for Fabric.
 *
 * <p>{@code zombified} and {@code cured} share one Fabric API event
 * ({@link ServerLivingEntityEvents#MOB_CONVERSION}) and are told apart from the event's own
 * before/after entity types, not a second hook: a {@link Villager} converting into a
 * {@link ZombieVillager} is {@code zombified} (the signal names the original villager, per the
 * ticket); a {@link ZombieVillager} converting into a {@link Villager} is {@code cured} (the
 * signal names the resulting villager). Both directions are jar-confirmed (not merely inferred)
 * to route through this one event: {@code javap -c} on the shipped 26.2
 * {@code minecraft-merged-deobf} jar shows {@code Zombie.convertVillagerToZombieVillager} and
 * {@code ZombieVillager.finishConversion} both call the same public
 * {@code Mob.convertTo(EntityType, ConversionParams, ConversionParams.AfterConversion)}, and
 * {@code javap -c} on the shipped {@code fabric-entity-events-v1} jar shows its
 * {@code MobMixin.afterEntityConverted} — injected into that exact method — is the only thing
 * that invokes {@code MOB_CONVERSION}'s listener. See VV-5's Findings in the ticket body for the
 * full evidence trail.
 *
 * <p>This ticket's own private equivalent of VV-4's shared {@code Signals} helper (a temporary
 * duplication the coordinator unifies after all three parallel event tickets merge, per the
 * ticket brief): {@link #signalFor} builds a {@link VillagerReactionSignal} from a
 * {@link Villager}, reading its sleeping/baby state and candidate hearing-range recipients the
 * same tick the signal is reported (matching {@link VillagerReactionSignal}'s own Javadoc
 * contract).
 */
public final class CombatAndStateEvents {

    /**
     * Candidate hearing-range radius, in blocks: the engine's own default cap on a sound event's
     * effective broadcast radius (docs/spec/domains/reaction.md §3 "Hearing range and silence
     * rules"). This is a broad candidate list only — narrowing it to players actually within one
     * selected line's own sound event range is {@code LineSink}'s concern, not this adapter's
     * (see {@code villager_voices.LineSink}'s own Javadoc).
     */
    private static final double HEARING_RANGE_BLOCKS = 16.0;

    private CombatAndStateEvents() {
    }

    /** Registers all six hooks against {@code bus}. Called once, at mod init. */
    public static void register(VillagerEventBus bus) {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (entity instanceof Villager villager) {
                bus.publish(signalFor(villager, VillagerReactionEvent.HURT));
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof Villager villager) {
                bus.publish(signalFor(villager, VillagerReactionEvent.KILLED));
            }
        });

        ServerLivingEntityEvents.MOB_CONVERSION.register((original, converted, params) -> {
            if (original instanceof Villager originalVillager && converted instanceof ZombieVillager) {
                bus.publish(signalFor(originalVillager, VillagerReactionEvent.ZOMBIFIED));
            } else if (original instanceof ZombieVillager && converted instanceof Villager resultingVillager) {
                bus.publish(signalFor(resultingVillager, VillagerReactionEvent.CURED));
            }
        });

        EntitySleepEvents.START_SLEEPING.register((entity, pos) -> {
            if (entity instanceof Villager villager) {
                bus.publish(signalFor(villager, VillagerReactionEvent.SLEEP));
            }
        });

        EntitySleepEvents.STOP_SLEEPING.register((entity, pos) -> {
            if (entity instanceof Villager villager) {
                bus.publish(signalFor(villager, VillagerReactionEvent.WAKE));
            }
        });
    }

    /**
     * Builds a signal from {@code villager}'s own state at the moment of detection: sleeping and
     * baby flags read live (docs/spec/domains/reaction.md §3's silence rules), candidate
     * recipients gathered from every player on the same level within {@link #HEARING_RANGE_BLOCKS}.
     */
    private static VillagerReactionSignal signalFor(Villager villager, VillagerReactionEvent event) {
        return new VillagerReactionSignal(
                villager.getUUID(),
                event,
                villager.isSleeping(),
                villager.isBaby(),
                nearbyPlayerIds(villager));
    }

    private static Set<UUID> nearbyPlayerIds(Villager villager) {
        if (!(villager.level() instanceof ServerLevel serverLevel)) {
            return Set.of();
        }
        double rangeSq = HEARING_RANGE_BLOCKS * HEARING_RANGE_BLOCKS;
        Set<UUID> ids = new HashSet<>();
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(villager) <= rangeSq) {
                ids.add(player.getUUID());
            }
        }
        return ids;
    }
}
