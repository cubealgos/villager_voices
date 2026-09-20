package villager_voices.fabric.sound;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import villager_voices.VillagerReactionEvent;
import villager_voices.catalogue.MiniJson;
import villager_voices.fabric.VillagerVoicesFabric;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Registers one {@link SoundEvent} per catalogue line — 64 at 1.0, one per event id in
 * {@link VillagerReactionEvent} times its shipped line count — generated at mod init from the same
 * 16 {@code data/villager_voices/reaction/<event>.json} resource files
 * {@link villager_voices.fabric.catalogue.CatalogueReloadListener} loads through Minecraft's own
 * resource system, never hand-typed per line (docs/spec/domains/audio.md {@code AUDIO-REQ-001}).
 *
 * <p>Reads the bundled resources directly off the classpath, via
 * {@link Class#getResourceAsStream}, instead of waiting for that reload listener: registry
 * mutation ({@link Registry#registerForHolder}) is only valid during
 * {@link net.fabricmc.api.ModInitializer#onInitialize()}, well before the first datapack reload
 * runs — the same reason {@link villager_voices.fabric.catalogue.CatalogueReloadListener}'s own
 * {@code soundExists} check could only be a permissive placeholder before this ticket (VV-3's own
 * note). {@link #registerAll()} must run before that reload listener's first {@code prepare()},
 * both from {@link VillagerVoicesFabric#onInitialize()}.
 */
public final class SoundRegistration {

    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerVoicesFabric.MOD_ID);
    private static final String RESOURCE_ROOT = "/data/" + VillagerVoicesFabric.MOD_ID + "/reaction/";

    private static volatile Map<String, Holder<SoundEvent>> registered = Map.of();

    private SoundRegistration() {
    }

    /**
     * Registers all 64 {@link SoundEvent}s into {@link BuiltInRegistries#SOUND_EVENT}. Not
     * idempotent — a registry id can only be registered once — so call this exactly once, from
     * {@link net.fabricmc.api.ModInitializer#onInitialize()}.
     */
    public static void registerAll() {
        Map<String, Holder<SoundEvent>> byId = new LinkedHashMap<>();
        for (VillagerReactionEvent event : VillagerReactionEvent.values()) {
            String eventId = event.name().toLowerCase(Locale.ROOT);
            for (String soundId : soundIdsOf(eventId)) {
                Identifier id = Identifier.parse(soundId);
                Holder<SoundEvent> holder = Registry.registerForHolder(
                        BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
                byId.put(soundId, holder);
            }
        }
        registered = Map.copyOf(byId);
        LOGGER.info("villager_voices: registered {} reaction sound event(s)", registered.size());
    }

    /** Every sound id registered by {@link #registerAll()} — 64 once it has run. */
    public static Map<String, Holder<SoundEvent>> all() {
        return registered;
    }

    /** The registered {@link Holder} for {@code soundId}, once {@link #registerAll()} has run. */
    public static Optional<Holder<SoundEvent>> get(String soundId) {
        return Optional.ofNullable(registered.get(soundId));
    }

    /**
     * Whether {@code soundId} names an already-registered {@link SoundEvent} — the real check
     * {@link villager_voices.fabric.catalogue.CatalogueReloadListener} wires in place of its VV-3
     * placeholder ({@code REACTION-REQ-012}).
     */
    public static boolean exists(String soundId) {
        return registered.containsKey(soundId);
    }

    /** {@link #exists(String)} as a {@link Predicate}, {@code CatalogueCodec}'s own constructor shape. */
    public static Predicate<String> existsPredicate() {
        return SoundRegistration::exists;
    }

    @SuppressWarnings("unchecked")
    private static List<String> soundIdsOf(String eventId) {
        String resourcePath = RESOURCE_ROOT + eventId + ".json";
        try (InputStream in = SoundRegistration.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException(
                        "villager_voices: no bundled catalogue resource at " + resourcePath
                                + " to generate sound registrations from (AUDIO-FAIL-001)");
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Object root = MiniJson.parse(json);
            Map<String, Object> obj = (Map<String, Object>) root;
            List<Object> lines = (List<Object>) obj.get("lines");
            List<String> ids = new ArrayList<>();
            for (Object entry : lines) {
                Map<String, Object> lineObj = (Map<String, Object>) entry;
                ids.add((String) lineObj.get("sound"));
            }
            return List.copyOf(ids);
        } catch (IOException e) {
            throw new IllegalStateException("villager_voices: failed to read " + resourcePath + ": " + e.getMessage(), e);
        }
    }
}
