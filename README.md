# Witcher's Bestiary 0.20.0

Village notice boards now offer five persistent procedural hunts from twelve monster species, with generated names, Common/Rare/Epic profiles, personal targets and independent player progress. Separate legendary hunts include Swamp Lord and Grave Devourer.

Press **V** for Witcher Senses. Follow approximate locations, study two sequential highlighted clues, discover the lair and defeat the named monster. Return to a board to claim the emerald reward.

Village offers rotate after 72,000 simulation ticks (three days of running world time). Sleep and `/time set` do not shorten this interval. After claiming all five rewards, a personal refresh becomes available with a 24,000-tick cooldown from cycle creation; other players keep their offers. Reopen the board after refreshing. Accepted investigations and rewards are preserved.

Boards appear in new and supported visited villages; tested CTOV villages and optional Moog coexistence are supported within documented limits. `/wbboard repair` offers a safe placement fallback. Graveyard placement attempts are more frequent. Both Nekker Claw and Drowner Claw independently craft into two vanilla Black Dye.


[Release notes](docs/RELEASE_NOTES_0.20.0.md) · [Contract guide](docs/RENEWABLE_CONTRACTS_GUIDE.md)

An unofficial Witcher-inspired Minecraft mod featuring monsters, three haunted graveyard structures, trophies, alchemy, workshop decorations and three dimensional swords. Not affiliated with CD PROJEKT RED, Mojang or Microsoft.

## Install

Minecraft **1.21.1**, **Fabric**, **Java 21**. Install Fabric API, GeckoLib **4.8.4** and Patchouli **1.21.1-93-FABRIC**, then put `witchersbestiary-0.20.0.jar` in your instance's `mods` folder. Remove older copies and back up worlds before updating.

Tested baseline: Fabric Loader 0.19.5 and Fabric API 0.116.17+1.21.1. Optional Better Combat 2.3.2+1.21.1 requires PlayerAnimator and Cloth Config. Optional Simply Swords 1.63.0-1.21.1 requires its dependencies, including Architectury, Fzzy Config and SimplyTooltips. Install dependency versions compatible with Minecraft 1.21.1.

## Features

- Sixteen monster types including Grave Hag and Infernal Basilisk.
- Three graveyards with loot, crypts and persistent limited guardian spawns.
- Fifteen workshop blocks, a 45-slot Witcher Chest and weapon displays.
- Steel Hunter Sword, Silver Witcher Sword and Grave Blade.
- Sixteen trophy heads, eight oil categories and a bilingual Patchouli bestiary.
- Configurable progressive difficulty and optional Trinkets/Mod Menu integration.

## Build

Use Java 21 and the supplied Gradle wrapper: `./gradlew build` (Windows: `.\gradlew.bat build`). The development game JAR is `build/libs/witchersbestiary-0.20.0-dev.jar`. Offline builds require cached dependencies. No development history or internal screenshots are included in this public snapshot.

## Validation and limitations

The preceding release candidate passed four targeted client profiles: required libraries, Better Combat, Simply Swords, and both together. Those are historical compatibility checks, not new stable-client tests or certification of every modpack. World generation, old-world migration and long multiplayer sessions were not retested for this release. New structures require unexplored chunks. Back up saves.

See [release notes](docs/RELEASE_NOTES_0.19.0.md), [changelog](CHANGELOG.md) and [resource notices](NOTICE.md).
