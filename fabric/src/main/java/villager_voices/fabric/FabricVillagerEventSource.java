package villager_voices.fabric;

import villager_voices.VillagerEventBus;
import villager_voices.VillagerEventSource;
import villager_voices.fabric.events.TradeAndSocialEvents;

/**
 * Wires Fabric's native events and mixin targets (docs/spec/04-architecture.md's loader-adapter
 * table) into the shared {@link VillagerEventBus}. Each event-wiring ticket registers its own
 * events here in one line: {@link TradeAndSocialEvents} (VV-4, trade and social) is the first;
 * VV-5 (combat/state) and VV-6 (poll-based) add their own alongside it.
 */
public final class FabricVillagerEventSource implements VillagerEventSource {
    @Override
    public void register(VillagerEventBus bus) {
        TradeAndSocialEvents.register(bus);
    }
}
