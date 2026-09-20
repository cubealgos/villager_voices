package villager_voices.fabric;

import villager_voices.VillagerEventBus;
import villager_voices.VillagerEventSource;
import villager_voices.fabric.events.CombatAndStateEvents;

/**
 * Wires Fabric's native events and mixin targets (docs/spec/04-architecture.md's loader-adapter
 * table) into the shared {@link VillagerEventBus}. Each event-wiring ticket adds its own class
 * under {@code villager_voices.fabric.events} and one registration line here (VV-1 only proved
 * the {@code ServiceLoader} discovery path end to end; this class fills in as the wiring tickets
 * land).
 */
public final class FabricVillagerEventSource implements VillagerEventSource {
    @Override
    public void register(VillagerEventBus bus) {
        CombatAndStateEvents.register(bus);
    }
}
