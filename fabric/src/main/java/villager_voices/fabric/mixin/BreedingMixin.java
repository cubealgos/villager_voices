package villager_voices.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import villager_voices.VillagerReactionEvent;
import villager_voices.fabric.events.Signals;
import villager_voices.fabric.events.TradeAndSocialEvents;

/**
 * {@code breeding} (docs/spec/domains/reaction.md §3): injects
 * {@link Villager#getBreedOffspring(ServerLevel, AgeableMob)} at TAIL, filtered to a non-null
 * return (the vanilla caller, {@code VillagerMakeLove.breed}, treats {@code null} as "breeding did
 * not happen" and never spawns a child). Publishes a {@code BREEDING} signal for {@code this} (the
 * villager the method was called on) and, when the other parent is itself a {@link Villager} (the
 * only case vanilla ever calls this for — {@code VillagerMakeLove} requires both parents to be
 * villagers), for the {@code partner} too: both villagers experienced the breeding, not just one.
 *
 * <p><b>Not {@code Animal.spawnChildFromBreeding}, and not the ticket's Approach-section-named
 * native {@code BabyEntitySpawnEvent}</b> — see {@link TradeAndSocialEvents}'s class Javadoc and
 * the ticket's Findings for the full reasoning. Two independent errors in the ticket's own
 * Approach text, both caught against the jar rather than assumed: (1) {@code Villager} does not
 * extend {@code Animal} — both extend {@code AgeableMob} directly as siblings, confirmed by
 * {@code javap} on both classes' superclass, so {@code Animal.spawnChildFromBreeding} is never
 * called for a villager at all, mixin or not; (2) {@code BabyEntitySpawnEvent} does not exist in
 * Fabric API. Villager breeding's real call path, found by grepping the merged jar's classes for
 * bytecode references to {@code Villager.getBreedOffspring} (the one villager-specific breeding
 * method that exists): {@code net.minecraft.world.entity.ai.behavior.VillagerMakeLove} (a Brain
 * behavior, not a goal) calls its own private {@code breed(ServerLevel, Villager, Villager)},
 * which calls this method to construct the child, then ages both parents and spawns the child
 * itself — this mixin's target is the one public, villager-specific step in that path.
 *
 * <p>Mixin target confirmed by {@code javap -p}: {@code Villager.getBreedOffspring(ServerLevel,
 * AgeableMob)}, public, package {@code net.minecraft.world.entity.npc.villager}. The method name
 * is overloaded with a synthetic covariant-return bridge (also named {@code getBreedOffspring},
 * returning the supertype {@code AgeableMob}) — the full descriptor below pins the injection to
 * the real, {@code Villager}-returning method, never the bridge.
 */
@Mixin(Villager.class)
public abstract class BreedingMixin {

    @Inject(
            method = "getBreedOffspring(Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/world/entity/AgeableMob;)"
                    + "Lnet/minecraft/world/entity/npc/villager/Villager;",
            at = @At("TAIL"))
    private void villager_voices$onGetBreedOffspring(
            ServerLevel level, AgeableMob partner, CallbackInfoReturnable<Villager> cir) {
        if (cir.getReturnValue() == null) {
            return;
        }
        Villager self = (Villager) (Object) this;
        TradeAndSocialEvents.publish(Signals.of(self, VillagerReactionEvent.BREEDING));
        if (partner instanceof Villager partnerVillager) {
            TradeAndSocialEvents.publish(Signals.of(partnerVillager, VillagerReactionEvent.BREEDING));
        }
    }
}
