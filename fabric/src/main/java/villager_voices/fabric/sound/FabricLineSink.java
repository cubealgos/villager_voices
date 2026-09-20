package villager_voices.fabric.sound;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import villager_voices.LineRef;
import villager_voices.LineSink;
import villager_voices.catalogue.Line;
import villager_voices.config.Config;
import villager_voices.display.DisplayLine;
import villager_voices.display.DisplayQueue;
import villager_voices.fabric.VillagerVoicesFabric;
import villager_voices.fabric.catalogue.CatalogueReloadListener;
import villager_voices.fabric.compat.TalkingStateSync;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * VV-2's {@link LineSink}, implemented: the adapter that closes the alpha loop. For a selected
 * line, enqueues its action-bar text (docs/spec/domains/display.md {@code DISPLAY-REQ-001}) for
 * every candidate player still within {@code display.hearingRangeBlocks}, using VV-7's
 * {@link villager_voices.display.DisplayFormat}-ready {@link DisplayLine} shape, and plays the
 * line's registered sound once at the villager's own position via {@link ReactionSoundPlayer}
 * (docs/spec/domains/audio.md §3).
 *
 * <p>{@code playerIds} arrives already filtered by the server-wide per-player rate limit
 * ({@code REACTION-REQ-008}, VV-2's own {@link LineSink} contract); narrowing it further to players
 * within hearing range, and actually online in the villager's own level, is this class's own job.
 *
 * <p>Also marks the villager talking for the sound's own duration and syncs that to nearby players
 * (VV-12, docs/spec/domains/compat.md {@code COMPAT-REQ-002}), via {@link TalkingStateSync} — kept
 * as a separate collaborator, not this class's own concern beyond the one call below.
 *
 * <p>VV-18 ({@code AUDIO-REQ-007}): a catalogue line may also carry a {@code grunt}, resolved here
 * from the same catalogue lookup as the subtitle text and handed to {@link ReactionSoundPlayer},
 * which plays it first and schedules the line's own sound after it
 * ({@link villager_voices.fabric.VillagerVoicesFabric#tickScheduler()}). The talking state's own
 * duration is extended by the same grunt delay ({@link ReactionSoundPlayer#delayTicksFor}), so the
 * EMF talking indicator spans grunt plus line, never just the line.
 */
public final class FabricLineSink implements LineSink {

    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerVoicesFabric.MOD_ID);

    /**
     * Base volume before {@code display.masterVolume} scales it (docs/spec/domains/audio.md §3:
     * "Volume tuned to 0.3-0.6 for a close, conversational feel rather than village-wide
     * audibility").
     */
    private static final float BASE_VOLUME = 0.4f;
    private static final float BASE_PITCH = 1.0f;

    private final MinecraftServer server;
    private final Config config;
    private final DisplayQueue displayQueue;
    private final TalkingStateSync talkingStateSync;

    public FabricLineSink(MinecraftServer server, Config config, DisplayQueue displayQueue,
            TalkingStateSync talkingStateSync) {
        this.server = server;
        this.config = config;
        this.displayQueue = displayQueue;
        this.talkingStateSync = talkingStateSync;
    }

    @Override
    public void show(UUID villagerId, Set<UUID> playerIds, LineRef line) {
        Entity entity = server.overworld().getEntityInAnyDimension(villagerId);
        if (entity == null) {
            return; // despawned/unloaded between selection and display: nothing to show or play.
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = entity.position();
        Line catalogueLine = catalogueLineOf(line);
        String text = catalogueLine != null ? catalogueLine.text() : line.id();
        String grunt = catalogueLine != null ? catalogueLine.grunt() : null;
        String speakerLabel = speakerLabelOf(entity);

        if (config.displayActionBar() && !playerIds.isEmpty()) {
            DisplayLine displayLine = new DisplayLine(speakerLabel, text, line.id());
            double rangeSq = config.hearingRangeBlocks() * config.hearingRangeBlocks();
            for (UUID playerId : playerIds) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player == null || player.level() != level) {
                    continue;
                }
                if (player.position().distanceToSqr(pos) > rangeSq) {
                    continue;
                }
                displayQueue.enqueue(playerId, displayLine);
            }
        }

        long now = server.getTickCount();
        ReactionSoundPlayer.play(level, pos, line.id(),
                (float) (BASE_VOLUME * config.displayMasterVolume()), BASE_PITCH,
                grunt, now, VillagerVoicesFabric.tickScheduler());

        long gruntDelayTicks = ReactionSoundPlayer.delayTicksFor(grunt);
        talkingStateSync.markTalking(server, level, villagerId, pos, playerIds, config, now, gruntDelayTicks);
    }

    /**
     * The line's own catalogue entry (subtitle text and optional grunt), looked up by sound id —
     * {@link LineRef} deliberately carries only the id (VV-2's own contract), not the text or the
     * grunt. {@code null} if no catalogue line matches (e.g. the catalogue reloaded between
     * selection and display).
     */
    private static Line catalogueLineOf(LineRef line) {
        String eventId = line.event().name().toLowerCase(Locale.ROOT);
        for (Line catalogueLine : CatalogueReloadListener.current().linesFor(eventId)) {
            if (catalogueLine.soundId().equals(line.id())) {
                return catalogueLine;
            }
        }
        LOGGER.warn("villager_voices: no catalogue line found for sound id {}, showing the id itself", line.id());
        return null;
    }

    /**
     * The villager's profession display text, or {@code null} for a villager with no profession
     * (nitwit, unemployed) or an entity that is no longer a {@link Villager} at all (e.g. it was
     * zombified between selection and display) — {@link villager_voices.display.DisplayFormat}
     * falls both back to {@code "Villager: <line>"} ({@code DISPLAY-REQ-004}).
     */
    private static String speakerLabelOf(Entity entity) {
        if (!(entity instanceof Villager villager)) {
            return null;
        }
        var profession = villager.getVillagerData().profession();
        if (profession.is(VillagerProfession.NONE)) {
            return null;
        }
        return profession.value().name().getString();
    }
}
