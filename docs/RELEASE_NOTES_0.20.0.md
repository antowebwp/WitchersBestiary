# Witcher's Bestiary 0.20.0 — Witcher Contracts Update

Village notice boards now offer five persistent procedural hunts from twelve monster species, with generated names, Common/Rare/Epic profiles, personal targets and independent player progress. Separate legendary hunts include Swamp Lord and Grave Devourer.

Press **V** for Witcher Senses. Follow approximate locations, study two sequential highlighted clues, discover the lair and defeat the named monster. Return to a board to claim the emerald reward.

Village offers rotate after 72,000 simulation ticks (three days of running world time). Sleep and `/time set` do not shorten this interval. After claiming all five rewards, a personal refresh becomes available with a 24,000-tick cooldown from cycle creation; other players keep their offers. Reopen the board after refreshing. Accepted investigations and rewards are preserved.

Boards appear in new and supported visited villages; tested CTOV villages and optional Moog coexistence are supported within documented limits. `/wbboard repair` offers a safe placement fallback. Graveyard placement attempts are more frequent. Both Nekker Claw and Drowner Claw independently craft into two vanilla Black Dye.

## Installation

Requires Minecraft **1.21.1**, Fabric Loader **>=0.17**, **Java 21**, **Fabric API**, **GeckoLib >=4.8.4 and <5**, and **Patchouli 1.21.1-93-FABRIC**. Remove older mod copies, back up your world and place the release JAR in your instance's mods folder. Optional integrations are not required dependencies.

## Validation and limitations

The RC5 baseline passed 55/55 GameTests and its Gradle build; dedicated-server/two-client village and restart checks were recorded. The owner additionally tested RC5 manually and approved this stable publication. No repeat QA was performed for publication; the stable artifact was rebuilt only to update version metadata.

Not every CTOV variant, overlapping settlement or modpack has been verified. Newer procedural species reuse native AI; additional randomized ability variety is limited. Some habitats can leave hunts waiting for safely loaded suitable terrain. Some complete five-hunt, active-RC3-migration and rotation multiplayer scenarios were not independently automated; see docs/STAGE20_H_REPORT.md. Reopen older board windows after rotation. Older active hunts may appear as additional entries. No universal compatibility or performance guarantee is claimed.

This release also includes contract persistence, network synchronization, reward replay protection and bounded terminal-history improvements. No crowns, magic, Rock Troll or fist fighting are included.
