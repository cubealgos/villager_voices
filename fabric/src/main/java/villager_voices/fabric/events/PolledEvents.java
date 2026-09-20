package villager_voices.fabric.events;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import villager_voices.PanicDetector;
import villager_voices.StareDetector;
import villager_voices.VillagerEventBus;
import villager_voices.VillagerReactionEvent;
import villager_voices.VillagerReactionSignal;

/**
 * VV-6: the two events with no push-based hook at all (docs/spec/domains/reaction.md §3, `panic`
 * and `player_staring` rows) — detected by polling loaded villagers once per server tick
 * (`ARCH-DEC-003`) instead of a mixin or native event. Wires {@link PanicDetector} and
 * {@link StareDetector} (pure, in {@code common}) to a per-{@link ServerLevel} scan of
 * {@link EntityTypes#VILLAGER} entities, run once from {@link ServerTickEvents#END_SERVER_TICK}.
 *
 * <p><b>Bounded, per {@code ARCH-DEC-003}</b>: {@link ServerLevel#getEntities(EntityType,
 * java.util.function.Predicate)} walks only the villagers Minecraft already has loaded for that
 * level (its per-chunk entity sections), never every entity of every type — the same reason a
 * server with no villagers loaded pays no cost from this poll (`reaction.md` §2 "Not-you"). Cost
 * is {@code O(villagers)} per tick for `panic`, {@code O(villagers × online players in the same
 * level)} for `player_staring` (`REACTION-FAIL-004`, `ARCH-FAIL-004`); see
 * {@code PolledEventsStressGameTest} for a measured run against a large village.
 *
 * <p>Builds its own {@link VillagerReactionSignal} via a private {@code signalFor} for now — VV-6's
 * own brief: VV-4 and VV-5 land their own copies of the same shape in parallel, in this same
 * {@code events} package, and the coordinator unifies the three into one shared helper once all
 * three merge.
 */
public final class PolledEvents {

    private PolledEvents() {
    }

    /** Registers the poll against {@code bus}: one {@link ServerTickEvents#END_SERVER_TICK} listener. */
    public static void register(VillagerEventBus bus) {
        PanicDetector panic = new PanicDetector();
        StareDetector stare = new StareDetector();
        ServerTickEvents.END_SERVER_TICK.register(server -> poll(server, bus, panic, stare));
    }

    private static void poll(MinecraftServer server, VillagerEventBus bus, PanicDetector panic, StareDetector stare) {
        for (ServerLevel level : server.getAllLevels()) {
            List<? extends Villager> villagers = level.getEntities(EntityTypes.VILLAGER, villager -> true);
            if (villagers.isEmpty()) {
                continue;
            }
            List<ServerPlayer> players = level.players();
            for (Villager villager : villagers) {
                pollPanic(villager, panic, bus, players);
                if (!players.isEmpty()) {
                    pollStaring(villager, players, stare, bus);
                }
            }
        }
    }

    private static void pollPanic(Villager villager, PanicDetector panic, VillagerEventBus bus, List<ServerPlayer> players) {
        boolean isPanicking = villager.getBrain().isActive(Activity.PANIC);
        if (panic.sample(villager.getUUID(), isPanicking)) {
            bus.publish(signalFor(villager, VillagerReactionEvent.PANIC, players));
        }
    }

    private static void pollStaring(Villager villager, List<ServerPlayer> players, StareDetector stare, VillagerEventBus bus) {
        Vec3 villagerEyes = villager.getEyePosition();
        UUID villagerId = villager.getUUID();
        for (ServerPlayer player : players) {
            Vec3 toVillager = villagerEyes.subtract(player.getEyePosition());
            double distanceBlocks = toVillager.length();
            // A player standing exactly at the villager's eye position has no defined look
            // direction toward it; treat that degenerate case as dead-on rather than divide by zero.
            double dot = distanceBlocks > 0.0 ? toVillager.scale(1.0 / distanceBlocks).dot(player.getLookAngle()) : 1.0;
            if (stare.sample(player.getUUID(), villagerId, dot, distanceBlocks)) {
                bus.publish(signalFor(villager, VillagerReactionEvent.PLAYER_STARING, players));
            }
        }
    }

    /**
     * Builds the signal for a polled event: {@code nearbyPlayerIds} is every player currently in
     * the villager's own level, the same "candidate set, narrowed later by the sink" contract
     * {@link villager_voices.LineSink}'s Javadoc already documents — this adapter does not itself
     * decide who is close enough to hear or notice the line.
     */
    private static VillagerReactionSignal signalFor(Villager villager, VillagerReactionEvent event, List<ServerPlayer> players) {
        Set<UUID> nearbyPlayerIds = new HashSet<>();
        for (ServerPlayer player : players) {
            nearbyPlayerIds.add(player.getUUID());
        }
        return new VillagerReactionSignal(villager.getUUID(), event, villager.isSleeping(), villager.isBaby(), nearbyPlayerIds);
    }
}
