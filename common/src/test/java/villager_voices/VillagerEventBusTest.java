package villager_voices;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VillagerEventBusTest {

    @Test
    void publishReachesEverySubscriber() {
        VillagerEventBus bus = new VillagerEventBus();
        List<VillagerReactionSignal> received = new ArrayList<>();
        bus.subscribe(received::add);

        VillagerReactionSignal signal = new VillagerReactionSignal(UUID.randomUUID(), VillagerReactionEvent.TRADE_COMPLETED);
        bus.publish(signal);

        assertEquals(List.of(signal), received);
    }

    @Test
    void discoverSourcesReturnsEmptyListWithNoneRegistered() {
        // common carries no ServiceLoader provider of its own; only loader modules do.
        assertEquals(List.of(), VillagerEventBus.discoverSources());
    }
}
