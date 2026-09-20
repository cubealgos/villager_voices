package villager_voices.fabric.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * The client half of VV-12's sync (docs/spec/domains/compat.md {@code COMPAT-REQ-002}): receives
 * {@link TalkingPayload} and applies it to {@link ClientTalkingState#instance()}, converting the
 * payload's own relative {@code ticks} into an absolute {@code untilTick} against the client's own
 * clock ({@link ClientLevel#getGameTime()}) — the two servers' and clients' tick counters are not
 * synchronised, so only a relative duration crosses the wire, matching {@link TalkingPayload}'s own
 * Javadoc.
 *
 * <p>{@link #register()} is called once, from
 * {@link villager_voices.fabric.client.VillagerVoicesFabricClient#onInitializeClient()}.
 */
@Environment(EnvType.CLIENT)
public final class TalkingClientNetworking {

    private TalkingClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TalkingPayload.TYPE, (payload, context) ->
                context.client().execute(() -> apply(payload, context.client().level)));
    }

    private static void apply(TalkingPayload payload, ClientLevel level) {
        if (level == null) {
            return; // received between levels (e.g. respawn/disconnect race) -- nothing to mark against.
        }
        ClientTalkingState.instance().startTalking(payload.villagerId(), level.getGameTime() + payload.ticks());
    }
}
