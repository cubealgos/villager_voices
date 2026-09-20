---
schema_version: 1
id: 01M2Z3D1P53VQ7M7PM83V92R3W
key: VV-17
type: chore
title: Modrinth slug wait-they-talk-now
created_by: kevin
created_at: 2026-09-20T09:47:19Z
---

## Scope

Kevin ruled the Modrinth slug: `wait-they-talk-now` (DEC-002 amended). Update `docs/modrinth/body.md`'s slug row and sync `docs/spec/`.

## Approach

`just spec-sync`, one row edit, the publish tool's `check` confirms the slug is free.

## Acceptance criteria

- [x] `body.md` slug is `wait-they-talk-now`; `docs/spec/decisions/DEC-002-name.md` synced.
- [x] Merged through a Forgejo pull request into `development`.

## Constraints and prior findings

VV-15's slug open question.
