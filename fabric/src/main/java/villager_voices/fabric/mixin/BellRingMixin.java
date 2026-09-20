package villager_voices.fabric.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import villager_voices.VillagerReactionEvent;
import villager_voices.fabric.events.TradeAndSocialEvents;

/**
 * {@code raid_bell}, ring target (docs/spec/domains/reaction.md §3): injects {@link
 * BellBlockEntity#onHit(Direction)} at TAIL. Every villager within
 * {@link TradeAndSocialEvents#publishToNearbyVillagers}'s proximity range of the bell reacts — the
 * "burst" this ticket's acceptance criteria names, observable as a queued sequence once VV-7
 * lands. The other raid_bell target ({@code Raids.createOrExtendRaid}) is {@link RaidStartMixin}.
 *
 * <p>Mixin target confirmed by {@code javap -p}: {@code BellBlockEntity.onHit(Direction)}, public,
 * package {@code net.minecraft.world.level.block.entity} (unmoved in 26.2).
 */
@Mixin(BellBlockEntity.class)
public abstract class BellRingMixin {

    @Inject(method = "onHit", at = @At("TAIL"))
    private void villager_voices$onBellHit(Direction direction, CallbackInfo ci) {
        BellBlockEntity self = (BellBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level != null) {
            TradeAndSocialEvents.publishToNearbyVillagers(
                    level, self.getBlockPos(), VillagerReactionEvent.RAID_BELL);
        }
    }
}
