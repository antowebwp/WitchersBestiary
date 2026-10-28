# Stage 20D — Witcher Senses, tracking and investigations

28 October 2026. Branch dev/0.20.0, base4950be4, version0.20.0-dev. Adds an investigation layer to the existing five named hunt templates. No push/release, protected model changes, separate game installation access, Stage20E or Stage21 work.

## Gameplay and architecture

Accept a procedural or legendary hunt -> existing shared queue finds suitable loaded terrain -> persisted private clue route -> study the sequence -> discover the lair -> existing spawn coordinator reserves/inserts the unique target -> existing combat/death attribution -> normal board reward.

`ContractClue` stores unique UUID, contract/owner UUIDs, type, supported ground position, dimension, ordinal, studied flag and next-clue UUID. `ContractInvestigation` is stored inside ContractInstance, alongside the existing lifecycle and SwampHunt record; no second database. Additive stages: SEARCHING, TRACKING, LAIR_FOUND, HUNTING, COMPLETED, CANCELLED. AVAILABLE/ACCEPTED/COMPLETED/REWARDED remain unchanged.

New named hunts **do not create a physical monster before the last clue**. The already persistent contract instance UUID is the reliable pre-spawn identity. At lair discovery the existing SwampLord/GraveDevourer/ProceduralHunts spawn entrypoint allocates and reserves the target UUID exactly as before, then records HUNTING. No second entity spawning system or replacement combat AI was introduced. A blocked lair remains LAIR_FOUND; a bounded nearby fallback and at-most-once-per100-tick retry can use loaded safe positions when the owner is within32blocks. No target is replaced while its UUID is reserved.

Existing saved physical targets without investigation data remain finishable and do not acquire a new blocking route. Previously accepted but still-searching hunts can obtain a route when their existing queue finds territory. Direct internal spawn calls used by legacy focused tests still work; normal player acceptance uses the new territory-to-investigation path. Exact bound death completes the contract and investigation, including the old/early-target case. Helpers, illusions and ordinary monsters cannot complete the wrong named instance.

## Witcher Senses and interaction

V is the default rebindable toggle under the Witcher's Bestiary controls category. This is a press toggle, not held input. Server-authoritative enabled state is scoped to a player session and reset on logout/dimension changes. Client world/disconnect callbacks immediately clear cached effects; no clue entity references are retained.

Only the owner's CURRENT clue is sent. With Senses active its exact point is sent only within the configurable radius (default24, bounded8–32); without Senses a faint mark can be visible within8blocks. Within96blocks an enabled player can receive one of eight coarse direction hints. The journal gives a128x128 region, not all clue positions or remote boss coordinates. Loaded-chunk checks precede point visibility; no chunk is loaded to show a trace.

Right-clicking a cached nearby trace uses ordinary item/block interaction callbacks and a client ray-box focus check. Server Inspect independently validates owner, accepted contract, dimension, current clue UUID/order, studied flag, loaded chunk and <=4.5block distance. Wrong/foreign/out-of-order/replayed requests cannot advance progress. Network requests are rate-limited and dispatched on the server thread. A successful study marks only the real state change dirty, opens the next clue and sends an owner-only chime, small local particle burst and short localized ActionBar finding/lair message. No reward is given for clue examination.

## Original lightweight visuals and lairs

All five types have original authored positive-volume geometry in `ClueVisuals.java`: paired footprints/toes, low blood pools, three claw gouges, six-part bone remains and translucent mist volumes. They are untextured temporary VFX, not new inventory items or static block decorations; no third-party textures, flat inventory sprites or existing monster/sword assets were used or changed. The Java geometry is the editable deterministic source. Static shapes and point colors are cached, with only the current private point rendered. Footprint orientation follows the next route segment.

Active Senses add gold outlines, one local particle per10client ticks, and a subtle cyan edge tint. There is no global red filter, FOV change, world scan, permanent status effect or heavy postprocessing. HUD sits at the upper left to avoid chat/armor and hides in screens, hidden-HUD mode and F3 debug view. Bones/mist at the lair provide a lightweight site marker alongside the existing natural habitat; no structure, block replacement, world entity or terrain destruction is needed for clues/lairs.

Drowner findings describe wet bank tracks, pondweed and prey remains. Ghoul routes use gouges/blood/bones. Foglets use mist residue and distorted prints. Legendary hunts use their base species' findings. RU/EN names, stages, hints, direction labels and species/type findings are included. The board retains name/monster/rarity/reward/coarse area and adds investigated count, stage and last-finding description.

## Routes and configuration

`config/witchersbestiary-investigations.json` is read once per server startup, with defaults:

```json
{"common":2,"rare":3,"epic":4,"legendary":5,"radius":24}
```

Counts are bounded1–5, radius8–32. Invalid user configuration is preserved and safe defaults are used. Changing settings affects new routes; existing serialized routes do not reroll.

Route construction runs inside the SAME cooperative1ms HuntSearchQueue step after a suitable lair candidate has been found. It checks at most five nodes and three lateral alternatives per node, using only loaded heightmap data. Nodes are built outward from the lair and reversed into travel order; consecutive sites have <=3block vertical difference and <=18block separation. Natural supported floor, dry space, border/build limits and loaded footprint are required. The existing lair habitat/building safety checks remain. If fewer nodes are safe, the saved route is shorter; if all approach nodes fail, one safe clue at the validated lair is the fallback. No blocks are modified and no new global search engine was added.

## Persistence, multiplayer and performance

NBT stores the complete route and stable IDs/positions/types/links, studied prefix/current index, owner/contract, dimension, lair and stage. Read validation rejects invalid schema/binding/links/duplicate IDs or inconsistent progress. Reload does not call route generation. Logout clears only the local session, not the contract; player death is not a cancellation event. Cross-dimension access hides traces and denies study. Private snapshots go only to their owner, so coincident routes for different owners cannot mix progress.

The server polls cached active contract IDs every10ticks, with an infrequent owner-history fallback; it does not scan chunks/entities for clues. Snapshot serialization is compared with the last sent value and no packet is sent for unchanged state. Client rendering uses one cached point; distant particles/effects stop locally. Clues create zero world entities/chunk tickets. PersistentState is marked dirty only on real route/progress/lifecycle changes, with standard Minecraft saving. Old completed histories remain in the existing contract store; this is not long-duration storage profiling.

A manual100-iteration snapshot-query scenario measured mean per-batch costs for1/5/10 concurrent private investigations: **16.414 / 51.096 / 86.116 microseconds**, with owner isolation and no clue entities. This measures view construction, NOT real multiplayer TPS/MSPT. Existing accelerated live-entity1/5/10 hunt regressions and synthetic shared-queue tests remain separate evidence; no production performance guarantee is inferred.

## Verification actually completed

- Final `runGametest build --offline --console=plain`: **38 required tests PASS**,8.498s test execution, **BUILD SUCCESSFUL in58s**; all9 release mixin/refmap checks PASS. Raw log: ignored `build/stage20d-final.log`.
- All32 Stage20A/B/C regression tests remain enabled, including Swamp Lord, Grave Devourer and all three procedural species.
- Six combined investigation tests cover: supported safe routes/links/IDs, dry/air/inside-block/unloaded rejection without forcing a chunk; order/owner/distance/replay/cancel/dimension checks; private nearby-only snapshots; compressed-NBT disk route/progress/lair roundtrip; no regeneration; logout session reset; all five templates through last-clue spawn, exact death and once-only reward; no duplicate target; legacy target completion;1/5/10 private investigations/configuration bounds; and the shorter one-clue fallback when approach floors are unsuitable.
- A bounded native Fabric-only client completed **one EPIC procedural Foglet investigation (four clues) and one legendary Grave Devourer investigation (five clues)**. It exercised the actual V keybinding path, private visuals, real right-click Inspect packets, sequential findings, lair-triggered unique spawn, BossBar, real attack packets, completion and real board claim packets. Both had no physical target before final study and zero BossBars after death. **Native task PASS in2m18s**.
- After the procedural first clue, the client disconnected/saved and reopened the world. The complete route signature, current index and no-target state were identical, then the remaining investigation and combat succeeded. This is real integrated-server world rejoin, not merely a synthetic NBT clone.
- Screenshots and `results.json` are under ignored `build/stage20d-review/`. Mist, footprints, gouges, blood, bones and completed journal frames were visually inspected. The first native attempt exposed a test-only biome edit not marked dirty for world saving, and a filled-box vertex-mode mismatch; the fixture now explicitly saves its FOREST biome and the VFX use the correct quad layer. The HUD was moved above chat. No production terrain/habitat safety check was weakened.
- Native uses a controlled flat FOREST fixture,48–96block test search radius (production remains150–500), automated approach/UUID attack driver, netherite armor and strong regeneration. This verifies protocol/mechanics/rendering, not natural-world distribution, manual survival balance or a two-human session. English was used natively; RU key coverage is checked from the built JAR.

## Limits and artifact

This is bounded waypoint tracking, not a terrain/pathfinding survey: unusual terrain can shorten the chain. Later player edits can obscure a saved mark or make a lair unsafe; route IDs/progress are retained and a blocked lair waits/retries safely. Cross-file abrupt-crash atomicity remains the inherited Stage20A/C limitation; no full separate-JVM/power-loss test or production latency benchmark is claimed. All players should use the matching development build. No new full modpack/optional-client-mod compatibility session is claimed.

JAR: `build/libs/witchersbestiary-0.20.0-dev.jar`, **8246337 bytes**. SHA-256: `d51bb8cb382b8026299be7cf8f2bdf1f783a98f47aa378c75ee7d93945978b5d`. ZIP/version/tracking classes/RU-EN key parity verified; native/GameTest harness classes and logs are absent from the game JAR. No accidental binary assets were added to Git.
