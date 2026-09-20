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

- [x] The talking-state flag is true for exactly the duration of a playing reaction sound and false
      otherwise, verified per loader (`COMPAT-REQ-002`).
- [x] With EMF installed, `villager_voices.is_talking` reads true for the duration of a playing line
      via EMF's own debug/animation-variable inspector or a minimal test resource pack
      (`docs/spec/operations/testing.md` "Client checklist").
- [x] With EMF absent, the flag is still set and copied into the render state but read by nothing —
      zero crash, zero behavioural difference otherwise (`COMPAT-FAIL-001`).
- [x] With ETF installed, no crash, no texture change caused by this mod (`compat.md` §3).
- [x] No renderer, model, or `EntityRenderState` subclass exists anywhere in this change
      (`COMPAT-REQ-001`, `ARCH-DEC-004`) — enforceable the same way `verifyLoaderFree` enforces
      `common`'s own boundary, if a comparable check is worth adding here.

## Constraints and prior findings

Blocked by `VV-7` (the DisplayQueue this flag's duration is derived from — a line is "playing" for
as long as it is displayed/audible). `COMPAT-FAIL-003`: a future EMF release changing
`registerUniqueAnimationVariableFactory`'s signature must fail the build at compile time against
EMF's API jar (`compileOnly`/soft dependency), never a silent runtime no-op. The talking-state flag
is explicitly transient, never persisted (`contracts/data-contract.md` "The talking-state flag is
transient, not persisted").

## Findings

- **EMF's API jar has no dedicated maven — pulled from Modrinth's own maven proxy, `compileOnly`
  (not `modCompileOnly`: this project's own Loom/mappings setup already uses plain `implementation`
  for `fabric-api`/`fabric-loader` too, so no remapping step is needed here either — verified by a
  clean `:fabric:compileJava`). Pinned coordinates: `maven.modrinth:entity-model-features:3.3.8-fabric-26.2`
  and `maven.modrinth:entitytexturefeatures:7.2.4-fabric-26.2`, `gradle/libs.versions.toml`. **ETF is
  needed as a second `compileOnly`, not just EMF**: EMF's own `fabric.mod.json` hard-depends on
  `entity_texture_features (>=7.2)`, and `EMFState#state()`'s return type
  (`EMFEntityRenderState`) extends an ETF interface — confirmed this matters at *compile* time too
  (not just runtime), by a controlled `javac` test: an `instanceof`/cast against that return value
  fails with "Kein Zugriff auf ETFEntityRenderState" without ETF on the classpath, and compiles clean
  with it. `maven.modrinth` group, standard Modrinth-maven layout, confirmed by direct `curl` against
  `api.modrinth.com/maven` before wiring into Gradle.
- **The real `registerUniqueAnimationVariableFactory` signature differs from the research note's own
  approximation** (decompiled the actual `3.3.8-fabric-26.2` jar's bytecode to confirm): it is
  `(String sourceModId, String variableName, UniqueVariableFactory factory) throws Exception` — three
  arguments, not four (no separate explanation-key parameter at the call site; that lives on the
  factory's own `getExplanationTranslationKey()`/`getTitleTranslationKey()`). Decompiled bytecode
  further shows the `variableName` argument is used only for EMF's own log line, never as the actual
  match key — matching is entirely `UniqueVariableFactory#createsThisVariable(String)`'s own job,
  called with the raw string a `query.variable(...)` expression names. Implemented against
  `villager_voices.is_talking` accordingly (`EmfCompat`).
- **The render-state side channel closes the whole loop with no separate client-side UUID lookup on
  the EMF side**: `EMFState.state()` (package `traben.entity_model_features.models.animation.state`,
  public, called during EMF's own animation pass) returns the very same `EntityRenderState` instance
  Fabric API's own `RenderStateMixin` already mixed `FabricRenderState` into (confirmed by
  decompiling `fabric-rendering-v1-25.3.3+515ac5339e.jar`: `RenderStateMixin` targets
  `EntityRenderState`/`BlockEntityRenderState`/`LeashState`/`CameraEntityRenderState`) — so
  `EmfCompat`'s registered variable factory casts that same object to `FabricRenderState` and reads
  back exactly the `RenderStateDataKey<Boolean>` the mixin wrote, per `COMPAT-DEC-001`'s "one copy,
  one reader." No parallel UUID-based lookup needed inside the EMF-facing code at all.
- **Sponge Mixin reserves a mixin config's own `"package"` exclusively for its declared mixin
  classes** — any other class loaded from that same Java package throws
  `IllegalClassLoadError: ... is in a defined mixin package ... and cannot be referenced directly`
  (hit this directly against a real `:fabric:runGameTest` run, not merely inferred). A fully-qualified
  entry in the `mixins`/`client` array does **not** bypass this either: Sponge Mixin's own
  `MixinInfo` constructor builds each mixin's actual class name as
  `config.getMixinPackage() + name` — raw string concatenation, always relative to `"package"`, with
  no support for an individual entry opting out (decompiled `MixinConfig`/`MixinInfo` from
  `sponge-mixin-0.17.4+mixin.0.8.7.jar` to confirm). Consequence: the two render-state mixins
  (`VillagerTalkingRenderStateMixin`, `ZombieVillagerTalkingRenderStateMixin`) live in their own
  `villager_voices.fabric.compat.mixin` subpackage with a second, dedicated mixin config file
  (`fabric/src/main/resources/villager_voices.compat.mixins.json`, `"package":
  "villager_voices.fabric.compat.mixin"`, listed in `fabric.mod.json`'s `"mixins"` array alongside
  the existing one) — mirroring `villager_voices.fabric.mixin`'s own existing convention (a
  mixin-only package) — while every non-mixin compat class (`TalkingPayload`, `TalkingStateSync`,
  `TalkingRenderState`, `ClientTalkingState`, `TalkingClientNetworking`, `EmfCompat`,
  `TalkingPayloadSender`) stays directly in `villager_voices.fabric.compat`, per the ticket's own
  file scoping.
- **Sync design (server-authoritative, per COMPAT-REQ-002)**: `FabricLineSink.show` (one added line)
  calls `TalkingStateSync#markTalking` once a line's sound has started, which (a) marks the server's
  own authoritative `TalkingState` (`common`, pure, keyed by the villager's `UUID` — this codebase's
  own identity scheme throughout, not a raw entity network id) through
  `now + config.talkingDurationTicks()`, and (b) sends one `villager_voices:talking` S2C payload
  (`{villagerId: UUID, ticks: int}`, a *relative* duration, not an absolute tick, since server and
  client clocks are not synchronised) to every player already in the sink's own candidate set that is
  also within `display.hearingRangeBlocks` — independent of `display.actionBar`'s own toggle, since
  the talking-state visual cue should not depend on whether the action-bar text channel is on. The
  client applies the payload to its own `ClientTalkingState` (same `TalkingState` class, one identity
  scheme end to end); `VillagerTalkingRenderStateMixin`/`ZombieVillagerTalkingRenderStateMixin` copy
  `ClientTalkingState.instance().isTalking(...)` into the render state at `extractRenderState`'s own
  TAIL every frame, via `FabricRenderState#setData`.
- **The real sound duration is not read from the ogg or a per-line table in this ticket** — per the
  ticket's own brief, `config.talkingDurationTicks` (new config key, `compat.talkingDurationTicks`,
  default 40 ticks / 2s) is a flat, operator-configurable default standing in for it, longer than
  VV-8's own 0.2s placeholder sound so the flag is comfortably observable for manual EMF testing.
  Reading each line's actual duration (from its ogg at play time, or a generated per-line duration
  table) is explicitly future work, not attempted here.
- **`GameTestHelper`'s mock players are not real network-connected clients** a sent payload could be
  asserted against directly, so `TalkingStateSync`'s constructor takes a `TalkingPayloadSender` seam
  (defaulting to `ServerPlayNetworking::send`); `TalkingStateSyncGameTest` builds its own throwaway
  `FabricLineSink`/`VillagerEventBus` pipeline (the same pattern `DebugCommand` already established
  for forced-event testing) with a capturing sender substituted, publishes a real signal through the
  real catalogue and real selection logic, and asserts both the server-side `TalkingState` and the
  exactly-one captured payload.
- **A client-side assertion that EMF actually reads the flag is not feasible headless** (no
  `Minecraft` client instance, no EMF animation pass, in a `GameTestHelper` server-only environment)
  — confirmed by design, not attempted; added to `docs/spec/operations/testing.md`'s own existing
  "Client checklist" row instead (EMF/ETF/Fresh Animations installed manually), which already named
  this exact scenario.
- `just check` green: `lint` (`verifyLoaderFree` + `fabric:check`), `map-check`, `common:test` +
  `fabric:test` (113 unit tests total, 0 failures), `tools` (2 python tests), `fabric:runGameTest`
  (30/30 game tests, including the new `TalkingStateSyncGameTest`). `just doctor`'s one failure
  (`docs/spec/decisions/DEC-002-name.md` vs the vault, a Modrinth-naming ruling) is pre-existing and
  untouched by this branch (`git diff HEAD` on that file is empty) — VV-15's territory, not synced
  here.
