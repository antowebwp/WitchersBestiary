# Stage 20C — named contract hunts

## Stage 20C.1 — archived checkpoint evidence

28 October 2026. Version 0.20.0-dev, branch dev/0.20.0, base 90497bf. Implements ONLY Swamp Lord; Grave Devourer, Mist Phantom and Stage20D are not implemented. No push, release or modification of the v0.19.0 tag. Existing uncommitted search-queue work was retained and integrated.

## Working game loop

The board offers `witchersbestiary:swamp_lord` alongside the three existing definitions. Accepting creates a normal Stage20A instance and an embedded SwampHunt record. The server searches loaded river/swamp/bank terrain, creates a unique named Drowner, tracks it by exact UUID, completes the owner's contract on its death (including another player's assistance), then grants the existing one-time reward through the board and ContractRewards. A same-name ordinary drowner or summoned helper never completes the named hunt. Developer `/wbcontracts hunt` (permission level2) displays owned instance/target UUID, status and exact spawn position; ordinary GUI exposes only a coarse 128×128 region.

## Implementation and combat

- `SwampHunt`: origin, dimension, target UUID, immutable spawn position, SEARCHING/WAITING/RESERVED/ACTIVE/DEAD/CANCELLED status and probe cursor, stored directly inside ContractInstance NBT in the existing PersistentState.
- `SwampLord`: definition gating, search queue integration, entity creation, exact death attribution, local BossBars, lifecycle hooks and bounded combat logic.
- Existing DrownerEntity has optional contract/parent UUID fields and conditional NBT/tick hooks. Ordinary drowner behavior takes the old path; models/textures/basic animation resources are unchanged. No new entity registry type is needed.
- Swamp Lord base health **108 (3×)**, attack damage **9 (1.5×)**, scale **1.12**. Existing progression is applied once to the new base; no custom damage multiplier or second oil/silver damage event. Existing necrophage categorization remains.
- A 15-tick dash warning stops navigation and emits a drowned sound/splash signal; short horizontal impulse follows, then **120-tick cooldown**. It does not stun players. Existing melee/navigation deals damage.
- Summoning checks **300-tick cooldown**, exactly two persistent UUID slots. Each slot reserves a summoned drowner even while unloaded; a confirmed helper death can free its slot. No unbounded helper collection or per-tick spatial scan. A helper is tagged with the parent contract UUID and cannot earn contract progress. Loaded helpers are removed on boss death/cancel; unloaded helpers check terminal parent state on normal reload/ticks and discard. No chunk tickets.
- BossBar updates every10 loaded entity ticks, only for non-spectator players in the same dimension within32 blocks. Removal/unload/disconnect/death clears viewers; no global server-wide bar. Standard model/name tracking and server health are used. The boss retains ordinary death animation rather than being respawned or immediately replaced.

## Loaded-only territory search and performance

Existing HuntSearchQueue is server-owned, round-robin, one queued job per owner, shared default **1,000,000ns/tick** budget and 64-step secondary cap. Each job probes at most128 candidates and then becomes WAITING. No biome locate calls, asynchronous world access, forced chunk loads or terrain generation. Corner chunks must already be loaded before collision/fluid/biome checks. Ground must be natural dirt; volume must be empty, open to the sky, non-fluid, within world bounds and outside occupied village POIs. River/swamp/mangrove biome or nearby water identifies habitat. Natural-ground/open-sky/POI checks reduce building conflicts; they cannot identify arbitrary player claims.

Default search radius **150–500 blocks from acceptance**, configured by JVM properties `witchersbestiary.huntMinRadius` and `witchersbestiary.huntMaxRadius` (minimum48, upper bound1000; validated/clamped). Search rejects positions within48 blocks of the current player. After a failed pass, searching resumes on a later player chunk change/login, not on a constant stationary polling loop. Idle queues do not tick/allocate samples. No world or entity scan runs in a background thread. Ordinary world autosave persists dirty state; no per-tick disk writes.

The budget is cooperative: one already-running candidate/spawn operation cannot safely be interrupted. A cold probe can exceed the nominal budget; this is not a hard real-time guarantee. Boss/hunt processing uses owner indexes/UUID lookup; helper lookup is bounded to two IDs. Existing mob AI remains responsible for navigation. Particle/sound recipients use vanilla distance-limited delivery.

## Persistence, reload and ownership

Authoritative hunt metadata lives in the same ContractInstance/PersistentState as its completion and claim state, not a competing second store. The entity uses standard Minecraft NBT for UUID, attributes, health and optional binding/cooldown/helper IDs. RESERVED is assigned before spawn; a rejected spawn resets to WAITING. Entity load confirms a matching RESERVED UUID as ACTIVE. Unknown/corrupt required bindings are quarantined by the existing contract loader. An entity missing from the loaded entity map is NOT considered dead and does NOT authorize a replacement.

Cancellation/expiration uses the existing contract lifecycle; entity ticks clean up terminal contracts. Logout cancels queued probes and removes bar viewers, without creating replacements or deleting the persistent target. The normal chunk lifecycle can unload target/helpers. A persisted DEAD marker can reconcile an ACCEPTED contract at server startup. Owner UUID stays authoritative even if a helper player delivers the final hit. Claim validation and one-time REWARDED state remain in ContractRewards.

Abrupt power/process failure between vanilla entity/chunk and PersistentState saves is not a cross-file transaction. Ambiguous saved reservations fail closed (no automatic replacement); operator diagnosis may be required. No restart-based respawn farming is permitted. Downgrading a development world to older Stage20A/B is unsupported; back up worlds.

## UI and localization

Board list/detail and vanilla screen-button packets are retained. New RU/EN name, description, start message, search/target statuses and coarse hunt area are displayed in the existing parchment interface. Opening/reopening does not create new instances; accepting the same definition again is blocked by the existing board rule. One active named hunt per owner is enforced server-side. Existing generic contracts, operator commands and reward screen continue to work.

## Tests actually performed

- Standalone Java queue tests for 1/5/10 synthetic jobs: fair scheduling, shared budget, step cap, WAITING, cancellation, re-enqueue and duplicate owner prevention PASS. The clock is injected; these are correctness checks, NOT Minecraft benchmark/TPS/MSPT measurements.
- Three combined SwampLord GameTests cover unique binding/spawn suppression, same-name ordinary kill rejection, other-player assistance and owner-only reward, repeated completion/claim rejection after store reload, 108HP, helper cap/replenishment, helper kills excluded, cooldown telegraph/impulse, unsuitable-air terrain, cancellation/helper/bar cleanup, exact UUID/position roundtrip, compressed entity NBT disk IO, UNLOADED_TO_CHUNK removal path, respawn of the same serialized UUID and suppression of a replacement while absent.
- Existing Stage20A/B focused tests remain enabled; expected definition count becomes four. An old village fixture was flaky because its heightmap sampled the GameTest barrier ceiling instead of terrain. Diagnostic output confirmed the barrier. The fixture now uses an isolated48×48 area and removes its own ceiling before the existing placement helper; production village checks were not relaxed.
- A native Fabric-only client ran in build/stage20c1-client, bounded to240seconds. Real board acceptance packet -> loaded-bank queue search -> named boss + two helpers + one BossBar -> real client attack packets -> COMPLETED -> return to board -> real claim packet -> REWARDED. Results confirmed bars after death=0 and12 emeralds. Native task PASS in1m46s, with graceful world save/shutdown.
- Native review used a controlled flat-bank fixture and **48–96 block test radius**, not an independent natural-swamp expedition at default150–500. Player was invulnerable during the automated combat review; this is not a balance/survival benchmark. Native screenshots were inspected: available.png, active.png, boss-and-bar.png, combat.png, completed.png, rewarded.png under ignored build/stage20c1-review/.

Final focused/build results are recorded below after verification. Raw logs, worlds and screenshots remain untracked under build/.

## Limitations and next work

This is a completed Swamp Lord vertical slice, NOT the whole three-boss Stage20C. Other two bosses, their visuals/combat and full mixed minion/illusion stress scenarios remain. No new natural-biome distribution survey, two-human multiplayer session, dedicated-process restart test, or real TPS/MSPT 1/5/10 live-hunt benchmark is claimed. Entity NBT disk roundtrip/live UUID rehydration and standalone server GameTests cover reload logic; native full-cycle review covers the actual client/server protocol. Existing Better Combat/Simply Swords were not rerun for this slice; their damage integration/resources were not changed. Stage20A's cross-file crash-atomic reward limitation remains. Stage20D not started.

## Verified checkpoint result

Final focused `runGametest build --offline --console=plain`: **20 required tests PASS** (3 Swamp Lord combined tests + previous17 focused/dependency tests),6.133s reported test execution. Gradle **BUILD SUCCESSFUL in1m18s**; all9 release mixins/refmap checks PASS. Entity compressed-NBT disk roundtrip, same UUID rehydration after UNLOADED_TO_CHUNK, minion death exclusion/replenishment and cancellation/unload BossBar cleanup included. The barrier-ceiling fixture cause was confirmed before correction; no production village safety rule was relaxed.

Game JAR: `build/libs/witchersbestiary-0.20.0-dev.jar`, 8173301 bytes; SHA-256 `bb55dc65311d8d15b4590794982da9ff2209c4a8617aa1feb94417ef092ef577`. ZIP/version/Swamp Lord definition verified; native/test harness classes excluded.


## Stage 20C.2 — Grave Devourer completed

28 October 2026, base c4c5ab6, version 0.20.0-dev. Adds the second fixed named hunt; no Mist Phantom or procedural hunts in this checkpoint.

### Mechanics and integration

- `GraveDevourer` is a contract profile on the existing GhoulEntity, not a new entity type or model. Ordinary ghoul AI/attributes remain unchanged. Named stats: 120HP, 10 damage, configured speed .43 (ordinary .40), knockback resistance .65. Existing animations/textures are reused unchanged.
- `grave_devourer` is offered by the existing board; normal ContractInstance, ContractState and ContractRewards provide owner binding, immutable definition snapshot, completion and one-time 150XP/14 emerald reward. Existing named-target exclusion prevents an ordinary or same-name ghoul from completing this hunt. Even a forged contract binding cannot replace the exact reserved UUID.
- The existing SwampLord coordinator dispatches both named profiles through ONE HuntSearchQueue with the existing global 1ms cooperative tick budget and owner fairness. No second scheduler, background world access, chunk tickets or world-generation search is introduced. One active named hunt per owner applies across both profiles.
- Search uses already loaded forest-tag biomes (including dark forest), natural dirt floor, clearance, world border and occupied-village exclusion. Leaf canopies are permitted; roofed cavities/buildings are rejected. A direct graveyard proximity scan was deliberately omitted; it is optional, and forests provide the required safe habitat. Unsuitable or unloaded terrain leaves the normal WAITING hunt; exploring another chunk retries it.
- Predator leap: 14-tick stationary telegraph with five warning particles and sound, existing leap animation at launch, 200-tick start-to-start cooldown. A bounded ballistic corridor checks loaded chunks, collision clearance, level difference and safe landing. Obstruction aborts the leap and retains cooldown. No extra pathfinding; failed trajectory probes are throttled to once per ten ticks.
- Rage: first crossing below 40%HP sets a persisted once-only latch; 300 ticks, +20% movement speed, melee inter-swing cooldown reduced from four to one tick. Modifier uses a fixed ID and is removed at expiry, preventing stacking. No repeated threshold activation.
- Flesh feeding: only a successful attack that actually reduces health/absorption can heal; two HP, 80-tick cooldown, lifetime cap twelve HP. Healing is bounded by missing HP and persisted across reloads.
- Existing hunt NBT stores owner/contract UUID, target UUID, dimension, spawn position and status. Entity NBT stores leap/feed cooldowns, rage remaining/used and healing spent; vanilla stores health/attributes. An unload never clears the reservation. Cancellation invalidates loaded targets and targets subsequently loaded. Shared nearby BossBar is cleared on death/unload/cancellation. No per-tick PersistentState dirty writes were added.
- Existing board GUI displays localized target, coarse area, state, progress and reward. RU/EN name/description/start text are included. A stale swamp-only start message was corrected for Grave Devourer after native review; the description was shortened to fit the existing detail area.

### Checks actually performed

- Five combined GraveDevourer GameTests cover creation/stats, exact UUID and cross-profile active limit, ordinary/forged target rejection, exact death, repeated reward rejection including store reload, compressed entity NBT disk roundtrip, persistent reservation, unload/same-UUID rehydration, cancellation/BossBar cleanup, telegraph/impulse/cooldown/obstacle, one-time rage, rejected-damage feeding, cooldown and twelve-HP cap, aerial/unloaded habitat rejection without creating a chunk.
- Existing Stage20A/B tests and all three Swamp Lord tests passed. Final `runGametest build --offline --console=plain`: **25 required tests PASS**, with release mixin verification PASS and **BUILD SUCCESSFUL**. Raw final output: ignored `build/stage20c2-verified.log`.
- Existing standalone queue checks passed for 1/5/10 synthetic jobs, budget/fairness/waiting/cancellation. These are correctness tests, not live TPS/MSPT benchmarks.
- One bounded native Fabric-only client: real board acceptance -> shared queue selects loaded forest fixture -> unique named ghoul/BossBar -> real client attack packets -> completion -> board return -> real reward packet. PASS in 1m43s, graceful save/shutdown. Results: one BossBar, leap cooldown observed169, rage used=true, actual feeding10HP, bars after death0, owner received14 emeralds. Screenshots available/active/boss-and-bar/combat/completed/rewarded under ignored `build/stage20c2-review/` were inspected.
- Native review uses a flat world whose already loaded chunks were assigned the actual FOREST biome, a 48–96 block test search radius and a survival player with 1000HP so incoming attacks genuinely deal damage without ending the automation. The extra heart rows are a test-player artifact. This is not a natural-forest distribution survey or survival/balance benchmark. Default production search remains150–500.
- No full historical GameTest suite, multi-hour audit or second concurrent client was run. No separate process-restart or two-human multiplayer benchmark is claimed; disk NBT/state reload plus live UUID rehydration cover the tested persistence paths. Shared cross-file crash-atomicity limitation from Stage20A remains. No gameplay assets changed; no user game installation access; no push/release/tag changes.

JAR: `build/libs/witchersbestiary-0.20.0-dev.jar`, 8183028 bytes; SHA-256 `1def8f6d880443b4a6f6d4171ebc84f63623ad3d2bb30ff92ce8cf2c6016a9f1`. ZIP metadata/definition and exclusion of native/test harness classes verified.
