---
title: "villager_voices spec — REACTION line catalogue: the 1.0 data"
type: "spec"
category: "villager_voices"
---

# `REACTION` line catalogue — the 1.0 data

## 1. Purpose

The actual 1.0 line text: 4 original lines per event, English words only, each paired with an
optional `grunt` naming the game's own vanilla villager sound (VV-18, `AUDIO-DEC-005`) rather than
a written hum. Originally written with villager hums inline, in the spirit of Kevin's own example
phrasing ("Mrrgh — traded! Nice." / "Hmnh, that'll do.",
`vault/technical/minecraft/villager-events-sounds-and-emf-compat.md` §"Proposal section"); Kevin's
own round-2 sample feedback replaced the written hum with the game's real sound ("stuff like
'hrngg' should be replaced with the actual fitting villager sound from the game," `AUDIO-DEC-005`),
so §3's subtitles are words only from VV-18 on. Never derived from or resembling any third-party
mod, add-on, or show's published dialogue — no such dialogue was found published anywhere to
compare against in any case (research §E2, "Not confirmed"). This is data, referenced by
`domains/reaction.md`'s mechanism and requirements, not a second copy of them.

## 2. JSON shape (confirmed at the first ticket, `VV-3`; `grunt` added by `VV-18`)

```json
// data/villager_voices/reaction/trade_completed.json
{
  "lines": [
    { "subtitle": "Traded! Nice.", "sound": "villager_voices:reaction.trade_completed.1", "grunt": "minecraft:entity.villager.trade" },
    { "subtitle": "Good trade, that.", "sound": "villager_voices:reaction.trade_completed.2", "grunt": "minecraft:entity.villager.trade" },
    { "subtitle": "Ha! Emeralds for me.", "sound": "villager_voices:reaction.trade_completed.3", "grunt": "minecraft:entity.villager.trade" },
    { "subtitle": "Pleasure doing business.", "sound": "villager_voices:reaction.trade_completed.4", "grunt": "minecraft:entity.villager.trade" }
  ]
}
```

`sound` ids follow `villager_voices:reaction.<event>.<n>`, matching the registered `SoundEvent`,
its `sounds.json` entry, and its `subtitles.villager_voices.reaction.<event>.<n>` lang key
one-to-one (`domains/audio.md` §2). `subtitle` here is the same text as the lang file's own string —
duplicated deliberately so a datapack can override which lines are eligible and read their text
without cross-referencing the lang file (`domains/reaction.md` `REACTION-DEC-001`). `grunt` is
optional (a line may omit it) and names a vanilla villager `SoundEvent` id, played first, the line
delayed until it finishes (`domains/audio.md` `AUDIO-REQ-007`); it is never this mod's own namespace
and never validated for existence by `common`'s codec, only by shape (`domains/audio.md` §3).
`spoken` (VV-11 round six, `AUDIO-DEC-006` amendment) is also optional and, when present, is the
text the voice pipeline's generator feeds the TTS engine instead of `subtitle` — for the rare line
whose subtitle spelling isn't plainly pronounceable ("Zzz.") or contains a written interruption
that reads as a truncated word rather than punctuation ("Wha—"). A line without one still gets help
if it needs it: `tools/voices/render.py`'s own fallback normalizer handles a written interjection
misspelling and a plain em-dash trail-off on its own (`domains/audio.md` §3 "Input"); `spoken` is
only for what that normalizer can't fix. No loader reads it — display and playback only ever use
`subtitle`/`sound`/`grunt`.

## 3. The sixteen catalogues

Every 1.0 line's `grunt` is the same vanilla event across all four lines of its own event (VV-18's
own choice, for a consistent voice per event rather than per line — nothing in `AUDIO-DEC-005`
requires variation within an event).

| Event | 1 | 2 | 3 | 4 | Grunt | Spoken (where it differs) |
|---|---|---|---|---|---|---|
| `trade_completed` | Traded! Nice. | Good trade, that. | Ha! Emeralds for me. | Pleasure doing business. | `entity.villager.trade` | |
| `offer_opened` | Lookin' to trade? | Hmm, what've you got? | Ah, a customer. | Step right up. | `entity.villager.ambient` | |
| `hurt` | Ow! Ow ow ow! | That hurt! | Watch it! | Ah! Rude! | `entity.villager.hurt` | |
| `killed` | No—! | Unfair... | Wha— no! | Argh! | `entity.villager.death` | 3: What, no! |
| `zombified` | Cold... | Something's wrong... | Hungry... | Can't... think... | `entity.villager.ambient` | |
| `cured` | Warm again! | Hmm, that's better. | Ah, thank you! | Good as new. | `entity.villager.ambient` | |
| `level_up` | Ha! Promoted! | Business is booming. | Moving up in the world. | Ah, a raise, of sorts. | `entity.villager.celebrate` | |
| `restock` | Hmm, restocking. | More goods, fresh in. | Ah, business never sleeps. | Back to work. | `entity.villager.ambient` | |
| `sleep` | Bed time. | Goodnight. | Zzz. | Ahh, rest at last. | `entity.villager.ambient` | 3: Shh. |
| `wake` | Morning. | Hmm, another day. | Ah, well rested. | Let's get to it. | `entity.villager.ambient` | |
| `raid_bell` | Raid! Raid! | Take cover! | Ah! Not again! | Everyone, hide! | `entity.villager.ambient` | |
| `golem_summoned` | Ha! Reinforcements. | Good, backup. | Ah, our protector. | Feel safer now. | `entity.villager.celebrate` | |
| `panic` | Ah! Ah! Run! | Danger! | Get away! | Ah, help! | `entity.villager.ambient` | |
| `player_staring` | Can I help you? | Hmm, something the matter? | Ah, personal space, please. | Yes? | `entity.villager.ambient` | |
| `breeding` | Hmm, ah, private moment. | Look away, please. | Ah— not now. | A bit of privacy? | `entity.villager.ambient` | |
| `baby_grows` | Ha! Grown up already. | My, how time flies. | Ah, look at them now. | All grown up. | `entity.villager.ambient` | |

16 events × 4 lines = **64 lines total** at 1.0, one registered `SoundEvent` each
(`domains/audio.md` `AUDIO-REQ-001`). Every `grunt` in this table is a vanilla `minecraft:` id, listed
above with the shared `entity.villager.` prefix omitted for width; none of the sixteen 1.0 events
uses a profession-specific `work_<profession>` grunt (`domains/audio.md` §3's category rule allows
it, but no 1.0 line's text is profession-specific enough to call for one).

"Spoken (where it differs)" (VV-11 round six) lists only the two lines that needed an explicit
catalogue `spoken` field: `sleep.3` ("Zzz." isn't pronounceable as written) and `killed.3` ("Wha—"
is a genuinely truncated word fragment, not just punctuation the generator's own fallback normalizer
can fix). A blank cell does **not** mean every other line's subtitle is spoken byte-for-byte
verbatim — `killed.1` ("No—!") and `breeding.3` ("Ah— not now.") both carry a written interruption
dash that the generator's fallback normalizer turns into a pause or drops on its own, with no
catalogue field needed (`domains/audio.md` §3 "Input").

## 4. Use cases

`UC-001` in `02-journeys.md` shows one line (`trade_completed`.1) end to end.

## 5. Requirements

| ID | Requirement | Priority | From |
|---|---|---|---|
| `LINES-REQ-001` | The system shall ship exactly the 64 lines in §3 as the 1.0 default catalogue, four per event across all 16 events. | Must | This file |
| `LINES-REQ-002` | Every line's subtitle text shall be original writing, never a translation or paraphrase of any third-party mod, add-on, or media's published dialogue. | Must | `decisions/DEC-009-positioning.md`; `operations/compliance.md` |
| `LINES-REQ-003` | Every line's sound id shall follow `villager_voices:reaction.<event>.<n>`, matching its `sounds.json` entry and lang key exactly (`domains/audio.md` §2). | Must | §2 |

## 6. Open questions

None. The 64 lines are this sheet's own data, written now rather than deferred. The codec's field
names, the one mechanical thing left open, are confirmed by `VV-3`: a top-level JSON object with a
`"lines"` array, each entry an object with a `"subtitle"` string and a `"sound"` string, exactly as
§2 already showed — no wrapper or renaming needed, since that shape already matched the data as
written. Parsing, validation (`LINES-REQ-003`'s id pattern, `REACTION-REQ-012`'s unregistered-sound
rejection) and datapack-override behaviour live in `common`'s `villager_voices.catalogue` package
(`Catalogue`/`CatalogueCodec`); loading that JSON off disk is `fabric`'s `CatalogueReloadListener`,
registered through Fabric's resource loader API. `VV-18` adds the optional `grunt` field to that
same codec (shape-validated only, never event-existence-validated in `common`) and to
`CatalogueReloadListener`'s own registered-`SoundEvent` check (a warning, never a rejection, if a
grunt doesn't resolve) — see `domains/audio.md` §3/§5/§8 (`AUDIO-REQ-007`, `AUDIO-DEC-005`) for the
playback behaviour itself, which this file does not repeat.
