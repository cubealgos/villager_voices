/**
 * The data-driven line catalogue (docs/spec/domains/reaction-lines.md, `REACTION-DEC-001`): a pure
 * Java codec for {@code data/villager_voices/reaction/<event>.json}, keyed by event id string (e.g.
 * {@code "trade_completed"}) rather than any loader-specific event enum, so this package does not
 * depend on VV-2's {@code ReactionEvent}/{@code LineCatalogue} types. Zero Minecraft, Fabric, or
 * NeoForge imports (docs/spec/04-architecture.md `ARCH-DEC-001`); reading the JSON off disk through
 * Minecraft's resource/datapack system is {@code fabric}'s {@code villager_voices.fabric.catalogue}
 * package, not this one's.
 */
package villager_voices.catalogue;
