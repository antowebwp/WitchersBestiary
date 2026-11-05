# Latest verified checkpoint: Stage21G complete

Read STAGE21G_REPORT.md and STAGE21G_RECOVERY.md for the current verified state. Historical stage recovery below remains preserved.

# Active continuation: Stage21F

Stage21A–E and contract hotfix progress is preserved. Stage21F completed native verification; read [STAGE21F_RECOVERY.md](STAGE21F_RECOVERY.md) first. Historical21B/C recovery follows.

# Current recovery - Contract Reliability Hotfix complete

2026-11-04. Branch dev/0.21.0. Current source checkpoints58571ec,007e274,4eb3912. Original Stage21B/C and user changes preserved. No push/release/reset/clean, no21D, no modification of E:/Minecraft/game or published0.20.0.

## Current verified state

- Player /wbcontract abandon; existing OP /wbcontracts inspect <player> and /wbcontracts cancel <instance-UUID> (own instance). GUI abandon has named confirmation/back, UUID payload, live screen/distance/owner/ACCEPTED validation and uses the common cancellation lifecycle. Completed unclaimed work is protected, including against silently selecting another order.
- Board and OP assignment prepare a full safe route before committing acceptance or offer receipt. Failed acceptance leaves the five-offer generation unchanged; GUI explains/retry on reopen. Bounded loaded terrain search and lair fallback, no production chunk generation/tickets; hostile hunts rejected in Peaceful. Legacy pending search is explicitly recoverable/cancellable. Saved-coordinate reconstruction keeps original profile and cannot duplicate a reserved/dead target.
- New routeVersion3 has two clues40..60 blocks apart with safe ground/sampled connection. Old v0..2 routes unchanged. Persisted tracked instance in additive ContractState Tracking list; immediate current-area snapshots, sequential first/second/lair/hunt/claim, negative-cell rounding, far/rejoin/restart tracking, other-dimension labels without false coordinates. Client world-change resync fixes lost identical snapshot race. HUD uses existing Stage21B position/scale and avoids its fist panel.
- All12 contract monster types now use existing progressive rules. Natural remains dynamic; contract phase snapshots at creation in WitcherContractProgressivePhase, old saved targets migrate0. No repeated HP modifiers or damage scaling; rarity/profile preserved, caps unchanged. Werewolf saved absolute HP and Katakan8HP bat/scaled beast pool verified. OP /wbcontracts difficulty <entity> inspects server stats. Global default phase every25 overworld calendar days; HP+30%, damage+15%, armor bypass+5% with existing caps; not player XP.

## Evidence - ignored build/

- contracts-combined-comprehensive.log:ONE83-scenario full run,80 initially passed. Three failures corrected; related repeats only afterward, not repeated full suite.
- contracts-difficulty-fixed.log:3/3. contracts-investigation-related.log:7/7. contracts-final-command-guard.log:6/6, including Peaceful rejection, persisted/foreign/GUI cancellation, legacy repair, new spacing, far/dimension tracking, and unclaimed reward guard. Last code change afterward only localized command errors.
- contracts-final-artifact.log:latest Gradle build +12 release mixin/refmap checks PASS. Production JAR verified without test/log leakage.
- stage20e-network/run-20261104-185650/complete.json:real dedicated/two-client acceptance, both clues/lairs, unique phase2 targets, actual attacks/claims, independent50-crown payments, replay rejection, GUI confirmation/back and broken/normal cancellation, restart of saved wallet/cancel/target UUID/HP/phase/tracked identity. World later day100 did not rescale saved target. Isolated bounded board/forest/high-player-attack fixture; not combat balance or random ecology proof.
- run190849/complete.json:both50-crown wallets reloaded, latest clean confirmation and visible far/dimension HUD screenshots actually inspected. WBOne-cancelPrompt.png, WBTwo-tracking-proof.png, WBTwo-dimension.png. Earlier loading-screen/toast captures are excluded. Review-only run clones185650 world and never changes source or user saves.

Current artifact:build/libs/witchersbestiary-0.21.0-dev.jar.
8521188bytes; SHA256f3db9782ca15f786f819c9e947ac25d48f5ee6350d1e6bbb195e5d55d5ea12cc.
This supersedes the older Stage21C artifact details below. See CONTRACT_RELIABILITY_HOTFIX.md and RENEWABLE_CONTRACTS_GUIDE.md for design/limits and commands.

## Continuing only if requested

No required hotfix mechanism/check remains open. Do not restart project, discard work, start21D or publish without instruction. Valid old routes retain shorter historical spacing; terrain changed after acceptance can still require bounded retry or safe abandonment. Random natural sightings of all12 species, long manual balance and full optional modpack compatibility are not claimed.

Java21 JAVA_HOME=C:/Program Files/Java/jdk-21.
./gradlew.bat runGametest build -Pstage19fProfile=A --console=plain
Related only: -PgameTestClass=ContractHotfixTests / ProgressiveContractTests / InvestigationTests.
./gradlew.bat build stage20ePrepareLaunch -Pstage19fProfile=A --console=plain
CONTRACT_RELIABILITY_NETWORK=1 python tools/run-stage20e-network.py (full native fixture +saved server restart).
Review-only:also CONTRACT_RELIABILITY_REVIEW=1 and STAGE21B_RECOVER_WORLD=<project>/build/stage20e-network/run-20261104-185650/server/world. Runner validates source inside ignored review tree; no unrelated resume marker required. Do not compile while native JVMs load. Preserve successful evidence sources185650/190849 and earlier Stage21C144946.

Automatic approval review previously rejected temporary-world deletion: "blocked by policy". No deletion/bypass performed. Do not retry through another shell/language. The historical Stage21 recovery below remains reference evidence.

---

# Stage21 recovery — Stage21B fixes + Stage21C locally verified

2026-11-04. Project E:/Minecraft/ModDevelopment/WitchersBestiary-public-0.19.0, branch dev/0.21.0. Reference implementation a50f707; gameplay/native b1e35f1; reports9748d8f. Earlier user changes to FistFights.java/FistRewards.java preserved and incorporated. No reset/clean/push/release/Stage21D.

## Final implementation

Persistent movable HUD defaults right; /wbconfig drag/corners/scale/reset, saved normalized Properties and hotbar/viewport clamp. Actual clients verified drag,110% scale and fresh-JVM reload.

All3 original NPC assets retained. Generator fixes coplanar hips/body, upper/forearm and hips/leg faces. Native four-angle/pose review141402 passed. Faster decisions/approach, guard-age exploit fixed, stamina retreat preserved.

Personal playerUUID:NPCUUID reward cooldown24000 overworld ticks, persisted schema2, duplicate-safe completion and transactional pending claims. SAME novice: two real native clients each12 (131805); real repeated victory after restart unpaid (132621).

Functional neutral/revenge troll with warning, quick/heavy, rock throw, stomp, frontal protection, recovery and movement after throw; no terrain destruction. Confirmed nonfatal bouts: troll180/player100 virtualHP, both stamina140, heavy multiplier1.4,90crowns, contact8/18 and recovery20/36. HUD packet carries max stamina.

TWO REAL CLIENT TROLL PAYOUT SUCCESS: WBOne won90 in133505; WBTwo independently won90 vs SAME saved troll after dedicated restart in144946. Both90 repeatedly reloaded. No winner/virtualHP/reward edits. Earlier failed bot attempts are excluded from successful evidence.

Rare natural spawning in mountain/stony_shore/dripstone tag with config admission. Real natural troll observed in fresh normal stony_shore world140142. Other habitat tables and contract exclusion passed GameTests. Egg/loot/creative/RU/EN/bestiary included.

Reference-guided original mesh:40 bones,202 volume cubes,512 atlas,18 clips. Layered dome, low tusked face, jaw/wrists, complete limb wraps, ragged hide/double rope. Editable source tools/generate-rock-troll.py. User reference image not distributed. UV bounds/non-overlap, parents/animated bones and key durations checked.

KO back-fall and actual death verified161758. Troll-only vanilla death roll disabled to avoid compounding its GeckoLib fall. Human rendering unchanged.

## Evidence (ignored build/)

- stage21-final-comprehensive.log:76/76 GameTests, Gradle build and12 release mixin/refmap checks PASS. Common gameplay code did not change afterward.
- stage21-grounded-build.log: latest client renderer/animation build and launch preparation PASS.
- stage21-grounded-native.log and stage20e-network/run-20261104-161758/complete.json: dedicated +two real native clients; both saved wallets90, actual death and clean model views PASS. Per-frame JSON records pose/dead/deathTime; KO390/400/410, actual death490/500 in this run. Client/server frame counters differ.
- run160617:48 captures of four idle directions and twelve motion states inspected. Later KO/back-fall and death-roll changes verified161758. Old cartwheel/floating KO captures superseded.
- stage21-reference-assets.json:40 bones/202 cubes/18 clips, no UV/bone/duration errors.
- Reports STAGE21B_REPORT.md and STAGE21C_REPORT.md; player guide FIST_FIGHTING_GUIDE.md. ROADMAP/PROGRESS top sections supersede historical entries.

Artifact build/libs/witchersbestiary-0.21.0-dev.jar,8510215bytes.
SHA25680335e898a48baaf05a811de2a7ef05d70a83d5a449b6e0c1b85efbdeee023b2.
Production JAR excludes test controllers and logs. Local artifact only, not published release.

## Continuation if requested

No mandatory21B/21C implementation remains open. Longer human balancing and optional modpack checks are ongoing quality work beyond bounded automated native verification. Do not start21D or publish without instruction.

JAVA_HOME=C:/Program Files/Java/jdk-21.
./gradlew.bat runGametest build -Pstage19fProfile=A --console=plain
./gradlew.bat build stage20ePrepareLaunch -Pstage19fProfile=A --console=plain
Use profileA: optional default CCA disconnects mock players lacking client handshake. Do not compile during native JVM loading.
Model-only: STAGE21B_NETWORK=1; STAGE21C_MODEL_ONLY=1; STAGE21B_RECOVER_WORLD=<project>/build/stage20e-network/run-20261104-144946/server/world; python tools/run-stage20e-network.py.
Preserve source144946 and successful evidence/screens/logs. Latest cloned review world contains a deliberately killed staged troll; original saved bout NPC is separate.

## Cleanup restriction

Automatic approval review rejected safe deletion of closed temporary test worlds: "blocked by policy". No deletion performed; do not bypass through another shell/language. User/protected game worlds untouched. Mention this briefly in final.
