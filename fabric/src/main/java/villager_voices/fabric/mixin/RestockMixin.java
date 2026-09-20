package villager_voices.fabric.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import villager_voices.VillagerReactionEvent;
import villager_voices.fabric.events.Signals;
import villager_voices.fabric.events.TradeAndSocialEvents;

/**
 * {@code restock} (docs/spec/domains/reaction.md §3): injects {@link Villager#restock()} at TAIL.
 *
 * <p>Mixin target confirmed by {@code javap -p}: {@code Villager.restock()}, public, no
 * parameters, package {@code net.minecraft.world.entity.npc.villager}.
 */
@Mixin(Villager.class)
public abstract class RestockMixin {

    @Inject(method = "restock", at = @At("TAIL"))
    private void villager_voices$onRestock(CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        TradeAndSocialEvents.publish(Signals.of(self, VillagerReactionEvent.RESTOCK));
    }
}
