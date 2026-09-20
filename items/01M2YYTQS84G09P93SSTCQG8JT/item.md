---
schema_version: 1
id: 01M2YYTQS84G09P93SSTCQG8JT
key: VV-12
type: feat
title: EMF, ETF, and Fresh Animations compatibility
created_by: kevin
created_at: 2026-09-20T08:27:25Z
---

## Scope

`docs/spec/domains/compat.md`: a server-authoritative per-villager talking-state boolean
(`COMPAT-REQ-002`), true for exactly the duration of a currently-playing reaction sound, copied into
each frame's render-state snapshot via the loader-appropriate side channel — never a custom
`EntityRenderState` subclass (`COMPAT-REQ-003`, `ARCH-DEC-004`: no renderer or model replacement,
ever, on any loader or version). Exposed to EMF as `villager_voices.is_talking` via
`registerUniqueAnimationVariableFactory`, a soft dependency absent with no error when EMF isn't
installed (`COMPAT-REQ-004`). ETF and Fresh Animations need no code of this mod's own — ETF is
textures-only with no shared hooks (`compat.md` §3 "ETF"), and any Fresh-Animations-style pack reads
the same EMF variable this ticket exposes. Ships a reference animation snippet in the repository,
not as a loaded game asset (`COMPAT-REQ-005`).

## Approach

Fabric 26.2: mixin at `extractRenderState` TAIL plus `FabricRenderState`/`RenderStateDataKey`
(`04-architecture.md` loader-adapter table). NeoForge 26.2: `RegisterRenderStateModifiersEvent`/
`ContextKey`, a native event — re-confirm its exact package/class names against raw NeoForge source
before wiring, since the original research only confirmed this from docs, not source
(`COMPAT-FAIL-002`). One flat boolean, not a richer per-line or per-category signal
(`COMPAT-DEC-002`) — do not over-build this past what EMF's own API takes.

## Acceptance criteria

- [ ] The talking-state flag is true for exactly the duration of a playing reaction sound and false
      otherwise, verified per loader (`COMPAT-REQ-002`).
- [ ] With EMF installed, `villager_voices.is_talking` reads true for the duration of a playing line
      via EMF's own debug/animation-variable inspector or a minimal test resource pack
      (`docs/spec/operations/testing.md` "Client checklist").
- [ ] With EMF absent, the flag is still set and copied into the render state but read by nothing —
      zero crash, zero behavioural difference otherwise (`COMPAT-FAIL-001`).
- [ ] With ETF installed, no crash, no texture change caused by this mod (`compat.md` §3).
- [ ] No renderer, model, or `EntityRenderState` subclass exists anywhere in this change
      (`COMPAT-REQ-001`, `ARCH-DEC-004`) — enforceable the same way `verifyLoaderFree` enforces
      `common`'s own boundary, if a comparable check is worth adding here.

## Constraints and prior findings

Blocked by `VV-7` (the DisplayQueue this flag's duration is derived from — a line is "playing" for
as long as it is displayed/audible). `COMPAT-FAIL-003`: a future EMF release changing
`registerUniqueAnimationVariableFactory`'s signature must fail the build at compile time against
EMF's API jar (`compileOnly`/soft dependency), never a silent runtime no-op. The talking-state flag
is explicitly transient, never persisted (`contracts/data-contract.md` "The talking-state flag is
transient, not persisted").
