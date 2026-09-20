package villager_voices.fabric.sound;

import java.util.Map;

/**
 * How long each vanilla villager grunt {@code SoundEvent} this mod actually uses takes to play, in
 * ticks (20/s, rounded up) -- how long {@link ReactionSoundPlayer#play} delays a line's own sound
 * after its grunt (docs/spec/domains/audio.md {@code AUDIO-REQ-007}, {@code AUDIO-DEC-005}).
 *
 * <p>Measured once, by hand, on 2026-09-20 against 26.2's own shipped clips (never copied into this
 * repo, {@code COMP-REQ-002}): the client's asset index at
 * {@code ~/.gradle/caches/fabric-loom/assets/indexes/26.2-32.json} maps each
 * {@code minecraft/sounds/mob/villager/<clip>.ogg} to a hash, the file itself sits at
 * {@code objects/<first two hex>/<hash>}, and each candidate clip's length was read with
 * {@code soxi -D} (seconds). Vanilla's own {@code sounds.json} pools several clips per event and
 * picks one at random at play time, so this table uses the <em>longest</em> clip in each event's
 * own pool -- the delay must cover every clip the grunt could have played, not just the shortest.
 * {@code entity.villager.celebrate} has no clip files of its own; vanilla's {@code sounds.json}
 * reuses the {@code yes} pool for it, so this table measures that pool for both.
 *
 * <table>
 *   <caption>Measured clip lengths (seconds) and the derived tick count</caption>
 *   <tr><th>Grunt event</th><th>Clip pool</th><th>Longest clip (s)</th><th>Ticks (ceil x20)</th></tr>
 *   <tr><td>{@code entity.villager.ambient}</td><td>idle1-3</td><td>0.971814 (idle2)</td><td>20</td></tr>
 *   <tr><td>{@code entity.villager.trade}</td><td>haggle1-3</td><td>0.422290 (haggle1)</td><td>9</td></tr>
 *   <tr><td>{@code entity.villager.hurt}</td><td>hit1-4</td><td>0.300794 (hit1)</td><td>7</td></tr>
 *   <tr><td>{@code entity.villager.death}</td><td>death</td><td>0.644853</td><td>13</td></tr>
 *   <tr><td>{@code entity.villager.celebrate}</td><td>yes1-3 (reused)</td><td>0.850340 (yes2)</td><td>18</td></tr>
 * </table>
 *
 * <p>An event this mod names as a {@code grunt} but that isn't in this table (a datapack override
 * naming e.g. {@code minecraft:entity.villager.no} or a {@code work_<profession>} event, neither of
 * which any shipped 1.0 line uses) falls back to {@value #FALLBACK_TICKS} ticks, a conservative
 * middle-of-the-road estimate rather than a hard failure -- a missing table entry should never block
 * a line from playing (docs/spec/domains/audio.md {@code AUDIO-REQ-007}'s own "never fatal" rule for
 * grunts, applied here too).
 */
public final class GruntLengths {

    private static final long FALLBACK_TICKS = 10;

    private static final Map<String, Long> TICKS = Map.of(
        "minecraft:entity.villager.ambient", 20L,
        "minecraft:entity.villager.trade", 9L,
        "minecraft:entity.villager.hurt", 7L,
        "minecraft:entity.villager.death", 13L,
        "minecraft:entity.villager.celebrate", 18L);

    private GruntLengths() {
    }

    /** {@code gruntId}'s measured length in ticks, or {@value #FALLBACK_TICKS} if not in the table. */
    public static long ticksFor(String gruntId) {
        return TICKS.getOrDefault(gruntId, FALLBACK_TICKS);
    }
}
