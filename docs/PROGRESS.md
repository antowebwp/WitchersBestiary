# Progress

## Stage21G — completed local implementation and verification (2026-11-05)

Five basic signs, independent concentration, Survival lesson recipes, R-hold radial/G cast, configurable RU/EN HUD, durable per-player progress, authoritative packets and fist/PvP protection implemented. One full115/115 suite; final targeted29/29; build/13 release mixin checks PASS. Two real BC/SS/SpellEngine/SpellPower clients passed five-sign gameplay, GUI2/3, remapping, held-key suppression and dedicated restarts without resource/cooldown refill. SkillTree NOT TESTED; no required dependency or claimed integration. [Stage21G report](STAGE21G_REPORT.md) records evidence and limits. No push/release or21H. Earlier status entries are historical.


## Stage21F — completed local implementation and verification (2026-11-05)

Four vanilla1.21.1 sword enchantments I–III implemented with bounded authoritative effects and persistent weapon charges/cooldowns. All94 prior regression tests passed in the one full run; corrected focused sword checks passed with BC14/14 and without optional mods15/15. Build and13 release mixin/refmap checks PASS. Two native BC2.3.2/SS1.63 clients passed all four Survival table/book/anvil operations, vanilla/one-hand/two-hand/unique sword combat, offhand-only Rune ownership and exact earned-state dedicated restart. Evidence and limits: [Stage21F report](STAGE21F_REPORT.md). No push/release or Stage21G. Older status entries are historical.


## Stage21E — completed local implementation and verification (2026-11-05)

The existing alchemy_workbench now browses/crafts17 real RecipeManager recipes (six existing elixirs, eight oils, three refinements) with exact server-owned inventory planning, missing counts, original output/storage and RU/EN GUI. Original effects, application, block ID and four-slot NBT remain intact. One original GeckoLib Arcane Merchant has saved VILLAGE/WANDERING modes, crown+wallet atomic purchasing, shared limited stock and stable settlement UUID claims. Actual vanilla and CTOV village discovery, production wandering placement, existing failed-claim migration, two native Better Combat clients, RU/EN GUI2/3, purchases, shared table crafting/Shift-click and dedicated restarts passed. Full94/94 GameTests plus necessary related repeats (alchemy3/3, merchant5/5), build and12 release mixin/refmap checks PASS. [Stage21E report](STAGE21E_REPORT.md) records evidence, economy and compatibility limits; [content audit](STAGE21E_ALCHEMY_AUDIT.md). Moog village compatibility remains unconfirmed; no blanket structure-mod claim. Stage21A–D and contract hotfix preserved. Stage21F not started; no push/release. Older entries are historical.


## Stage21D — completed local implementation and verification (2026-11-04)

Duel Stone, original volume model/recipe/loot, voluntary two-player consent/countdown, shared virtual fists and stamina, remote poses, nonfatal KO, confirmed surrender, arena grace and lifecycle cleanup implemented. Two real Better Combat clients completed full100VHP KO/rematch/surrender/boundary/disconnect/stone destruction and same-world dedicated restart; no realHP/inventory/crowns/XP loss. Separate pvp=false two-client campaign safely refused both. Existing83 tests passed in the one combined89-test run; all6 new duel scenarios passed on corrected targeted rerun (7/7 with smoke). Final build and12 mixin/refmap checks PASS. [Stage21D report](STAGE21D_REPORT.md) records evidence, controls and protection-adapter limitations. Stage21A/B/C and contract hotfix preserved. Stage21E remains unimplemented; no push or release. Older entries are historical and do not supersede this status.


## Contract reliability hotfix - 0.21.0-dev (2026-11-04)

Confirmed GUI abandonment, pre-acceptance two-clue route validation, saved-route recovery, immediate/persistent tracking with40..60-block new clue spacing, and per-target contract progressive phase are implemented. One83-scenario combined run plus necessary related repeats completed; corrected classes passed6/6,7/7 and3/3. Native two-client Katakan investigation/kill/reward/cancel/restart and clean HUD/confirmation review passed. Stage21B/C preserved; no21D, push or release. See [hotfix report](CONTRACT_RELIABILITY_HOTFIX.md) and [player guide](RENEWABLE_CONTRACTS_GUIDE.md). Historical status entries below do not supersede this follow-up.


## Stage21B fixes + Stage21C � implemented and locally verified (2026-11-04)

Movable persistent right-side HUD, repaired seams on all three original fighters, responsive AI and personal player/NPC reward cooldowns are implemented. Rock Troll has an original articulated reference-guided model, neutral/revenge combat, rock throw/stomp/guard, rare natural spawning and confirmed nonfatal90-crown bouts. Two real native clients independently earned90 for the SAME persisted troll; both balances survived dedicated restarts. Fresh normal-world natural spawning was observed. Final76/76 GameTests, Gradle build and12 release mixin/refmap checks passed. Native four-angle/motion review is documented in the [Stage21C report](STAGE21C_REPORT.md); [Stage21B follow-up](STAGE21B_REPORT.md) and [player guide](FIST_FIGHTING_GUIDE.md).

Development branch dev/0.21.0. Local checkpoints only; no push, release or Stage21D. Older entries below are historical and do not supersede this status.


## Stage21B — NPC fist fighting implemented

Virtual nonlethal fights, three original animated NPC variants, authoritative inputs/HUD and once-only crowns.69/69 tests/build PASS; two-client Better Combat campaign, restart and unpaid winning rematch verified. NPC placement is manual, no new village worldgen. [Report](STAGE21B_REPORT.md), [guide](FIST_FIGHTING_GUIDE.md). Stage21C not started; no push/release.

## Stage21A — crowns and wallet implemented

64/64 GameTests/build PASS; two native wallet GUIs, transfers and saved-world recovery validated. New contracts pay crowns; existing snapshots retain emeralds. Local0.21.0-dev checkpoint only. [Report](STAGE21A_REPORT.md), [progress](STAGE21_PROGRESS.md). No21B or publication.

## Stage21 — development started

Working branch dev/0.21.0 from published0.20.0. Currency arithmetic/component foundation only; gameplay not yet integrated. [Progress](STAGE21_PROGRESS.md), [scope](STAGE21_MASTER_PLAN.md). No release or push.

## Stage20H final fixes — RC5 validation pending

Origin-independent village references, separate legendary category and bounded terminal history implemented. Full native five-hunt/rotation/RC3 migration gates remain; see [report](STAGE20_H_REPORT.md). No release/push/Stage21.

## Stage20H — RC4 built, validation pending

Village offer generations, five-offer board integration, per-offer acceptance identity, lazy72000-tick rotation and personal refresh implemented. 53/53 tests/build PASS. Native release gates and identity/legendary/history limits remain; see [report](STAGE20_H_REPORT.md). No push or Stage21.

## Stage20G — RC3 built, validation pending

12/12 approved procedural species integrated with existing entities, saved named profiles and two-clue/lair lifecycle. Automatic visited-village ledger and63 optional surface CTOV IDs added. Real production CTOV hunt/reward/restarts and natural/saved vanilla+CTOV villages verified; Moog coexistence without misclassifying a statue. Two independent claw→2black-dye recipes/book unlocks added. Final50/50 GameTests and Gradle build PASS. Extra trait diversity, broader ecology/large-village/full modpack/manual combat gates remain. [Report](STAGE20_G_REPORT.md), [player guide](STAGE20_G_PLAYER_GUIDE.md). No push/release or Stage21.


## Stage 20D — Witcher Senses and investigations

V-toggle private clue rendering, persisted species-specific routes, authoritative right-click study, journal progress/findings and lair-triggered existing target spawn implemented for procedural/legendary hunts. All38 focused tests/build PASS; native EPIC Foglet and legendary Grave Devourer cycles plus mid-investigation world rejoin PASS. Evidence, configuration and performance limits: [report](STAGE20_D_REPORT.md). No push/release or Stage20E/21.


## Stage 20C.3 — procedural named hunts

Drowner/Ghoul/Foglet renewable procedural hunts implemented with persisted RU/EN names, rarity/attributes, compatible traits and existing search/store/reward/BossBar integration. Swamp Lord and Grave Devourer remain legendary. All32 focused tests/build PASS; native cycles for all three species and ACTIVE-world rejoin PASS. Controlled accelerated1/5/10-hunt measurements and limits are in the [report](STAGE20_C_REPORT.md). No push/release or Stage21.


## Stage 20C.2 — Grave Devourer hunt

Second fixed named hunt implemented: existing Ghoul model/AI, 120HP, safe telegraphed leap, one-time rage and capped on-hit healing; shared search, persistence, board and BossBar. All25 focused tests and Gradle build PASS; native board-to-reward cycle PASS. See [report](STAGE20_C_REPORT.md). No push/release.


## Stage 20C.1 — Swamp Lord hunt

0.20.0-dev: named-target binding in existing contract state, loaded-only budgeted bank search,108HP/9damage Drowner variant, dash, capped helpers, local BossBar, hunt statuses and ordinary board reward flow implemented. Native acceptance/combat/completion/claim cycle verified; see [report](STAGE20_C_REPORT.md). Other two bosses remain unimplemented. No push/release.

## Stage 20B — notice boards and contract interface

0.20.0-dev: original 163-cuboid board, exact multiblock collision/cleanup, village placement hook, localized journal GUI, secure player acceptance/claims using 20A. Native normal-world plains generation and real client acceptance verified. Focused checks/limitations: [Stage 20B report](STAGE20_B_REPORT.md). No push/release; Stage 20C not started.

## Stage 20A — contract foundation, 0.20.0-dev

Public-history development branch dev/0.20.0 starts at published v0.19.0. Server contract definitions, lifecycle, PersistentState, kill attribution, rewards and operator-only test commands implemented. Focused tests and build are summarized in [Stage 20A report](STAGE20_A_REPORT.md). No Stage 20 push/publication; previous release unchanged.

## Released baseline

Witcher's Bestiary v0.19.0 is published. Original development history remains separate and backed up; excluded third-party references/internal diagnostics are not restored into this history.

## Stage20E — local RC1, validation pending

44/44 focused GameTests PASS; Gradle build PASS. Stability fixes and approximately3x graveyard candidate density implemented. See [Stage20E report](STAGE20_E_REPORT.md) for real-network evidence and remaining checks. No push/release or Stage21.

Two real native clients completed independent Ghoul investigations/combat/rewards, unload/reload and two dedicated restarts; repeat rewards rejected. Remaining basilisk, optional-stack and natural graveyard validation stays pending.

## Stage20F — RC2 built, validation pending

Versioned two-clue routes and separate lair approach, approximate area HUD, bright raised outline, extensible village tag/central chunk candidates and operator repair implemented. Full44 tests/build PASS; natural village and full visual/Moog runtime checks pending. See [report](STAGE20_F_REPORT.md). No push/release/Stage21.

Stage20F two-real-client normal-world investigation-to-reward and two dedicated restarts PASS after bounded repeat. Remaining natural village and visual/Moog requirements prevent full readiness.
