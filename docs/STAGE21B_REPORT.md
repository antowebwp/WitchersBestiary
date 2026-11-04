# Stage21B — NPC fist fighting

Date:2026-11-04. Base3c8b5b6, branch dev/0.21.0.

**Status: COMPLETED for the NPC fist-fighting scope, with the validation limits below.** This is a local0.21.0-dev checkpoint, not the full Stage21 RC1 or a public release.21C/21D/21E/signs/tournaments are not implemented.

## Architecture and rules

FistFightSession is an entity-independent two-fighter server simulation: virtual vitality, bounded stamina, actions, frame contacts, defense/counter windows, outcome and cancellation. FistFights is the Minecraft adapter for ownership, exclusive NPC locks, arenas, world collision/line-of-sight, navigation, input gating, synchronization and result delivery. FistNpc holds appearance/animation and its first-win reservation; FistRewards stores only pending earned sums per player. Future troll/PvP/tournament controllers can reuse the session model without another combat engine.

Minecraft health is never substituted for duel vitality. Virtual hits do not invoke vanilla damage; no health restoration or inventory confiscation occurs. Participants cannot use ordinary weapon damage against each other or outsiders. LivingEntity damage interception blocks outgoing real damage from participants; incoming external damage cancels first and then uses ordinary Minecraft damage. This is not environmental invulnerability. Player block destruction/placement and item use are denied during the mode. No blocks are destroyed by fist effects.

Confirmation is two-step: right-click a fighter, then sneak-right-click within10seconds with both hands empty. First interaction does not start combat. Navigation pauses for the invitation; countdown3–2–1 lasts60ticks. /wbfight cancel declines an invitation or stops a bout. Session radius12blocks, maximum6000 combat ticks; logout, NPC removal/unload/death, different dimensions, leaving the arena or external damage cancel safely. Cancelled models now explicitly expose CANCELLED, and the live map/HUD/poses are cleared. Sessions are deliberately not resumed after restart; normal player health/inventory remain ordinary saved data.

## Actions and AI

Quick contact at4ticks, recovery10ticks, cost8; heavy contact14ticks, recovery28ticks, cost24; counter contact5ticks, recovery14ticks, cost12. Quick pairs gain a small second-hit bonus. Heavy can follow a recovered quick attack; LMB becomes a counter within a server-granted defense window. A hit is resolved once even if invoked repeatedly.

Block drains.65stamina/tick plus10per quick or28per heavy contact; a front block reduces damage to25%, never absolute immunity. A guard without enough stamina breaks and staggers. Rear attacks ignore the guard. Timely guard and a dodge during its first6ticks open a short counter window. Dodge costs20, recovers after10ticks and uses ordinary collision-constrained velocity, not teleportation. Regeneration is.65/tick after an action delay; heavy/dodge delay35ticks. Quick has a limited chance to interrupt a heavy windup; counters stagger. Staggered fighters cannot start attacks.

Three separate creature entity IDs: novice_fighter, experienced_fighter, champion_fighter. Virtual NPC vitality70/95/120; stamina75/87/99. Decision intervals20/16/12ticks, weighted quick/heavy/guard/dodge choices and counter opportunities; no per-frame reaction to player input. Exhausted opponents retreat/recover with hysteresis instead of an endless low-stamina dodge loop. Standard navigation handles approach/obstacles and return toward the arena. Bouts never set a general hostile target; navigation/aggression stop on exit.

## NPC acquisition and controls

NPCs are deliberately placed with the three creative spawn eggs or vanilla operator /summon commands. This stage chose safe direct NPC interaction rather than introducing settlement worldgen; no automatic village spawn or new structure is claimed. A survival player can challenge an already placed fighter. Existing CTOV/notice-board generation was not changed.

LMB quick/contextual counter; H heavy; hold J guard; K dodge. All new keys are rebindable. V Witcher Senses unchanged; R/G remain reserved for future signs. Client input is active only in an authenticated session; server independently checks the current owner/session and stamina/state. Packets contain no client damage, winner, reward amount or foreign participant UUID. Illegal action ordinals are ignored; cooldown/action states reject rapid repeated attack requests. Duplicate block requests cannot reset parry timing.

The compact left HUD shows opponent, round/action, virtual vitality and stamina, with wrapped localized controls. It is outside the tested crosshair/hotbar/Witcher Senses areas. State is sent to its owner; separate bounded pose updates let observers render a participant without receiving their combat HUD. World changes/disconnect clear client state; stale pose entries expire.

## Models and animations

Original editable deterministic generator: tools/generate-fighters.py. Three separate geometries11/13/14bones and18/21/30cubes, original128px skins. Ragged tunic/wraps, vest/beard/bandana, and champion mantle/cape/headband have different geometry and silhouettes rather than one recolored skin. No protected monster/sword resources or third-party model files were replaced.

Twelve clips: idle, walk, combat_walk, stance, quick, heavy, block, dodge, counter, stagger, knockout, victory. Contact keys correspond to.2/.7/.25seconds. Server-tracked NPC actions drive GeckoLib. Knockout raises the model off the floor and holds the last frame instead of restarting the fall. Standard physical NPC collision bounds remain while the living entity plays its brief knockout animation.

Server-confirmed player poses affect normal/slim player models and sleeves; first-person empty-hand guard/heavy/counter/step poses run inside vanilla's balanced matrix scope. Item/sword transforms, GUI and racks are untouched. Body roll is restored before the next ordinary model update. Native views reviewed the main combat poses and hold; this is not an exhaustive camera/skin/left-handed frame-by-frame certification.

## Crowns and persistence

First server-wide defeat of a given NPC pays12/28/60crowns. The NPC's paid boolean persists in its own NBT. All subsequent victories against that same NPC are unpaid training, including victories by another player. This intentionally strict choice prevents repeat farming; there is no PvP reward.

Earned unpaid sums are stored compactly in a schema-versioned overworld PersistentState. Existing Stage21A ContractRewards.plan preflights delivery into wallets/physical inventory; no currency system or contract migration was rewritten. Full inventory keeps a pending sum. /wbfight claim or interacting with a fighter retries collection. Reward reservation and delivery occur on the server thread; completed fights are removed before subsequent requests. Pending sums and first-win flags survive normal saves/restarts. Unknown/corrupt reward schemas are preserved read-only. No per-tick PersistentState writes or chunk tickets/scanners.

## Automated validation

69/69 required GameTests PASS, final build/stage21b-checkpoint-verified.log. All prior64 retained; five composite scenarios cover quick/heavy contact frames, front guard/counter, single-contact resolution, dodge/counter timing, stamina exhaustion/recovery, invitation/confirmation, virtual victory/defeat, unchanged real health, first payout and unpaid rematch, paid-NBT reset, independent sessions/NPC exclusivity, blocked real damage/arena breaking, cancellation terminal state, external damage, unloaded NPC cleanup, full-bag pending payment and saved once-only wallet collection.

Gradle build, Java21/client compilation and12 release mixin/refmap checks PASS. Static asset validation checked positive cube sizes, UV atlas bounds, parent bones, skin dimensions and required clips/contact keys. No old tests were removed or disabled. After the initial combined run, fixes were checked and the final touched state received a closing full regression; there were no builds/tests after every individual class.

## Native client / dedicated server evidence

Production/intermediary namespace with original unmodified optional dependency jars: Better Combat2.3.2+1.21.1, playerAnimator2.0.4+1.21.1, Cloth Config15.0.140, GeckoLib4.8.4; Fabric API0.116.17+1.21.1, Loader0.19.5 and Patchouli1.21.1-93-FABRIC.

- run-20261104-015111: two real clients used actual entity-interaction packets and normal keybinding input on independent NPCs. Novice and Experienced ended in player victories;12and28crowns reached separate wallets. Champion defeated the player. All NPCs remained alive, normal player health remained unchanged. A new active bout was saved/interrupted by a real JVM restart; sessions/poses/HUD cleared and rewards did not repeat. complete=true.
- The native fixture temporarily freezes an NPC while a client prepares its interaction, then re-enables its ordinary AI before countdown ends. It does not edit virtual health, designate winners or award money directly. Operator arena construction/teleports happen only in the ignored review world.
- run-20261104-015818: a copy of that same saved world checked the held knockout pose at35/55ticks; operator pose review is separate from the actual bout/AI evidence. It is not presented as another three newly played category fights.
- run-20261104-022052: recovery from the same saved state exercised a real training rematch with the final left-side HUD. The player won and wallet balances stayed12/28: no repeated prize after restart. The final knockout pose review completed normally. complete=true. Historical category results in its JSON are imported from the first campaign; the new observations are the rematch, UI and pose review.

The first native attempt exposed invitation navigation/tracking timing and was stopped; invitation freeze and fixture readiness were corrected. A later UI-review controller repeated phase11; only that test controller was repaired, and022052 completed. Failed attempts are not counted as PASS. The two superseded failed directories were removed only after analysis/summary retention. Final model CANCELLED-state cleanup was subsequently rechecked by the final69 tests; it changes no rendered or saved combat outcome.

Screenshots:015111/WBOne-fist-tier-2-300.png,022052/WBOne-fist-tier-0-150.png,022052/WBOne-fist-knockout-55.png under build/stage20e-network. Reviewed final compact HUD, three distinct NPCs, visible attacks, virtual damage feedback, normal hearts, clean HUD after exit and held knockout. A screenshot alone is not used to prove AI correctness; actual server outcomes/vitality changes and independent rewards are the gameplay evidence.

## Limits and follow-up

This is NPC fighting only: no troll, duel stone, merchant, signs, enchantments or tournament gameplay. No automatic NPC settlement spawning. No exhaustive human balance study, all default/slim/left-handed camera combinations, arbitrary external spell/automation mod integration, new CTOV/Moog runtime matrix or whole-server TPS/MSPT benchmark. The no-Better-Combat mandatory profile passed server tests/compilation; native interactive review used the named Better Combat profile. No universal modpack compatibility is asserted. HUD layout was reviewed at960x640/GUI2; other overlays and extreme GUI configurations may need further tuning.

## Artifact / Git / cleanup

Local Stage21B checkpoint: build/libs/witchersbestiary-0.21.0-dev.jar (8423865bytes).
SHA256 f27590d308fc9e62d839711cf07111e1ecfd2ad69fa4451d0f52efeda6102399

Metadata0.21.0-dev and production resources inspected; test controllers/classes and logs excluded. This is not the full0.21.0 RC1. Published0.20.0 and RC artifacts restored/hash-checked; the21A dev checkpoint is backed up separately. v0.19.0 and v0.20.0 tags and protected directories unchanged. No push/publication.

Current-stage safe cleanup succeeded with checked absolute paths and no forced process termination: run-20261104-013520 (138616841bytes), run-20261104-021844 (138430161bytes). Total277047002bytes, approximately264.2MiB. Summary:build/stage21b-cleanup.json; parent logs and successful evidence worlds/screenshots retained. Previously blocked Stage21A files, global caches, sources, releases and user worlds were not touched.

## Stage21B fixes — verified follow-up, 2026-11-04

This section supersedes the earlier global first-win reward and fixed left-side HUD descriptions.

Personal reward reservations now use player UUID + exact NPC UUID, with a persisted24000-tick cooldown and duplicate-safe server completion. run-20261104-131805 paid12 crowns EACH to two real native clients defeating the SAME novice. run-20261104-132621 verified a real rematch victory after dedicated restart without another payout; balances/session reset were checked from saved world state. Pending/full-bag and persistence regressions passed GameTests.

HUD defaults to top right; /wbconfig opens its position/drag/scale/reset editor. Real-client drag and110% scale captures, Properties saves and new-JVM reload checks passed (run132621 and follow-ups). Placement clamps above the hotbar and within the viewport.

The original fighter assets were retained. Coplanar side surfaces overlapped at hips/body y12..13, upper arm/forearm y15..16, and hips/legs y10..11. The generator trims those hidden overlaps to joint boundaries. All three models were reviewed from four directions and combat poses in run-20261104-141402 (frames30/120/210/300); the waist interference is gone. Prior run133119 model captures are excluded because a saved test troll overlapped staged humans; the isolated review fixture now clears its own staging area.

AI decisions are faster, approach speed increased, repeated guard cannot reset timely-block age, and recovery retreat is no longer cancelled by the proximity stop. Novice remains beatable; experienced/champion retain stronger decisions and can defeat the real test client. Full73 tests passed under -Pstage19fProfile=A, then the touched FistFightTests class passed11/11 after adding habitat coverage. No push/release.


Final shared21B/21C regression:76/76 GameTests and Gradle build PASS (build/stage21-final-comprehensive.log), including max-stamina packet codec and troll contact timing. Release mixin/refmap checks12/12. Two actual native clients also independently earned90 vs the same troll and reloaded both balances; see STAGE21C_REPORT.md. The player guide now describes personal cooldowns and the movable right-side HUD.
