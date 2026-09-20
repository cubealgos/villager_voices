package villager_voices.fabric.mixin;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import villager_voices.VillagerReactionEvent;
import villager_voices.fabric.events.Signals;
import villager_voices.fabric.events.TradeAndSocialEvents;

/**
 * {@code trade_completed} (docs/spec/domains/reaction.md §3): injects {@link
 * AbstractVillager#notifyTrade(MerchantOffer)} at TAIL, publishing a {@code TRADE_COMPLETED}
 * signal without observing or altering the method's own effect ({@code notifyTrade} returns
 * {@code void}; this mixin reads nothing from and writes nothing to {@code offer} or {@code
 * this}) — this ticket's TEST-REQ-003 obligation for this event.
 *
 * <p>Mixin target confirmed by {@code javap -p} against
 * {@code minecraft-merged-deobf-26.2.jar}: {@code AbstractVillager.notifyTrade(MerchantOffer)},
 * public, package {@code net.minecraft.world.entity.npc.villager} (26.2's post-move location,
 * docs/spec/domains/reaction.md §3's 26.2-package-move note).
 */
@Mixin(AbstractVillager.class)
public abstract class TradeCompletedMixin {

    @Inject(method = "notifyTrade", at = @At("TAIL"))
    private void villager_voices$onNotifyTrade(MerchantOffer offer, CallbackInfo ci) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        TradeAndSocialEvents.publish(Signals.of(self, VillagerReactionEvent.TRADE_COMPLETED));
    }
}
