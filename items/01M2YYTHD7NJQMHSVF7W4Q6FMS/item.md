---
schema_version: 1
id: 01M2YYTHD7NJQMHSVF7W4Q6FMS
key: VV-3
type: feat
title: "Line catalogue: data-driven JSON loading and the 64-line text catalogue"
created_by: kevin
created_at: 2026-09-20T08:27:18Z
---

## Scope

The datapack-overridable catalogue system `docs/spec/domains/reaction-lines.md` and
`docs/spec/domains/reaction.md` §3 describe: one `data/villager_voices/reaction/<event>.json` file
per event, 16 total, each holding 4 `{subtitle, sound}` entries per `reaction-lines.md` §2's JSON
shape. Ships the already-written 64-line 1.0 default catalogue verbatim from
`reaction-lines.md` §3 (`LINES-REQ-001`) — the text itself is not open, only the codec's exact field
names are (`reaction.md` §7). Loading, decoding, and datapack-override behaviour live in `common`
(no Minecraft imports); reading the JSON off disk via Minecraft's resource/datapack system is a thin
per-loader shim outside this ticket's `common`-side scope.

## Approach

A pure-Java codec in `common` for the JSON shape in `reaction-lines.md` §2, keyed by event and
line index, exposing eligible lines per event to `VV-2`'s selection logic. Reject (not silently
drop or crash) a catalogue entry naming a sound id that isn't registered, with a clear error naming
the missing id (`REACTION-REQ-012`) — this ticket can only test the codec's own rejection path with
a fake registry check, since real `SoundEvent` registration is `VV-8`'s territory; wire the real
check there. Ship `reaction-lines.md` §3's 64 lines as the default resource content, sound ids
following `villager_voices:reaction.<event>.<n>` exactly (`LINES-REQ-003`).

## Acceptance criteria

- [ ] All 16 `data/villager_voices/reaction/<event>.json` files exist with exactly the 4 lines each
      from `reaction-lines.md` §3, 64 lines total (`LINES-REQ-001`).
- [ ] A datapack replacing one catalogue file is used in place of the shipped default, decoded the
      same way (`REACTION-REQ-011`).
- [ ] A catalogue entry naming an unregistered sound id is rejected at load with a clear error
      naming the id, never silently dropped or a crash (`REACTION-REQ-012`, `REACTION-FAIL-003`).
- [ ] Every line's subtitle text matches its lang key's text and its `sound` id 1:1
      (`reaction-lines.md` §2, `LINES-REQ-003`).
- [ ] `common:verifyLoaderFree` still passes.

## Constraints and prior findings

The 64 lines are already fully drafted in `reaction-lines.md` §3 — "written now, not deferred"
(`decisions/DEC-005-alpha-scope.md`) — this ticket ships that exact text, not new writing.
`LINES-REQ-002`: every line is original, never a translation or paraphrase of any third-party mod,
add-on, or media's dialogue (`decisions/DEC-009-positioning.md`). Blocked by `VV-2` (the selection
logic this catalogue feeds).
