package villager_voices.fabric.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import villager_voices.VillagerReactionEvent;
import villager_voices.fabric.events.Signals;
import villager_voices.fabric.events.TradeAndSocialEvents;

/**
 * {@code level_up} (docs/spec/domains/reaction.md §3): injects {@link
 * Villager#setVillagerData(VillagerData)} at HEAD, comparing the villager's current level (read
 * before the new data is applied) against the incoming {@code data}'s level; publishes a {@code
 * LEVEL_UP} signal only when the level actually increases. Catches every level-change source since
 * {@code Villager.increaseMerchantCareer} (private, the vanilla driver, research note §A) always
 * calls this setter internally — hooking the private method directly was ruled out by the ticket's
 * own Approach in favour of this public, broader-coverage HEAD inject.
 *
 * <p>Guards against the villager's very first {@code setVillagerData} call (construction, where
 * the prior value may still be unset) by skipping when the previous data is {@code null} — avoids
 * both a {@code NullPointerException} and a spurious level-up signal on spawn.
 *
 * <p>Mixin target confirmed by {@code javap -p}: {@code Villager.setVillagerData(VillagerData)},
 * public, package {@code net.minecraft.world.entity.npc.villager}.
 */
@Mixin(Villager.class)
public abstract class LevelUpMixin {

    @Inject(method = "setVillagerData", at = @At("HEAD"))
    private void villager_voices$onSetVillagerData(VillagerData data, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        VillagerData previous = self.getVillagerData();
        if (previous != null && data != null && data.level() > previous.level()) {
            TradeAndSocialEvents.publish(Signals.of(self, VillagerReactionEvent.LEVEL_UP));
        }
    }
}
