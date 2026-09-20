---
title: "villager_voices spec — REACTION: the 16-event trigger catalogue"
type: "spec"
category: "villager_voices"
---

# `REACTION` — the 16-event trigger catalogue

## 1. Purpose

Which events trigger a reaction, how each is detected per loader and Minecraft version, how one
line is picked among an event's catalogue, and the cooldown/hearing/silence rules that decide
whether a detected event actually produces a line. The line *text* itself is
`domains/reaction-lines.md`'s data, not this file's. What a player sees or hears is
`domains/display.md`/`domains/audio.md`'s concern, not this one's.

## 2. Dimensions

| Dimension | Answer |
|---|---|
| **Actors** | The server (`ACTORS-002`) detects every event and runs selection; the loaders (`ACTORS-004`) supply the underlying hook, native event, or mixin target each detection uses; a datapack author (`ACTORS-005`) may retune or extend a catalogue. |
| **Over time** | A hook fires (native event, mixin, or a per-tick poll for the three poll-based events) → cooldown/rate/silence checks run → a line is selected → `domains/display.md`/`domains/audio.md` apply it. Nothing here is loaded once and cached beyond the JSON catalogues themselves, reloaded on every datapack (re)load. |
| **Multiplicity** | Exactly 16 events at 1.0 (`REACTION-REQ-001`–`016`); each with 4 lines in the shipped catalogue (`domains/reaction-lines.md`); a datapack may add more per event, subject to §7's registration limit. |
| **Unwanted** | Two events firing on the same villager in the same tick (independent cooldowns per event, so both may still produce lines, queued by `domains/display.md`); an event firing on a sleeping or muted-category villager (§3 silence rules); a catalogue JSON with a line pointing at an unregistered sound event (rejected at load, `REACTION-FAIL-003`). |
| **Not-you** | A player who never interacts with a villager never sees a line. A server with no villagers loaded pays no tick cost from the poll-based events, since polling iterates loaded villager entities only. |

## 3. Enumerations

### The 16 events, category, and hook per loader/version

Source: `vault/technical/minecraft/villager-events-sounds-and-emf-compat.md` §A (jar-verified 26.2,
1.21.1 unverified where flagged). 26.2 package moves (`world.entity.npc` →
`world.entity.npc.villager`, `world.entity.monster` → `world.entity.monster.zombie`) do not change
which method is hooked, only its package — not repeated per row.

| Event | Category | Hook (both versions unless noted) | Fabric API | NeoForge | Mixin? |
|---|---|---|---|---|---|
| `trade_completed` | trade | `AbstractVillager.notifyTrade(MerchantOffer)` | none | none per-trade (`VillagerTradesEvent` fires at offer-list reload only) | **Y**, both loaders — inject TAIL |
| `offer_opened` | trade | `Villager.mobInteract` → `startTrading` | `UseEntityCallback.EVENT`, confirmed | `PlayerInteractEvent.EntityInteract` | N — filter `instanceof Villager` |
| `hurt` | combat | `LivingEntity.actuallyHurt` | `ServerLivingEntityEvents.AFTER_DAMAGE`, confirmed | `LivingHurtEvent`/`LivingDamageEvent` | N |
| `killed` | combat | `LivingEntity.die` → `Villager.die` | `ServerLivingEntityEvents.AFTER_DEATH`, confirmed | `LivingDeathEvent`, confirmed | N |
| `zombified` | combat | `Zombie.convertVillagerToZombieVillager` | `ServerLivingEntityEvents.MOB_CONVERSION`, confirmed | `LivingConversionEvent.Pre`/`.Post` | N (inferred; smoke-test at first ticket) |
| `cured` | combat | `ZombieVillager.finishConversion` (private) | `ServerLivingEntityEvents.MOB_CONVERSION` | `LivingConversionEvent.Pre`/`.Post` | N |
| `level_up` | trade | `Villager.increaseMerchantCareer` (private, gated by `VillagerData.canLevelUp`) | none (`ServerEntityLevelChangeEvents` is a dimension-change trap, not this) | none found | **Y**, both loaders — inject `setVillagerData` HEAD, diff old vs. new level |
| `restock` | trade | `Villager.restock()` | none | none | **Y**, both loaders — inject TAIL |
| `sleep` | social | `LivingEntity.startSleeping` | `EntitySleepEvents.START_SLEEPING`, confirmed | none found (NeoForge's sleep events are Player-only) | N on Fabric, **Y** on NeoForge |
| `wake` | social | `LivingEntity.stopSleeping` (Villager releases its bed POI) | `EntitySleepEvents.STOP_SLEEPING`, confirmed | none found | N on Fabric, **Y** on NeoForge |
| `raid_bell` | raid | `BellBlockEntity.onHit` (ring) + `Raids.createOrExtendRaid` (raid start) | none | `VillageSiegeEvent` exists but is zombie-siege-specific, not this | **Y**, both loaders — two targets |
| `golem_summoned` | social | `Villager.spawnGolemIfNeeded` → `SpawnUtil.trySpawnMob` | `ServerEntityEvents.ENTITY_LOAD`, confirmed; filter `IRON_GOLEM` | `MobSpawnEvent`/`FinalizeSpawnEvent`, filter `MOB_SUMMONED` | N — type/proximity filter suffices |
| `panic` | combat | `Brain<Villager>.isActive(Activity.PANIC)` (public) | none | none | N (recommended) — poll `ServerTickEvents.END_SERVER_TICK`/`ServerTickEvent.Post`, edge-detect per UUID |
| `player_staring` | social | no vanilla hook | none | none | N — public API raycast/dot-product on the same tick poll |
| `breeding` | social | `Animal.spawnChildFromBreeding` (not overridden by Villager) | none | `BabyEntitySpawnEvent`, confirmed, generic | **Y** on Fabric (filter `Villager`), N on NeoForge |
| `baby_grows` | social | `Villager.ageBoundaryReached()` (protected) | none | none found | **Y**, both loaders |

**Pattern for `common`**: a `VillagerEventBus` (plain interface/enum + dispatch, zero
`net.minecraft.*` imports) fed from three sources, per
`vault/technical/minecraft/villager-events-sounds-and-emf-compat.md`'s own recommendation: (1)
loader-native events wired through a thin per-loader adapter; (2) a `common`-module tick poll using
only public API, identical on both loaders (`panic`, `player_staring`); (3) mixins, one per loader's
mixin config, same target method, behind a shared `VillagerEventSource` interface. `job_site
acquired/lost` and `gossip exchange` are in the research's own hook table but **not** in this mod's
16-event 1.0 scope — named here only so a reader of the research doesn't wonder why two rows are
missing; adding them later is a `domains/reaction-lines.md`-shaped, not architecture-shaped, change.

### Selection rule

Random among the event's eligible lines (catalogue minus muted-category exclusion), excluding the
one line that played last for *that villager* on *that event* — no immediate repeat
(`REACTION-REQ-005`). With 4 lines per event at 1.0, this is a uniform pick over the remaining 3.

### Cooldowns and rate limit (config defaults, not invented here)

| Throttle | Scope | Proposed default | Confirm at |
|---|---|---|---|
| Per-villager-per-event | One villager, one event id | 60s | first ticket |
| Per-villager-global | One villager, any event | 5s | first ticket |
| Server-wide per-player rate | One player, across every villager they can hear | 1 line per 2s | first ticket |

All three are independent config values, not hard invariants — mirroring
`create_synthetic_diamonds`' `RECIPE-DEC-001`-style discipline of naming a mechanism's shape without
inventing its tuned numbers where Kevin has not ruled on them. The numbers above are this sheet's
proposal, not a ruling.

### Hearing range and silence rules

- **Hearing range** = the line's sound event's own effective broadcast radius (`attenuation_distance`
  × `volume`, capped at 16 blocks × volume by engine default, research §B3) — the same set of
  players receives the action-bar text and, once real audio exists, hears the sound, so the two
  channels never disagree (`domains/display.md` `DISPLAY-REQ-002`).
- **Sleeping villagers are silent**: no reaction line while `LivingEntity.isSleeping()` is true,
  regardless of what triggers around it (`REACTION-REQ-009`; the `sleep` event itself is the one
  exception, firing exactly at the moment sleep begins, before the silence takes effect).
- **Baby villagers are silent** for every event except `baby_grows`, which fires exactly once, at
  the moment a baby becomes an adult (`REACTION-REQ-010`).
- **Muted categories**: `trade`, `combat`, `social`, `raid` (the grouping in the event table above)
  are independent config toggles (`domains/display.md` `DISPLAY-REQ-005`); a muted category's events
  are still detected (so cooldowns keep ticking consistently) but never reach selection.

### Data format: `data/villager_voices/reaction/<event>.json`

One file per event, an ordinary datapack JSON list, loaded the same way vanilla loads
per-registry-id datapack content (advancements, loot tables): each entry names a subtitle text and
the id of an already-registered `SoundEvent` (`domains/reaction-lines.md` §3 shows the shipped
shape). `REACTION-DEC-001` below is this domain's data-vs-hardcoded call.

## 4. Use cases

`UC-001` through `UC-007`, `UC-010` in `02-journeys.md`.

## 5. Requirements

| ID | Requirement | Priority | From |
|---|---|---|---|
| `REACTION-REQ-001`–`016` | The system shall detect each of the 16 events in §3's table, via the hook named for each loader/version cell, and offer it to selection subject to §3's cooldown, rate, and silence rules. | Must | `vault/technical/minecraft/villager-events-sounds-and-emf-compat.md` §A |
| `REACTION-REQ-005` | The system shall select one line at random from an event's eligible catalogue, excluding the line that played last for that villager on that event. | Must | Selection rule above |
| `REACTION-REQ-006` | The system shall suppress a detected event if the triggering villager is within its own per-event cooldown for that event. | Must | Cooldowns above |
| `REACTION-REQ-007` | The system shall suppress a detected event if the triggering villager is within its per-villager-global cooldown, regardless of which event fired. | Must | Cooldowns above |
| `REACTION-REQ-008` | The system shall suppress a line reaching a given player if that player is within their server-wide rate-limit window, independent of how many villagers are nearby. | Must | Cooldowns above |
| `REACTION-REQ-009` | The system shall suppress every event except `sleep` itself while the triggering villager is asleep. | Must | Silence rules above |
| `REACTION-REQ-010` | The system shall suppress every event except `baby_grows` itself while the triggering villager is a baby. | Must | Silence rules above |
| `REACTION-REQ-011` | Where a datapack replaces one of the 16 catalogue files, the system shall use the datapack's line list in place of the shipped defaults, decoded the same way. | Must | `UC-007` |
| `REACTION-REQ-012` | A catalogue entry naming a `SoundEvent` id not present in the registry at load time shall be rejected with a clear error, not silently dropped or crash the load. | Must | `REACTION-FAIL-003` |

## 6. Failure modes

| ID | Failure | Response |
|---|---|---|
| `REACTION-FAIL-001` | The custom-recipe-style hook (a mixin target) is renamed or removed by a future Minecraft/loader update | Build fails at compile time for a direct method reference; a reflection-based or looser mixin target fails at mod load with a named error, never silently no-ops (mirrors `create_synthetic_diamonds` `PLATFORM-REQ-002`'s discipline). |
| `REACTION-FAIL-002` | Two hooks for the same event fire in the same tick (e.g. a rapid re-trigger) | The per-villager-global cooldown (`REACTION-REQ-007`) is the backstop even if a per-event cooldown edge case is missed. |
| `REACTION-FAIL-003` | A catalogue JSON names an unregistered sound event | Rejected at load with a clear error naming the missing id (`REACTION-REQ-012`). |
| `REACTION-FAIL-004` | The three poll-based events (`panic`, `player_staring`) run at real server tick cost on a large village | Poll runs once per server tick over loaded villagers only (not all entities), edge-detected per UUID so no allocation happens on the steady-state case — the same pattern the research recommends for its own poll-based rows. |

## 7. Open questions

| Question | Blocks | Decided by |
|---|---|---|
| The exact cooldown/rate default values (§3's proposed 60s/5s/1-per-2s) | `REACTION-REQ-006`–`008` | first ticket, config-overridable regardless |
| Whether `zombified`/`cured`'s "no mixin" inference and the sleep/wake NeoForge split hold against a running server | `REACTION-REQ-001` (rows `zombified`, `cured`, `sleep`, `wake`) | first ticket (research flags both as inferred, not `javap`-confirmed for NeoForge) |
| Player-staring's exact raycast/dot-product/distance thresholds | `REACTION-REQ-001` (`player_staring` row) | first ticket — no research precedent, genuinely new design |

## 8. Decisions

- `REACTION-DEC-001` — **Data-driven catalogue for the alpha, not hardcoded, with a named
  registration ceiling.** Full reasoning in `decisions/DEC-010-data-driven-catalogue.md`: each
  event's `data/villager_voices/reaction/<event>.json` is an ordinary, datapack-overridable list of
  `{subtitle, sound}` entries; the `SoundEvent`s themselves stay code-registered (a Minecraft
  registry requirement, not a design choice, research §B1) from a single generator source so
  registration, `sounds.json`, and lang entries never drift from the catalogue. **Cost if wrong**: a
  hardcoded Java `Map<Event, List<Line>>` is a strict subset of this shape, reachable by deleting the
  JSON loader and inlining its output — not a redesign.
- `REACTION-DEC-002` — **16 events at 1.0, `job_site`/`gossip` deferred, not built.** The two extra
  events the research's own hook table finds (`job_site acquired/lost`, `gossip exchange`) are
  real and reachable the same way as `panic`/`player_staring` (a `common`-module poll or a mixin),
  but are outside this mod's stated 16-event scope (`rulings-2026-09-20.md`'s originating brief) and
  are not built now. **Cost if wrong**: additive — a 17th/18th event follows the same catalogue shape
  as any of the 16, no architecture change.
