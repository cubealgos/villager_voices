# Wait, they talk now?

Villagers react to what happens to them, with voices and text. A multi-loader, multi-version
Minecraft mod, described entirely on its own terms.

Mod id `villager_voices`. The alpha ships the reaction system — all 16 trigger events, cooldowns,
hearing range, and the action-bar display — on Fabric 26.2 only, with near-silent placeholder sound
events carrying real subtitle text (`docs/spec/decisions/DEC-005-alpha-scope.md`). NeoForge 26.2,
then 1.21.1 for both loaders, follow as additive fast-follows; real recorded audio replaces the
placeholders once the text catalogue and display path are proven
(`docs/spec/04-architecture.md`).

MIT (LICENSE); credits in NOTICE. All shipped writing and audio are original to this project.

Support and issues go through the issue tracker only:
https://github.com/cubealgos/villager_voices/issues.

Source: https://git.cubealgos.de/cubealgos/villager_voices (Forgejo, the home of this repository).
Mirror: https://github.com/cubealgos/villager_voices, read-only code, and the issue tracker.

Development: `just --list`. The specification is `docs/spec/`.
