package villager_voices.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import villager_voices.VillagerReactionEvent;
import villager_voices.fabric.events.TradeAndSocialEvents;

/**
 * {@code raid_bell}, raid-start target (docs/spec/domains/reaction.md §3): injects {@link
 * Raids#createOrExtendRaid(ServerPlayer, BlockPos)} at TAIL, using the triggering player's own
 * level (the {@code Raids} instance itself holds no {@link net.minecraft.world.level.Level}
 * reference of its own — confirmed by {@code javap}) and the given {@code pos} to reach every
 * nearby villager. Fires on a Bad-Omen player entering a village, not directly from a bell ring —
 * the other raid_bell target ({@link BellRingMixin}) covers the ring itself.
 *
 * <p>Mixin target confirmed by {@code javap -p}: {@code Raids.createOrExtendRaid(ServerPlayer,
 * BlockPos)}, public, non-static, package {@code net.minecraft.world.entity.raid} (unmoved in
 * 26.2).
 */
@Mixin(Raids.class)
public abstract class RaidStartMixin {

    @Inject(method = "createOrExtendRaid", at = @At("TAIL"))
    private void villager_voices$onCreateOrExtendRaid(
            ServerPlayer player, BlockPos pos, CallbackInfoReturnable<Raid> cir) {
        TradeAndSocialEvents.publishToNearbyVillagers(player.level(), pos, VillagerReactionEvent.RAID_BELL);
    }
}
