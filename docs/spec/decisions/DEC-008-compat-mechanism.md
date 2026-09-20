---
title: "villager_voices DEC-008 — Never replace the villager renderer or model; expose talking state via EMF's own API"
type: "spec"
category: "villager_voices"
---

# `DEC-008` — Never replace the villager renderer or model; expose talking state via EMF's own API

**Status:** decided by Kevin, 2026-09-20.

"I would love for it to be compatible with EMF and ETF and Fresh Animations so they also get
animated properly" (original ruling), resolved as: **never replace the villager renderer or model**;
expose a "talking" state through `EMFAnimationApi.registerUniqueAnimationVariableFactory` (and the
loader render-state side channels the research names) so Fresh Animations packs can move the mouth;
ship a reference animation snippet; ETF untouched (`rulings-2026-09-20.md`).

Full mechanism in `domains/compat.md`: EMF's own public API (`registerUniqueAnimationVariableFactory`,
confirmed to exist and be the intended external-mod path, research §D1) is the read side; each
loader's already-shipped render-state side channel (`FabricRenderState`/`RenderStateDataKey` on
Fabric 26.2, `RegisterRenderStateModifiersEvent`/`ContextKey` on NeoForge 26.2, an added
`FeatureRenderer`/render layer on both loaders' 1.21.1) is the write side, needed specifically
because 26.2 replaced live-entity rendering with an immutable per-frame snapshot that a feature
renderer cannot read the live entity from (research §D4). Neither loader nor either version ever
needs a custom `EntityRenderState` subclass.

Alternative considered (rejected outright, not weighed): a custom villager renderer or model replacing
Fresh Animations' or the vanilla one, to guarantee mouth movement without depending on any other
pack's cooperation. Rejected because it directly contradicts the ruling and would break every
texture/animation pack a player already has installed — the entire point of exposing a variable
instead is that this mod never needs to know or care what renders the villager. Cost if wrong: if
EMF's `registerUniqueAnimationVariableFactory` wiring does not work exactly as its (unverified)
per-entity-keying shape suggests, the fallback is a `SynchedEntityData` value read some other way by
a future EMF version or a dedicated compat shim — still never a renderer replacement
(`domains/compat.md` `COMPAT-FAIL-003`).
