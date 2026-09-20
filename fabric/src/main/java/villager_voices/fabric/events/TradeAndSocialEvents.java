package villager_voices.fabric.events;

import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import villager_voices.VillagerEventBus;
import villager_voices.VillagerReactionEvent;
import villager_voices.VillagerReactionSignal;

/**
 * VV-4's eight events — trade and social — per docs/spec/domains/reaction.md §3:
 * {@code trade_completed}, {@code offer_opened}, {@code level_up}, {@code restock},
 * {@code raid_bell}, {@code breeding}, {@code baby_grows}, {@code golem_summoned}. Two are wired
 * here directly as native Fabric API listeners ({@code offer_opened}, {@code golem_summoned}); the
 * other six are mixins under {@code villager_voices.fabric.mixin} ({@code TradeCompletedMixin},
 * {@code LevelUpMixin}, {@code RestockMixin}, {@code BellRingMixin}, {@code RaidStartMixin},
 * {@code BreedingMixin}, {@code BabyGrowsMixin}) that call back into {@link #publish}.
 *
 * <p><b>Deviation from this ticket's own Approach note, also recorded in the ticket's
 * Findings</b>: {@code breeding} is wired as a mixin ({@code BreedingMixin}), not the native
 * {@code BabyEntitySpawnEvent} the Approach section names, and not on {@code
 * Animal.spawnChildFromBreeding} either (the Approach section's own implied target). Two
 * independent errors in that text, both caught against the jar: {@code BabyEntitySpawnEvent} does
 * not exist anywhere in Fabric API {@code 0.161.0+26.2} (it is NeoForge-only), and {@code Villager}
 * does not even extend {@code Animal} — both extend {@code AgeableMob} directly as siblings
 * (confirmed by {@code javap} on both classes), so {@code Animal.spawnChildFromBreeding} is never
 * invoked for a villager regardless. docs/spec/domains/reaction.md §3's breeding row and
 * docs/spec/contracts/platform-matrix.md's mixin table both independently list {@code breeding} as
 * a Fabric mixin target; {@code BreedingMixin}'s own Javadoc has the full trace to the real vanilla
 * call path ({@code VillagerMakeLove} → {@code Villager.getBreedOffspring}).
 */
public final class TradeAndSocialEvents {

    /**
     * Proximity search radius for the two "burst" events ({@code raid_bell}, {@code
     * golem_summoned}) that reach every nearby villager rather than one specific villager, reusing
     * {@link Signals}' own hearing-range default (docs/spec/domains/reaction.md §3).
     */
    private static final double PROXIMITY_RANGE_BLOCKS = Signals.HEARING_RANGE_BLOCKS;

    private static volatile VillagerEventBus bus;

    private TradeAndSocialEvents() {
    }

    /** Registers this ticket's native-event hooks against {@code bus}. Called once, at mod init. */
    public static void register(VillagerEventBus bus) {
        TradeAndSocialEvents.bus = bus;
        registerOfferOpened();
        registerGolemSummoned();
    }

    /**
     * {@code offer_opened}: native, {@code UseEntityCallback.EVENT}, filtered to
     * {@code instanceof Villager} (docs/spec/domains/reaction.md §3 marks this {@code N} — no
     * mixin — the more specific source over platform-matrix.md's conditional wording, per this
     * ticket's own Constraints). Never changes the interaction outcome: always returns
     * {@link InteractionResult#PASS}.
     */
    private static void registerOfferOpened() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!level.isClientSide() && entity instanceof Villager villager) {
                publish(Signals.of(villager, VillagerReactionEvent.OFFER_OPENED));
            }
            return InteractionResult.PASS;
        });
    }

    /**
     * {@code golem_summoned}: native, {@code ServerEntityEvents.ENTITY_LOAD}, filtered to
     * {@code EntityTypes.IRON_GOLEM} (the event carries no spawn reason, so a type filter is all
     * that's available — docs/spec/domains/reaction.md §3). Every villager within
     * {@link #PROXIMITY_RANGE_BLOCKS} of the golem's spawn position reacts, since the event does
     * not identify which villager's job site summoned it.
     */
    private static void registerGolemSummoned() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity.getType() == EntityTypes.IRON_GOLEM) {
                publishToNearbyVillagers(level, entity.blockPosition(), VillagerReactionEvent.GOLEM_SUMMONED);
            }
        });
    }

    /** Called by this ticket's mixins to publish a single-villager signal. */
    public static void publish(VillagerReactionSignal signal) {
        VillagerEventBus current = bus;
        if (current != null) {
            current.publish(signal);
        }
    }

    /**
     * Publishes {@code event} once per {@link Villager} within {@link #PROXIMITY_RANGE_BLOCKS} of
     * {@code origin} — the "burst" a raid bell ring/raid start or a golem summon produces (every
     * nearby villager reacts independently), queued for display once VV-7 lands (this ticket's own
     * acceptance criteria: observable as a burst, not required to display correctly yet). Called
     * by this ticket's mixins ({@code BellRingMixin}, {@code RaidStartMixin}) and by
     * {@link #registerGolemSummoned()}.
     */
    public static void publishToNearbyVillagers(Level level, BlockPos origin, VillagerReactionEvent event) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB range = new AABB(origin).inflate(PROXIMITY_RANGE_BLOCKS);
        List<Villager> nearby = serverLevel.getEntitiesOfClass(Villager.class, range);
        for (Villager villager : nearby) {
            publish(Signals.of(villager, event));
        }
    }
}
