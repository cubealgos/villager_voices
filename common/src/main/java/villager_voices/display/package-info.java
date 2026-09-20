/**
 * The action-bar display channel: the per-player {@link villager_voices.display.DisplayQueue},
 * its {@link villager_voices.display.DisplayLine} input, and the pure
 * {@link villager_voices.display.DisplayFormat} (docs/spec/domains/display.md). Pure Java, zero
 * Minecraft, Fabric, or NeoForge imports, ever ({@code common}'s own {@code verifyLoaderFree}
 * task, docs/spec/04-architecture.md ARCH-DEC-001) — the fabric module's push shim
 * ({@code villager_voices.fabric.display}) adapts this package's output to
 * {@code ServerPlayer#sendOverlayMessage}.
 */
package villager_voices.display;
