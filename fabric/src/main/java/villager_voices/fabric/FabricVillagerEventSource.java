package villager_voices.fabric;

import villager_voices.VillagerEventBus;
import villager_voices.VillagerEventSource;
import villager_voices.fabric.events.PolledEvents;

/**
 * Wires Fabric's native events and mixin targets (docs/spec/04-architecture.md's loader-adapter
 * table) into the shared {@link VillagerEventBus}. Each event-wiring ticket adds its own
 * registration line here, calling into its own class under {@code villager_voices.fabric.events}.
 */
public final class FabricVillagerEventSource implements VillagerEventSource {
    @Override
    public void register(VillagerEventBus bus) {
        PolledEvents.register(bus);
    }
}
