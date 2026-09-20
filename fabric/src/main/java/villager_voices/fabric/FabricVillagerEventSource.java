package villager_voices.fabric;

import villager_voices.VillagerEventBus;
import villager_voices.VillagerEventSource;

/**
 * Wires Fabric's native events and mixin targets (docs/spec/04-architecture.md's loader-adapter
 * table) into the shared {@link VillagerEventBus}. Empty until the event-wiring tickets land; this
 * bootstrap ticket (VV-1) only proves the {@code ServiceLoader} discovery path end to end.
 */
public final class FabricVillagerEventSource implements VillagerEventSource {
    @Override
    public void register(VillagerEventBus bus) {
    }
}
