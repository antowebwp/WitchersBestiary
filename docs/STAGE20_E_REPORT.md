# Stage 20E — stability and local release candidate audit

Status: IN PROGRESS. Base1e7c5c0, branch dev/0.20.0; initial worktree clean. No publication authorized. User game installation, separate archived project and v0.19.0 tag must remain unchanged.

## Executive Summary

Audit and targeted verification in progress. No RC readiness or unexecuted multiplayer/compatibility checks are claimed.

## Initial audit findings

- Rapid V presses use the last received server value, while the server drops toggles inside five ticks; final input can be lost. Reproduce and fix with bounded last-intent handling.
- A target removed without normal death retains its active reservation forever. Preserve chunk-unload reservations, but distinguish terminal destruction from unload and keep owner/claim safety.

Further evidence, commands, test results and known/manual limitations will be appended before checkpoint.

## Resume checkpoint — 2026-10-29

All prior uncommitted Stage20E changes retained. Base-profile run `build/stage20e-base-tests.log` confirms **43 required tests passed**, including owner/saddle/riding/lava/compressed-NBT basilisk identity. Optional CCA disconnects the GameTest fake player; this is not evidence of a real player riding failure. Full real riding/Nether checks remain pending.

The second real two-client dedicated run reached normal-world contracts and first-clue study, then the **review harness** dereferenced a missing second clue on a valid shortened route. No product terrain rule was weakened. Harness now accepts short routes and records whether peer progress actually advanced while the first owner was offline. A new bounded run is in progress; multiplayer PASS is not yet claimed.

New regression added for dead/spectator Inspect rejection and preserved contract progress, awaiting next targeted test run. No RC version/tag/publication changes yet.

## Interrupted network evidence — 2026-10-30

Run `build/stage20e-network/run-20261029-232550` has no completion/failure marker and no remaining JVMs after session interruption. Logs confirm two actual localhost players, board acceptance, WBOne disconnect/rejoin, death and Nether entry. They do NOT establish completion, restart, reward replay or full multiplayer PASS. Preserve this world and logs. Next: finish/re-run bounded network harness, investigate phase9 dimension acknowledgement before changing production code. The shortened-route harness fix is retained. Final targeted tests/build log: `build/stage20e-resume-final.log` (record outcome before claiming PASS). RC1 not prepared; optional stack, real basilisk control/Nether, natural graveyard regression and final packaging remain pending.

Latest resume run: 44 tests executed, 43 passed, new dead/spectator regression FAILED at Spectator cannot advance. Production inspect checks isSpectator; verify mock game-mode behavior before attributing this to production. Combined Gradle run stopped at runGametest; build not reached. Do not mark RC ready or commit as completed. Next inspect mock player spectator semantics, fix fixture or production as evidence requires, repeat only stage20e batch then build. All work preserved uncommitted.

Finalization attempt: replaced fixed-mode GameTest player in dead/spectator regression with regular ServerPlayerEntity sharing the fixture connection. Explicit assertions now require actual spectator and survival modes before authorization checks. Targeted stage20e run log: build/stage20e-fixture-fix.log. Record final outcome before proceeding to full44/build; no readiness claim.

## Final push checkpoint — 2026-10-30

Full `runGametest -Pstage19fProfile=A --offline --console=plain`: **44/44 PASS**, including corrected ordinary ServerPlayerEntity Survival/Spectator/dead authorization test. Log `build/stage20e-all44.log`. Creative mock player mode overrides were unsuitable for this test; production guard was not weakened.

`build stage20ePrepareLaunch -Pstage19fProfile=A --offline --console=plain`: **BUILD SUCCESSFUL**,32s. Log `build/stage20e-rc-build.log`. Local metadata version0.20.0-rc1, Java21, Minecraft1.21.1. JAR ZIP/metadata checked; native and GameTest harness classes/logs absent. JAR8247350bytes, SHA256 `8ee010034c824366ce6ee209316dbbd460f607c92f17ead732d5b03d114fe3ff`.

**RC1 BUILT — VALIDATION PENDING**, not public-release ready. Real multiplayer final run launched with incremental phase result journal and 2400tick per-phase/900second process limits. Original interruption has no recorded fatal exception or final marker; exact external cause cannot be established from logs. No invented crash cause is asserted.

## Graveyard Generation Rebalance

Existing three independent vanilla random_spread structure sets retained. Parameters:

| Type | Old spacing/separation | New | Expected candidate density ratio |
|---|---|---|---|
|Abandoned village|20/8|12/5|2.778|
|Forgotten forest|26/10|15/6|3.004|
|Ancient necropolis|42/16|24/9|3.063|

Density is proportional to1/spacing². Same salts, biome tags, terrain/dry crypt checks and eight-chunk vanilla village exclusion retained. No geometry, contents, loot or Grave Hag spawn changes. Crypts are part of graveyard pieces, not independent structure sets: more graveyards necessarily also means more embedded crypts; no independent crypt frequency changed. Existing exclusions do not guarantee exclusion against every vanilla/modded structure or other graveyard set; no such new guarantee is claimed. Minimum region offsets remain much larger than39block largest footprint, but cross-set coincidences remain possible as before.

Candidate-only comparison over [-1024,1024) chunks on both axes, same three seeds and old/new salt/linear spread JavaRandom offsets. Counts BEFORE exclusion, biome and terrain filtering (NOT generated graveyards or a natural-world benchmark):

|Seed|Type|Old candidates|New candidates|Ratio|
|---|---|---:|---:|---:|
|202001|village|10481|29101|2.777|
|202001|forest|6184|18626|3.012|
|202001|necropolis|2362|7298|3.090|
|42|village|10467|29101|2.780|
|42|forest|6185|18612|3.009|
|42|necropolis|2359|7310|3.099|
|8675309|village|10469|29108|2.780|
|8675309|forest|6181|18617|3.012|
|8675309|necropolis|2358|7297|3.095|

Natural generated-structure count, chunk-boundary visual inspection and multi-seed old-world reopening remain **NOT TESTED** for this rebalance. Candidate arithmetic supports approximately3x placement attempts, not a promise of exactly3x surviving structures. Existing generated chunks remain; newly generated terrain uses changed placement. Back up saves before changing worldgen settings. Server owners can lower density with a datapack overriding the three existing worldgen/structure_set JSON files and restoring old spacing/separation; no extra runtime configuration engine added. Changes limited to these three resource files.

## Manual validation still required

- Two-client full contract-to-reward, chunk unload/reload and dedicated restart outcomes: inspect final network journal; do not infer from connection alone.
- Basilisk real forward/back/turn inputs, logout owner retention, real chunk unload/reload, Nether transfer and mounted lava travel; server NBT/taming/saddle/lava checks passed.
- Pinned optional stack runtime/combat and animations: BetterCombat2.3.2, SimplySwords1.63.0, SpellEngine1.9.9, SpellPower1.4.6, Trinkets3.10.0 downloaded only, not compatibility PASS. Mandatory Fabric/GeckoLib/Patchouli server/client startup previously observed. SkillTree exact configuration absent, NOT TESTED.
- No new full-server TPS/MSPT result yet. Earlier Stage20C/D synthetic/accelerated timings remain historical, not production benchmark.
- No new assets introduced; existing public-history asset provenance retained, no independent legal clearance claimed.

## Final real-network result (supersedes pending network entries above)

**PASS**: two separate native Fabric client JVMs on a localhost dedicated normal world,seed202001, default150–500block hunt radius; two different Ghoul contracts (procedural and legendary). Production board/Inspect/attack/reward packets used; operator teleport/equipment/test coordination used. Both actual combat-to-reward cycles complete. Private snapshots/foreign clue rejection; owner logout while peer advances four-clue route; exact state after reconnect; player death/respawn; Nether roundtrip; saved-route dedicated JVM restart; target chunk actual unload/reload with same UUID; distinct targets; simultaneous owner rewards; repeat/stale claims denied; second dedicated JVM restart preserves REWARDED and rejects further claims. Machine-readable complete.json and incremental scenario-results.json preserved under ignored build. This is automated two-client integration, not two-human balance testing.

Bounded500tick combat window mean ring-buffer server processing **23.128ms/tick**. The raw wall-time driver reports26.61ticks/sec because catch-up/server tick timing is not a stable20TPS measurement; do not interpret that as sustainable TPS. No long-run percentile/production benchmark. Removal-without-death regression is GameTest coverage, not separately network-tested.

Basilisk remaining controls/Nether and optional stack still NOT TESTED; multi-seed generated-graveyard counting still pending. Accordingly final status **PARTIALLY COMPLETED / RC1 BUILT — VALIDATION PENDING**. Local checkpoint permitted; no public readiness claim. RU/EN key sets match exactly. No new protected assets or Stage21 implementation.

Network evidence directory (local, ignored): `build\stage20e-network\run-20261030-142430`.
