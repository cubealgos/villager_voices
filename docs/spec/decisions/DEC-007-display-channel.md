---
title: "villager_voices DEC-007 — The action bar is the primary display channel, with a per-player queue"
type: "spec"
category: "villager_voices"
---

# `DEC-007` — The action bar is the primary display channel, with a per-player queue

**Status:** decided by Kevin, 2026-09-20.

Kevin's own words, from the original rulings session: "the subtitle system for Minecraft sucks as
display; I remember commands could spawn temporary text overlays in the user's HUD; can't we use
that additionally?" Resolved, and restated in the final rulings: the action bar
(`Player.displayClientMessage(..., true)` on 1.21.1, `ServerPlayer.sendOverlayMessage` on 26.2) for
every player within hearing range, a per-player queue with a minimum display time; vanilla
subtitles as the accessibility layer once audio exists; an in-world speech bubble is a later ticket
(`rulings-2026-09-20.md`).

Both version-specific method names are confirmed by direct bytecode/source read, including the
26.1-era rename between them (`vault/technical/minecraft/villager-events-sounds-and-emf-compat.md`
§B6) — not inferred from documentation alone. The per-player queue exists because the engine's own
action-bar slot is single-occupancy and last-write-wins with a hard-reset 60-tick timer on every
call (confirmed by bytecode read of `Hud.setOverlayMessage`, same section) — without a queue, a
busy village (`UC-003`) would flicker unreadably. Full design in `domains/display.md` `DISPLAY-DEC-001`,
`DISPLAY-DEC-002`.

Alternative considered: subtitles alone, with no action bar. Rejected explicitly by Kevin as
insufficiently visible display, the reason this ruling exists at all. Alternative considered:
firing the overlay call unconditionally on every event, no queue. Rejected as the confirmed cause of
unreadable flicker under any multi-villager burst — the queue is this sheet's addition to make
Kevin's ruling actually work in the case that matters most (`UC-003`). Cost if wrong: none
identified — both channels are independently toggleable (`domains/display.md` `DISPLAY-REQ-005`),
so a simpler unconditional-call fallback (dropping the queue) is a small code change, visible
immediately in a raid-bell test, not a silent regression.
