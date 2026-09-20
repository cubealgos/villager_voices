# villager_voices

Villagers react to what happens to them, with voices and text: a multi-loader, multi-version
Minecraft mod (Fabric and NeoForge, 1.21.1 and 26.2). A `common`/`fabric`/`neoforge` module split
with a loader-free `common` (the JEI shape); the alpha ships the reaction system on Fabric 26.2
only, before any recorded audio.

**This file routes. It does not hold content.** The specification is `docs/spec/`.

## Read this before you do that

| about to… | read first |
|---|---|
| anything at all | `docs/spec/README.md`, then the one domain file you need |
| find where something lives | `docs/map.md`; generated, never edited |
| touch `common` | `docs/spec/04-architecture.md` `ARCH-DEC-001`: zero Minecraft, Fabric or NeoForge imports, ever, enforced by the build's `verifyLoaderFree` task |
| add or change an event trigger, mixin target, cooldown, or silence rule | `docs/spec/domains/reaction.md` |
| touch the line catalogue or a datapack override | `docs/spec/domains/reaction-lines.md` |
| touch the action bar, the per-player display queue, or a future speech bubble | `docs/spec/domains/display.md` |
| touch sound registration, subtitles, or the Piper pipeline | `docs/spec/domains/audio.md` |
| touch EMF, ETF, or Fresh Animations integration, or the render-state side channel | `docs/spec/domains/compat.md`, `docs/spec/04-architecture.md` `ARCH-DEC-004` (never replace the villager renderer or model) |
| add a loader or Minecraft version | `docs/spec/contracts/platform-matrix.md`, `docs/spec/04-architecture.md` `ARCH-DEC-002` |
| change the config or save format | `docs/spec/contracts/data-contract.md` |
| add a dependency | `docs/spec/decisions/DEC-003-licence.md` (MIT) and heimathafen's dependency policy |
| name, describe, or write anything player-facing | the compliance rule below |
| commit | scope `villager_voices`, the ticket key (`VV-N`) in the subject |

## Compliance rule

The mod is described entirely on its own terms: "villagers react to what happens to them, with
voices and text." No other product — game, mod, or brand — is ever named anywhere in the mod, its
listing, its code comments, or this repository's history
(`docs/spec/decisions/DEC-009-positioning.md`, `docs/spec/operations/compliance.md`
`COMP-REQ-002`). All shipped line text and audio are original, written or generated for this
project, never third-party content (`docs/spec/decisions/DEC-003-licence.md`).

## Working here

```
kontor claim VV-N
kontor branch new VV-N <slug>
just check
```

`just --list` shows the task surface; `just spec-sync` refreshes `docs/spec/` from the vault; `just map` regenerates the map.

## Standing rules

- The spec is authoritative; `docs/spec/` is a copy of heimathafen's vault.
- A design question the spec does not answer is asked, never decided inline.
- The mod makes no network call of its own, at build time in shipped code or at runtime
  (`docs/spec/operations/compliance.md` `COMP-REQ-001`).
- Always keep a playable build: `just client` boots the alpha's own combination (Fabric 26.2) at
  every merge.
