---
title: "villager_voices spec — REACTION line catalogue: the 1.0 data"
type: "spec"
category: "villager_voices"
---

# `REACTION` line catalogue — the 1.0 data

## 1. Purpose

The actual 1.0 line text: 4 original lines per event, English with villager hums, in the spirit of
Kevin's own example phrasing ("Mrrgh — traded! Nice." / "Hmnh, that'll do.",
`vault/technical/minecraft/villager-events-sounds-and-emf-compat.md` §"Proposal section"), never
derived from or resembling any third-party mod, add-on, or show's published dialogue — no such
dialogue was found published anywhere to compare against in any case (research §E2, "Not
confirmed"). This is data, referenced by `domains/reaction.md`'s mechanism and requirements, not a
second copy of them.

## 2. JSON shape (confirmed at the first ticket, `VV-3`)

```json
// data/villager_voices/reaction/trade_completed.json
{
  "lines": [
    { "subtitle": "Mrrgh — traded! Nice.", "sound": "villager_voices:reaction.trade_completed.1" },
    { "subtitle": "Hmnh, good trade, that.", "sound": "villager_voices:reaction.trade_completed.2" },
    { "subtitle": "Ha! Emeralds for me.", "sound": "villager_voices:reaction.trade_completed.3" },
    { "subtitle": "Mmh-hmm, pleasure doing business.", "sound": "villager_voices:reaction.trade_completed.4" }
  ]
}
```

`sound` ids follow `villager_voices:reaction.<event>.<n>`, matching the registered `SoundEvent`,
its `sounds.json` entry, and its `subtitles.villager_voices.reaction.<event>.<n>` lang key
one-to-one (`domains/audio.md` §2). `subtitle` here is the same text as the lang file's own string —
duplicated deliberately so a datapack can override which lines are eligible and read their text
without cross-referencing the lang file (`domains/reaction.md` `REACTION-DEC-001`).

## 3. The sixteen catalogues

| Event | 1 | 2 | 3 | 4 |
|---|---|---|---|---|
| `trade_completed` | Mrrgh — traded! Nice. | Hmnh, good trade, that. | Ha! Emeralds for me. | Mmh-hmm, pleasure doing business. |
| `offer_opened` | Mrgh? Lookin' to trade? | Hmm, what've you got? | Ah, a customer. | Mmh, step right up. |
| `hurt` | Ow! Ow ow ow! | Hngh — that hurt! | Mrgh! Watch it! | Ah! Rude! |
| `killed` | Mrgh — no—! | Hnnh... unfair... | Wha— no! | Argh! |
| `zombified` | Nnngh... cold... | Grrh... something's wrong... | Nnh, hungry... | Hrrgh... |
| `cured` | Mrgh! Warm again! | Hmm, that's better. | Ah, thank you! | Mmh, good as new. |
| `level_up` | Ha! Promoted! | Mrgh, business is booming. | Hmnh, moving up in the world. | Ah, a raise, of sorts. |
| `restock` | Hmm, restocking. | Mrgh, more goods, fresh in. | Ah, business never sleeps. | Mmh, back to work. |
| `sleep` | Mrgh... bed time. | Hnnh, goodnight. | Mmh... zzz. | Ahh, rest at last. |
| `wake` | Mrgh, morning. | Hmm, another day. | Ah, well rested. | Mmh, let's get to it. |
| `raid_bell` | Mrgh! Raid! Raid! | Hnngh — take cover! | Ah! Not again! | Everyone, hide! |
| `golem_summoned` | Ha! Reinforcements. | Mrgh, good, backup. | Ah, our protector. | Mmh, feel safer now. |
| `panic` | Ah! Ah! Run! | Mrgh — danger! | Hnngh, get away! | Ah, help! |
| `player_staring` | Mrgh... can I help you? | Hmm, something the matter? | Ah, personal space, please. | Mmh, yes? |
| `breeding` | Hmm, ah, private moment. | Mrgh, look away, please. | Ah— not now. | Mmh, a bit of privacy? |
| `baby_grows` | Ha! Grown up already. | Mrgh, my, how time flies. | Ah, look at them now. | Mmh, all grown up. |

16 events × 4 lines = **64 lines total** at 1.0, one registered `SoundEvent` each
(`domains/audio.md` `AUDIO-REQ-001`).

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
registered through Fabric's resource loader API.
