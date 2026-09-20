package villager_voices.fabric.compat;

import net.minecraft.server.level.ServerPlayer;

/**
 * The one seam {@link TalkingStateSync} sends a {@link TalkingPayload} through — production wires
 * {@code ServerPlayNetworking::send} (its own default constructor); a game test substitutes a
 * capturing implementation instead, since a {@code GameTestHelper}'s mock players are not real
 * network-connected clients a sent payload could otherwise be observed arriving at (this ticket's
 * own "capture through a seam" test note).
 */
@FunctionalInterface
public interface TalkingPayloadSender {

    void send(ServerPlayer player, TalkingPayload payload);
}
