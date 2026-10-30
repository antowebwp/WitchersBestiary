# Roadmap

- v0.19.0: published baseline, unchanged.
- Stage 20A: Witcher Contracts server foundation — domain lifecycle, definitions, persistence, kill tracking, rewards, operator test interface and focused tests. See [report](STAGE20_A_REPORT.md).
- Stage 20B: 3D notice boards, safe best-effort village generation, localized contract browser/acceptance and focused verification implemented. See [report](STAGE20_B_REPORT.md).
- Stage 20C.1: Swamp Lord vertical slice implemented; [report](STAGE20_C_REPORT.md).
- Stage20C.2: Grave Devourer implemented, focused tests/build and native hunt cycle verified.
- Stage20C.3: procedural Drowner/Ghoul/Foglet hunts complete; fixed legendary hunts retained. Deterministic persistence, compatible abilities, focused regressions and native three-species/rejoin cycles verified.
- Future verification: longer production multiplayer/performance sessions and optional additional species; no further implementation started.
- Stage20D: Witcher Senses, private persisted investigation chains and deferred lair encounters implemented;38 focused tests/build and two native hunt/rejoin cycles verified. See [report](STAGE20_D_REPORT.md).
- Stage20E / Stage21: not started.

Development branch: dev/0.20.0. No Stage 20 push or new release authorized.

## Stage20E — RC1 built; manual validation pending

44/44 focused GameTests PASS; Gradle build PASS. Stability fixes and approximately3x graveyard candidate density implemented. See [Stage20E report](STAGE20_E_REPORT.md) for real-network evidence and remaining checks. No push/release or Stage21.

Two real native clients completed independent Ghoul investigations/combat/rewards, unload/reload and two dedicated restarts; repeat rewards rejected. Remaining basilisk, optional-stack and natural graveyard validation stays pending.
