---
schema_version: 1
id: 01M2YYSAB316RJJ65AX3KSS988
key: M2
title: "Platform expansion: NeoForge, then 1.21.1"
status: backlog
created_at: 2026-09-20T08:26:38Z
---

## Goal

Reach all four target combinations before beta, additively, without rewriting `common`
(`docs/spec/04-architecture.md` `ARCH-DEC-002`).

## Scope

A real `neoforge` module mirroring `fabric`'s event/display/audio coverage for Minecraft 26.2;
Stonecutter layered over the existing module split for the version axis, adding 1.21.1 for both
loaders. Each combination gets its own CI job; a failing one never blocks another's release
(`contracts/platform-matrix.md` `PLATFORM-REQ-003`).

## Exit criteria

- `neoforge` (26.2) builds and passes game tests for all 16 events, independent of the Fabric job.
- Stonecutter's `1.21.1-fabric` and `1.21.1-neoforge` version nodes build and pass game tests.
- Every 1.21.1 hook the original research flagged unverified is confirmed against real source or a
  real game test.
- `common` remains unmodified except for genuinely shared additions.

## Tickets

`VV-9`, `VV-10`.

## Depends on

`M1` (the Fabric 26.2 alpha this milestone's modules mirror).
