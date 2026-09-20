package villager_voices.fabric.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * The client entrypoint. The render-state side channel and EMF variable (docs/spec/domains/
 * compat.md) are fast-follow work, not this bootstrap ticket; nothing registers here yet.
 */
public final class VillagerVoicesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
    }
}
