---
schema_version: 1
id: 01M2YYTQXA39DMQKXXVAQXQR9X
key: VV-14
type: feat
title: "Speech bubble: in-world display channel"
created_by: kevin
created_at: 2026-09-20T08:27:25Z
---

## Scope

`docs/spec/domains/display.md` §3 "Speech bubble (deferred, not built)": an in-world text bubble
over the villager's head, via a client render hook per loader — explicitly named a real, scoped
follow-up, not alpha or 1.0 scope ("the action bar covers 'visibly working' for the first
releases"). A second display channel alongside the action bar (`VV-7`), not a replacement — both
independently toggleable, matching `DISPLAY-REQ-005`/`DISPLAY-REQ-006`'s existing independence
rules for the action bar and subtitle channels.

## Approach

A client-only render hook per loader (not a mixin into any shared render-state path
`ARCH-DEC-004` already governs for `VV-12`'s talking-state work — this is new, separate rendering,
not a reuse of the compat side channel) drawing short text above the villager model when a line is
selected for it, timed against the same `DisplayQueue` (`VV-7`) rather than a second independent
timer. Config toggle for on/off, consistent with `display.md` §3's config table shape.

## Acceptance criteria

- [ ] A selected line shows as an in-world bubble over the correct villager, timed consistently with
      the action bar's own display duration for that line.
- [ ] The bubble channel and the action bar channel are independently toggleable; disabling one does
      not affect the other (`DISPLAY-REQ-006`'s existing independence rule, extended to the bubble).
- [ ] No renderer, model, or `EntityRenderState` subclass is replaced or subclassed to build this
      (`ARCH-DEC-004`, `COMPAT-REQ-001`) — an added feature renderer or equivalent per-loader hook
      only.

## Constraints and prior findings

Blocked by `VV-7` (the `DisplayQueue` this channel's timing is driven by). Explicitly deferred past
the alpha and 1.0 by `display.md` §3 — do not pull this forward ahead of `VV-9`–`VV-13` without a
new decision recorded in the spec, since the existing decision record treats the action bar alone as
sufficient for "visibly working."
