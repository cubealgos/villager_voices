---
title: "villager_voices spec — testing"
type: "spec"
category: "villager_voices"
---

# Testing (`TEST`)

| Layer | What | Where |
|---|---|---|
| Unit | The selection rule (no-immediate-repeat, uniform over the remainder); the three cooldown checks and the rate limiter, given a fake clock; the per-player `DisplayQueue`'s hold-time/advance logic; the config-file default/clamp behaviour — all pure, no Minecraft imports, in `common`, checked by a package-purity check (`verifyPurePackage`/`verifyPureCore`, the same discipline `create_synthetic_diamonds`' `operations/testing.md` uses) | `common/src/test` |
| Game tests | Each of the 16 events actually fires its hook and reaches the bus in a real world (belt-mode-equivalent: a real `Villager` entity, a real trade/hurt/zombify/etc.); a sleeping or baby villager stays silent except for its one exempt event; a raid-bell burst produces a queued, not flickered, sequence for a test player; a datapack-overridden catalogue with a missing sound id is rejected at load with a clear error | `fabric/src/gametest`, Loom `runGameTest` (alpha combination first; mirrored per loader once each module lands) |
| Manual / release checklist | Trading, hurting, breeding, and ringing a bell against a real villager and confirming the action-bar text, timing, and category-mute toggles behave as specified; confirming the vanilla subtitle appears for a placeholder sound with "Show Subtitles" on; the EMF/ETF/Fresh-Animations client checklist below | release checklist |
| Client checklist (EMF/ETF/Fresh Animations installed) | With EMF installed: `villager_voices.is_talking` reads true for the duration of a playing line, confirmed via EMF's own debug/animation-variable inspector if one exists, or a minimal test resource pack using the reference snippet (`domains/compat.md` §3); villager model and texture are pixel-identical to vanilla when idle. With ETF installed: no crash, no texture change caused by this mod. With a hypothetical Fresh-Animations-style pack that reads the variable: mouth animates only while talking-state is true. | manual, per release for the combination under test |
| Development tool | A `/villager_voices debug trigger <event> <target>` command that forces a specific event on a targeted villager, bypassing cooldowns — the closest analogue to the siblings' debug commands, and genuinely useful here since several of the 16 events (raid, zombification, breeding) are slow or awkward to trigger manually during testing | `villager_voices.debug`, `just client` |

**What is genuinely hard here, stated plainly:** the selection/cooldown/queue logic is fully
unit-testable in `common` given a fake clock and a fixed random source — no Minecraft dependency
once events, timestamps, and a roll value are inputs. What is *not* pure-testable is whether each of
the 16 hooks actually fires the way `domains/reaction.md` §3's table (several rows marked inferred,
not `javap`-confirmed — `zombified`, `cured`, the NeoForge `sleep`/`wake` split,
`player_staring`'s whole design) predicts; only a game test against a real server world, per loader,
confirms that.

`TEST-REQ-001`: every `REACTION-REQ`, `DISPLAY-REQ`, `AUDIO-REQ`, and `COMPAT-REQ` names its test in
the ticket that implements it.
`TEST-REQ-002`: a deliberate-break proof for the `common` package-purity check, once.
`TEST-REQ-003`: a game test proves each event's mixin or native-event hook coexists with vanilla and
Create-adjacent behaviour without side effects — e.g. the `trade_completed` mixin does not alter
`notifyTrade`'s own return value or the trade's actual outcome, only observes it.
`TEST-REQ-004`: the alpha's manual checklist is run against Fabric 26.2 before every alpha release;
each fast-follow combination gets the identical checklist run against its own loader/version before
its first release.
