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
 * {@code baby_grows} (docs/spec/domains/reaction.md §3): injects {@link
 * Villager#ageBoundaryReached()} at TAIL — fires exactly once, at the moment a baby becomes an
 * adult (REACTION-REQ-010's one exemption), narrower than hooking the shared aging tick.
 *
 * <p>Mixin target confirmed by {@code javap -p}: {@code Villager.ageBoundaryReached()}, protected,
 * declared directly on {@code Villager} itself (a real override, not just inherited unchanged from
 * {@code AgeableMob}), package {@code net.minecraft.world.entity.npc.villager}.
 */
@Mixin(Villager.class)
public abstract class BabyGrowsMixin {

    @Inject(method = "ageBoundaryReached", at = @At("TAIL"))
    private void villager_voices$onAgeBoundaryReached(CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        TradeAndSocialEvents.publish(Signals.of(self, VillagerReactionEvent.BABY_GROWS));
    }
}
