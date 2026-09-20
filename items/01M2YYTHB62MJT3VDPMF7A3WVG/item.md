---
schema_version: 1
id: 01M2YYTHB62MJT3VDPMF7A3WVG
key: VV-2
type: feat
title: "Reaction core: VillagerEventBus cooldown, rate-limit, silence rules, and selection"
created_by: kevin
created_at: 2026-09-20T08:27:18Z
---

## Scope

The pure logic in `common` that decides whether a detected event actually produces a line, and
which line: `docs/spec/domains/reaction.md` §3's cooldown/rate/silence rules and its selection
rule (`REACTION-REQ-005`–`010`). `VV-1`'s `VillagerEventBus` already dispatches
`VillagerReactionSignal`s to subscribers with no cooldown, selection, or silence logic at all —
this ticket adds that logic as the bus's actual behaviour, still with zero Minecraft imports
(`04-architecture.md` `ARCH-DEC-001`). Does not include any event's actual hook (`VV-4`–`VV-6`),
the catalogue itself (`VV-3`), or the config file that will later make cooldown values
operator-overridable (`VV-8`, `contracts/data-contract.md` `DATA-REQ-002`) — hardcode reasonable
defaults here.

## Approach

Extend `VillagerEventBus` (or a class it delegates to) with: a per-villager-per-event cooldown map,
a per-villager-global cooldown, a per-player server-wide rate limiter, and the two silence rules
(suppress every event but `sleep` while asleep, every event but `baby_grows` while a baby) —
`REACTION-REQ-006`–`010`. Add the selection rule: pick one line at random from an event's eligible
catalogue, excluding the line that played last for that villager on that event
(`REACTION-REQ-005`). All of this takes a clock and a random source as constructor/method
parameters (not `System.currentTimeMillis()`/`Math.random()` directly) so the fake-clock unit tests
`docs/spec/operations/testing.md` calls for are possible without a real server tick loop.

## Acceptance criteria

- [ ] A per-event and a per-villager-global cooldown suppress a detected event within their window
      (`REACTION-REQ-006`, `REACTION-REQ-007`), unit-tested with a fake clock.
- [ ] A server-wide per-player rate limit suppresses a line independent of how many villagers are
      nearby (`REACTION-REQ-008`), unit-tested.
- [ ] Every event but `sleep` is suppressed while the triggering villager is asleep, and every
      event but `baby_grows` is suppressed while it is a baby (`REACTION-REQ-009`, `REACTION-REQ-010`).
- [ ] Selection excludes the line that played last for that villager on that event
      (`REACTION-REQ-005`), unit-tested with a fixed random source.
- [ ] `common:verifyLoaderFree` still passes; no Minecraft/Fabric/NeoForge import anywhere in this
      change.

## Constraints and prior findings

Builds directly on `VV-1`'s `VillagerEventBus`/`VillagerEventSource`/`VillagerReactionEvent`/
`VillagerReactionSignal` (already committed, `common/src/main/java/villager_voices/`). Cooldown/
rate-limit default values are hardcoded here per `docs/spec/README.md`'s "Open questions gathered"
(60s/5s/1-per-2s proposed, not yet confirmed by Kevin) — `VV-8` wires them to the config file
without changing this ticket's logic. `docs/spec/operations/testing.md`: "the selection/cooldown/
queue logic is fully unit-testable in `common` given a fake clock and a fixed random source — no
Minecraft dependency once events, timestamps, and a roll value are inputs."
