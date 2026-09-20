package villager_voices.fabric;

import villager_voices.VillagerEventBus;
import villager_voices.VillagerEventSource;
import villager_voices.fabric.events.TradeAndSocialEvents;
import villager_voices.fabric.events.CombatAndStateEvents;
import villager_voices.fabric.events.PolledEvents;

/**
 * Wires Fabric's native events and mixin targets (docs/spec/04-architecture.md's loader-adapter
 * table) into the shared {@link VillagerEventBus}. Each event-wiring ticket registers its own
 * events here in one line: {@link TradeAndSocialEvents} (VV-4, trade and social) is the first;
 * VV-5 (combat/state) and VV-6 (poll-based) add their own alongside it.

/**
 * Wires Fabric's native events and mixin targets (docs/spec/04-architecture.md's loader-adapter
 * table) into the shared {@link VillagerEventBus}. Each event-wiring ticket adds its own
 * registration line here, calling into its own class under {@code villager_voices.fabric.events}.

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
        TradeAndSocialEvents.register(bus);
        PolledEvents.register(bus);
        CombatAndStateEvents.register(bus);
    }
}
