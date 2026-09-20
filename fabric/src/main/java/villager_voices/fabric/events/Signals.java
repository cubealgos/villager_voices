package villager_voices.fabric.events;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import villager_voices.VillagerReactionEvent;
import villager_voices.VillagerReactionSignal;

/**
 * Turns a real, jar-side {@link LivingEntity} into a {@link VillagerReactionSignal}: the
 * villager's own asleep/baby flags (docs/spec/domains/reaction.md §3's silence rules,
 * REACTION-REQ-009/010) and the ids of the players within hearing range
 * (docs/spec/domains/reaction.md §3 "Hearing range", REACTION-REQ-008).
 *
 * <p>Shared by every Fabric event-wiring ticket that needs to build a signal from an entity —
 * {@code VV-4} (trade/social), {@code VV-5} (combat/state), {@code VV-6} (poll-based) — so the
 * asleep/baby/hearing-range logic is written once. Deliberately typed to {@link LivingEntity}, not
 * {@code Villager}: some hook sites (e.g. {@code AbstractVillager.notifyTrade}, a wandering-trader
 * ancestor) and some other tickets' events (e.g. a {@code ZombieVillager} mid-conversion) hand this
 * a villager-family entity that is not itself a {@code Villager}.
 */
public final class Signals {

    /**
     * Hearing-range search radius, in blocks. docs/spec/domains/reaction.md §3 defines hearing
     * range as the eventual line's own sound-event broadcast radius, capped at 16 blocks × volume
     * by engine default (research §B3) — display/audio (VV-7/VV-8) narrows the actual recipient
     * set at play time; this is the candidate-recipient search radius the loader adapter reports,
     * matching the documented default cap.
     */
    public static final double HEARING_RANGE_BLOCKS = 16.0;

    private Signals() {
    }

    /**
     * Builds a signal for {@code event} detected on {@code entity}: {@code villagerAsleep}/
     * {@code villagerBaby} read straight off the entity at the moment of detection (the same tick
     * the hook fired, per {@link VillagerReactionSignal}'s own contract), {@code nearbyPlayerIds}
     * from {@link #nearbyPlayerIds(LivingEntity)}.
     */
    public static VillagerReactionSignal of(LivingEntity entity, VillagerReactionEvent event) {
        return new VillagerReactionSignal(
                entity.getUUID(), event, entity.isSleeping(), entity.isBaby(), nearbyPlayerIds(entity));
    }

    /** The ids of every player within {@link #HEARING_RANGE_BLOCKS} of {@code entity}. */
    public static Set<UUID> nearbyPlayerIds(LivingEntity entity) {
        return nearbyPlayerIds(entity.level(), entity.getBoundingBox());
    }

    /**
     * The ids of every player within {@link #HEARING_RANGE_BLOCKS} of {@code origin}. Returns an
     * empty set off the logical server (client-side simulation, or a level not yet a
     * {@link ServerLevel}) since the server is the sole detector (docs/spec/domains/reaction.md §2
     * "Actors": "The server (ACTORS-002) detects every event").
     */
    public static Set<UUID> nearbyPlayerIds(Level level, AABB origin) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Set.of();
        }
        AABB range = origin.inflate(HEARING_RANGE_BLOCKS);
        Set<UUID> ids = new HashSet<>();
        for (ServerPlayer player : serverLevel.players()) {
            if (range.intersects(player.getBoundingBox())) {
                ids.add(player.getUUID());
            }
        }
        return Set.copyOf(ids);
    }
}
