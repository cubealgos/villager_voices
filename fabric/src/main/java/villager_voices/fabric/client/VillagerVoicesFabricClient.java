package villager_voices.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import villager_voices.fabric.compat.EmfCompat;
import villager_voices.fabric.compat.TalkingClientNetworking;
import villager_voices.fabric.compat.TalkingPayload;

/**
 * The client entrypoint. Registers {@link TalkingPayload}'s own receiver (VV-12, docs/spec/domains/
 * compat.md {@code COMPAT-REQ-002}) unconditionally, and EMF's own {@code is_talking} variable
 * ({@code COMPAT-REQ-004}) only once EMF is confirmed loaded — the mixin render-state side channel
 * itself ({@code COMPAT-REQ-003}) needs no registration of its own, {@code villager_voices.mixins.json}
 * already wires it.
 *
 * @see TalkingPayload
 * @see TalkingClientNetworking
 * @see EmfCompat
 */
public final class VillagerVoicesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TalkingClientNetworking.register();

        if (FabricLoader.getInstance().isModLoaded("entity_model_features")) {
            EmfCompat.register();
        }
    }
}
