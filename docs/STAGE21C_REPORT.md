# Stage21C — Rock Troll

Local development on dev/0.21.0, 2026-11-04. No push, release or Stage21D.

## Implemented behaviour

RockTrollEntity is a neutral/revenge creature with140 real health, knockback resistance and a practical1.7×2.8-block collision box. It warns before attacking, alternates quick/heavy melee, throws an8-damage cobblestone projectile, stomps nearby grounded targets and reduces frontal incoming damage while guarding. Recovery windows and movement after a ranged throw prevent continuous attack spam. It never destroys terrain.

The confirmed empty-hand challenge uses the shared nonfatal bout system. Troll virtual health180, both stamina pools140, heavy multiplier1.4, reward90crowns. Troll contact/recovery frames are8/18 and20/36; its dedicated animations follow those contact times. Player virtual health is100. Normal health remains intact in a valid bout; external damage cancels it. Each player/NPC pair has an independent persisted24000-tick reward cooldown, with transactional pending rewards.

Rare natural spawning uses an expandable rock_troll_habitats biome tag (mountains, stony_shore, dripstone_caves), solid/fluid-free placement, additional1-in-8 admission and the existing spawn configuration. Egg, creative entry, cobblestone loot, RU/EN names and bestiary page are included. Procedural contracts do not admit this species.

## Visual source

The reproducible tools/generate-rock-troll.py generates original articulated geometry, a512×512 material atlas and GeckoLib clips. The user's supplied turnaround guides its hunched rock dome, low tusked face, large plated forearms/knuckles, short feet, ragged hide and double rope belt. The reference image is not distributed as a game asset. The mesh has40 bones and202 volume cubes. Separate wrists and jaw move during committed attacks. UV bounds, bone references and animation key durations pass asset validation. Native run160617 recorded48 frames covering four idle directions and twelve pose clips. Final run161758 verified the corrected back-fall KO and actual fatal death. Per-frame JSON distinguishes forced pose review from dead=true/deathTime10/20; actual death also produced cobblestone loot. The troll renderer disables only its extra vanilla death roll so it does not compound the GeckoLib fall.

## Verification evidence

All paths below are ignored local build evidence, not shipped assets.

- stage21-telegraph-full.log:76/76 GameTests and Gradle build passed, including reward identity/cooldown/persistence, duplicate completion, same-NPC payouts for two players, nonfatal troll KO, balance, spawn habitat/contract exclusion, stamina packet codec and troll contact frames. All12 release mixins/refmap checks passed. Use -Pstage19fProfile=A; the default optional Cardinal Components profile disconnects test mock players before its client handshake.
- Native dedicated + two real clients run-20261104-131805: same novice paid12 to each player. run132621: actual repeated victory after restart paid no extra; HUD drag/scale and config persistence; troll's ordinary retaliation, all attack poses and real damage observed.
- run133505: WBOne beat the active troll and received90. run144946: WBTwo beat the SAME saved troll after restart and independently received90. No winner, virtual-health or reward edits were used; clients fight with normal inputs. run145814/run150353 reloaded BOTH90 balances in fresh JVMs.
- Fresh normal-world run140142 observed a naturally spawned troll in minecraft:stony_shore; mountains/dripstone entries also passed integration GameTests. No entity summon was used for the natural-spawn proof.
- Stage21B original three fighters: run141402 four-angle/pose review confirmed removal of coplanar waist/arm seams. HUD saved Properties and reload verified. See STAGE21B_REPORT.md.

Automated client scenarios are bounded integration evidence, not a claim of indefinite manual playtesting or complete third-party-modpack compatibility. Old invalid overlapping review captures (run133119) and failed bot combat attempts are excluded from successful evidence.

## Cleanup

An attempted deletion of closed temporary test worlds was rejected by automatic approval review: "blocked by policy". No deletion was performed and no alternate deletion method was used. User worlds and protected game directories were untouched.


## Final local checkpoint

Reference assets and renderer/harness checkpoint:a50f707. Previous gameplay/native payout checkpoint:b1e35f1; stamina/timing checkpoint:fa68816; verified Stage21B:751647c. Final current gameplay regression76/76 and build:build/stage21-final-comprehensive.log. After the final client-only death-roll/back-fall correction, build/stage21-grounded-build.log passed; run161758 on a dedicated server with two real clients completed with both saved90 balances, actual death and clean model review. Common gameplay code did not change after the76-test pass. No additional full regression was run for each visual adjustment.

Artifact:build/libs/witchersbestiary-0.21.0-dev.jar,8510215 bytes.
SHA256:80335e898a48baaf05a811de2a7ef05d70a83d5a449b6e0c1b85efbdeee023b2.
Production JAR contains the troll assets and biome tag; test controllers/logs are excluded. This is a local development artifact, not a published release.

Stage21B fixes and Stage21C required mechanics are implemented and locally verified. Longer human balancing sessions and optional modpack compatibility remain ongoing quality work, outside the bounded checks above. No mandatory implementation item is being represented by a placeholder.
