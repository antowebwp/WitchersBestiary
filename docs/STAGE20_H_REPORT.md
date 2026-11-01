# Stage20H — village-specific renewable contracts

Date: 2026-11-01. Base: 33ca86e. Branch: dev/0.20.0.

Status: PARTIALLY COMPLETED. RC4 BUILT — VALIDATION PENDING. This is a local development checkpoint, not a release-ready claim.

## Implemented integration

The old board used all global definitions and matched progress by player/definition alone. BoardScreenHandler now obtains five distinct procedural definitions from VillageOffers. Offer UUID, generation UUID and village source are persisted on each accepted ContractInstance; instance and target UUIDs remain independently generated. Repeated acceptance is rejected by owner/offer identity. RewardService and immutable accepted reward snapshots are retained.

Village identity uses dimension/structure registry ID/start chunk packed coordinate, sharing the Stage20G visited-village identity. Only loaded chunk structure references/start data are consulted. Standalone boards use dimension/root position. No chunk generation or locate is performed. Overlapping starts are sorted deterministically. Limitation: an unloaded origin start can currently lead to fallback identity; this requires a persisted board-to-village binding before release.

Offers retain their UUIDs across reopening and NBT reload. Lazy rotation occurs at 72000 ticks using overworld world age (not mutable daylight time). /time set and sleep do not advance this age; it measures elapsed simulation ticks. A backwards age leaves the generation unchanged; a forward jump produces one generation, not catch-up generations. There is no global per-tick village sweep.

A personal cycle can replace a player's offers only after all five have REWARDED status and at least 24000 simulation ticks since the cycle began. Other players retain the shared base. Personal cycles expire with the base generation; old personal-cycle map entries are removed. The refresh button closes the old screen on success; reopening loads the new cycle. Invalid refresh leaves state unchanged. Stale generations cannot accept; claims for existing contracts remain available.

Offer UUID seeds the existing profile generator, so preview rarity/reward and accepted rarity/reward agree. Accepted instance UUID and target identity remain personal. Current offers plus up to25 older active/unclaimed entries are displayed. Old accepted records without source fields remain legacy and are not assigned an invented village. Existing reward states, investigations, clues and target UUIDs remain unchanged.

## Persistence

Separate overworld PersistentState, Schema1, stores current generations and personal cycles. Unsupported or malformed offer records preserve the complete raw state read-only rather than rerolling IDs. ContractState schema remains1; optional source fields are backward compatible. Existing historical ContractState growth is not yet bounded/archived; release gate remains open.

## GUI and compatibility

Existing journal style retained. Five base offers show existing rarity/reward/status; day countdown and RU/EN refresh label added. Older active/unclaimed entries remain accessible. The open screen deliberately keeps its old mapping so delayed vanilla button packets cannot silently accept a replacement offer. Reopen after timed rotation. Full stale-screen notification UX is pending.

The normal five-offer selection excludes the fixed legendary hunts; operator assignment remains available, but a separate board route for the two special legendary hunts still needs restoration before release. Ecology is still Stage20G's safe pending-search behavior, not a proof every selected species has nearby valid terrain. No other worldgen/model/balance changes.

## Automated verification

53/53 required GameTests PASS: build/stage20h-regression.log. Existing50 retained; old global-catalog count assertion updated to five offers. New tests cover independent village generations, five distinct selections, stable reopen, exact72000 deadline, NBT roundtrip, personal-cycle isolation/cooldown, real screen-handler acceptance/source identity, duplicate acceptance and private player status. Existing tests cover hunt/reward/legacy serialization regressions. These do not substitute for all requested native scenarios.

Gradle build PASS: build/stage20h-final.log. Java21, client compilation, remap and existing mixin checks PASS. RC4 metadata/resources inspected; no GameTest classes/logs included. Previous RC1–RC3 artifacts preserved under build/stage20h-artifacts and restored to build/libs.

RC4: build/libs/witchersbestiary-0.20.0-rc4.jar, 8285455bytes.
SHA256: 3887d517393d1576df64e45f075a4ba869687cb44943d5e2d9864b437d3c9a56.

## Native verification

Production dedicated server and two native clients with raw CTOV3.6.3/Lithostitched1.6.3/Cloth15.0.140 plus Moog5.0.9/helper2.0.4 launched in build/stage20e-network/run-20261101-163618. New CTOV village board and both real GUI openings confirmed. Saved-village automatic retrofit PASS, both native GUI openings PASS, two JVM restarts PASS with one physical board retained and no duplicates (scenario-results.json complete=true). This controlled pre-installation fixture removes only its test board/withholds its placement ledger, not arbitrary personal RC3 saves.

NOT TESTED: full native five-completion personal refresh, two-village independence, natural72000-tick rotation with active investigations, native legacy RC3 hunt migration and all multiplayer stale/replayed packet combinations. No new representative TPS/MSPT benchmark. No production performance claim.

## Remaining release gates

Persist stable board association when origin chunks unload; retain special legendary board access; archive old terminal history safely; broaden migration/rotation/replay GameTests; complete native scenarios A–H and vanilla A/B/CTOV comparisons. Review countdown/refresh error messages and full manual GUI. RC4 is not READY.

No push, release, stable-tag change, crowns or Stage21. Protected game/archived project directories untouched; installed dependency jars only read/copied into ignored review runs.
