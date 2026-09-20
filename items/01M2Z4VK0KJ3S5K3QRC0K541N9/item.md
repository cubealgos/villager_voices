---
schema_version: 1
id: 01M2Z4VK0KJ3S5K3QRC0K541N9
key: VV-18
type: feat
title: "The game's own villager grunt before each line: an optional grunt sound event per catalogue line, played first, the line after its length"
created_by: kevin
created_at: 2026-09-20T10:12:44Z
---

## Scope

<fill this in before committing>

## Approach

<fill this in before committing>

## Acceptance criteria

- [ ] <fill this in before committing>

## Constraints and prior findings

<fill this in before committing>

## Scope

Kevin, 2026-09-20, on VV-11's round-two samples: "stuff like 'hrngg' should be replaced with the actual fitting villager sound from the game." Recorded as `AUDIO-DEC-005` and `AUDIO-REQ-007` in `docs/spec/domains/audio.md` (vault first). A catalogue line gains an optional `"grunt": "minecraft:entity.villager.trade"` (any vanilla villager sound event id); the mod plays the grunt first at the line's position, source and volume, then the line's own sound after the grunt's duration, as one utterance. The written grunts in the subtitles ("Mrrgh —", "Hngh —", "Mrgh!", "Hmnh,", "Mmh-hmm,") come out of every catalogue line's `subtitle` and the fitting vanilla event goes in as `grunt` (trade → `entity.villager.trade` or `.yes`, hurt → `.hurt`, refusal → `.no`, celebration → `.celebrate`, panic/ambient → `.ambient`, work → the profession's `.work_*` where the event is profession-specific, death → `.death`). Vanilla audio is never bundled; the reference is by id, resolved on the client.

## Approach

`common`: `Catalogue`/`CatalogueCodec` parse the optional `grunt` string (a namespaced id, validated by shape only in common; existence is the loader's check as for `sound`), `LineSink` carries it. `fabric`: `CatalogueReloadListener` verifies the grunt id names a registered `SoundEvent` (`BuiltInRegistries.SOUND_EVENT`) and warns otherwise; `ReactionSoundPlayer.play` becomes play-grunt-then-schedule-line: the delay is a per-event table of vanilla clip lengths in ticks (measure them once from the client's asset index `~/.gradle/caches/fabric-loom/assets/indexes/26.2-32.json` → `objects/<hash>` with `soxi -D`/`ffprobe`, record the table with the measurements in the class Javadoc; villager ambient/trade/hurt clips are roughly 0.4–1.0 s), scheduled on the server tick (a tick-scheduler in `VillagerVoicesFabric`, drained at `ServerTickEvents.END_SERVER_TICK`). The talking state for EMF (VV-12) spans grunt plus line. A game test: a line with a grunt schedules the line for a later tick; a line without plays immediately. Update `tools/voices/render.py`'s input derivation only if it still reads the grunt from the subtitle (it uses the subtitle verbatim; the subtitles are now words only, so nothing to change, but confirm).

## Acceptance criteria

- [ ] Optional `grunt` per line parsed, validated, carried to the sink; every shipped catalogue line has its written grunt removed from `subtitle` and a fitting vanilla event as `grunt`.
- [ ] Grunt then line as one utterance, the line delayed by the grunt's measured length; lines without a grunt unchanged; EMF talking state spans both.
- [ ] Game test for the sequencing; `just check` green; spec copy (`docs/spec/`) synced from the vault.
- [ ] Merged through a Forgejo pull request into `development`.

## Constraints and prior findings

`AUDIO-DEC-001` placeholders untouched. `COMP-REQ-002`: no vanilla audio files in the repo. VV-11 round three is rendering samples in parallel from three hard-coded lines and must not be blocked on the catalogue change.
