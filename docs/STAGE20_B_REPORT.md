# Stage 20B — 3D notice boards and contract interface

27 October 2026. Version **0.20.0-dev**, branch **dev/0.20.0**, continuation of Stage 20A f4e54ef9ef11b881d8033a1060b7f039590d1d58 in the clean public history. No push or release. The v0.19.0 tag and separate archived development history are unchanged. No Stage 20C implementation.

## Board architecture and placement

`witchersbestiary:notice_board` is a nine-cell **3 wide × 3 high × 1 deep** structure. `NoticeBoardBlock` has horizontal facing and part 0..8; bottom center is the logical root, center cell renders the full static model. No block entity, ticking renderer, background board scan or second contract database.

The BlockItem places the root facing the player and installs the other eight cells only after checking all nine positions and the three supporting blocks. Free replaceable vegetation is allowed, fluids and solid occupied cells are rejected. Three full supporting top faces are required. Commands/creative functional inventory provide the item; no new survival recipe is added. Example: `/give @s witchersbestiary:notice_board`.

Facing/part are ordinary vanilla blockstate properties persisted in chunks. Root lookup works from every part, for all four directions. Outline/collision are the clipped union of the actual model cuboids in each cell, rotated and cached for four orientations; empty regions have no invisible full-cell collision. Removing any part removes the other eight without additional loot; only the originally broken cell's loot table drops one board. Creative/forced removal does not generate extra items. Lost foundation schedules a normal break and removes the whole object.

## Original art and model

- Static Java block model: **163 positive-volume cuboids**, six explicit faces each.
- Model coordinates stay in vanilla's -16..32 element range; central cell anchors the 3-block span.
- Two oak uprights, individual backing planks, cross-beams, segmented diagonal rear brace, layered pitched shingle roof with ridge/rafters, separate masonry blocks, iron bands/rivets, four parchment volumes, original ink monster silhouettes/handwriting marks, wax seals, a framed hanging lantern.
- Original RGB block atlas **256×256**; no alpha holes and no reference pixels copied.
- Original parchment/wood/brass GUI texture **384×240**.
- Lantern cell emits block light **10**. Vanilla lighting, no shader/bloom dependency or animated/emissive renderer.
- Editable source: `art_source/notice_board/notice_board.bbmodel` with original embedded atlas, plus PNG.
- Generator: `tools/generate-notice-board.py`, deterministic procedural painting and cuboids, narrow output list, no external image input.
- Focused validator: `tools/validate-notice-board.py`, checks volumes/bounds/faces/UVs, texture sizes, all hand/GUI/ground/fixed contexts, 36 facing/part variants and source parity. PASS. The private-history aggregate validator was absent and was not restored; a public scoped asset policy is documented instead.

Roof backing was inset from shingle face planes after native review to remove coplanar flicker. The final client frames show intact roofing, readable original parchment motifs, visible thickness/feet/lantern and rear support. This is an authored Minecraft interpretation of the supplied concept, not a copy of its pixels.

## Village generation

`VillageNoticeBoardMixin` injects at the mapped tail of vanilla `StructureStart.place`, after its normal village pieces. Filters only vanilla structure IDs prefixed village_: plains, desert, savanna, taiga, snowy. Existing templates/pools are not overwritten. The expanded final start bounding box determines **one deterministic candidate chunk** per village. Inside that chunk the helper checks 256 surface positions for an existing board, then at most nine fixed roots × four orientations with nine-cell clearance and three supports. No full terrain scan, chunk tickets, player tick jobs or forced loading of distant regions.

Supports must be suitable natural dirt/sand/stone/gravel/snow blocks; dirt paths are explicitly excluded. Fluid/occupied volume, uneven supports and important village blocks are left intact. Snow layers/replaceable vegetation can be cleared by normal placement. All footprint cells stay inside the candidate chunk. Chunk generation stages provide normal persistence; loading a completed chunk does not invoke a new placement job. A same-chunk existing board also suppresses repeated attempts.

This is **best effort**, not a strict exactly-one guarantee: a village may receive zero boards when no safe candidate fits, or share a candidate chunk with another nearby structure. It does not retrofit already generated villages. It is not a global village counter/registry. Other later mods may still alter the terrain after placement.

Validation: the actual injected vanilla placement method was exercised for all five village structure types on controlled matching support fixtures. This is not five separate natural-biome surveys. Native normal-world generation (seed 12345) located a plains village near x=160,z=880 and found exactly one naturally generated board in its candidate chunk. Day/night screenshots show it on terrain without overwriting houses/roads. The separate raised stone mount nearby is a diagnostic art fixture, not generated village content.

## Working GUI and server security

`BoardScreenHandler` is an ordinary server-owned screen handler, opened by right-clicking any part through an extended factory containing the root position. `NoticeBoardScreen` provides list/detail panes, paging (three rows per page), monster trophy icons, localized title/description/target, difficulty, kill requirement/progress, full rewards and state. Parchment, dark wooden controls, brass frame and wax accent are procedurally drawn original assets. Keyboard-capable vanilla ButtonWidgets have visible hover/focus and readable contrast. Layout is capped by scaled window dimensions; long list titles are trimmed while the detail view wraps them. RU/EN checked natively at 1280×900, GUI scale 2; extreme scale/font/mod combinations were not certified.

A bounded JSON **S2C-only** display snapshot includes the screen sync ID and the player's own offers/status. Clients do not submit ownership, reward amounts, target/progress or instance data. Acceptance/claim use vanilla C2S screen-button packets: vanilla validates the active sync ID, then the handler verifies current identity, player UUID, full live multiblock and squared distance <=64 (8 blocks) before any operation. Opening also checks proximity. Closed/foreign/distant/destroyed-board handlers reject requests. A five-tick action throttle prevents rapid repeats; live progress syncs once a second while usable. Vanilla closes unusable server menus. No reward is given by opening or viewing.

Session offers are a sorted snapshot of up to 32 server-loaded definition IDs. Missing definitions are unavailable until reopening/reload; the current definition is revalidated at each action. Only three internal definitions currently exist. Each player's first instance of an ID determines its board status; normal board acceptance is **once per definition ID per player**, including cancelled/expired/rewarded records. Reopening/restarting never creates fresh instances or rerolls rewards. No timed refresh/economy is introduced. A future repeatable contract policy must explicitly change this rule.

## Integration with 20A

Acceptance delegates to `Contracts.assign`, retaining eligibility checks, ownership, cap, definition/reward snapshot and PersistentState. No Stage 20A service/model is replaced. Existing AFTER_DEATH progress, unique-death ledger and XP/item reward service remain authoritative. COMPLETED contracts expose a claim button calling `Contracts.claim` after the same session/owner checks. Full inventory leaves the claim pending according to 20A. `/wbcontracts` remains an operator development tool. The board is a separate ordinary-player access path with stronger location/session checks.

## Testing and evidence

Targeted source sets contain only Stage 20A/20B tests and the new bounded review client; the old full suite/internal diagnostics were not restored. Worlds/logs/screenshots are in ignored build/. A standalone GameTest server validates common/server classloading; the native integrated client validates rendering and real C2S acceptance. UUID ownership isolation uses server mock players, not two human network clients.

Coverage: registry, physical BlockItem placement/facing, support loss, all four orientations/root mapping, collision/outline presence, blockstate NBT roundtrip, whole cleanup/single drop, blocked volume, real screen opening, proximity, closed/destroyed/foreign menu denial, duplicate acceptance/no instance creation on repeated opening, owner isolation, reuse/persistence of 20A progress, deterministic candidate placement/repeat suppression, all five actual vanilla structure-type tail hooks with matching terrain fixtures. Stage 20A's eight lifecycle/reward/death/reload/persistence tests remain in the focused run; Patchouli adds its own smoke-test.

Initial 6-board + 8-contract + 1 Patchouli run PASS (15). A later all-five-villages fixture initially failed because Jigsaw expands structure bounds for terrain adaptation; the fixture was corrected to prepare the actual expanded-box candidate chunk. Then 7-board + 8-contract + 1 Patchouli run PASS (16, 3.192 s reported test time). Final BlockItem/support test and build results recorded below after completion.

Native review: bounded at 300 seconds per process, one client at a time. The first art sweep was invalidated after the underside fixture removed its own support and triggered correct cleanup. Subsequent native frames exposed a missing AVAILABLE translation, roof gaps/coplanar backing and poor underside framing. These were corrected and the final 13-frame sweep completed successfully. Final results: natural plains board count=1, volume model=true, real C2S acceptance=true, exactly one accepted instance, EN/RU GUI rendered. Final task completed in 1m37s, including normal-world generation and graceful save/shutdown. No extra human confirmation, push or public release.

Local screenshot directory: `build/stage20b-review/` (not committed). Files: front.png, back.png, left.png, right.png, above.png, below.png, front-45.png, back-45.png, village-day.png, village-night.png, gui-available.png, gui-accepted.png, gui-russian.png. The underside fixture temporarily suppresses support updates solely for photography, then restores the review mount; production placement always requires support. Native framebuffer images were inspected directly; desktop occlusion capture was not used as rendering evidence.

## Limitations

- Natural generation verified in a plains village; other four village types exercised through real placement hooks on controlled fixtures, not independent biome expeditions.
- Ordinary chunk blockstate persistence is roundtripped and the native world saved gracefully; a full manual client reopen of this world was not performed.
- Multiplayer UUID/session isolation and standalone server loading were tested, not two connected human clients.
- Snapshot offer cap=32, no refresh/repeatable contracts or final story economy. Existing accepted definitions retain the 20A snapshot policy.
- 20A's cross-file crash-atomic reward limitation remains unchanged. Vanilla light without shader bloom; no custom lantern animation.
- The model's item display contexts are statically validated; the dedicated native sweep focuses on the placed board and GUI, not every hand/ground/frame context separately.

Stage 20C remains unstarted. Public release v0.19.0 and protected game/monster/sword assets remain unchanged.

## Final checkpoint validation

Final `runGametest build --offline --console=plain`: **PASS in 32 seconds**. **17 tests: 8 board + 8 contract + 1 Patchouli**, no failures/errors; reported execution 3.054 seconds. Includes actual BlockItem placement/support loss and repeat acceptance after reopening independent of the action throttle. All **9 release mixins/refmap mappings verified**. Resource validator PASS (163 cuboids). JAR integrity/version/model/translations/contracts verified; test/review classes excluded.

Artifact: `build/libs/witchersbestiary-0.20.0-dev.jar`

Size: 8151331 bytes. SHA-256: `bcf59a930d8628f4c3c5283317efdcc9aa7cc11b0321655826a4f531e4bd0e79`.

Final native results: one generated plains board, one accepted contract through the client packet path, 13 diagnostic PNGs retained locally. Final front/rear/side/top/underside/oblique, day/night and EN/RU UI frames reviewed.
