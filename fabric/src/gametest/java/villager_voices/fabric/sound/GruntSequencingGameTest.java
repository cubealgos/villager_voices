package villager_voices.fabric.sound;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * VV-18 ({@code AUDIO-REQ-007}, {@code AUDIO-DEC-005}): {@link ReactionSoundPlayer#play}'s own
 * sequencing, tested through {@link TickScheduler} itself as the recording seam -- a line with a
 * grunt schedules its own sound for a later tick rather than playing it on the spot; a line without
 * a grunt plays immediately, with nothing scheduled. Deliberately not tested by listening (a
 * gametest server has no audio output to observe): what {@link TickScheduler#schedule} was or
 * wasn't called with is the whole of what "scheduled for a later tick" means here.
 */
public final class GruntSequencingGameTest {

    private static final String TRADE_SOUND_ID = "villager_voices:reaction.trade_completed.1";
    private static final String TRADE_GRUNT_ID = "minecraft:entity.villager.trade";

    @GameTest
    public void aLineWithAGruntSchedulesItsOwnSoundForALaterTickInsteadOfPlayingItImmediately(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 pos = Vec3.atCenterOf(new BlockPos(1, 1, 1));
        TickScheduler scheduler = new TickScheduler();
        long now = level.getServer().getTickCount();

        ReactionSoundPlayer.play(level, pos, TRADE_SOUND_ID, 0.4f, 1.0f, TRADE_GRUNT_ID, now, scheduler);

        helper.assertTrue(scheduler.pendingCount() == 1,
                "expected the line's own sound scheduled (not played immediately) when a grunt is present, found "
                        + scheduler.pendingCount() + " pending task(s)");

        long gruntTicks = GruntLengths.ticksFor(TRADE_GRUNT_ID);
        helper.assertTrue(gruntTicks > 0, "expected a positive measured grunt delay, found " + gruntTicks);

        // Not yet due: draining at `now` must not run it.
        scheduler.drain(now);
        helper.assertTrue(scheduler.pendingCount() == 1,
                "expected the scheduled task to still be pending before its due tick");

        // Due once the grunt's own measured length has passed: draining then must run and remove it.
        scheduler.drain(now + gruntTicks);
        helper.assertTrue(scheduler.pendingCount() == 0,
                "expected the scheduled task to have run and been removed once due");

        helper.succeed();
    }

    @GameTest
    public void aLineWithoutAGruntPlaysOnTheSameTickWithNothingScheduled(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 pos = Vec3.atCenterOf(new BlockPos(1, 1, 1));
        TickScheduler scheduler = new TickScheduler();
        long now = level.getServer().getTickCount();

        ReactionSoundPlayer.play(level, pos, TRADE_SOUND_ID, 0.4f, 1.0f, null, now, scheduler);

        helper.assertTrue(scheduler.pendingCount() == 0,
                "expected the line's own sound played immediately with nothing scheduled when there is no grunt, found "
                        + scheduler.pendingCount() + " pending task(s)");

        helper.succeed();
    }
}
