---
schema_version: 1
id: 01M2YXQPBWHEB8NVPHNFSWFNEK
key: VV-1
type: chore
title: "Bootstrap: Gradle multi-loader scaffold (common/fabric/neoforge), CI, docs"
created_by: kevin
created_at: 2026-09-20T08:08:17Z
---

## Scope

The repository as `docs/spec/decisions/DEC-004-toolchain.md` and `docs/spec/04-architecture.md`
`ARCH-DEC-001` describe it: a `common`/`fabric`/`neoforge` module split (the JEI shape), Gradle 9.5.1
wrapper, Kotlin DSL and version catalog. `common` carries zero Minecraft/Fabric/NeoForge imports,
ever, enforced by the build. `fabric` targets 26.2 (Java 25, Loom 1.17.21, Fabric API
0.161.0+26.2), the alpha's one shipped combination (`decisions/DEC-005-alpha-scope.md`). MIT
`LICENSE`, `NOTICE`, `README.md`, `SUPPORT.md`, `CHANGELOG.md`, `CLAUDE.md` routing stating the
positioning compliance rule, `.gitignore`, `.ci/install-tools.sh`, `.woodpecker.yml`,
`.gitea/default_merge_message/*`, `justfile`, `tools/` (`doctor.py`, `map.py` and its test; no
`icon.py` yet), `docs/spec/` as a byte-identical copy of the vault spec via `just spec-sync`,
`docs/modrinth/body.md` as a placeholder. Main and client entrypoints for mod id `villager_voices`
(package `villager_voices`/`villager_voices.fabric`), a `ServiceLoader`-discovered
`VillagerEventSource` platform interface in `common` per `ARCH-DEC-003`, and a smoke game test
proving the mod loads on Fabric 26.2.

## Approach

Follow the `create_firearms` `FA-1` bootstrap's verified sequence and gotchas (local git identity
before `kontor init`; `git checkout development` immediately after; executable bit on
`.ci/install-tools.sh` preserved before the first commit; `VV-1` created first so it lands as the
first ticket key), adapting its toolchain/file scaffold to a genuine multi-module build instead of
`create_firearms`' single-project shape, since this mod's `common` module carries real pure logic
(the future event bus, cooldowns, selection, display queue) large enough to justify a build-enforced
module boundary (`DEC-004-toolchain.md`).

## Acceptance criteria

- [x] `kontor init` run with local git identity set first; both root commits (`chore/bootstrap`,
      `gitkontor/data`) carry the correct author.
- [x] `common` and `fabric` are real Gradle subprojects; `common:verifyLoaderFree` fails the build on
      any Minecraft/Fabric/NeoForge import in `common`.
- [x] `fabric` builds, its unit tests pass, and its smoke game test (`SmokeGameTest`) proves the mod
      loads on Fabric 26.2.
- [x] `just check` is green end to end (lint, map-check, unit tests, game test); `just doctor` is
      fully clean.
- [x] Executable bits on `.ci/install-tools.sh` and `gradlew` are `100755` in the index before the
      first commit.

## Constraints and prior findings

`create_firearms`' `FA-1` bootstrap is the direct model for this ticket's sequence and gotchas
(identity-before-init, `development`-checkout-immediately-after, executable-bit preservation,
ticket-ordering). `docs/spec/decisions/DEC-004-toolchain.md`, `DEC-005-alpha-scope.md`,
`04-architecture.md` `ARCH-DEC-001`–`003`, `contracts/platform-matrix.md`.

## Findings

**Repos.** Forgejo `https://git.cubealgos.de/cubealgos/villager_voices`, public, created via `tea
repos create` (stage 1, prior to this ticket's own execution). GitHub mirror
`https://github.com/cubealgos/villager_voices`, public, issues enabled, projects disabled, wiki
disabled (stage 1). Push-mirror wiring and Woodpecker enablement are Kevin's own, not set up here.

**Local identity set before `kontor init`**: `git config user.name`/`user.email` set to `Kevin
Scheeren <scheeren@cubealgos.de>` in the fresh checkout before running `kontor init`, matching
`FA-1`'s own finding. Both `chore/bootstrap`'s root commit and `gitkontor/data`'s root commit
carried the correct author from the start; no `--reset-author` pass was needed.

**`git checkout development` run immediately after `kontor init`**, before any `pull` or `branch
new`, avoiding the stale-branch trap `FA-1`'s Findings and the vault's own note both name.

**Executable bit preserved**: `.ci/install-tools.sh` and `gradlew` were `chmod +x`'d immediately
after copying, before `git add`; `git ls-files -s` confirmed mode `100755` in the index prior to the
first commit.

**Deliberate scope correction versus the task directive, grounded directly in the spec (read
firsthand, not taken on the directive's word): no `neoforge` module and no Stonecutter were built in
this ticket.** `docs/spec/decisions/DEC-005-alpha-scope.md` and `04-architecture.md` `ARCH-DEC-002`
are explicit that the alpha ships Fabric 26.2 only, with NeoForge 26.2 and then 1.21.1 for both
loaders as "additive fast-follows," specifically to avoid "several hundred MB of first-run
downloads, plus a 4-way build matrix to get green before any release" — NeoForge MDG, Stonecutter,
and `loom-back-compat` were all confirmed uncached on this machine before this ticket began. Building
either now would have contradicted the spec's own stated reasoning for deferring them. NeoForge
`26.2.0.88` artifacts were independently confirmed to exist on `maven.neoforged.net` (live check,
prior to this ticket) — the deferral is a scope choice, not an artifact-availability constraint.
`VV-9`/`VV-10` (this backlog) carry that work forward as explicit fast-follow tickets.

**`common` module's loader-freedom, verified two ways**: the build's own `verifyLoaderFree` task
(scans `common/src` for `import net.minecraft`/`net.fabricmc`/`net.neoforged` and fails the build on
a match), and a real deliberate-break proof (`docs/spec/operations/testing.md` `TEST-REQ-002`):
temporarily added `import net.minecraft.world.entity.npc.Villager;` to a `common` source file, ran
`:common:verifyLoaderFree`, confirmed it failed naming the exact bad import, then reverted and
confirmed a clean pass.

**`just check` tail, fully green**: `lint` (`:common:check :fabric:check` minus test/gametest) —
`BUILD SUCCESSFUL`. `map-check` — passed after `just map` regenerated `docs/map.md` and `docs/map/`
(stale on the first run before generation, current after). `test` — `:common:test :fabric:test`
`BUILD SUCCESSFUL`; `python3 -m unittest discover -s tools` — 2 tests, `OK`. `gametest` —
`:fabric:runGameTest`: "2 GAME TESTS COMPLETE", "All 2 required tests passed :)", `BUILD
SUCCESSFUL`. `just doctor`: `kontor doctor` 10/10 checks passed (merge-template check correctly
skipped pre-push, no remote default branch known yet); `tools/doctor.py` fully clean (java 25,
gradle wrapper 9.5.1, just 1.58.0, python3 3.14.7, kontor present, map current, spec copy
byte-identical to the vault).

**Cache-hit confirmed**: Loom `1.17.21` and the built MC 26.2 merged jar were already present in
`~/.gradle/caches/fabric-loom/`; the entire `fabric` build (including the game test's own server
boot) ran with zero new toolchain downloads, matching the research's own claim.

**Toolchain versions actually used**: Java 25 (Temurin 25.0.4.1), Gradle wrapper 9.5.1, Fabric Loom
1.17.21, Fabric Loader 0.19.5, Fabric API 0.161.0+26.2 — the platform-matrix.md-specified version,
one point release ahead of `create_firearms`' own pinned 0.160.0+26.2, resolved and built cleanly
from cache with no substitution needed.

**`kontor ticket new` numbering**: `VV-1` was created first, as intended, landing as `VV-1`
regardless of the fact that ticket authorship for `VV-2`–`VV-15` happened in a later pass — no
reordering was needed since `VV-1` was already first.

**`kontor milestone new` auto-creates `M0` "Foundation" at `kontor init` time**, at `backlog`
status; moved to `todo` before claiming `VV-1`, mirroring the ticket-level `backlog`→`todo` rule one
level up.

**`kontor ticket` body editing has no CLI subcommand**: `kontor ticket {new,status,cancel,milestone,
relate}` — no `edit`/`comment`. Findings and Scope/Approach/Acceptance-criteria content is written by
editing `.gitkontor/items/<id>/item.md` directly, a plain frontmatter+Markdown file `kontor ticket
new` scaffolds with `<fill this in before committing>` placeholders — this is gitkontor's own
file-based workflow (`tickets are authored with kontor ticket new`, bodies filled in by hand), not a
workaround. Confirmed by inspecting `create_firearms`' own `FA-1` item file, whose rich Findings
section exists in exactly this form.
