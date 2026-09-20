package villager_voices.fabric.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import villager_voices.compat.TalkingState;

/**
 * The client's own synced {@link TalkingState} instance — the same class the fabric server module
 * holds its own authoritative instance of (docs/spec/domains/compat.md {@code COMPAT-REQ-002}'s own
 * Javadoc), kept current by {@link TalkingClientNetworking}'s {@link TalkingPayload} receiver and
 * read once per frame by {@link VillagerTalkingRenderStateMixin}/
 * {@link ZombieVillagerTalkingRenderStateMixin}. One instance for the client's whole session —
 * matching {@code villager_voices.fabric.VillagerVoicesFabric}'s own static-holder pattern for
 * {@code displayQueue}/{@code eventBus}, except this one never needs rebuilding on server (re)start,
 * since it carries no server-sized configuration of its own.
 */
@Environment(EnvType.CLIENT)
public final class ClientTalkingState {

    private static final TalkingState INSTANCE = new TalkingState();

    private ClientTalkingState() {
    }

    public static TalkingState instance() {
        return INSTANCE;
    }
}
