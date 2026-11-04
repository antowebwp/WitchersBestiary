# Contract reliability + progressive difficulty hotfix

Development branch dev/0.21.0. Existing Stage21B/C work preserved. Local only: no push, release, v0.20.0 changes or access to the separate game installation.

## Causes

procedural_katakan is valid: registered katakan target, required1, overworld eligibility. The approved species mapping and entity factory exist. The broken0/0 is the investigation's empty clue list, not the kill objective.

Previously acceptance immediately committed SEARCHING with no lair/clues. The loaded-chunk-only search probed an origin-centred150..500 ring. That ring can lie outside the loaded view distance, and forest/terrain checks further narrow valid sites. After128 failed probes it left WAITING; a new search was queued only after crossing a chunk/rejoining. The HUD formatted the empty route as0/0. Saved SEARCHING/WAITING records are valid under the existing schema, so reload faithfully preserved the blockage. There was only an operator cancellation command.

A separate client synchronization race cleared tracking on world change and could discard a snapshot sent before the new world was ready. The server suppressed identical subsequent snapshots, leaving HUD blank until a state change. The client now explicitly requests a fresh authoritative snapshot when its world changes, even if senses were disabled.

ProgressiveDifficulty.eligible explicitly excluded every entity with a contract binding. Thus existing contract freshSpawn calls could not apply progressive HP, outgoing damage or armor bypass. No new difficulty formula is introduced.

## Acceptance, recovery and target creation

Board and player-facing acceptance validate a complete route before committing the contract/offer receipt. There are128 bounded territory probes: original distant preference followed by a48..128 loaded-neighbourhood fallback. No chunk generation/tickets are used by production planning. Failed preparation leaves the offer available and explains the failure in RU/EN GUI; reopening allows a new attempt as conditions change. Peaceful difficulty is rejected for hostile hunt preparation.

All approved species use the registered type/factory and existing profile/rarity model. Safety checks include loaded footprint, suitable ground/habitat, full entity volume, fluid, height, world border and actual spawn collision. The contract target is created directly, bound to the exact instance UUID and persisted; it does not wait for random natural spawning. A reserved/live target UUID prevents a second spawn. If the lair is obstructed, bounded nearby surface probes retry within12 blocks. If terrain has become wholly unsuitable, the contract remains explicitly recoverable/cancellable rather than granting rewards or duplicating targets.

Old valid routes retain their positions and IDs. An empty legacy investigation with saved lair/area can reconstruct its missing clues without rerolling its profile; missing coordinates use the bounded loaded search on join/chunk movement. Existing target UUIDs and dead/completed hunts cannot be reconstructed into another boss. If recovery cannot find safe terrain, GUI/command abandonment remains available. No village-wide state/history reset is performed.

## Tracking and spacing

New routeVersion3 records have two clues40..60 horizontal blocks apart (preferred50, bounded42/58 alternatives), validated ground points and a sampled safe surface connection. Existing versions0..2 remain readable and unchanged. Failure to place both clues rejects acceptance. Logical routes are saved before physical targets exist.

Tracking stores the selected Contract Instance UUID in an additive Tracking list in the existing ContractState schema. Acceptance and clue study send snapshots immediately. Areas are the current objective only, rounded to16-block cell centres with radius20; negative coordinates use floor division. The second clue/lair is not disclosed early. Tracking survives distance, board chunk unload, reconnect and server restart. Other dimensions show the target dimension and suppress false current-world coordinates. Claim/abandon clears tracking. The investigation HUD shares the existing persisted Stage21B position/scale, defaults right and moves clear of the fist panel while boxing.

## Cancellation

- Player command: /wbcontract abandon (no operator permissions). Selects only the caller's tracked ACCEPTED contract; an ambiguous untracked selection fails without changes. Completed/unclaimed work is protected.
- Existing operator syntax: /wbcontracts inspect <player>, then /wbcontracts cancel <instance-UUID>. Existing owner checks remain; this is not a command for cancelling another player's record.
- Board: open the active order, choose Abandon contract / Отказаться от контракта. The confirmation shows its name and warns about lost progress/no reward. Back leaves it untouched. Confirmation sends screen sync ID and Contract Instance UUID; the server validates the current board, distance, owner, visible instance and ACCEPTED state, then uses Contracts.cancel.

Cancelled clues deactivate, bound entities/helpers/boss bars are cleaned by the existing lifecycle, rewards become unclaimable and history remains bounded under Stage20H. Cancellation does not refresh shared offers, count as a paid completion, or affect another player. Five offers, village/generation/offer identities,72000-tick rotation, personal early refresh and separate legendary tab are retained.

## Progressive difficulty

Server configuration remains config/witchersbestiary.properties (ProgressiveSettings). Global overworld calendar day determines phase: floor(day/25) by default. Per phase, default HP adds30% of base (normal cap700/elite cap3500), outgoing damage adds15% (cap4x), armor bypass adds5 percentage points (cap85%). Existing exclusions, pets, human werewolf forms and illusion rules remain; vanilla bystanders are excluded.

Natural monsters keep the existing dynamic world phase. Contract targets snapshot the current server phase at creation, saved as WitcherContractProgressivePhase in entity NBT. Different player XP/progress does not change the global rule or another player's target. New targets can use a later phase; reloading an existing target retains its phase. Admin live configuration remains authoritative. Legacy already-spawned contract targets without the field migrate to phase0, preserving their prior unscaled balance.

Rarity retains its profile stats/abilities, then the existing progressive formula scales that base under existing caps. One persistent health modifier ID replaces itself, never stacks; outgoing damage and armor bypass use the shared server hooks once. Fresh targets fill their final HP; loaded targets retain current absolute HP. Werewolf NBT restoration explicitly preserves saved HP after form/attribute restoration, fixing a constructor-modifier fraction error exposed by the high-phase test. Katakan's8HP bat decoy remains fixed while its stored beast pool and health fraction use the scaled beast maximum.

Operator diagnostics: /wbcontracts difficulty <entity selector or UUID>. Reports species, ordinary/contract source, phase, rarity, maximum HP, effective attack damage and health modifiers. No per-spawn log spam or client-supplied difficulty values.

## Verification evidence

Implementation checkpoint58571ec. One combined full run:83 total,80 initially passed. Failures led to a real werewolf reload-health fix and two fixture updates for the wider valid route search. Related repeats passed: ProgressiveContractTests3/3, ContractHotfixTests6/6, InvestigationTests7/7. Full suite was not repeatedly run. Gradle build/release mixin checks pass. Native dedicated/two-client evidence follows below. The final tracked-unclaimed-reward command guard passed6/6 related tests in build/contracts-final-command-guard.log. The last change afterward only localizes command errors in RU/EN; no gameplay transition changed.

Tests cover all twelve registered species, natural initialization, common/rare/epic contract stats at phase0/2, fixed phase at later world age, NBT/HP preservation, no duplicate modifiers, Katakan bat pool, vanilla exclusion, safe failed acceptance, own/foreign cancellation, protected completed history, legacy route repair, private sequential clues,40..60 spacing, persisted tracking, approximate/negative coordinates and cross-dimension data.

The native scenario uses a bounded isolated forest/board fixture and controlled high player attack for packet/ownership/reward verification, not a combat-balance claim. Actual clients drive board acceptance, clue inspection, attacks, claims and confirmation buttons; rewards/winners are never edited by the controller.


## Native and visual evidence

- build/stage20e-network/run-20261104-185650/complete.json: real two-client Katakan acceptance, two private clue routes,40..60 spacing, far/cross-dimension snapshot data, actual client inspection/attacks/claims, two unique phase2 targets, GUI confirmation/back/abandon of saved empty legacy work, dedicated restart with a live target, saved cancellation/wallet/target UUID/HP/phase and tracked identity, second independent payout and rejected reward replay, ordinary GUI cancellation. Both clients earned50 crowns for their own instances. A later world calendar day did not rescale the saved target.
- run190849/complete.json and screenshots: latest readable confirmation, real visible far HUD and other-dimension HUD. Both50-crown wallets reloaded from the saved preceding world. Images WBOne-cancelPrompt.png, WBTwo-tracking-proof.png and WBTwo-dimension.png were visually inspected; no loading-screen/notification-obscured frames are used as final visual evidence.
- Earlier run185201 found the real dimension snapshot race and is excluded from successful evidence. run190431 verified data but its HUD screenshots were obscured by toasts/loading frames; run190849 supersedes those visual captures. A failed review setup expected an unrelated fist resume marker; the review runner now copies only the needed isolated world and skips that marker.

Current implementation checkpoints:58571ec (core) and007e274 (tracking/UI/native fixes). Stage21B/C assets and mechanics remain preserved. Positive early-refresh/cycle/isolation coverage remains in VillageOfferTests from the combined run; this hotfix does not claim a new native five-hunt early-refresh campaign.

## Practical limits

Preparation checks currently loaded terrain only. It safely rejects unavailable regions rather than generating remote chunks. A player can change terrain after acceptance; spawn safety is rechecked with bounded fallback, and unrecoverable work remains cancellable. Valid old routes are not moved to meet the new spacing rule. Unknown/corrupt save records retain the existing quarantine/future-schema safety policy. Natural initialization was exercised for all12 types; this is not a claim of observing random natural spawns of all12 species in the native fixture. Longer manual combat balance and optional modpack compatibility are not claimed.

Automatic approval review previously rejected temporary-world deletion with "blocked by policy". No bypass or deletion was performed. User worlds, separate game installation and published release/tag were untouched.


## Final artifact and checkpoints

Final source checkpoint4eb3912 protects an unclaimed tracked reward from accidentally abandoning some other accepted order, with localized RU/EN command errors. Related tests6/6 passed before the text-only localization change. build/contracts-final-artifact.log:final build PASS, including12 release mixin/refmap checks. Current artifact validation:build/contracts-final-artifact.json. No test controller/log leakage in the production JAR.

Artifact:build/libs/witchersbestiary-0.21.0-dev.jar, 8521188bytes.
SHA256:f3db9782ca15f786f819c9e947ac25d48f5ee6350d1e6bbb195e5d55d5ea12cc.

The combined83-test run was executed once. Its three failures were resolved and affected classes repeated; the final related results are6/6,7/7 and3/3. This is aggregate verification, not a claim of another uninterrupted83/83 run after every visual/text adjustment. The latest native cycle and visual review use the final gameplay logic; the subsequent command-only protection has its own related GameTest.

Required hotfix mechanics and bounded checks are complete. Stage21 remains on the existing branch/checkpoints; Stage21D was not started and nothing was published.
