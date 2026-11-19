# 0.22.0 — Legends Reforged

- **Contracts:** Rotating named Legends, two-clue investigations with route recovery, and saved vanilla bonus rewards alongside crowns. Existing contracts and claim states carry forward.
- **Legendary swords:** Blood Legacy builds charge in eight hits. Bloodlust releases a blood wave, then restores 20% of health damage dealt for 10 seconds, up to the wielder’s maximum health. The cooldown is 30 seconds after Bloodlust ends. Grave Blade summons an allied wraith. Both activate on a tap of the rebindable Legendary Weapon Ability key (B by default). The Ogroid Hammer shares that key and slams on release after at least half a second of windup.
- **Higher Vampire:** Combat phases, blood cocoon and Crimson Phylactery sealing. The first hunt awards Blood Legacy, and repeat hunts have their own payout.
- **Griffin:** Refined anatomy, wing animations and flight combat, with telegraphed dives that Aard can interrupt.
- **Unicorn:** Wild spawning, riding progression, follow and guard behavior, a whistle, and ownership and riding progression preserved through revival.
- **Signs and effects:** Sign Mastery is available from the R radial menu. Arcane FX and Cascade power updated Signs, bomb effects and Blood Legacy’s blood wave. The retired Grimoire still loads as an inert item in old inventories.
- **Compatibility:** Minecraft 1.21.1 on Fabric and Java 21. Requires Fabric API, GeckoLib 4.8.4 or later in the 4.x series, Patchouli 1.21.1-93-FABRIC and Cascade 1.0.0+1.21.1. Use the same build on server and clients.

# 0.21.0 - Witcher Signs and Tournaments

Adds crowns and wallets, nonfatal fist fights, the Rock Troll, PvP duels, alchemy and merchants, sword enchantments, five signs, the Grimoire and three tournament tiers. Contract hunts work outside natural biomes, legendary offers rotate independently, and new payouts scale by monster danger. See [release notes](docs/RELEASE_NOTES_0.21.0.md).

# 0.20.0 — Witcher Contracts Update

Persistent village offers, twelve procedural hunt species, two-clue Witcher Senses investigations, renewable personal hunts, separate legendary contracts, visited-village boards, bounded terminal history and two claw-to-black-dye recipes. See [release notes](docs/RELEASE_NOTES_0.20.0.md) for requirements and limitations. Owner-approved RC5 promoted with version-only rebuild; no repeat QA.

# Changelog

## 0.20.0-dev — Stage 20A–20C.1 (unreleased)

Adds the Swamp Lord named contract hunt: unique persisted target, loaded-only cooperative search, dash, at most two helpers, local BossBar and existing board reward flow. Other named bosses are not included yet.

Adds original 3D notice boards, best-effort placement near new vanilla villages, localized parchment contract browsing/acceptance, secure server menu validation and existing-service reward claiming.

Server-authoritative contract lifecycle, JSON definitions, persistent ownership/progress/reward states, kill attribution, once-only claims and operator test commands. Three internal contract examples and focused tests. No tracking effects or new monsters. Based on public v0.19.0; not published.

## 0.19.0

First stable packaging of the 0.19 release candidate. Adds Grave Hag combat, haunted graveyards, persistent guardian budgets, workshop storage/decorations, three dimensional swords, trophy heads and alchemy refinements from the 0.19 development cycle. Preserves the accepted sword hand rendering fix. Stable packaging changes metadata and documentation, without changing gameplay from 0.19.0-rc.1.
