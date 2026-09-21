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

`LINES-DEC-001` (Kevin, 2026-09-21, VV-20): the words-only lines VV-18 left behind still leaned on
written interjection strings to carry tone ("Ow! Ow ow ow!", "Zzz.", "Wha—", the "..." trail-offs)
rather than actual sentences — Kevin: "the lines aren't very good themselves; they do too much 'um,
ahh, ouhdfubv' for my liking; they don't need to in their lines, they can talk now." §3's 64 lines
are rewritten as plain, natural, under-twelve-word spoken sentences with no written grunt, stammer,
or interjection string standing in for a sound — the character lives in the words now, not in the
spelling. The event list, the four-per-event count, and every line's `grunt` field are unchanged.

## 2. JSON shape (confirmed at the first ticket, `VV-3`; `grunt` added by `VV-18`)

```json
// data/villager_voices/reaction/trade_completed.json
{
  "lines": [
    { "subtitle": "Good trade. Come back tomorrow.", "sound": "villager_voices:reaction.trade_completed.1", "grunt": "minecraft:entity.villager.trade" },
    { "subtitle": "That was a fair deal for both of us.", "sound": "villager_voices:reaction.trade_completed.2", "grunt": "minecraft:entity.villager.trade" },
    { "subtitle": "Emeralds. I could get used to this.", "sound": "villager_voices:reaction.trade_completed.3", "grunt": "minecraft:entity.villager.trade" },
    { "subtitle": "Pleasure doing business with you.", "sound": "villager_voices:reaction.trade_completed.4", "grunt": "minecraft:entity.villager.trade" }
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
text the voice pipeline's generator feeds the TTS engine instead of `subtitle` — for a line whose
subtitle spelling isn't plainly pronounceable, or contains a written interruption that reads as a
truncated word rather than punctuation. A line without one still gets help if it needs it:
`tools/voices/render.py`'s own fallback normalizer handles a written interjection misspelling and a
plain em-dash trail-off on its own (`domains/audio.md` §3 "Input"); `spoken` is only for what that
normalizer can't fix. No loader reads it — display and playback only ever use `subtitle`/`sound`/
`grunt`. The field itself stays in the codec (`common`'s `CatalogueCodecTest` still exercises
parsing it), but `LINES-DEC-001`'s rewrite left no 1.0 line needing it: every one of the 64 lines
is plain, pronounceable spoken English as written, so `spoken` is currently unused by the shipped
catalogue — the two lines that once needed it (`sleep.3`, `killed.3`) were rewritten to not.

## 3. The sixteen catalogues

Every 1.0 line's `grunt` is the same vanilla event across all four lines of its own event (VV-18's
own choice, for a consistent voice per event rather than per line — nothing in `AUDIO-DEC-005`
requires variation within an event). Rewritten to plain spoken sentences by `LINES-DEC-001`
(VV-20): no written grunt, stammer, or interjection string stands in for a sound anywhere below,
every line is under twelve words, and every line is plainly pronounceable as written — none of the
64 needs the `spoken` override (§2).

| Event | 1 | 2 | 3 | 4 | Grunt |
|---|---|---|---|---|---|
| `trade_completed` | Good trade. Come back tomorrow. | That was a fair deal for both of us. | Emeralds. I could get used to this. | Pleasure doing business with you. | `entity.villager.trade` |
| `offer_opened` | Looking to trade? | What have you got for me? | Ah, a customer. Come, look. | Step right up, friend. | `entity.villager.ambient` |
| `hurt` | That hurt. | Stop that, it hurts. | Watch where you swing that. | That was uncalled for. | `entity.villager.hurt` |
| `killed` | No, please, not like this. | This is not fair. | Wait, no! | I did not deserve this. | `entity.villager.death` |
| `zombified` | Something is wrong with me. | I feel so cold. | I cannot stop this. | I can barely think straight. | `entity.villager.ambient` |
| `cured` | I am warm again. | That is much better. | Thank you, truly. | Good as new, thanks to you. | `entity.villager.ambient` |
| `level_up` | I have been promoted! | Business is booming these days. | I am moving up in the world. | Consider this my raise. | `entity.villager.celebrate` |
| `restock` | Just restocking, one moment. | Fresh goods, right this way. | Business never really stops. | Back to work, I suppose. | `entity.villager.ambient` |
| `sleep` | Time for bed. | Goodnight, then. | Quiet now, I am sleeping. | Rest at last. | `entity.villager.ambient` |
| `wake` | Morning already. | Another day, I suppose. | Well rested, at least. | Let's get to it, then. | `entity.villager.ambient` |
| `raid_bell` | A raid! Everyone, take cover! | Take cover, now! | Not this again. | Everyone, get inside! | `entity.villager.ambient` |
| `golem_summoned` | Reinforcements, finally. | Good, we could use the backup. | Our protector is here. | I feel much safer now. | `entity.villager.celebrate` |
| `panic` | Run, everyone, run! | Danger. Get inside. | Get away from here! | Somebody help, please! | `entity.villager.ambient` |
| `player_staring` | Can I help you with something? | Is something the matter? | A little personal space, please. | Yes? Can I do something for you? | `entity.villager.ambient` |
| `breeding` | This is a private moment. | Please, look away. | Not now, please. | A little privacy, if you would. | `entity.villager.ambient` |
| `baby_grows` | Already grown up. | My, how the time flies. | Look at them now. | All grown up, just like that. | `entity.villager.ambient` |

16 events × 4 lines = **64 lines total** at 1.0, one registered `SoundEvent` each
(`domains/audio.md` `AUDIO-REQ-001`). Every `grunt` in this table is a vanilla `minecraft:` id, listed
above with the shared `entity.villager.` prefix omitted for width; none of the sixteen 1.0 events
uses a profession-specific `work_<profession>` grunt (`domains/audio.md` §3's category rule allows
it, but no 1.0 line's text is profession-specific enough to call for one).

The prior wording (VV-18-era, superseded by `LINES-DEC-001`) leaned on written interjections and
trail-offs to carry tone — "Ow! Ow ow ow!" for `hurt.1`, "Zzz." for `sleep.3`, "Wha— no!" for
`killed.3`, "..." trail-offs across `zombified` — two of which needed an explicit catalogue
`spoken` override to be pronounceable at all. The rewrite above says the same reactions in actual
sentences instead, so no line in the current catalogue needs that override any more (§2).

## 4. Use cases

`UC-001` in `02-journeys.md` shows one line (`trade_completed`.1) end to end.

## 5. Requirements

| ID | Requirement | Priority | From |
|---|---|---|---|
| `LINES-REQ-001` | The system shall ship exactly the 64 lines in §3 as the 1.0 default catalogue, four per event across all 16 events. | Must | This file |
| `LINES-REQ-002` | Every line's subtitle text shall be original writing, never a translation or paraphrase of any third-party mod, add-on, or media's published dialogue. | Must | `decisions/DEC-009-positioning.md`; `operations/compliance.md` |
| `LINES-REQ-003` | Every line's sound id shall follow `villager_voices:reaction.<event>.<n>`, matching its `sounds.json` entry and lang key exactly (`domains/audio.md` §2). | Must | §2 |

## 6. Open questions

None. The 64 lines are this sheet's own data, written now rather than deferred, rewritten once
already (`LINES-DEC-001`, §1/§3) to plain spoken sentences. The codec's field names, the one
mechanical thing left open, are confirmed by `VV-3`: a top-level JSON object with a
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
