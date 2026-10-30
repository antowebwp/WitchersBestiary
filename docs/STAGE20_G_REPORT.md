# Stage20G — Procedural hunts, visited village boards and RC3

Date:2026-10-30. Base3160cd4, branch dev/0.20.0.

**Status: PARTIALLY COMPLETED. RC3 BUILT — VALIDATION PENDING.** Core contract lifecycle is integrated for12/12 approved species; new/saved village scenarios and the two dye recipes are verified below. This is a local candidate, not a public release. Remaining feature/coverage limits are explicit in section18.

## 1. Approved catalogue and source architecture

|Species|Existing entity ID suffix|Native category|Base HP / attack|Contract controller|
|---|---|---|---|---|
|Leshen|leshen|RELICT|200 /12|Generic carrier + native AI|
|Drowner|drowner|NECROPHAGE|36 /6|Existing procedural controller|
|Ghoul|ghoul|NECROPHAGE|30 /5|Existing procedural controller|
|Noonwraith|noonwraith|SPECTER|140 /10|Generic carrier + native AI|
|Foglet|foglet|NECROPHAGE|45 /7|Existing procedural controller|
|Fiend|fiend|RELICT|220 /14|Generic carrier + native AI|
|Endrega|endrega_warrior|INSECTOID|76 /6|Generic carrier + native AI|
|Katakan|katakan|VAMPIRE|80 /8|Generic carrier + native AI|
|Werewolf|werewolf|CURSED|120 /9|Generic carrier + native AI|
|Specter|wraith|SPECTER|45 /6|Generic carrier + native AI|
|Griffin|griffin|HYBRID|180 /11|Generic carrier + native AI|
|Grave Hag|grave_hag|NECROPHAGE|120 /9|Generic carrier + native AI|

All entity IDs have witchersbestiary namespace. ContractSpecies is the approved alias/type registry; no EntityType duplicates, models or animations added. Per-species JSON definitions, native category/loot/attributes and ContractAbility compatibility sets remain separate small data sources. New types use a MobEntity ContractTargetAccess carrier with exact contract UUID/profile NBT; existing Drowner/Ghoul/Foglet fields/controllers are preserved. Seventeen board definitions:12 procedural,3 original ordinary kill contracts,2 fixed legendary hunts. Infernal Basilisk, Nekker, Water Hag and other unapproved species are NOT added to procedural pool.

## 2. Names

Existing3 pools remain unchanged. Each new species has8 RU/EN name entries combined with8 existing thematic epithets:64 base combinations/species. New BossBars also carry the localized Common/Rare/Epic label. The board offers a species hunt; identity is rolled once at acceptance, not on opening/reloading the GUI. Seed, name/epithet indices, tier, attributes and compatible markers are serialized. Existing saved names retain their old formatting. RU/EN key parity checked; actual Russian visual layout still needs manual review.

## 3. Rarities and attributes

Common/Rare/Epic retained for renewable procedural hunts; Swamp Lord and Grave Devourer remain fixed Legendary. Uncommon and generalized renewable Legendary tiers are not implemented. New9 use registered default attributes rather than a guessed common template: health multipliers1/1.15/1.3, damage1/1.1/1.2, HP jitter±5%. Original3 retain former rolls (HP1.5/2/3; damage1.1/1.25/1.5). Profiles are instance-local. Contract-bound targets are excluded from world progressive difficulty; ordinary monster progression is unchanged.

## 4. Abilities and form correctness

Original procedural pools remain: Drowner charge/frenzy/poison/summoning; Ghoul charge/frenzy/lifesteal/poison; Foglet illusions/phase shift. New9 currently select INNATE, explicitly delegating to their existing species controller, cooldowns, phases, serialization and summon limits. **This is not a new randomized extra-power catalogue for those9.** No artificial duplicate root/poison/illusion engine or extra summon spam added. Full per-species optional trait diversity remains pending.

Named Werewolves remain beasts in daylight and preserve rolled HP/damage across NBT/form handling; ordinary day/night behavior is unchanged. Named Katakan restores its rolled beast HP limit after bat form, keeps capped native recovery, and BossBar reports stored beast health during the8HP bat decoy. Ordinary Katakan behavior retains native values. These form adapters and baseline regression tests pass; full human combat/form balance matrix is not claimed.

## 5. Emerald rewards and service boundary

Reward calculation is the public ProceduralHunts.snapshot boundary; delivery remains the existing ContractRewards service. Immutable ContractDefinition.Reward uses generic item IDs, enabling a future currency adapter without rewriting accepted rewards. No crowns added.

New9: threat=clamp(ceil(rolledHP/50),1,6); Common=4+floor(threat/2), Rare=14+threat, Epic=24+2×threat emeralds; XP30/60/90. Original3 retain3/6/10emeralds and their snapshots. Fixed legendary values remain unchanged12/14emeralds. Thus the proposed unified economy/legendary reward uplift is **not fully adopted**; manual economy/balance review remains. No extra guaranteed contract trophies added; native loot/trophies continue. Reward snapshots cannot reroll on reload; exact target death, owner and once-only claim checks remain authoritative.

## 6. Territory and large targets

Same global1ms cooperative hunt queue, loaded chunks only. Existing water-bank rule for Drowner; forest rule for Ghoul/Foglet/Leshen/Endrega/Werewolf/Katakan; supported dry surface fallback for remaining species. These are preliminary ecology rules: dedicated plains/mountain/graveyard affinity and offer filtering when no reachable habitat exists are not complete. A search can wait, rather than invent coordinates or generate chunks.

Before selecting a lair, the complete EntityType bounding volume must fit loaded, collision-free space; actual entity bounds are checked again before reservation/insertion. No forced chunk loads or background-thread world API. This prevents large entities relying on a Ghoul-sized clearance test. Flying controllers remain native; Griffin aerial gameplay has not been manually verified here.

## 7. Two clues and persistence

New routes keep Stage20F RouteVersion2: exactly two safe distinct sequential clues, separate lair approach within3blocks, unique target creation, then board reward. Approximate16cell centres are within11.314horizontal blocks of the clue (20block advertised area). Existing4.5block study reach retained. The five existing visual types are selected by species; no new clue entities or permanent blocks. Legacy route versions/counts are not regenerated. Entity/contract profile aliases now validate endrega_warrior and wraith correctly. Owner/order/UUID/dimension/death/spectator/replay protection retained. Active named target cap remains one/player.

## 8. Automatic village architecture

Original new-generation hook retained and supported through the shared tag. VisitedVillageBoards additionally inspects one player's current loaded chunk structure references every100ticks, up to32 references and only already loaded start chunks. Jobs are keyed by dimension/structure/origin. One job chunk scan/placement step per5ticks, round-robin, maximum16jobs and256chunks/village. No global villager/bell/world scan, locate, regeneration or chunk tickets in production.

Central-piece-first deterministic search, original safe placement, no occupied building block replacement or road support. Section palettes detect existing boards even below foliage, rather than relying only on heightmaps. Scan footprint before new placement; missing chunks wait for ordinary visits. VillageBoardLedger persists terminal outcomes only, with dirty-on-change and read-only preservation of future schemas. Pending jobs expire after inactivity (at least2400ticks, scaled with player count) and can be rediscovered, so abandoned jobs do not permanently occupy all16slots.

## 9. Existing villages and configuration

config/witchersbestiary-village-boards.json: automaticVisitedVillages=true by default. Set false and restart to disable automatic visit retrofit; new-generation hook remains. No personal configs modified. Existing complete boards are recognized. No-site/oversized footprints receive a saved diagnostic outcome and no terrain destruction. /wbboard repair remains operator fallback, not the normal path; it now detects covered boards through loaded section palettes.

Visits may need to load more of a large village naturally before completion; villages over256chunks are conservatively refused for automatic retrofit. No guarantee of placement in every possible custom/large/overlapping structure. Terminal outcomes do not automatically replace a deliberately destroyed board; use administrative recovery. These limits are part of RC3 validation, not hidden successes.

## 10. CTOV

Exact installed CTOV3.6.3 identified read-only. Lithostitched1.6.3 and Cloth Config15.0.140 used. The tag adds63 explicit optional surface village IDs across small/medium/large variants, derived from actual CTOV start pools. Outposts and underground variants excluded; no namespace wildcard or mandatory CTOV dependency. Vanilla-only production run also passed.

Loom1.7.4 cannot consume the new Lithostitched ClassTweaker header; a discarded header-only development adapter still hit unsupported refmap-less mixin remapping. Final successful tests used **original unmodified CTOV/Lithostitched/Cloth JARs in production/intermediary namespace**, with the actual RC3 game JAR and separately remapped review harnesses. No dependency/game file was patched. Development-toolchain limitation is distinct from production compatibility.

## 11. Moog's

Installed MVS5.0.9 (MoogsVoyagerStructures) and moogs_structures2.0.4-fabric identified. MVS has129 standalone structure definitions, no declared village structure tags; villager_statue is not treated as a village. Both original JARs co-loaded in the CTOV savanna production test; actual registry assertion confirms the statue is excluded. No unsupported claim of universal boards on all Moog buildings. Real village/datapack IDs can extend witchersbestiary:notice_board_villages without a required mod dependency.

## 12. Natural worldgen results

Normal worlds, fixed seeds, no flat fixture or biome replacement. Bounded diagnostic locate in the isolated review process selected the structure; this is NOT production placement logic. Actual board cells and real GUI were checked, not only a placement return value. Centre below is the horizontal centre of the structure piece bounding-box union (first two read from saved Anvil data, later cases recorded during review), not a guessed bell location.

|Seed|Structure|Centre X/Z|Initial board X/Y/Z|Distance to centre|Initial boards|
|---|---|---|---|---:|---:|
|202001|minecraft:village_plains|928.0, -294.5|916, 97, -292|12.26|1|
|202001|ctov:small/village_plains|745.5, 872.5|740, 80, 868|7.11|1|
|42|minecraft:village_desert|-1616.0, -1789.5|-1608, 69, -1784|9.71|1|
|8675309|ctov:small/village_savanna|-891.0, -355.5|-893, 69, -349|6.80|1|

Vanilla plains village phase passed, but the broader early development hunt run later failed a harness assumption; that whole run is NOT counted as PASS. CTOV plains production full hunt passed. Desert and CTOV savanna saved-village scenarios completed three dedicated JVM runs each.

## 13. Saved-village upgrade scenarios

For actual naturally generated desert/savanna villages, after validating the original GUI, the review removed only the test board, saved/stopped the isolated world and backed up its ledger outside that world. Reopening therefore emulated a pre-installation saved village with structure data but no board/processed key. No chunks were regenerated and no repair command was used. Automatic visit placement restored one complete board, real GUI worked, then another JVM restart retained exactly one complete board and the saved ledger. This is a controlled upgrade fixture, not a claim that an arbitrary personal RC2 save was opened.

|Seed / structure|Automatically restored board|After next restart|
|---|---|---|
|42 / minecraft:village_desert|-1608, 69, -1784|1 board; GUI PASS|
|8675309 / ctov:small/village_savanna|-903, 71, -343|1 board; GUI PASS|

Final savanna test includes original CTOV, Lithostitched, Cloth and both Moog JARs. Earlier savanna attempt was invalidated: a heightmap-only review scan missed a covered board and permitted manual fallback; it also hit Windows IPC file sharing. Final review uses full section palette/block verification, forbids manual fallback in village-only mode, and retries only transient IPC sharing failures for at most100ms. The final complete.json is the valid evidence.

## 14. Automated tests and build

**Final50/50 required GameTests PASS.** Command: `.\gradlew.bat runGametest build -Pstage19fProfile=A --offline --console=plain`. Log build/stage20g-release-final.log; BUILD SUCCESSFUL in1m1s. Original44 tests retained;4 Stage20G tests cover all12 complete cycles in a combined scenario, real multi-tick generic target/NBT reload, visited-village physical placement/covered-board detection/idempotent ledger, future-ledger preservation;2 recipe tests below. Old fixed offer-count assertions were updated8→17; no tests deleted. Empty/unsafe two-clue routes still wait; spawn checks were not weakened.

Server fixtures disable AI for the combined12 lifecycle test; this is not a12-species combat benchmark or animation review. Optional Trinkets/CCA are not in profileA. Native production uses real connections instead of GameTest mock players.

## 15. Actual client and multiplayer

Two independent native client JVMs plus isolated dedicated server. Actual RC3 release JAR, standard packets and production namespaces. CTOV plains run completed Endrega and legendary Grave Devourer: private clue/foreign UUID rejection, owner logout while peer advanced, death/respawn, Nether roundtrip, tracking JVM restart, two-clue/lair sequence, independent target UUIDs, actual unload/reload, combat, exact owner rewards, stale/double claims and post-reward JVM restart/replay denial. Review uses operator teleports, equipment/regeneration and automated attacks; not human survival balance. Bodies were allowed to become tracked before asserting lookup; players approach beside, not inside the spawn volume.

Later village runs used two real clients for generated/saved-village GUI checks and three JVM runs. Screenshots/logs/journals are preserved under ignored build, including actual CTOV/Moog coexistence. Core combat run preceded the final recipe/text-only changes and visited-job refinements; final recipes were verified by the final50 tests and subsequent native joins. No claim of every species' special attacks being visually verified.

## 16. Performance

Production CTOV Endrega/Ghoul two-hunt window: mean server processing **23.346ms/tick**,414ticks. Short controlled diagnostic, doMobSpawning=false, view distance8, operator assistance. Catch-up wall-clock tick rate is not a sustainable TPS measurement; no long-run percentiles/FPS guarantee. Prior accelerated1/5/10 tests remain in regression and are explicitly synthetic, not new12-species/modpack stress tests. Baseline environments differ, so no claimed matched speedup.

Habitat membership sets are reused rather than allocated per candidate. No permanent entity/chunk retention, disk writes every tick or background world API. Worldgen diagnostic locate costs are recorded per case; production visit logic never runs locate.

## 17. Local artifact and dependencies

`build/libs/witchersbestiary-0.20.0-rc3.jar` — **8274984bytes**. SHA256 `60a56cbe834ce06f33bde1ac5d7883dd58c7531c941b151e554fce1d79e4511a`.

Minecraft1.21.1, Java21 (class major65), Fabric Loader>=0.17 (tested0.19.5), Fabric API (tested0.116.17+1.21.1), GeckoLib>=4.8.4<5 (tested4.8.4), Patchouli exactly1.21.1-93-FABRIC. Optional CTOV/Moog/Cloth/Lith jars are not embedded or mandatory.

ZIP, all JSON, metadata,12 procedural definitions, both recipes/advancements and532-key RU/EN parity checked. No harness/test classes, test tags, logs or nested dependency jars in game JAR. All551 non-language assets,53 existing recipes and9 worldgen resources match RC2 byte-for-byte. No new model/texture/music assets or third-party binaries added to Git/JAR. Baseline license unchanged; no ownership claim over test dependencies. RC1/RC2 game artifacts retained with original SHA256; stable0.19.0 untouched.

## 18. Known limits / pending release gates

- New9 use existing native ability sets, not a fully diversified random additional-trait engine. No Uncommon/general procedural Legendary rollout; legacy/special reward normalization and economic playtesting pending.
- Habitat/structure affinity and filtering offers without reachable habitat are preliminary. A hunt can wait if its territory is unsuitable.
- Not all63 CTOV variants, large/overlapping villages, special terrain or player-built settlements tested. Retrofit cap256chunks, incomplete loaded footprint waits, strict site refusal and admin fallback remain intentional limits. Full survival walking/accessibility around every board is not proven by operator GUI tests.
- Fresh/saved scenarios cover two vanilla types and two CTOV types on three seeds, not every world. The saved-village upgrade fixture is controlled, not arbitrary old-world migration validation.
- Native battles for Griffin flight, big Leshen/Fiend, Noonwraith/Specter and Katakan transformations still need manual/category coverage. Native controller/helper cleanup caps were reused, not universally re-benchmarked.
- Full user modpack/BetterCombat/SpellEngine/Trinkets/SkillTree compatibility not newly verified. Stage20F HUD/minimap positioning options and through-obstacle visual matrix remain outstanding.
- No power-loss cross-file atomicity or long-duration production load test. No public READY claim.

## 19. Evidence and reproduction

Ignored logs: stage20g-release-final.log, stage20g-production-native.log, stage20g-vanilla-retro.log, stage20g-ctov-moog-final.log, stage20g-artifact-audit.json and stage20g-worldgen-summary.json under build/. Each native case contains complete.json, incremental scenario-results.json, per-role logs, screenshots, original copied runtime mods and the saved worlds.

- `build/stage20e-network/run-20261030-202554`: village phase PASS; later harness failure.
- `build/stage20e-network/run-20261030-210906`: production full hunt PASS.
- `build/stage20e-network/run-20261030-212959`: production saved-village PASS.
- `build/stage20e-network/run-20261030-215301`: production CTOV/Moog saved-village PASS.

Production harness: Gradle stage20ePrepareLaunch/remapStage20gServerHarness/remapStage20gClientHarness with profileA; tools/prepare-stage20g-production.py --mods-dir <installed mods directory> (--without-ctov or --with-moog as appropriate); tools/run-stage20e-network.py with STAGE20G_PRODUCTION=1 and STAGE20G_VILLAGE=1. Village-only cases additionally set STAGE20G_VILLAGE_ONLY=1, seed, exact village ID and tag. Whole process bounded900seconds plus bounded cleanup. Ledger withholding only touches a resolved path inside a newly created ignored review case, preserving its backup; never personal worlds/mods.

## 20. Before public release

Complete remaining native combat, ecology/ability/reward balance, large-village and full modpack gates; review manual visual UX; repeat targeted failures only. See separate STAGE20_G_PLAYER_GUIDE.md. No push, GitHub Release, history rewrite, stable-tag change or Stage21/crowns implementation performed.

## 21. Black dye crafting — additional Stage20G task

Existing items reused: witchersbestiary:nekker_claw and witchersbestiary:drowner_claw. Independent recipes witchersbestiary:black_dye_from_nekker_claw and witchersbestiary:black_dye_from_drowner_claw, each1claw→2 minecraft:black_dye, vanilla crafting_shapeless/misc serializer and black_dye book group. Files: data/witchersbestiary/recipe/black_dye_from_{nekker,drowner}_claw.json, plus matching advancement/recipes files. No new item/drop or vanilla recipe override.

Inventory-change advancement rewards unlock only the corresponding recipe; independent recipe_unlocked fallback follows vanilla pattern. Two new GameTests verify RecipeManager registration, actual first matching recipe and exactly2output in every slot of both2×2 and3×3 inputs (26placements total), natural screen-handler inventory criterion/book reward, opposite recipe still locked, and original ink-sac/wither-rose recipes still giving1dye. Final50/50/build PASS includes both.

REI/EMI not found in the configured installed mods. JEI19.27.0.340 was detected but not included in these runtime profiles; no viewer UI PASS claimed. Recipes use the normal vanilla recipe type rather than a custom viewer plugin. Manual mouse-driven crafting/viewer inspection remains NOT TESTED.
