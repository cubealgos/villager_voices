package villager_voices.fabric.catalogue;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import villager_voices.catalogue.Catalogue;
import villager_voices.catalogue.CatalogueLoadException;
import villager_voices.fabric.VillagerVoicesFabric;
import villager_voices.fabric.sound.SoundRegistration;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

/**
 * Loads {@code data/villager_voices/reaction/<event>.json} through Minecraft's own resource/datapack
 * system into a {@link Catalogue} (docs/spec/domains/reaction.md "Data format" §3), the thin
 * per-loader shim `common`'s own codec deliberately leaves out. Registered against Fabric's
 * resource loader API in {@link VillagerVoicesFabric#onInitialize()}, so it runs once at server
 * start and again on every {@code /reload}.
 *
 * <p>Override semantics ({@code REACTION-REQ-011}): {@link ResourceManager#listResources} already
 * resolves exactly one {@link Resource} per {@link Identifier} -- the pack with the highest
 * priority for that id, vanilla's own per-id resolution -- so loading a later, higher-priority
 * pack's file for an event id already replaces the earlier one entirely by the time this class ever
 * sees it; no merge logic lives here.
 *
 * <p>The sound-id registry check ({@code REACTION-REQ-012}) is {@link SoundRegistration#exists},
 * a real {@code BuiltInRegistries.SOUND_EVENT} lookup (VV-8) — VV-3's original note above described
 * a permissive placeholder here, before {@link SoundRegistration#registerAll()} existed to run
 * first, from {@link VillagerVoicesFabric#onInitialize()}, and register all 64 ids before this
 * listener's first {@code prepare()} ever runs. {@code common}'s own codec and its rejection path
 * are already fully exercised with a fake predicate in {@code common/src/test}.
 */
public final class CatalogueReloadListener extends SimplePreparableReloadListener<Catalogue> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerVoicesFabric.MOD_ID);
    private static final String DIRECTORY = "reaction";
    private static final String EXTENSION = ".json";
    private static final Identifier ID = Identifier.fromNamespaceAndPath(VillagerVoicesFabric.MOD_ID, "reaction_catalogue");

    private static volatile Catalogue current = Catalogue.builder().build();

    public static Identifier id() {
        return ID;
    }

    /** The most recently loaded catalogue; empty until the first reload completes. */
    public static Catalogue current() {
        return current;
    }

    @Override
    protected Catalogue prepare(ResourceManager manager, ProfilerFiller profiler) {
        Catalogue.Builder builder = Catalogue.builder();
        Map<Identifier, Resource> resources = manager.listResources(DIRECTORY,
            resourceId -> resourceId.getNamespace().equals(VillagerVoicesFabric.MOD_ID) && resourceId.getPath().endsWith(EXTENSION));

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier resourceId = entry.getKey();
            String eventId = eventIdOf(resourceId);
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                String json = reader.lines().reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
                builder.load(eventId, json, SoundRegistration::exists);
            } catch (IOException e) {
                throw new CatalogueLoadException("failed to read " + resourceId + ": " + e.getMessage());
            }
        }
        return builder.build();
    }

    @Override
    protected void apply(Catalogue catalogue, ResourceManager manager, ProfilerFiller profiler) {
        current = catalogue;
        LOGGER.info("villager_voices: loaded the reaction line catalogue for {} event(s)", catalogue.events().size());
    }

    @Override
    public String getName() {
        return "villager_voices/reaction_catalogue";
    }

    private static String eventIdOf(Identifier resourceId) {
        String path = resourceId.getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        return fileName.substring(0, fileName.length() - EXTENSION.length());
    }
}
