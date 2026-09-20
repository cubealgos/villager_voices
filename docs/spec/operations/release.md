---
title: "villager_voices spec — release engineering and distribution"
type: "spec"
category: "villager_voices"
---

# Release engineering, distribution and support (`REL`, sheet §7)

| Item | Position |
|---|---|
| Version scheme | `<semver>[-alpha.N]+<mc>-<loader>` — e.g. the alpha's first build is `0.1.0-alpha.1+26.2-fabric`; the first full 1.0 release across all four combinations is `1.0.0+26.2-fabric`, `1.0.0+26.2-neoforge`, `1.0.0+1.21.1-fabric`, `1.0.0+1.21.1-neoforge`. Both `<mc>` **and** `<loader>` are encoded, unlike `create_synthetic_diamonds`' `<mod>+<mc>` scheme, because here a fabric jar and a neoforge jar for the *same* Minecraft version are two different artifacts, not one build with two loader tags (`REL-DEC-001`). |
| Branches | gitkontor's: `development`, `production`; releases are tags on `production` |
| Channels | Modrinth only; CurseForge deferred |
| CI | `just check` on every merge: lint, unit tests, `common`-module package-purity check, game tests per shipped combination (`contracts/platform-matrix.md`), by the Woodpecker file, live from the first push since the repo is public on Forgejo from the bootstrap, GitHub mirror carrying the public issue tracker |
| Always a playable build | `just client` boots on the alpha's combination (Fabric 26.2) at every merge, and a reaction visibly appears on the action bar within a few seconds of trading with, or otherwise triggering, a villager |
| Support | Issue tracker only; no SLA; a `SUPPORT.md` says so |
| Ports | A new Minecraft version or loader is an additive Stonecutter node plus loader module (`04-architecture.md` `UC-009`), not a rewrite; the event-hook table (`domains/reaction.md` §3) is re-verified against the new jar on each port, since several rows are flagged inferred rather than `javap`-confirmed |

## Modrinth publish matrix

Following `vault/technical/minecraft/multi-loader-multi-version-mods-2026.md` §D's reading of
`standards/marketing/modrinth/modrinth-publish.py`: **one `cmd_version` call uploads exactly one
jar**, and a fabric jar can never claim `Loaders: neoforge` regardless of the tag, so a full
four-combination release needs **four separate invocations**, one per jar — the identical pattern
the research already worked out for this mod's own matrix (research §D, written against this
project before the rename). A `Game versions` array may legitimately span more than one tag only
within the same Java/toolchain generation (e.g. `1.21` and `1.21.1` together); never across the
Java-21/Java-25 boundary and never across loaders.

`REL-REQ-001`: every release jar is built by `just release` from a clean checkout at a tag.
`REL-REQ-002`: the release notes list the Minecraft version, loader, loader version, and (once
applicable) EMF/ETF/Fresh-Animations versions tested for that specific jar.
`REL-REQ-003`: the release notes state the default cooldowns, categories, and display settings in
force (`contracts/public-surface.md`).
`REL-REQ-004`: every release's Modrinth listing and `NOTICE` shall disclose that voice audio is
AI-generated (Piper TTS), per `operations/compliance.md`'s AI-content-disclosure row, from the first
release that ships real audio onward — not required for the alpha's near-silent placeholders, which
are not AI-generated content.

## Decisions

- `REL-DEC-001` — **`<semver>[-alpha.N]+<mc>-<loader>`, not `create_synthetic_diamonds`'
  `<mod>+<mc>`.** This sheet's own call, justified by the loader/version matrix being genuinely
  two-dimensional here (four real combinations, not one mod pinned to one Create fork across
  versions). **Cost if wrong**: a version-string format change before the first tagged release is
  free; after one exists, it is a documentation note in release history, not a functional break.
