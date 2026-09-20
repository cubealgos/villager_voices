---
schema_version: 1
id: 01M2YYTQV9YN7X9KPYSEFG1759
key: VV-13
type: feat
title: "Debug command: force-trigger an event on a targeted villager"
created_by: kevin
created_at: 2026-09-20T08:27:25Z
---

## Scope

`docs/spec/operations/testing.md`'s "Development tool" row: a `/villager_voices debug trigger
<event> <target>` command that forces a specific one of the 16 events on a targeted villager,
bypassing cooldowns — "genuinely useful here since several of the 16 events (raid, zombification,
breeding) are slow or awkward to trigger manually during testing." Registered under
`villager_voices.debug`, available via `just client`.

## Approach

A brigadier command taking an event-name argument (validated against `VillagerReactionEvent`'s 16
values, `VV-1`) and a targeted-entity selector, constructing a `VillagerReactionSignal` directly and
publishing it to the bus — bypassing `VV-2`'s cooldown/rate-limit checks deliberately (the whole
point is forcing an otherwise-throttled event), but still running selection and display normally so
the command is a faithful end-to-end test of the real pipeline, not a separate code path.

## Acceptance criteria

- [x] `/villager_voices debug trigger <event> <target>` fires the named event on the targeted
      villager immediately, ignoring cooldowns, for all 16 event names.
- [x] The forced event still goes through real selection (`VV-2`) and real display (`VV-7`) — not a
      hardcoded test line.
- [x] An invalid event name is rejected with a clear command-syntax error, not a silent no-op or
      crash.
- [x] The command is available in a `just client` session without additional setup.

## Constraints and prior findings

Blocked by `VV-2` (the bus this command publishes directly into). This is a development tool, not
public surface (`contracts/public-surface.md` names no debug command as stable API) — no
compatibility guarantee across versions is implied by this ticket.
