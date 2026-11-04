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
