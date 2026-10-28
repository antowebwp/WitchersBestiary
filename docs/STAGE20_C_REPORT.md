# Stage 20C.1 — Swamp Lord vertical slice

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
