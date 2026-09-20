package villager_voices.fabric.compat.mixin;

import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import villager_voices.fabric.compat.ClientTalkingState;
import villager_voices.fabric.compat.EmfCompat;
import villager_voices.fabric.compat.TalkingRenderState;

/**
 * Copies the talking-state flag into {@link VillagerRenderState}'s own per-frame snapshot via
 * Fabric API's render-state side channel (docs/spec/domains/compat.md {@code COMPAT-REQ-003},
 * {@code ARCH-DEC-004}: never a custom {@code EntityRenderState} subclass) — confirmed the correct
 * 26.2 mechanism by direct source read of {@code fabric-rendering-v1}'s own
 * {@code RenderStateMixin} (mixed into {@code EntityRenderState} itself, so every subclass,
 * {@link VillagerRenderState} included, already implements {@link FabricRenderState}; research
 * §D4's own worked example, {@code PigRendererMixin}, is the identical pattern for a different
 * entity).
 *
 * <p>Targets the {@code Villager}-specific {@code extractRenderState} overload by full method
 * descriptor ({@code javap -p} against {@code minecraft-merged-deobf-26.2.jar}):
 * {@link VillagerRenderer} declares four overloads of {@code extractRenderState} (one per ancestor
 * class it narrows the generic parameters of), so an unqualified method name is ambiguous.
 *
 * <p>Lives in its own {@code villager_voices.fabric.compat.mixin} subpackage, not directly in
 * {@code villager_voices.fabric.compat} alongside {@link TalkingRenderState}/
 * {@link ClientTalkingState}/{@link EmfCompat}: Sponge Mixin reserves a config's own {@code
 * "package"} exclusively for its declared mixin classes and refuses to load any other class from
 * that same package ({@code IllegalClassLoadError}, this ticket's own Findings) — mirroring
 * {@code villager_voices.fabric.mixin}'s own existing convention (mixin-only package, one level
 * up from every collaborator it touches).
 *
 * @see ZombieVillagerTalkingRenderStateMixin the same pattern for a zombified villager
 * @see EmfCompat the reader half — never touched unless EMF is installed
 */
@Mixin(VillagerRenderer.class)
public abstract class VillagerTalkingRenderStateMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/npc/villager/Villager;"
                    + "Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;F)V",
            at = @At("TAIL"))
    private void villager_voices$copyTalkingState(
            Villager villager, VillagerRenderState state, float partialTick, CallbackInfo ci) {
        boolean talking = ClientTalkingState.instance().isTalking(villager.getUUID(), villager.level().getGameTime());
        ((FabricRenderState) (Object) state).setData(TalkingRenderState.IS_TALKING, talking);
    }
}
