---
schema_version: 1
id: 01M2YYTQK71HY1YY97BRM3Z36E
key: VV-9
type: chore
title: NeoForge 26.2 module
created_by: kevin
created_at: 2026-09-20T08:27:25Z
---

## Scope

The first of `ARCH-DEC-002`'s "additive fast-follows": a real `neoforge` Gradle subproject
alongside `common`/`fabric` (`04-architecture.md` "Shape"), ModDevGradle `2.0.147`, NeoForge
`26.2.0.88`, Java 25 (`contracts/platform-matrix.md`). Implements `VillagerEventSource` for
NeoForge's own hook set (`platform-matrix.md`'s mixin table: `trade_completed`, `level_up`,
`restock`, `sleep`, `wake`, `raid_bell`, `baby_grows` as mixins; native events elsewhere per
`reaction.md` §3's NeoForge column), sound registration via `DeferredRegister<SoundEvent>`
(`audio.md` §3), and the render-state side channel via `RegisterRenderStateModifiersEvent`/
`ContextKey` once `VV-12` (compat) needs it — this ticket only needs the module to exist, build,
and reach event/display/audio parity with `fabric` for 26.2; it does not need to precede `VV-12`.
`VV-1`'s own bootstrap deliberately deferred this module (`docs/spec/decisions/DEC-005-alpha-scope.md`,
`ARCH-DEC-002`): "zero new toolchain downloads for the alpha... NeoForge MDG... uncached, several
hundred MB of first-run downloads" — this ticket is where those downloads happen.

## Approach

Mirror `fabric`'s module shape (`build.gradle.kts`, entrypoints, `META-INF/services` wiring,
`neoforge.mods.toml`) against NeoForge's own APIs instead of Fabric's, reusing every `common` class
unchanged — "the fast-follow order is 'add `fabric/`'s NeoForge sibling... around the existing
`common/` code', not a rewrite" (`ARCH-DEC-002`). One new CI job for this combination
(`platform-matrix.md` "CI matrix"), never a rebuild of the Fabric job.

## Acceptance criteria

- [ ] `neoforge` builds and passes its own game tests for the same 16 events `fabric` already
      covers (`VV-4`–`VV-6`'s Fabric-side equivalents, ported).
- [ ] `just check` (or its NeoForge-specific equivalent) is green for this combination without
      touching the Fabric job (`PLATFORM-REQ-003`: a NeoForge job failing never blocks a Fabric
      release and vice versa).
- [ ] `common` remains unmodified except for genuinely shared additions — `verifyLoaderFree` still
      passes.
- [ ] NeoForge's own licence is verified for `NOTICE` (`operations/compliance.md`: "NeoForge...
      licence not verified by the research this spec is built on — to verify at the first ticket,
      before the NeoForge module lands"). This is that ticket.

## Constraints and prior findings

Blocked by `VV-4`, `VV-5`, `VV-6`, `VV-7`, `VV-8` (the Fabric alpha this module mirrors must be
stable first). `ARCH-FAIL-002`'s stated fallback (per-version-per-loader physical subprojects, the
EMF/ETF pattern) applies if Stonecutter's node wiring (`VV-10`) doesn't fit cleanly — not expected
to affect this ticket, which has no Stonecutter dependency of its own. NeoForge `26.2.0.88`
artifacts were confirmed to exist on `maven.neoforged.net` before `VV-1`'s bootstrap (live check,
2026-09-20) — no availability risk, only the download-size/time cost `DEC-005` deferred.
