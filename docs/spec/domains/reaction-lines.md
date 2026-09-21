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

`LINES-DEC-002` (Kevin, 2026-09-21, VV-11 round ten): the shipped batch (round nine, `AUDIO-DEC-006`
final amendment) renders all 64 lines against one reference clip and one delivery setting — Kevin,
hearing it: "they always sound surprised; it is not conveying the correct emotions for everything
yet." Chatterbox clones prosody from its reference clip and scales it with `exaggeration`; one
reference and one setting can only ever produce one mood, regardless of a line's own words. Six
emotion classes are defined over the 16 events, each mapped to exactly one class (§3's new "Mood"
column) and each class given its own emotion-matched reference segment (a 15-25s stretch of the
same public-domain giordano recording, `domains/audio.md` §3 "Reference", scored by pitch variance/
energy variance/speaking rate rather than picked by ear) and its own generation settings:

| Class | Events | Exaggeration | CFG weight | Reference profile |
|---|---|---|---|---|
| `calm` | `sleep`, `wake`, `baby_grows`, `restock` | 0.25 | 0.3 | low pitch variance, low energy variance, slow |
| `pleased` | `trade_completed`, `level_up`, `cured` | 0.35 | 0.3 | moderate variance, rising contours |
| `annoyed` | `breeding`, `player_staring`, `offer_opened` | 0.4 | 0.3 | low pitch variance, clipped rhythm (short voiced runs) |
| `hurt` | `hurt` | 0.5 | 0.3 | short phrases, falling contours |
| `alarmed` | `panic`, `killed`, `zombified`, `raid_bell` | 0.65 | 0.2 | high pitch variance, high energy variance, faster |
| `gentle` | `golem_summoned` | 0.35 | 0.3 | soft dynamics (low energy variance), unhurried, some warmth |

Temperature stays fixed at 0.8 and the `open_warm_mix` post-processing chain is unchanged across
every class (round nine's own chain, `AUDIO-DEC-006`) — only the reference clip and the
exaggeration/cfg pair vary per class; `pleased` and `gentle` share the same exaggeration/cfg pair
(both a moderate, unhurried register) but condition on different reference segments, since the
event content differs (business satisfaction vs. a quieter relief) even though the acoustic
intensity target is the same. Three of the ticket's own five explicit anchors needed a judgment
call to place onto an actual event id, recorded here rather than left implicit: "idle-like" (the
fourth `calm` member) is `restock` — the one remaining routine, low-arousal event; "refusal/no-trade"
(the third `annoyed` member) is `offer_opened` — the guarded, pre-agreement moment, terser than
`trade_completed`'s warmth, and literally the event during which a villager cannot yet be traded
with; `raid_bell` and `golem_summoned` are not literal leftovers assigned by elimination — their
actual line text (`raid_bell`: "A raid! Everyone, take cover!"; `golem_summoned`: "I feel much safer
now") reads as alarmed and gentle-relief respectively, not as a forced fit into whatever remained,
so they were placed by content rather than by the ticket's own five-class arithmetic. This keeps
every class's reference segment matched to lines that actually sound like the class, rather than
having `gentle`'s hushed, unhurried reference under an urgent raid announcement. Per-class reference
segment offsets, the selection method, and measured prosody are recorded in
`tools/voices/reference.py`'s `EMOTION_REFERENCE_SEGMENTS` table and `tools/voices/VOICES.md`
"Round 10"; `--mood-override` on `tools/voices/render.py` lets a single line be rendered at a
different class for comparison, without touching the catalogue.

## 2. JSON shape (confirmed at the first ticket, `VV-3`; `grunt` added by `VV-18`; `mood` by round ten)

```json
// data/villager_voices/reaction/trade_completed.json
{
  "lines": [
    { "subtitle": "Good trade. Come back tomorrow.", "sound": "villager_voices:reaction.trade_completed.1", "grunt": "minecraft:entity.villager.trade", "mood": "pleased" },
    { "subtitle": "That was a fair deal for both of us.", "sound": "villager_voices:reaction.trade_completed.2", "grunt": "minecraft:entity.villager.trade", "mood": "pleased" },
    { "subtitle": "Emeralds. I could get used to this.", "sound": "villager_voices:reaction.trade_completed.3", "grunt": "minecraft:entity.villager.trade", "mood": "pleased" },
    { "subtitle": "Pleasure doing business with you.", "sound": "villager_voices:reaction.trade_completed.4", "grunt": "minecraft:entity.villager.trade", "mood": "pleased" }
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
64 needs the `spoken` override (§2). `Mood` (`LINES-DEC-002`, round ten) is likewise the same value
across all four lines of an event — `common`'s `CatalogueCodec` rejects a file whose lines disagree.

| Event | 1 | 2 | 3 | 4 | Grunt | Mood |
|---|---|---|---|---|---|---|
| `trade_completed` | Good trade. Come back tomorrow. | That was a fair deal for both of us. | Emeralds. I could get used to this. | Pleasure doing business with you. | `entity.villager.trade` | `pleased` |
| `offer_opened` | Looking to trade? | What have you got for me? | Ah, a customer. Come, look. | Step right up, friend. | `entity.villager.ambient` | `annoyed` |
| `hurt` | That hurt. | Stop that, it hurts. | Watch where you swing that. | That was uncalled for. | `entity.villager.hurt` | `hurt` |
| `killed` | No, please, not like this. | This is not fair. | Wait, no! | I did not deserve this. | `entity.villager.death` | `alarmed` |
| `zombified` | Something is wrong with me. | I feel so cold. | I cannot stop this. | I can barely think straight. | `entity.villager.ambient` | `alarmed` |
| `cured` | I am warm again. | That is much better. | Thank you, truly. | Good as new, thanks to you. | `entity.villager.ambient` | `pleased` |
| `level_up` | I have been promoted! | Business is booming these days. | I am moving up in the world. | Consider this my raise. | `entity.villager.celebrate` | `pleased` |
| `restock` | Just restocking, one moment. | Fresh goods, right this way. | Business never really stops. | Back to work, I suppose. | `entity.villager.ambient` | `calm` |
| `sleep` | Time for bed. | Goodnight, then. | Quiet now, I am sleeping. | Rest at last. | `entity.villager.ambient` | `calm` |
| `wake` | Morning already. | Another day, I suppose. | Well rested, at least. | Let's get to it, then. | `entity.villager.ambient` | `calm` |
| `raid_bell` | A raid! Everyone, take cover! | Take cover, now! | Not this again. | Everyone, get inside! | `entity.villager.ambient` | `alarmed` |
| `golem_summoned` | Reinforcements, finally. | Good, we could use the backup. | Our protector is here. | I feel much safer now. | `entity.villager.celebrate` | `gentle` |
| `panic` | Run, everyone, run! | Danger. Get inside. | Get away from here! | Somebody help, please! | `entity.villager.ambient` | `alarmed` |
| `player_staring` | Can I help you with something? | Is something the matter? | A little personal space, please. | Yes? Can I do something for you? | `entity.villager.ambient` | `annoyed` |
| `breeding` | This is a private moment. | Please, look away. | Not now, please. | A little privacy, if you would. | `entity.villager.ambient` | `annoyed` |
| `baby_grows` | Already grown up. | My, how the time flies. | Look at them now. | All grown up, just like that. | `entity.villager.ambient` | `calm` |

16 events × 4 lines = **64 lines total** at 1.0, one registered `SoundEvent` each
(`domains/audio.md` `AUDIO-REQ-001`). Every `grunt` in this table is a vanilla `minecraft:` id, listed
above with the shared `entity.villager.` prefix omitted for width; none of the sixteen 1.0 events
uses a profession-specific `work_<profession>` grunt (`domains/audio.md` §3's category rule allows
it, but no 1.0 line's text is profession-specific enough to call for one). `Mood` is six classes
over 16 events (`LINES-DEC-002`, §1) — 4/3/3/1/4/1 events for `calm`/`pleased`/`annoyed`/`hurt`/
`alarmed`/`gentle` respectively; unequal sizes are expected, since the classes group by how a line
actually reads, not by a fixed quota.

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
| `LINES-REQ-004` | Every line's optional `mood`, when present, shall be one of the six classes in `LINES-DEC-002`'s table (§1), and shall agree with every other line in the same event's catalogue file. | Should | `LINES-DEC-002`; VV-11 round ten |

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
playback behaviour itself, which this file does not repeat. `LINES-DEC-002` (round ten) adds the
optional `mood` field to that same codec (closed-set-validated, and cross-checked for per-event
agreement, `common`'s `CatalogueCodecTest`); like `grunt`/`spoken`, `fabric`'s
`CatalogueReloadListener` never reads it — only `tools/voices/render.py`'s generator does
(`domains/audio.md` §3, `AUDIO-DEC-006` round ten amendment).
