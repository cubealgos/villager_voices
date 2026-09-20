package villager_voices.fabric.sound;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import villager_voices.fabric.VillagerVoicesFabric;

import java.util.Optional;

/**
 * Plays one line's registered sound server-side (docs/spec/domains/audio.md §3 "Playing
 * server-side"), via {@code ServerLevel.playSeededSound(Entity, double, double, double,
 * Holder&lt;SoundEvent&gt;, SoundSource, float, float, long)} — the 26.2 signature confirmed by
 * {@code javap -p} against {@code minecraft-merged-deobf-26.2.jar}. A pure position-based
 * broadcast (the {@code Entity} parameter is {@code null}), not tied to one player's client-side
 * prediction, so every player the server itself judges in range hears it, independent of this
 * mod's own hearing-range filtering for the action-bar text.
 *
 * <p>{@code SoundSource.NEUTRAL} ("Friendly Creatures") matches existing villager ambience
 * (audio.md §3); {@code SoundSource.VOICE}'s vanilla purpose could not be confirmed by research and
 * is deliberately not used.
 *
 * <p>VV-18 ({@code AUDIO-REQ-007}, {@code AUDIO-DEC-005}): when a line names a {@code grunt} — a
 * vanilla villager {@code SoundEvent} id — {@link #play} plays that grunt immediately, at the same
 * position/source/volume and pitch 1.0, then schedules the line's own sound for
 * {@code now + GruntLengths.ticksFor(grunt)} on the caller-supplied {@link TickScheduler} instead of
 * playing it on the spot, so the two read as one utterance. A grunt that is {@code null}/blank, or
 * that does not resolve to a registered vanilla {@code SoundEvent} (logged as a warning — never
 * fatal, matching {@code AUDIO-REQ-007}'s "a line without a grunt plays as before"), falls back to
 * playing the line immediately, exactly as before this ticket.
 */
public final class ReactionSoundPlayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerVoicesFabric.MOD_ID);
    private static final float GRUNT_PITCH = 1.0f;

    private ReactionSoundPlayer() {
    }

    /**
     * @param level the sound's own level (the villager's level, not necessarily the overworld)
     * @param pos the position to broadcast from
     * @param soundId the line's own sound id to look up in {@link SoundRegistration}
     * @param volume tuned by the caller (docs/spec/domains/audio.md §3: "0.3-0.6 for a close,
     *     conversational feel"), already scaled by {@code display.masterVolume} — the grunt plays
     *     at this same volume ({@code AUDIO-REQ-007}: "the line's position, source and volume")
     * @param pitch the line's own sound's pitch, 1.0 for unmodified; the grunt always plays at 1.0
     * @param gruntId the line's optional grunt sound id ({@link villager_voices.catalogue.Line#grunt()}),
     *     or {@code null}/blank for no grunt
     * @param now the current server tick ({@code MinecraftServer#getTickCount()})
     * @param scheduler where the delayed line-sound task is queued when a grunt actually plays
     */
    public static void play(ServerLevel level, Vec3 pos, String soundId, float volume, float pitch,
            String gruntId, long now, TickScheduler scheduler) {
        Optional<Holder<SoundEvent>> gruntHolder = resolveVanillaSoundEvent(gruntId);
        if (gruntHolder.isEmpty()) {
            if (gruntId != null && !gruntId.isBlank()) {
                LOGGER.warn("villager_voices: grunt {} does not resolve to a registered SoundEvent, playing {} without it",
                        gruntId, soundId);
            }
            playNow(level, pos, soundId, volume, pitch);
            return;
        }

        level.playSeededSound(null, pos.x, pos.y, pos.z, gruntHolder.get(), SoundSource.NEUTRAL,
                volume, GRUNT_PITCH, level.getRandom().nextLong());
        long delayTicks = GruntLengths.ticksFor(gruntId);
        scheduler.schedule(now + delayTicks, () -> playNow(level, pos, soundId, volume, pitch));
    }

    /**
     * {@code gruntId}'s measured delay in ticks, or {@code 0} if it is {@code null}/blank or does
     * not currently resolve to a registered vanilla {@code SoundEvent} — the same check {@link
     * #play} itself makes, exposed so a caller (VV-12's {@code TalkingStateSync}, via {@code
     * FabricLineSink}) can extend the talking-state duration by exactly what will actually be
     * scheduled, never by a grunt that silently fell back to "no grunt."
     */
    public static long delayTicksFor(String gruntId) {
        if (resolveVanillaSoundEvent(gruntId).isEmpty()) {
            return 0;
        }
        return GruntLengths.ticksFor(gruntId);
    }

    private static void playNow(ServerLevel level, Vec3 pos, String soundId, float volume, float pitch) {
        SoundRegistration.get(soundId).ifPresentOrElse(
                holder -> level.playSeededSound(null, pos.x, pos.y, pos.z, holder, SoundSource.NEUTRAL,
                        volume, pitch, level.getRandom().nextLong()),
                () -> LOGGER.warn("villager_voices: no registered SoundEvent for {}, nothing played", soundId));
    }

    private static Optional<Holder<SoundEvent>> resolveVanillaSoundEvent(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        Identifier identifier;
        try {
            identifier = Identifier.parse(id);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        return BuiltInRegistries.SOUND_EVENT.get(identifier).map(holder -> (Holder<SoundEvent>) holder);
    }
}
