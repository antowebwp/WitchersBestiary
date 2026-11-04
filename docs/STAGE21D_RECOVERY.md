# Stage21D active recovery - implementation present, verification pending

Project E:/Minecraft/ModDevelopment/WitchersBestiary-public-0.19.0, branch dev/0.21.0. Base checkpoint793401a; preserve all current uncommitted Stage21D work. No push/release/21E. Never modify E:/Minecraft/game. Previous cleanup deletion was blocked by policy; do not bypass.

Implemented core/UI/assets:
- DuelStone block/item/group/recipe/unlock/loot/mining tag, original16-volume model and4 material textures; tools/generate-duel-stone.py +validate-duel-stone.py. Static, no BE needed.
- DuelSessions state orchestration uses SAME FistFightSession(Type.PLAYER_VS_PLAYER) engine. WAITING30s, CONFIRMING20s, both consent, safe opposed starts,60tick countdown, virtual HP/stamina, radius12/return60ticks,1sec terminal grace/cleanup, zero rewards. Input UUID binding/rate limits; noclientresults; exclusive NPC/player enrollment, overlapping arena denial. Lifecycle checks disconnect/dimension/teleport/stone/chunks/worlddamage/timeout/serverstop. pvp=false SAFE REFUSAL, never globally enables PvP.
- DuelPermissions API and standard Fabric ALLOW_DAMAGE veto checked before start/contact; other regional protection integrations need adapters, no universal compatibility claim.
- DuelPackets View/Control/Input and existing FistPackets.State/Pose +FistClient reused. DuelClient/DuelScreen waiting/confirm/decline/O-menu/surrender-confirm. Existing HUD position/scale retained. PlayerFistPoseMixin adds KO/victory and restores changed body/head angles. Real health/inventory/rewards never modified by gameplay engine.
- Six meaningful DuelTests cover recipe/consent/forgery/PvPflag, actual timed combat block/counter/dodge/KO/nonfatal/no rewards/rematch, wall/facing/third party/weapon, simultaneous independent arenas/overlap/surrender/stone, boundary/timeouts, lifecycle/NPC exclusivity/protection veto. Player fixtures food10/saturation0 prevent vanilla natural regen from corrupting low-health assertions. Native KO starts at real full100 virtual HP (no health/winner mutation); unit KO test seeds virtual HP1 only for bounded assertion.

Validation:
- Main/client/test/native compilation PASS. Static16-volume UV/texture validation PASS. No gameplay PASS yet.
- First combined full run build/stage21d-comprehensive.log stopped BEFORE tests at missing net.tiny_config.ConfigManager from Better Combat's embedded runtime jar. tools/prepare-duel-compat.py exposes already local nested jars (no downloads) into ignored build/stage21d-compat; build.gradle -Pstage21dCompat includes them +3 existing BC/playeranimator/cloth jars from build/stage20e-compat/mods. Do not use full stage20eCompat/CCA for fake-player tests.
- Next execute prepare-duel-compat.py, then ONE actual full runGametest build stage20ePrepareLaunch with -Pstage19fProfile=A -Pstage21dCompat. Prior failed bootstrap did not execute test suite. Correct actual failures; repeat only affected tests.

Native harness:
- DuelNetworkServer test-only main registered; runner tools/run-stage20e-network.py uses DUEL_NETWORK=1 exclusively. Better Combat required on BOTH clients (reply flag asserted). WBOne960x640 GUI2, WBTwo1280x900 GUI3, actual scale factor checked. Actor health2, food10, empty hands after REAL survival placement, wallets47 and3diamonds/bread sentinel. No rewards/winner/VHP edits.
- Native sequence: place block, A waiting/B declines, fresh two confirmations, countdown +actual100virtualHP KO, nohealth/inventory/XP/crowns loss; rematch+surrender GUI; actual walking boundary; real disconnect/reconnect; stone destruction; active server stop/restart with saved block/inventory; old UUID replay inert/new waiting challenge cancancel.
- Client actions duelPlace/duelUse/duelClick/duelFight/duelSurrender/duelBoundary/duelReplay. Pose sets and delayed per-action screenshots. A offense heavy/quick +stamina pauses; B scripted heavy/guard/counter/dodge. No bot winner assumption, actual KO required. If active scripted counter is not seen, fix timing or run targeted native proof, do not fakePASS.
- Run DUEL_NETWORK=1 python tools/run-stage20e-network.py after launch prep. Then fresh DUEL_NETWORK=1 DUEL_PVP=false run verifies actual dedicated flagsafe refusal. Do NOT compile while native clients load. All worlds/screens/logs ignored build. Runner caps900sec, restarts same world using duel-resume.json+resume.json.

Still required:
1. Complete combined GameTests/build and necessary fixes (no repeated full regression without reason).
2. Native PvPtrue whole lifecycle +restart, then PvPfalse, inspect screenshots model/HUD/GUI/animation (both scales), pose sets.
3. Address bugs until real KO/rematch works. No premature final, no21E.
4. Local component checkpoints; final exact message "Stage 21D: duel stone and PvP fist fighting" after verification. docs/STAGE21D_REPORT.md +Stage21Progress/PROGRESS/ROADMAP/recovery. Keep old Stage21/hotfix assets and tags.


## Latest validation update
- Actual complete run:build/stage21d-comprehensive-actual.log executed89 tests with Better Combat runtime. Existing83 plus new recipe/consent case passed;5 duel setup failures were GameTest's default pvp=false. Fixture now explicitly enables PvP (negativeflag case retained).
- Related DuelTests run build/stage21d-duel-focused.log:7 total,5passed/2failed. Independent arenas/overlap/surrender/stone, boundary/expiry, wall/facing/outsider/weapon and recipe/consent all passed. Remaining timed guard/counter assertion needs diagnostic actual state; external damage fixture hit ordinary fresh-player spawn invulnerability, now uses outOfWorld0.5 damage to exercise real external damage without killing the2HP actor.
- Current related run build/stage21d-duel-diagnostics.log, exec72153. No native run launched yet because full chain stopped at test failure. Fix only actual failures and repeat this class, then build+launch prep andnative (do not rerun full89 without justification).
- Pending invite declines now never open combat HUD/restrictions:started=-1 until countdown; pending FINISHED cleans immediately. Started matches keep1s terminal grace.
- Preserve all code; continue through native true/false campaigns and screenshots. Stage21D NOT COMPLETED yet.
