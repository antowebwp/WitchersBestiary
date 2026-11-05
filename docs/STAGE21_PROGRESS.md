# Stage21 progress

## Stage21H — completed compatibility verification (2026-11-05)

Actual Skill Tree RPG Series skill_tree_rpgs1.4.4+1.21.1 and its pinned Pufferfish/RangedWeapon dependencies verified. Optional client-only legacy K/K dodge conflict fixed without changing signs or progression. Full123/123 GameTests, build/13 release checks and four real two-client WITH/WITHOUT gameplay/HUD/restart campaigns PASS. No required Skill Tree dependency or new branches/bonuses. [Stage21H report](STAGE21H_REPORT.md) records exact versions, evidence and limits. No push/release/21I. Earlier entries are historical.


## Stage21G — completed local implementation and verification (2026-11-05)

Five basic signs, independent concentration, Survival lesson recipes, R-hold radial/G cast, configurable RU/EN HUD, durable per-player progress, authoritative packets and fist/PvP protection implemented. One full115/115 suite; final targeted29/29; build/13 release mixin checks PASS. Two real BC/SS/SpellEngine/SpellPower clients passed five-sign gameplay, GUI2/3, remapping, held-key suppression and dedicated restarts without resource/cooldown refill. SkillTree NOT TESTED; no required dependency or claimed integration. [Stage21G report](STAGE21G_REPORT.md) records evidence and limits. No push/release or21H. Earlier status entries are historical.


## Stage21F — completed local implementation and verification (2026-11-05)

Four vanilla1.21.1 sword enchantments I–III implemented with bounded authoritative effects and persistent weapon charges/cooldowns. All94 prior regression tests passed in the one full run; corrected focused sword checks passed with BC14/14 and without optional mods15/15. Build and13 release mixin/refmap checks PASS. Two native BC2.3.2/SS1.63 clients passed all four Survival table/book/anvil operations, vanilla/one-hand/two-hand/unique sword combat, offhand-only Rune ownership and exact earned-state dedicated restart. Evidence and limits: [Stage21F report](STAGE21F_REPORT.md). No push/release or Stage21G. Older status entries are historical.


## Stage21E — completed local implementation and verification (2026-11-05)

The existing alchemy_workbench now browses/crafts17 real RecipeManager recipes (six existing elixirs, eight oils, three refinements) with exact server-owned inventory planning, missing counts, original output/storage and RU/EN GUI. Original effects, application, block ID and four-slot NBT remain intact. One original GeckoLib Arcane Merchant has saved VILLAGE/WANDERING modes, crown+wallet atomic purchasing, shared limited stock and stable settlement UUID claims. Actual vanilla and CTOV village discovery, production wandering placement, existing failed-claim migration, two native Better Combat clients, RU/EN GUI2/3, purchases, shared table crafting/Shift-click and dedicated restarts passed. Full94/94 GameTests plus necessary related repeats (alchemy3/3, merchant5/5), build and12 release mixin/refmap checks PASS. [Stage21E report](STAGE21E_REPORT.md) records evidence, economy and compatibility limits; [content audit](STAGE21E_ALCHEMY_AUDIT.md). Moog village compatibility remains unconfirmed; no blanket structure-mod claim. Stage21A–D and contract hotfix preserved. Stage21F not started; no push/release. Older entries are historical.


## Stage21D — completed local implementation and verification (2026-11-04)

Duel Stone, original volume model/recipe/loot, voluntary two-player consent/countdown, shared virtual fists and stamina, remote poses, nonfatal KO, confirmed surrender, arena grace and lifecycle cleanup implemented. Two real Better Combat clients completed full100VHP KO/rematch/surrender/boundary/disconnect/stone destruction and same-world dedicated restart; no realHP/inventory/crowns/XP loss. Separate pvp=false two-client campaign safely refused both. Existing83 tests passed in the one combined89-test run; all6 new duel scenarios passed on corrected targeted rerun (7/7 with smoke). Final build and12 mixin/refmap checks PASS. [Stage21D report](STAGE21D_REPORT.md) records evidence, controls and protection-adapter limitations. Stage21A/B/C and contract hotfix preserved. Stage21E remains unimplemented; no push or release. Older entries are historical and do not supersede this status.


## Stage21B — completed NPC-fighting scope

Three original GeckoLib NPC variants, server-owned virtual fights, confirmation/countdown, quick/heavy/guard/dodge/counter, stamina, knockout, HUD and once-per-NPC crown prizes implemented.69/69 required GameTests and build PASS. Two native clients with Better Combat completed independent bouts, victory/defeat, wallet payouts and restart; a winning rematch paid nothing. Final HUD/knockout reviewed. NPCs are placed with eggs/operator summon; settlement worldgen is not added. [Report](STAGE21B_REPORT.md), [player guide](FIST_FIGHTING_GUIDE.md).

## Stage21A — implemented

Physical crowns, bound component wallet/GUI, atomic transfers, pickup and new-contract rewards preserving old emerald snapshots.64prior tests retained in the69-test regression. See [report](STAGE21A_REPORT.md).

Local artifact remains0.21.0-dev, not the full Stage21 RC1.21C–21J unimplemented; no push/publication. Current21B safe cleanup freed approximately264.2MiB; previous blocked files untouched.

## Stage21B follow-up, 2026-11-04
HUD editor/persistence, three fighter geometry seams, responsive AI and personal per-NPC rewards validated. Two native clients each received12 for the same novice; a saved-world training win did not repeat payment. Four-angle/pose review completed for all three original fighters. See STAGE21B_REPORT.md. Stage21C implementation exists; its final native checks remain in progress.


## Stage21C � completed local implementation and gameplay verification, 2026-11-04

Neutral troll combat, all five attacks/defence modes, rare natural spawning, egg/loot/bestiary and confirmed nonfatal shared bouts are implemented. Real clients WBOne/WBTwo separately won90crowns against the same persisted troll; dedicated restart reloaded both90. Full76/76 tests and Gradle build passed. The original model was rebuilt around the user's turnaround (40 bones,202 cubes,512 atlas,18 clips), with four idle views and full motion review. See STAGE21C_REPORT.md for evidence and limits. No push/release/Stage21D.

## Contract reliability hotfix - 0.21.0-dev (2026-11-04)

Confirmed GUI abandonment, pre-acceptance two-clue route validation, saved-route recovery, immediate/persistent tracking with40..60-block new clue spacing, and per-target contract progressive phase are implemented. One83-scenario combined run plus necessary related repeats completed; corrected classes passed6/6,7/7 and3/3. Native two-client Katakan investigation/kill/reward/cancel/restart and clean HUD/confirmation review passed. Stage21B/C preserved; no21D, push or release. See [hotfix report](CONTRACT_RELIABILITY_HOTFIX.md) and [player guide](RENEWABLE_CONTRACTS_GUIDE.md). Historical status entries do not supersede this follow-up.
