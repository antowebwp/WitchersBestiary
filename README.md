# Witcher's Bestiary 0.19.0

An unofficial Witcher-inspired Minecraft mod featuring monsters, three haunted graveyard structures, trophies, alchemy, workshop decorations and three dimensional swords. Not affiliated with CD PROJEKT RED, Mojang or Microsoft.

## Install

Minecraft **1.21.1**, **Fabric**, **Java 21**. Install Fabric API, GeckoLib **4.8.4** and Patchouli **1.21.1-93-FABRIC**, then put `witchersbestiary-0.19.0.jar` in your instance's `mods` folder. Remove older copies and back up worlds before updating.

Tested baseline: Fabric Loader 0.19.5 and Fabric API 0.116.17+1.21.1. Optional Better Combat 2.3.2+1.21.1 requires PlayerAnimator and Cloth Config. Optional Simply Swords 1.63.0-1.21.1 requires its dependencies, including Architectury, Fzzy Config and SimplyTooltips. Install dependency versions compatible with Minecraft 1.21.1.

## Features

- Sixteen monster types including Grave Hag and Infernal Basilisk.
- Three graveyards with loot, crypts and persistent limited guardian spawns.
- Fifteen workshop blocks, a 45-slot Witcher Chest and weapon displays.
- Steel Hunter Sword, Silver Witcher Sword and Grave Blade.
- Sixteen trophy heads, eight oil categories and a bilingual Patchouli bestiary.
- Configurable progressive difficulty and optional Trinkets/Mod Menu integration.

## Build

Use Java 21 and the supplied Gradle wrapper: `./gradlew build` (Windows: `.\gradlew.bat build`). The game JAR is `build/libs/witchersbestiary-0.19.0.jar`. Offline builds require cached dependencies. No development history or internal screenshots are included in this public snapshot.

## Validation and limitations

The preceding release candidate passed four targeted client profiles: required libraries, Better Combat, Simply Swords, and both together. Those are historical compatibility checks, not new stable-client tests or certification of every modpack. World generation, old-world migration and long multiplayer sessions were not retested for this release. New structures require unexplored chunks. Back up saves.

See [release notes](docs/RELEASE_NOTES_0.19.0.md), [changelog](CHANGELOG.md) and [resource notices](NOTICE.md).
