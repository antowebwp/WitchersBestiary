# Stage20F — implementation in progress

Base1082b14, branch dev/0.20.0. RC1 retained; no RC2/readiness claim.

Initial causes: snapshots exposed only a short-range bearing and ground-level marker; no area coordinates were available at normal board-to-hunt distances. Village placement hardcoded minecraft namespace and a single candidate chunk, with nine attempted roots and no alternate-chunk retry.

Implemented first changes:
- Owner-only snapshot now includes approximate current search area regardless of distance, rounded to a16block cell center. Maximum horizontal center error is sqrt(8²+8²)=11.314blocks, within20. Coordinates are absent across dimensions/dead/spectator as before; client still sends UUID only.
- HUD lines moved to right edge with scaled-window coordinates, added localized approximate X/Z.
- Additional bright pulsing1.3x1.38block outline, independent of particle settings. Depth-tested; no through-wall claim.
- Village eligibility now uses datapack-extensible witchersbestiary:notice_board_villages structure tag containing five vanilla villages; no namespace wildcard. Placement algorithm itself not yet repaired.

compileJava/compileClientJava passed before additional outline edit. Full44 regression started: build/stage20f-initial-tests.log; verify final outcome. Bright outline and HUD need native visual review.

Remaining: exact two-clue routes with legacy preservation, distinct lair approach, strong highlighting visual QA, client HUD settings, safe village fallback and saved one-board status/admin repair, identify actual Moog JAR (not present in downloaded compatibility folder; user game installation untouched), normal multi-seed village/client/two-player checks, final tests/build, RC2 and checkpoint. No unsupported Moog compatibility or gameplay PASS claimed. Existing Stage20E evidence remains historical.

Confirmed checkpoint: full44/44 GameTests PASS (stage20f-initial-tests.log); final outline compileClientJava PASS (stage20f-outline-compile.log). No RC artifact rebuilt, RC1 untouched. Work remains uncommitted and Stage20F incomplete. Next implement versioned two-clue routes while preserving persisted legacy chains; adapt route tests, then repair bounded village placement.

## Completion pass — implemented changes

Newly prepared routes persist RouteVersion=2 with exactly two distinct supported clues. Only loaded chunks and bounded existing heightmap/safety checks used; if two safe sites cannot be found preparation returns false and remains SEARCHING. Existing RC1 routes without RouteVersion deserialize as0, retaining saved IDs, count, progress and legacy arrival behavior. No regeneration/mutation of existing routes. New routes require both studies and a separate approach within3blocks of lair before the existing unique target factory runs. Old routes retain32block trigger. Existing inspect interaction remains4.5blocks for client/server consistency, rather than requested3; UUID/order/owner/dimension/dead/spectator and loaded-chunk checks retained. Route offsets6/12blocks keep tests and natural searches bounded; these are compact investigations, not long trails.

HUD approximate16cell coordinates remain within11.314horizontal blocks of current point, but nearby successive areas can overlap. Strong outline is independent of particles; through-obstacle rendering and customizable HUD position still NOT IMPLEMENTED. No unsupported visual PASS claim.

Village placement now selects first-piece central chunk instead of northern edge of the entire expanded structure bounds; tries9 preferred plus36 additional in-chunk roots, four facings each, without replacing occupied cells. Vanilla five IDs remain explicit supported baseline even when a world registry has not bound optional extension tags; additional structure IDs use notice_board_villages tag. Single selected chunk limits duplicate generation; no automatic old-world scan/retrogen. Guarantee of a board in every village NOT established; no cross-chunk fallback introduced.

Operator recovery command `/wbboard repair`: level2, current chunk only safe attempt, rejects existing board detected by loaded heightmaps within3chunks, never loads missing chunks. Intended for an operator standing in an existing village. It does not validate village membership and is not a whole-village placement guarantee. Visible top-board duplicate scan cannot guarantee detection of buried/user-modified boards. Normal placement also retains existing per-chunk duplicate check.

Located actual installed JARs by read-only metadata inspection (user installation unchanged): `MoogsVoyagerStructures-1.21-5.0.9.jar` modid mvs5.0.9,129 standalone structure definitions; `moogs_structures-fabric-1.21.1-2.0.4.jar` modid moogs_structures2.0.4-fabric, no worldgen/structure definitions. mvs: villager_statue is a statue, not a village. Neither adds vanilla village template pools. No namespace-wide tag inclusion, no required dependency. Actual Moog runtime worldgen compatibility NOT TESTED. Explicit village IDs in third-party datapacks can extend tag; individual huts/statues are not automatically villages.

Regression adapted without deleting tests: requires exactly2, checks separate LAIR_FOUND/no target after final study and explicit lair approach before spawn; unsafe short-route fallback now waits. Dead/spectator fixture removes arena barriers and sets origin explicitly. Five-village synthetic hook fixture uses new central piece coordinates. Final full run `build/stage20f-village-repeat.log`: **44/44 PASS**,40seconds. Failed earlier runs retained; final vanilla ID eligibility fallback resolved registry-tag fixture failure. This is synthetic hook/physical board block coverage, not several naturally generated villages.

RC2 build and real two-client integration running; results must be recorded. Previous Stage20E evidence is not new Stage20F validation.

## RC2 artifact and real-test checkpoint

Gradle `build stage20ePrepareLaunch -Pstage19fProfile=A --offline --console=plain`: PASS23s, log build/stage20f-rc2-build.log. RC2 metadata/ZIP/RU-EN parity and absence of native/GameTest/log files checked. RC2 is **BUILT — VALIDATION PENDING**, Stage20F PARTIALLY COMPLETED, not fully ready.

Loom remap output cleanup removed previous RC1 game artifact when RC2 was built. Restored RC1 by remapping the preserved RC1 development JAR to isolated artifact directory; restored SHA256 exactly matches original `8ee010034c824366ce6ee209316dbbd460f607c92f17ead732d5b03d114fe3ff`. Both RC1 and RC2 now retained in build/libs and build/stage20f-artifacts. Stable0.19.0 tag and protected user files unchanged.

First Stage20F native run used two actual clients on normal seed202001; acceptance, private clue study, reconnect/death/Nether and first save/restart reached. It failed on a Windows AccessDeniedException during repeated test-file command replacement while entering lair stage, not production packet handling. Removed redundant per-tick wait command there; one bounded repeat launched. Do not reuse historical Stage20E success as Stage20F PASS.

Inspected actual WBOne-study-1.png: orange raised outline and HUD with0/2/search coordinates rendered; unloaded terrain was still visible in the teleported camera. This is limited rendering evidence, not forest/night/foliage or stable-terrain visual PASS. No minimap installed in review. Full visual matrix and natural multi-seed village placement/GUI remain NOT TESTED. Moog metadata/static structure inspection only, no runtime compatibility PASS.

User instructions: accept a named hunt, explore while territory search waits; follow approximate area coordinates, activate V, aim at the bright trace and right-click within4.5blocks, repeat for second clue, follow new area to lair and approach within3blocks. Kill only bound target, return to board to claim once. Existing routes retain old count/behavior. Operators can stand in an existing village and use /wbboard repair after backup; safe refusal leaves terrain unchanged.

RC2: build/libs/witchersbestiary-0.20.0-rc2.jar, 8250562bytes, SHA256 `3b3a28fba2515fd106aedbb4657ab65b39edaccb197318d9e048ed09f690195a`.

## Final verified network result — supersedes pending repeat above

Bounded repeat **PASS** with two real native clients, normal worldseed202001 and default search radius. Both different Ghoul hunts used the new two-clue routes, separate lair approach, unique targets and actual attack/board reward packets. Private clue/forged request rejection, owner logout while peer advances, death/respawn, Nether roundtrip, saved-route dedicated restart, actual entity chunk unload/reload with same UUID, two independent combat/reward cycles, stale/double claims and second dedicated restart/reward replay denial all passed. Test-only operator teleports/equipment and native automation remain; this is not a two-human survival balance test. Worldgen village creation itself is not covered by the manually placed test board.

The test-only repeated file-command write at lair phase was removed; no production protocol was changed for the Windows file lock. Complete JSON/incremental scenario journals, logs and screenshots are retained under ignored directory listed below. Validator tools/validate-notice-board.py PASS for163cuboids, faces/UV/textures/display/36states.

Final Stage20F status: **PARTIALLY COMPLETED — RC2 BUILT, VALIDATION PENDING**. Exact two-clue flow and separate lair implemented and tested. Remaining mandatory validation/implementation: normal multi-seed village locations/physical boards/GUI; supported Moog runtime worldgen; configurable HUD placement/minimap matrix; through-foliage/obstacle highlighting; full day/night/grass/distance visual matrix; persisted cross-chunk fallback/no-site diagnostics and stronger village-wide duplicate recovery. No claim that every suitable village now reliably has a board. These limits prevent COMPLETED/READY.

Network directory: `build\stage20e-network\run-20261030-184034`. Combat mean diagnostic MSPT: 12.076878932584258. Wall-clock catch-up tick rate is not sustainable TPS.
