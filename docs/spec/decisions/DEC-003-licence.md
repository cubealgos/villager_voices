---
title: "villager_voices DEC-003 — MIT, public under cubealgos from the first commit"
type: "spec"
category: "villager_voices"
---

# `DEC-003` — MIT, public under cubealgos from the first commit

**Status:** decided by Kevin, 2026-09-20.

MIT, no CLA (`rulings-2026-09-20.md`); `NOTICE` credits Fabric API and, once its own module lands,
NeoForge (licence to verify at the first ticket, `operations/compliance.md`). All 64 shipped lines'
text and audio are original, not third-party content requiring their own notice
(`domains/reaction-lines.md` `LINES-REQ-002`). Public under the `cubealgos` organisation on Forgejo
from the first commit, mirrored to GitHub with the issue tracker there, gitkontor's file-based
ticket workflow, ticket prefix `VV`, spec copied into the repository once bootstrapped.

This diverges from two heimathafen defaults, the same two the Create Fly siblings already
established: `standards/legal/dependency-license-policy.md`'s Apache-2.0-plus-CLA default, and "no
remote until justified" (every repo starts local-only). This project inherits the divergence rather
than deciding it fresh, the same reasoning `create_synthetic_diamonds`' `DEC-003` gives for its own
identical choice. Cost if wrong: MIT and a public remote are hard to walk back once someone has
forked.
