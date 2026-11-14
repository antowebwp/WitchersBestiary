# Stage 22G recovery

Implementation checkpoint is the commit containing this note. The boss, seal, reward and replay regression scenarios passed in the targeted run. The final Gradle build passed. Full board-to-reward Survival integration is still pending.

The attempted vanilla locate probe inside Fabric GameTest found no tomb; the test fixture runs on a low flat surface while the tomb requires a dry site above sea level. Use one isolated ordinary Overworld with a fixed seed, not a GameTest flat world. Verify the registry tag, locate a start, inspect the stairs and arena, then drive the actual board selection and both clues into arena awakening and sealing. Record board, entrance and arena coordinates. Verify BoP and structure exclusions separately if available.

Tomb acceptance currently locates the nearest eligible start synchronously and loads up to 81 approach chunks before route planning. Check latency and the case where the nearest tomb has no viable surface route. The audit confirmed ordinary targets are instantiated from the contract species; the reported ghoul/drowner skew has not been reproduced or fixed. Sample board generations and terrain rejection rates across the twelve species before changing weights.

Normal Overworld diagnostic (seed 220719) passed: entrance 200 69 -920, arena 200 49 -888. The server probe used an artificial player and called prepared acceptance and clue inspection directly, verifying two valid clues and one arena spawn. Actual board packets, natural village board, real Survival combat and held sealing on this route remain unverified.

## Finalization checkpoint on a923787 — PARTIAL

- HigherVampireTests passed all five scenarios on Minecraft 1.21.1, plus the Patchouli smoke test. See build/stage22g-final-tests.log. This includes phase transitions, defeat persistence and the sealing/replay reward regression fixture.
- The existing normal Overworld (seed 220719) was reopened by runStage19Locate. It found entrance 200 69 -920 and arena 200 49 -888, verified 20 stair blocks and headroom plus the central arena volume, prepared the contract, inspected both clues through the server API, and rejected a duplicate awakening. See build/stage22g-overworld-final.log.
- This rerun confirms the persisted structure, not fresh generation in that run. Board offer and acceptance packets, native V and study interaction, a walkable surface route, a full arena/AI inspection, real combat, held-use sealing, and the board-to-reward sequence still need one connected Survival client scenario. No comparison PNG was captured. BoP and structure overlap remain unverified.
- Continue with a dedicated clientSmoke review using the existing normal-world seed and native NoticeBoardScreen screen-handler buttons, private clue packets and held item-use path. Capture only completed render frames at the four checkpoints specified in the brief. Reuse the isolated test directory and keep the Superflat restriction intact. Arcane FX and gameplay sources were not changed.

## Survival client integration

Run `runStage22gReview` with `ALSOFT_DRIVERS=null` using the isolated Loom client. The final run generated a normal Overworld (seed 220719), opened an actual test board, accepted Blood of the Ancients with the screen-handler packet, verified one phylactery after closing the board, and used the V binding and both client right-click clue interactions. The generated entrance and arena were reached by test teleports. The automatic arena spawn displayed its bossbar. The fixture lowered boss health to trigger normal transitions, applied finishing damage after FINAL, and held item use through the live sealing channel. The contract reached REWARDED with one Blood Legacy and exactly 2000 crowns in the client inventory.

Evidence: build/stage22g-client-proof.log and build/stage22g-review/{result.txt,comparison.png}. HigherVampireTests previously passed on the same gameplay code. The build passed before the last clientSmoke-only refinements, which compiled and ran successfully. This run did not exercise an unassisted walk or fight, multi-participant scaling, live AFK timeout/interruption, live replay, or BoP.
