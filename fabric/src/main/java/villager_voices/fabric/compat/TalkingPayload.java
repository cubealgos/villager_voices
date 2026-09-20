package villager_voices.fabric.compat;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import villager_voices.fabric.VillagerVoicesFabric;

import java.util.UUID;

/**
 * {@code villager_voices:talking} — VV-12's own S2C sync (docs/spec/domains/compat.md
 * {@code COMPAT-REQ-002}/{@code COMPAT-REQ-003}): the server side of a played reaction line's
 * talking-state mark, sent once per line per player in range (never per tick), carrying {@code
 * villagerId} (this codebase's own identity scheme throughout, {@link villager_voices.LineSink#show}
 * — not a raw entity network id, deliberately, this ticket's own Findings) and {@code ticks}, the
 * duration from the moment the client applies it (not an absolute server tick, since the two clocks
 * are not synchronised) for {@link ClientTalkingState}'s own {@code startTalking} call.
 *
 * <p>Registered against {@code PayloadTypeRegistry.clientboundPlay()} once, from
 * {@link VillagerVoicesFabric#onInitialize()} — common-side, since both the client and an
 * integrated server's own client run that entrypoint, matching every other Fabric mod's own
 * 1.20.5+ custom-payload convention.
 */
public record TalkingPayload(UUID villagerId, int ticks) implements CustomPacketPayload {

    public static final Type<TalkingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerVoicesFabric.MOD_ID, "talking"));

    public static final StreamCodec<ByteBuf, TalkingPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, TalkingPayload::villagerId,
            ByteBufCodecs.VAR_INT, TalkingPayload::ticks,
            TalkingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
