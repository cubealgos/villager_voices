/**
 * Reads and writes {@code config/villager_voices.json} (docs/spec/contracts/data-contract.md
 * {@code DATA-REQ-002}) — the one file-system-touching piece {@code villager_voices.config}
 * (the pure model and codec) cannot itself be, since a config directory is a loader concept
 * ({@code ARCH-DEC-001}).
 */
package villager_voices.fabric.config;
