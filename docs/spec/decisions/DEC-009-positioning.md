---
title: "villager_voices DEC-009 — Positioning: described on its own terms, no Villager News/Element Animation/Bedrock mention anywhere"
type: "spec"
category: "villager_voices"
---

# `DEC-009` — Positioning: described on its own terms, no mention anywhere

**Status:** decided by Kevin, 2026-09-20, resolving the flag the research's first pass raised rather
than resolved.

The first research pass found that Modrinth's own rules (§1.3 IP non-infringement, §2/§1.7 honest
non-misleading description) would likely tolerate the mod's listing naming Villager News as
inspiration, worded carefully — and separately flagged that Element Animation's own IP holder had,
twelve days before that research, released an official, paid, voiced "Villager News 1.0 Add-On" on
the Bedrock Marketplace, a fact that "needs a ruling, not a silent decision"
(`vault/technical/minecraft/villager-events-sounds-and-emf-compat.md` §E2).

**Kevin's ruling goes further than the platform minimum**: "the listing describes the mod on its own
terms ('villagers react to what happens to them, with voices and text'); no mention of Villager
News, Element Animation, Bedrock or any add-on anywhere in the mod, listing or repository; original
audio and writing only" (`rulings-2026-09-20.md`). This is a stricter bar than Modrinth's rules
require — not because the platform demands it, but as a deliberate choice given that the reference
product is now itself a live commercial release from the original IP holder, not just a decade-old
web series.

**Enforcement is discipline, not a mechanical gate** (`operations/compliance.md` `COMP-REQ-002`): no
automated check can verify "no third party is ever named," so this is a writing-review norm applied
to the description, README, code comments, and commit messages, the same way originality of the
line text and audio (`domains/reaction-lines.md` `LINES-REQ-002`, `domains/audio.md`
`AUDIO-REQ-004`) is a discipline rather than a linter rule.

Alternative considered: naming Villager News as inspiration with careful, non-endorsement-implying
wording, as the research's own first pass suggested might be safe. Not chosen: Kevin's ruling
supersedes it outright, for reasons beyond what the platform rules alone would require. Cost if
wrong: none identified — omitting a reference costs nothing functionally; the mod's own concept
(event-triggered villager reactions with voice and text) stands on its own description regardless of
what it is or isn't compared to.
