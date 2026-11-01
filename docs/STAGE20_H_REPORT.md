# Stage20H final fixes — RC5

Date: 2026-11-01. Base e5f0cbd, dev/0.20.0.

Status: PARTIALLY COMPLETED; RC5 BUILT — VALIDATION PENDING. The older RC4 record below is retained as historical evidence and is superseded by this section.

## Defect fixes

Village identity now reads dimension/structure/origin directly from the board chunk's saved structure references. It never requests the origin chunk, so origin unload/reload does not cause standalone fallback. When references contain multiple supported starts, nearest origin is selected deterministically, with registry-key ordering as a tie breaker. Standalone root fallback is unchanged. Overlap boundaries and multiple boards across unusually large neighboring settlements still require native coverage; no universal geometry guarantee.

The board has a separate Legends/Regular selector (RU/EN). The two existing fixed hunts retain their normal definitions, rewards, eligibility and owner receipts. They consume no regular slots, do not get procedural source IDs, and never get profile-rerolled by village rotation. Rewarded legendary entries remain permanently retained; regular selection still has five offers. Active legacy contracts remain selectable on initial opening; reopening restores the extra active list after switching categories.

ContractState.compact retains every active contract, every completed-unclaimed contract, permanent legendary receipts and all receipts referenced by any current offer generation. Other terminal details are bounded to128 per player. Pruned contract UUIDs cannot be rewarded: claim requires an exact existing owned COMPLETED instance, and ordinary board acceptance requires a current Offer ID. Pruning is invoked when opening a board; worlds/villages/players can still naturally increase world data, but completed cycles cannot retain unlimited full contract payloads for one player. Current generation and personal-cycle receipts stay available for refresh; obsolete personal generations are removed by existing rotation. No active investigation or unclaimed reward is discarded.

## Verification scope

Existing53 tests retained. Added hundred-rotation/current-generation-size,500-terminal-record/128-limit/NBT reload, and unloaded-origin reference/no-load tests. Extended real screen-handler integration to switch to two legendary entries and back to five ordinary slots. Final counts/logs recorded below after completion. These are automated server tests, not a claim of a full five-hunt native playthrough.

The RC5 production review uses two native clients, CTOV/Lithostitched/Cloth and both Moog mods. New/saved village GUI and server-restart scenarios use fresh ignored worlds. It does not automatically exercise five accepted hunts, all refresh/replay cases or a real active RC3 migration; those remain NOT TESTED. The old combat runner hardcodes Endrega and Grave Devourer in a global catalogue and must be adapted to the new randomized five-offer category workflow before it can prove those scenarios. This is unfinished test coverage, not an external blocker or a PASS.

RC5 is not READY. Remaining mandatory gates: full five hunts/refresh with an unaffected peer; timed rotation while both investigations are active; actual RC3 partial-clue save migration through reward and replay denial; vanilla A/B and neighboring/overlapping village identity cases. No new representative MSPT benchmark.

No publication, push, tag changes or Stage21. Previous candidate jars are backed up under build/stage20h-rc5-backup and restored to build/libs. Protected directories are not modified.

## Final evidence

Final source:55/55 GameTests PASS; Gradle build PASS, build/stage20h-rc5-final-source.log. Production review run-20261101-175237: CTOV new/saved board GUI, two native clients, automatic retrofit and two server restarts PASS, complete=true. Coexistence uses original Moog dependencies. This native case preceded the final GUI-spacing/nearest-origin tie-selection refinements; final artifact is covered by the final55 tests/build, not by an asserted identical native binary replay.

Final JAR: build/libs/witchersbestiary-0.20.0-rc5.jar; 8287034bytes; SHA256 2935655daf621822787a60d26fa95727efd60560a96190f4c530504b7a2a8bc2. Metadata0.20.0-rc5, both dye recipes present, no test classes/logs. RC1–RC4 restored and byte-compared to prebuild backups. Mandatory five-hunt/RC3-active-migration/full rotation multiplayer validation remains NOT TESTED. The work is therefore not a completed Stage20H or public READY candidate.

---

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
