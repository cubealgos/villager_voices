---
title: "villager_voices DEC-002 — repo and mod id villager_voices, title Wait, they talk now?, slug conflict flagged"
type: "spec"
category: "villager_voices"
---

# `DEC-002` — repo and mod id `villager_voices`, title "Wait, they talk now?", slug conflict flagged

**Status:** decided by Kevin, 2026-09-20, with one part — the Modrinth slug — verified false and
carried forward as an open question rather than silently substituted.

Repo `villager_voices`, mod id `villager_voices` (renamed from the working `they_talk_now`), title
"Wait, they talk now?" (`rulings-2026-09-20.md`). The project's own vault folder was `git mv`'d from
`vault/projects/they_talk_now/` to `vault/projects/villager_voices/` to match, in the same change
that wrote this spec.

## The proposed slug is taken

Kevin's ruling names Modrinth slug `villager-voices`, "verify free with the publish tool's check if
possible; else mark to verify." Checked directly against the Modrinth API, 2026-09-20 (HTTP 200 =
taken): **`villager-voices` is taken** — by "Villager voices" (project id `nh55LKSo`), an unrelated
resource pack published 2023-11-01 (MIT, 12.6k downloads) that replaces existing villager sounds
with AI-voiced versions of them. Different project type (resource pack, not mod) and different
mechanism (replacing existing sounds, not new event-triggered lines), but the identical slug text —
so it cannot be used as given.

**Checked as alternatives, all confirmed free (HTTP 404)**: `villager-voices-mod`,
`wait-they-talk-now`, `they-talk-now-mod`, `villager-voices-reactions`, `talking-villagers-mod`,
`villagers-react`, `wait-they-talk-now-mod`.

**Not decided here**: which alternative to use. `villager-voices-mod` is this sheet's own proposal
— closest to the ruling's intent, unambiguous about being a mod rather than a resource pack — but
this is exactly the shape of open question `docs/spec/README.md`-style process asks be raised, not
resolved inline (`README.md` "Open questions gathered"). The repo name and mod id (`villager_voices`,
underscored, a different namespace from the hyphenated Modrinth slug either way) are unaffected by
this and stay as ruled.

## No domain, no icon decided here

No domain registered, no six-TLD availability check run: there is no standalone web presence
planned. The icon is explicitly a proposal, not decided (`rulings-2026-09-20.md`'s own "not yet
decided" note) — flagged in `README.md` "Open questions gathered", not designed in this pass.

**Ruled 2026-09-20 (Kevin):** the Modrinth slug is `wait-they-talk-now` (free, matches the title); `villager-voices-mod` is not used.
