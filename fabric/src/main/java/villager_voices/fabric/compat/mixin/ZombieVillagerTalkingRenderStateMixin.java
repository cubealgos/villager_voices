package villager_voices.fabric.compat.mixin;

import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import villager_voices.fabric.compat.ClientTalkingState;
import villager_voices.fabric.compat.TalkingRenderState;

/**
 * {@link VillagerTalkingRenderStateMixin}'s own twin for a zombified villager — docs/spec/
 * 04-architecture.md's loader-adapter table names both {@code VillagerRenderer} and
 * {@code ZombieVillagerRenderer} explicitly, since a talking villager can be zombified mid-session
 * and {@code ClientTalkingState} is keyed by the entity's own {@link java.util.UUID}, unaffected by
 * that transformation.
 */
@Mixin(ZombieVillagerRenderer.class)
public abstract class ZombieVillagerTalkingRenderStateMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/monster/zombie/ZombieVillager;"
                    + "Lnet/minecraft/client/renderer/entity/state/ZombieVillagerRenderState;F)V",
            at = @At("TAIL"))
    private void villager_voices$copyTalkingState(
            ZombieVillager villager, ZombieVillagerRenderState state, float partialTick, CallbackInfo ci) {
        boolean talking = ClientTalkingState.instance().isTalking(villager.getUUID(), villager.level().getGameTime());
        ((FabricRenderState) (Object) state).setData(TalkingRenderState.IS_TALKING, talking);
    }
}
