/**
 * The Fabric-side push for {@code common}'s {@code villager_voices.display} package
 * (docs/spec/domains/display.md): {@link villager_voices.fabric.display.ActionBarDisplay} drains
 * the per-player {@code DisplayQueue} every server tick and calls
 * {@code ServerPlayer#sendOverlayMessage}.
 */
package villager_voices.fabric.display;
