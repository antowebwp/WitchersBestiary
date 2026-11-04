# Stage 21D — Duel Stone and multiplayer fist fighting

Status: COMPLETED — implemented and locally verified (2026-11-04). Development version 0.21.0-dev, branch dev/0.21.0. No Stage21E, push or publication. Final local checkpoint message: `Stage 21D: duel stone and PvP fist fighting`.

## Playing

Craft `witchersbestiary:duel_stone` in a crafting table:

```
III
BSB
BBB
```

I = iron ingot, B = stone bricks, S = smooth stone (3 iron, 5 bricks, 1 smooth stone). The block is in Functional creative inventory, has tool-aware survival loot and a tiered collision shape. Its original16-element volume model uses four original32px textures, metal corner inserts, two engraved fists and restrained red markings. Editable deterministic generator: tools/generate-duel-stone.py; validator: tools/validate-duel-stone.py.

Both players use the same stone with empty hands. A opens a30-second waiting challenge; B joins a20-second confirmation. BOTH explicitly accept, then a server-owned3-second countdown begins. Escape may close the screen without accepting; O (rebindable) reopens it. Pending challenge can be cancelled/declined in GUI. Active duel menu O → Surrender → Confirm surrender; Back preserves combat. Existing `/wbfight cancel` also surrenders.

Controls reuse Stage21B: left mouse quick punch, H heavy, J hold guard, K dodge; bindings can be changed. A timely guard/dodge grants a server-owned counter window; next quick punch consumes it. The shared movable/scalable HUD shows opponent name, own/other virtual HP, own stamina, action/countdown, and duel-menu binding/result. Existing persisted HUD settings remain applicable.

## Server mechanics and safety

DuelSessions adapts the SAME FistFightSession engine, with explicit PLAYER_VS_PLAYER type. No copied damage engine, AI substitution or external NPC payout path. Every combat input is bound to an unguessable session UUID and invoking member, limited to one per server tick; controls also have a five-tick rate limit. Client cannot submit damage, winner, reward or a counter action. Server checks reach, height, facing and line of sight at contact. Ordinary player damage (including Better Combat and indirect player sources) is suppressed for duelists; outsiders cannot deal/receive duel damage or join another player's session. Environment damage ends the duel and follows normal world rules; no global invulnerability.

Lifecycle: WAITING → CONFIRMING → COUNTDOWN → ACTIVE → FINISHED → CLEANUP; IDLE is absence from registry. One participant membership across NPC/PvP sessions, one match per stone, no overlapping arenas within24 blocks; independent arenas can run concurrently. Start searches loaded, solid, fluid-free opposed positions with collision/world-border/entity checks. Players are moved once to face each other; no per-tick teleport. Radius12 blocks, warning plus60-tick return grace, then technical defeat. A movement jump above8 blocks, dimension change, unloaded required chunk, missing stone or invalid hands terminates safely. Disconnect awards the remaining player; stale packets cannot affect a rematch. Pending decline never enters combat restrictions/HUD. Started bouts keep a one-second result grace before both memberships/HUD/poses clear. Runtime maps are discarded on server stop; block and ordinary player data persist.

KO changes only virtual health/poses. Real health, inventory, XP, wallet crowns and NPC reward ledger remain untouched. Server protection checks query Fabric ALLOW_DAMAGE (zero-damage permission probe) and DuelPermissions.ALLOW at start, periodically and at contact; veto or callback exception refuses/cancels the duel. `FistFights.abilitiesAllowed` is the server integration hook for future spells/signs.

`pvp=false` deliberately REFUSES to start a duel, with localized explanation; it never enables global PvP. This is the safe-refusal option explicitly permitted by the task.

## Validation and evidence

- Original full combined run:89 GameTests,84 passed and5 new fixture setup failures (test server defaults pvp=false); existing83 tests passed. Fixture correction followed by related class only, not repeated full regression.
- Final focused run:6/6 DuelTests plus Patchouli smoke =7/7 PASS, build/stage21d-final-related.log and build/gametest-results.xml. Covers recipe/collision, dual consent, disabled PvP, foreign/stale UUIDs, symmetric guard/counter/dodge/KO, realHP2 and inventory/crowns/XP/reward immutability, wall/backward attacks, third parties, weapon cancellation, independent arenas/overlap, surrender, boundary grace, expiry, disconnect/dimension, external damage, NPC exclusion and protection veto. Mock head yaw explicitly matches intended gaze.
- Gradle build PASS with12 release mixins/refmap verified, same final log. Static model/texture/UV validator PASS16 bounded volume elements.
- Two REAL native clients + dedicated server with Better Combat2.3.2, Player Animator2.0.4, Cloth Config15.0.140: build/stage20e-network/run-20261104-204849/complete.json, orchestration build/stage21d-native4.log. WBOne960x640 actual GUI2, WBTwo1280x900 actual GUI3. Survival BlockItem placement, GUI decline, fresh two-player consent/countdown, FULL100virtualHP knockout via normal client inputs (no virtualHP/winner edits), unchanged realHP2/inventory/crowns/XP, immediate rematch, confirmed GUI surrender, real walk beyond boundary/grace, actual disconnect/reconnect, destroyed stone, active stop/restart, persisted stone/inventory, stale UUID rejection and new challenge PASS.
- Native remote pose sets: One saw HEAVY/BLOCK/COUNTER/DODGE/KNOCKOUT; Two saw HEAVY/STAGGER/QUICK/VICTORY. Rendered stone, GUI at both scales, guard, counter, connected-joint KO and victory inspected. Native testing found/fixed periodic updates closing surrender GUI and a torso/leg separation in KO; restored full transforms and copied clothing parts. Test command files now replace atomically.
- Native pvp=false PASS: build/stage20e-network/run-20261104-205322/complete.json and build/stage21d-native-pvpfalse.log. Both real clients received safe refusal, real server flag stayed false, no session or inventory/health/crown change. All temporary worlds, screenshots and raw logs stay ignored under build/. No test-only modules or compatibility mods are included in release JAR.

Reproduce (Java21):

```powershell
python tools/prepare-duel-compat.py
./gradlew.bat runGametest build stage20ePrepareLaunch -Pstage19fProfile=A -Pstage21dCompat --console=plain
# Related-only repeat: add -PgameTestClass=DuelTests
$env:DUEL_NETWORK='1'
python tools/run-stage20e-network.py
$env:DUEL_PVP='false'
python tools/run-stage20e-network.py
```

Compatibility preparation extracts nested runtime libraries from already-local Better Combat jars to ignored build/; it downloads nothing. Build/prepare before native JVM launch; never rebuild live class directories while those JVMs load.

## Limits and retained scope

Third-party region plugins that do not honor Fabric ALLOW_DAMAGE require an explicit DuelPermissions adapter; universal compatibility is not claimed. Required-chunk-unload cancellation is implemented and checked before world access; forced mid-bout chunk unload was not separately reproduced with native players because they naturally keep nearby chunks loaded. No Spell Engine dependency or actual spell system is introduced; the future server hook is provided. Session data is intentionally transient across restart; matches resume as ordinary players, not a recovered partial bout. No PvP crowns, XP, betting, ranked ladder, spectator mode or Stage21E.

Previous automatic approval review refused test-world deletion (reason: blocked by policy). It was not retried through another mechanism. Stage21D evidence and test worlds are retained, no additional disk cleanup claimed. User game installation and prior saves untouched.
