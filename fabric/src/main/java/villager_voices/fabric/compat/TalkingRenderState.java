package villager_voices.fabric.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

/**
 * The one {@link RenderStateDataKey} VV-12 copies the talking-state flag through (docs/spec/domains/
 * compat.md {@code COMPAT-REQ-003}, {@code ARCH-DEC-004}: the render-state side channel, never a
 * custom {@code EntityRenderState} subclass) — written by
 * {@link VillagerTalkingRenderStateMixin}/{@link ZombieVillagerTalkingRenderStateMixin} at
 * {@code extractRenderState}'s own TAIL, read back by {@link EmfCompat}'s registered variable
 * factory from that same per-frame render-state instance. Client-only: {@link RenderStateDataKey}
 * and {@link net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState} are both
 * {@code fabric-rendering-v1} client API, never present on a dedicated server's own classpath.
 */
@Environment(EnvType.CLIENT)
public final class TalkingRenderState {

    public static final RenderStateDataKey<Boolean> IS_TALKING =
            RenderStateDataKey.create(() -> "villager_voices:is_talking");

    private TalkingRenderState() {
    }
}
