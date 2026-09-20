---
schema_version: 1
id: 01M2Z2QJ5D684MKQJJJDTWC4CG
key: VV-16
type: chore
title: The poll stress test's 5 ms bound is flaky under parallel load
created_by: kevin
created_at: 2026-09-20T09:35:35Z
---

## Scope

`PolledEventsStressGameTest` asserts the poll's cost delta stays under 5 ms per tick for 40 villagers; under parallel load on the build machine it read 5.3 ms and failed an unrelated merge. A 5 ms bound is a machine-timing assertion, not a correctness one. Keep the measurement logged, raise the assertion to a genuine budget breach (25 ms per tick, half a server tick) so only a real regression trips it.

## Approach

One constant and its comment in the test.

## Acceptance criteria

- [x] The stress test logs the measured delta and asserts only against 25 ms per tick.
- [x] Merged through a Forgejo pull request into `development`.

## Constraints and prior findings

VV-6's Findings: measured 0.7 to 2.4 ms per tick in isolation.
