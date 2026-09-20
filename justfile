# The task surface: the fleet's standard recipe names.

main_checkout := parent_directory(`git rev-parse --path-format=absolute --git-common-dir`)
vault_spec := env("VV_VAULT_SPEC", main_checkout / ".." / "heimathafen" / "vault" / "projects" / "villager_voices" / "spec")

default:
    @just --list

# Resolve every dependency and prove the toolchain.
bootstrap:
    ./gradlew --version

# Static analysis and the project's own rules, without the tests.
lint:
    ./gradlew :common:check :fabric:check -x test -x runGameTest

# Everything with a build step, including the jar (the alpha's one shipped combination).
build:
    ./gradlew :fabric:build

# Unit tests, then the repository tools as commands.
test: test-java test-tools

test-java:
    ./gradlew :common:test :fabric:test

test-tools:
    python3 -m unittest discover -s tools -p 'test_*.py'

# Server-side game tests on a headless dedicated server (Fabric 26.2, the alpha's own combination).
gametest:
    ./gradlew :fabric:runGameTest

# Minecraft 26.2 with this mod (the alpha's own combination).
client:
    ./gradlew :fabric:runClient

# Refresh docs/spec/ from the vault; the vault is authoritative.
spec-sync:
    rsync -a --delete "{{vault_spec}}/" docs/spec/

# The Modrinth icon: no generator exists yet (no design has been decided,
# docs/spec/README.md "Open questions gathered"). Render it by hand and place it at
# docs/modrinth/icon.png once the icon itself is designed.
icon:
    @echo "icon: no generator yet -- the icon itself is an open design question (docs/spec/README.md); place docs/modrinth/icon.png by hand once decided"

# Regenerate docs/map.md and docs/map/ from the source.
map:
    python3 tools/map.py

map-check:
    python3 tools/map.py --check

# Repository conformance, read-only.
doctor: doctor-repo doctor-toolchain

doctor-repo:
    kontor doctor

doctor-toolchain:
    python3 tools/doctor.py

# Everything a merge must survive.
check: lint map-check test gametest
