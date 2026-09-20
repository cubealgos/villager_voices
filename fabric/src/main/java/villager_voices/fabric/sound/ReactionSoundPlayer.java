package villager_voices.fabric.sound;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import villager_voices.fabric.VillagerVoicesFabric;

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
 */
public final class ReactionSoundPlayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerVoicesFabric.MOD_ID);

    private ReactionSoundPlayer() {
    }

    /**
     * @param level the sound's own level (the villager's level, not necessarily the overworld)
     * @param pos the position to broadcast from
     * @param soundId the sound id to look up in {@link SoundRegistration}
     * @param volume tuned by the caller (docs/spec/domains/audio.md §3: "0.3-0.6 for a close,
     *     conversational feel"), already scaled by {@code display.masterVolume}
     * @param pitch the sound's pitch, 1.0 for unmodified
     */
    public static void play(ServerLevel level, Vec3 pos, String soundId, float volume, float pitch) {
        SoundRegistration.get(soundId).ifPresentOrElse(
                holder -> level.playSeededSound(null, pos.x, pos.y, pos.z, holder, SoundSource.NEUTRAL,
                        volume, pitch, level.getRandom().nextLong()),
                () -> LOGGER.warn("villager_voices: no registered SoundEvent for {}, nothing played", soundId));
    }
}
