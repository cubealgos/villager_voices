---
title: "villager_voices DEC-001 — Distributed product, full spec sheet"
type: "spec"
category: "villager_voices"
---

# `DEC-001` — Distributed product, full spec sheet

**Status:** decided by Kevin, 2026-09-20.

A new, standalone mod, not a `create_civilization` building block and not a Create Fly add-on: "I
want to build the same thing as a mod for Fabric/NeoForge/Forge across as many versions as
possible" (`rulings-2026-09-20.md`). It ships to real users on Modrinth from its very first alpha —
"I would want to release that already so I am the first Java port of this idea" — which if anything
raises the bar for a full spec sheet over the four Create Fly siblings: this mod has a larger public
surface (two loaders, two versions, an audio pipeline, a soft-dependency animation API) than any of
them, and Kevin's own stated urgency ("we need to be fast") is a reason to get the scope and rules
right once, not a reason to skip writing them down.

Alternative considered: treating the alpha as a quick prototype and deferring the full sheet until
after it ships. Rejected: the alpha is a real public release with real users from day one by design
(`decisions/DEC-005-alpha-scope.md`), and the mutual-exclusivity-style engineering risk here (16
event hooks, several inferred rather than confirmed, `domains/reaction.md` §7) is exactly the kind
of thing a spec sheet is for. Cost if wrong: an evening of spec for a mod whose first shippable
combination is genuinely small (one loader, one version, no audio yet).
