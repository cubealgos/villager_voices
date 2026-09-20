---
title: "villager_voices spec — context: why, for whom, and what it will not do"
type: "spec"
category: "villager_voices"
---

# 00 — Context

## Why this exists

Kevin's idea, verbatim: "There is one more mod I'd love to build: for Bedrock there is a new
Villager News add-on; I want to build the same thing as a mod for Fabric/NeoForge/Forge across as
many versions as possible; I would want to name the Modrinth title 'Wait, they talk now?'; I would
love for it to be compatible with EMF and ETF and Fresh Animations so they also get animated
properly" (`rulings-2026-09-20.md`). Unlike the four Create Fly add-ons, this is not a
`create_civilization` building block: it is a standalone mod for any Minecraft world, Create
installed or not, and it is multi-loader and multi-version from day one rather than pinned to one
Create fork.

## Who it is for

- Players who want their villages to feel alive: villagers audibly and visibly react to trades,
  danger, breeding, and the rest of `domains/reaction.md`'s 16-event catalogue, instead of staying
  silent props.
- Server operators who install it and expect it to work with no configuration, with a config file
  available for anyone who wants to retune cooldowns or mute a category.
- Resource-pack and animation-pack authors — especially a Fresh-Animations-style pack once one adds
  a villager mouth bone — who want a supported, non-invasive hook to animate a "talking" villager
  without this mod touching the renderer or model (`domains/compat.md`).
- Datapack authors who want to retune or extend the line catalogue through JSON
  (`domains/reaction.md` `REACTION-DEC-001`).

## Business context

No business model, no revenue, no telemetry. Published on Modrinth under MIT, source on the
cubealgos Forgejo with a GitHub mirror and tracker, public from the first commit
(`decisions/DEC-003-licence.md`).

## Positioning: described on its own terms

**Decided by Kevin, 2026-09-20**: the listing describes the mod on its own terms — "villagers
react to what happens to them, with voices and text" — with **no mention of Villager News, Element
Animation, Bedrock, or any add-on anywhere in the mod, listing, or repository**; original audio and
writing only (`rulings-2026-09-20.md`, `decisions/DEC-009-positioning.md`). This is a firmer rule
than the first research pass's own finding that Modrinth's rules (§1.3, §2/§1.7) would tolerate
naming Villager News as "inspiration" if worded carefully
(`vault/technical/minecraft/villager-events-sounds-and-emf-compat.md` §E1–§E2) — Kevin's ruling
goes further than the minimum the platform rules require, not because the platform demands it, but
because it is also newly a live commercial product from the original IP holder as of 2026-09-08,
twelve days before this research (§E2). The mod's own description, README, and code comments never
name it.

## What it will not do

- No Villager News, Element Animation, or Bedrock branding, imagery, or audio anywhere: this mod's
  writing and voice work are original, from the line text (`domains/reaction.md`) through the audio
  pipeline (`domains/audio.md`).
- No renderer or model replacement: the villager looks exactly as vanilla, EMF, ETF, or Fresh
  Animations already render it; this mod only ever *adds* a feature layer and a render-state flag
  (`domains/compat.md` `COMPAT-REQ-001`).
- No recorded macOS System Voice output shipped, ever, at any tier: Apple's SLA bars public sharing
  of System Voice output, and a free published mod is public sharing regardless of profit
  (`vault/technical/minecraft/villager-events-sounds-and-emf-compat.md` §C; `domains/audio.md`
  `AUDIO-REQ-004`).
- No shared throughput cap or "annoyance" limiter beyond the cooldowns and rate limit this spec
  defines (`domains/reaction.md` §3): those exist to keep the action bar and audio usable, not as a
  balance mechanic — there is no in-game resource this mod gates.
- No in-world speech bubble, no NeoForge build, no 1.21.1 build, and no recorded/generated voice
  audio at the alpha: all four are real, scoped follow-ups, not cut features
  (`decisions/DEC-005-alpha-scope.md`, `contracts/platform-matrix.md`).
- No Forge 1.20.1 build in this wave (a later ruling, not part of this spec's scope).

## Success

**The alpha**: Kevin plays on Fabric 26.2, trades with a villager, hurts one, waits for a raid
bell — and sees short original text appear on the action bar every time, throttled sensibly, with a
near-silent sound and a real vanilla subtitle behind it, on a mod that visibly works the same day it
first builds — "we need to be fast," being the first Java port of this idea
(`rulings-2026-09-20.md`).

**1.0**: the same experience, now with real Piper-generated voice lines in Kevin's approved timbre
replacing the placeholders with no code change, on Fabric and NeoForge, 1.21.1 and 26.2, and with a
Fresh-Animations-style pack (once one exists) able to move the villager's mouth through the
published EMF variable — all without this mod ever having touched the villager renderer.
