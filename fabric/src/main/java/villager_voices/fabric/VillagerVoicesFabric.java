package villager_voices.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import villager_voices.VillagerEventBus;
import villager_voices.VillagerEventSource;
import villager_voices.fabric.catalogue.CatalogueReloadListener;

/**
 * The mod's server-and-common entrypoint. Wires every {@link VillagerEventSource} discovered on
 * the classpath into a {@link VillagerEventBus}; the sources themselves land with the event
 * wiring tickets, not this bootstrap (docs/spec/domains/reaction.md). Also registers the line
 * catalogue's {@link CatalogueReloadListener} (VV-3, docs/spec/domains/reaction-lines.md), so it
 * loads once at server start and again on every {@code /reload}.
 */
public final class VillagerVoicesFabric implements ModInitializer {
    public static final String MOD_ID = "villager_voices";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        VillagerEventBus bus = new VillagerEventBus();
        for (VillagerEventSource source : VillagerEventBus.discoverSources()) {
            source.register(bus);
        }

        ResourceLoader.get(PackType.SERVER_DATA)
            .registerReloadListener(CatalogueReloadListener.id(), new CatalogueReloadListener());

        LOGGER.info("Wait, they talk now? ready");
    }
}
