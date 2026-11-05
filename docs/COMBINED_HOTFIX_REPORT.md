# Combined hotfix — Rock Troll / Bestiary signs

COMPLETED, 2026-11-05; branch dev/0.21.0, base 83af6e1.

- Root cause: head core/cap shared y=40 and z=1 planes; both leg cores/feet shared rear z=5 planes. Recessed only the three internal cores by 0.25 model units. Same-bone transforms retain separation throughout animation. UV bounds validated; original texture and animation bytes unchanged, no troll AI/stats/rewards changes. Generator updated without regenerating approved assets.
- Existing Patchouli Bestiary now has EN/RU Witcher Signs category and five symbol/description/status/learning pages. Learn uses existing authenticated intent, SignPlayerState and SignLedger; server requires possession of the existing Bestiary. Free, idempotent, personal, saved and synchronized. Button disables after acknowledgement; hint uses actual rebound keys. No new learning requirements or resources.
- Retired five lesson crafting recipes/advancements; legacy item IDs/assets remain for saved-world compatibility. Existing learning progress retained. Five signs, balance, concentration, cooldown and official-fist prohibition unchanged.
- Fixed custom-component transient widget serialization and Patchouli widget vertical offset during real GUI review. Native test controller uses correct landing navigation and bounded retry for Windows atomic-file sharing contention.

## Verification

- `python tools/validate-rock-troll-seams.py`: PASS, four coplanar interfaces separated, all positive volumes and 512x512 UV bounds.
- Java 21 focused GameTests SignTests,SwordEnchantmentTests,FistFightTests: **42/42**; free/idempotent/personal/forged learning included. Log `build/combined-hotfix-check.log`. No repeated full regression.
- Final Gradle build PASS, 13 release mixins/refmap verified: `build/combined-hotfix-final-build.log`.
- Real dedicated server + two native production clients + same-world restart: **PASS**. Both Survival players use actual book Learn widgets (A initially Igni=1, B Aard=2, then both=31), then real R/G handlers and server-observed effects for every sign; selection/learning survive restart. SkillTree 1.4.4+1.21.1 with supplied dependencies, Better Combat, Simply Swords, Spell Engine and Spell Power loaded. Cached isolated copies only; no access to E:/Minecraft/game.
- Evidence `build/stage20e-network/run-20261105-172534/complete.json`, individual book/radial/sign PNGs and client/server logs. Actual EN/RU book images inspected; learned status and button separated. Troll front/rear/both sides/top, walk/quick/heavy/stomp/throw/alert/block frames inspected (`troll-review-sheet.png`, `troll-detail-sheet.png` and originals); no offending overlay observed. Geometry proof complements static frames.
- No push/release. No outstanding mandatory checks. Full earlier-stage regression intentionally not repeated; test fixtures temporarily disable troll AI only in the excluded harness.

Repeat native affected checks: Java21; `gradlew.bat stage20ePrepareLaunch remapStage20gServerHarness remapStage20gClientHarness`; `python tools/prepare-stage21h-production.py`; set STAGE20G_PRODUCTION=1, STAGE21G_NETWORK=1, STAGE21H_PROFILE=with, COMBINED_HOTFIX=1; `python tools/run-stage20e-network.py`.
