---
schema_version: 1
id: 01M2YYSAEZ99VFAQ2953CY4SN8
key: M4
title: Release
status: backlog
created_at: 2026-09-20T08:26:38Z
---

## Goal

Publish the first alpha on Modrinth: "I would want to release that already so I am the first Java
port of this idea; we need to be fast" (Kevin, `docs/spec/decisions/DEC-005-alpha-scope.md`).

## Scope

The Modrinth icon (an open design question at bootstrap), the real listing body, slug confirmation,
and the first tagged release build via `just release`, version `0.1.0-alpha.1+26.2-fabric`
(`docs/spec/operations/release.md` `REL-DEC-001`).

## Exit criteria

- The icon exists and matches this mod's own identity.
- The listing body is written and reviewed against the no-reference-product rule
  (`docs/spec/decisions/DEC-009-positioning.md`).
- The Modrinth slug is confirmed with Kevin before publishing.
- `just release` produces a jar, its SHA-256, and release notes naming the Minecraft version,
  loader, loader version, and default settings.

## Tickets

`VV-15`.

## Depends on

`M1` (a working alpha build is the thing being released). Deliberately does not depend on `M2` or
`M3` — the alpha releases on Fabric 26.2 with placeholder audio; NeoForge, 1.21.1, and real audio
ship in later releases of their own.
