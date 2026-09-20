---
schema_version: 1
id: 01M2YYTQZA1YKJXRTFBVPYPAW9
key: VV-15
type: chore
title: "Modrinth release prep: icon, listing body, first alpha release"
created_by: kevin
created_at: 2026-09-20T08:27:25Z
---

## Scope

The alpha's actual first release, per `docs/spec/operations/release.md`: version scheme
`<semver>[-alpha.N]+<mc>-<loader>`, first build `0.1.0-alpha.1+26.2-fabric` (`REL-DEC-001`). Three
things `VV-1`'s bootstrap deliberately left as placeholders: the Modrinth icon (an open design
question, `docs/spec/README.md` "Open questions gathered" — speech-bubble or subtitle-caret motif
over a villager silhouette proposed, not designed), the Modrinth listing body
(`docs/modrinth/body.md`, currently a settings table only), and confirming the provisional slug
`villager-voices-mod` (`decisions/DEC-002-name.md`: the proposed `villager-voices` slug is taken by
an unrelated resource pack). Release notes state the Minecraft version, loader, loader version, and
default cooldowns/categories/display settings (`REL-REQ-002`, `REL-REQ-003`); the listing and
`NOTICE` disclose Piper-generated audio as AI-generated if `VV-11` has landed by this point
(`REL-REQ-004`) — if not, the alpha ships with placeholder audio and no disclosure is yet due.

## Approach

Design and render the icon (a `just icon` generator or a by-hand asset, per `VV-1`'s justfile note
that no generator exists yet); write the actual listing body replacing `docs/modrinth/body.md`'s
placeholder, staying within `decisions/DEC-009-positioning.md`'s positioning rule (described on its
own terms, no other product named anywhere); confirm the final slug with Kevin before publishing
(`DEC-002-name.md`'s divergence is still "not yet confirmed"); run `just release` from a clean
checkout at a tag (`REL-REQ-001`).

## Acceptance criteria

- [ ] The Modrinth icon exists at `docs/modrinth/icon.png` (or wherever `just icon` places it),
      matching this mod's own identity, distinct from the Create-family navy badge.
- [ ] `docs/modrinth/body.md` carries the real listing body, reviewed against `COMP-REQ-002`/
      `DEC-009-positioning.md` for no reference-product mention anywhere.
- [ ] The slug is confirmed with Kevin (either `villager-voices-mod` or a chosen alternative) before
      the listing is published.
- [ ] `just release` produces `dist/` with the jar, its SHA-256, and release notes naming the
      Minecraft version, loader, loader version, and default settings (`REL-REQ-001`–`003`).
- [ ] If `VV-11` (real audio) has landed, `NOTICE` and the listing disclose AI-generated audio
      (`REL-REQ-004`); if not, this criterion does not apply yet and is re-checked at the next
      release that does include real audio.

## Constraints and prior findings

Blocked by `VV-8` (a working alpha build to actually release) and `VV-3` (the catalogue whose
content the release notes may reference). This is the alpha release specifically — "I would want to
release that already so I am the first Java port of this idea; we need to be fast"
(`decisions/DEC-005-alpha-scope.md`, quoting Kevin) — do not block this ticket on `VV-9`–`VV-14`'s
fast-follow work, which ships in later releases of its own.
