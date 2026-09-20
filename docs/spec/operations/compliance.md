---
title: "villager_voices spec — compliance, security and governance"
type: "spec"
category: "villager_voices"
---

# Compliance, security and governance (`COMP`, sheet §6)

| Area | Position |
|---|---|
| GDPR: what leaves the user's machine | Nothing. No telemetry, no update check, no outbound network call of any kind (`COMP-REQ-001`) — including the Piper pipeline, which is a build-time-only local tool never invoked at runtime (`domains/audio.md` `AUDIO-REQ-005`). |
| Hosted parts we run | None. Modrinth hosts the file and its page. |
| Impressumspflicht | Attaches to a public web presence; there is none beyond the platform pages. Revisit if a site exists. |
| Licence and notices | MIT (`decisions/DEC-003-licence.md`); `NOTICE` credits Fabric API (Apache-2.0) and NeoForge (its own licence not verified by the research this spec is built on — **to verify at the first ticket**, before the NeoForge module lands). **All 64 shipped lines' audio and text are original, generated or performed by the project, never third-party content** (`domains/reaction-lines.md` `LINES-REQ-002`, `domains/audio.md` `AUDIO-REQ-004`). |
| Original content only — no Villager News, Element Animation, or Bedrock content | Enforced by writing discipline (`decisions/DEC-009-positioning.md`), not by a mechanical check: no line's text, no audio sample, and no description text is ever copied, translated, or paraphrased from Element Animation's Villager News or Oreville Studios' Villager News Bedrock add-on. |
| AI-content disclosure | Modrinth §6.1/§6.2 requires disclosure if a shipped asset is AI-generated or AI-assisted (research §E1). **Piper TTS output is AI-generated audio** — the listing and `NOTICE` shall disclose this plainly (`REL-REQ-004`), regardless of how the mod itself is marketed. |
| Supply chain and release integrity | Builds from a tagged commit with pinned dependencies; the release checksum is in the release notes; no signing at 1.0. |
| Vulnerability disclosure | The public issue tracker only, on the GitHub mirror; no private channel, no e-mail address published. Forgejo stays the source of truth for code. |
| Server trust boundary | Event detection, cooldowns, selection, and display are entirely server-side (`04-architecture.md` "Runtime topology"); no client packet or input this mod trusts for any of it. |
| AI Act, GoBD, sector regulation | The Piper-generated audio is a build artifact, not a live AI feature reachable by an end user — no AI Act "system" obligations attach the way they would for a runtime AI feature; not a financial-records or regulated-sector product. |

`COMP-REQ-001`: the mod shall make no network call of its own, at build time in shipped code or at
runtime; a source-scan test asserts it, following the same discipline the Create Fly siblings use.
`COMP-REQ-002`: the mod's own description, README, code comments, and commit history shall never
name Villager News, Element Animation, or Bedrock (`decisions/DEC-009-positioning.md`).

Open: release signing (minisign) before 1.0 or after.
