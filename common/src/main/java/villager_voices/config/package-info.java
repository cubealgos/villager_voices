/**
 * The one config file this mod writes (docs/spec/contracts/data-contract.md {@code DATA-REQ-002}):
 * a pure Java model ({@link villager_voices.config.Config}) and its parser/serialiser
 * ({@link villager_voices.config.ConfigCodec}), built on {@link villager_voices.catalogue.MiniJson}
 * rather than a new dependency. Carries no file-system access of its own — reading and writing
 * {@code config/villager_voices.json} is the fabric module's {@code ConfigLoader} (VV-8), since a
 * config directory is a loader concept this package cannot resolve (ARCH-DEC-001).
 */
package villager_voices.config;
