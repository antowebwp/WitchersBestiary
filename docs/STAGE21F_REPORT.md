# Stage21F — four sword enchantments

Status: implementation complete; native verification in progress. Do not interpret this draft as stage completion.

## Registry and survival

Minecraft1.21.1 vanilla dynamic enchantment registry; four data/witchersbestiary/enchantment JSON definitions, RegistryKey lookups and standard ItemEnchantmentsComponent. No obsolete Enchantment subclass registry or hard BC/SS dependency. All maximum levels areIII. Non-treasure tag participation exposes normal table/books and inherited vanilla random-enchantment pools. Arcane Merchant goods remain alchemy-only. The native fixture provisions materials/XP and searches a table seed for each desired clue, then actual Survival clients click the vanilla table button, spend3 levels/3 lapis and take the enchanted book. Anvil output is taken by normal client slot action and charges XP; no /enchant acquisition is counted as survival evidence.

| ID (witchersbestiary:) | I | II | III | Weight / anvil cost |
|---|---|---|---|---|
| monsters_bane | +5% | +9% | +13% | 5 / 2 |
| frost_blade | SlownessI,30ticks | SlownessI,50ticks | SlownessII,50ticks | 3 / 3 |
| blood_harvest | .5HP | 1HP | 1.5HP | 1 / 4 |
| runic_resonance | 5hits,radius2 | 4hits,radius2.5 | 3hits,radius3 | 1 / 4 |

All six WB pairs combine. Bane conflicts with vanilla damage-exclusive enchants (Sharpness/Smite/Bane of Arthropods); Frost conflicts with Fire Aspect. Other ordinary vanilla compatibility is retained. Level names use standard localized enchantment tooltips (RU/EN), and Runic adds synchronized numeric charge only.

## Authoritative damage and balance

The existing LivingEntity damage wrapper remains the sole effect commit point. Fist/PvP session interception stays first. A scoped real PlayerEntity.attack records its actual damage source/weapon; unrelated player-labelled damage and unique sword secondary ability sources cannot open their own proc scope. No reflection or BC polling per hit. HP plus absorption loss must be positive; shield, miss, invulnerability and virtual fist damage do not proc. A per-attack victim set prevents duplicate effect callbacks. Bane multiplies only damage to existing categorized WB monsters and never modifies monster stats, contract binding or progression. It composes once with category elixir1.25 and inventory oil1.15. Rock Troll retains its existing uncategorized classification (no category/alchemy redesign).

Frost uses a saved per-victim60tick cooldown, leaving a genuine gap after50ticks; no movement/AI override. Blood uses a saved per-player60tick cooldown across weapons, caps healing by actual damage and missingHP, and rejects decorative/passive/friendly/tamed/merchant/villager/fist targets. Ordinary hostile Minecraft mobs including Slime/Ghast qualify. Ordinary PvP follows server PvP rules; Runic pulse always excludes players.

Runic progress is a persistent/networked witchersbestiary:runic_state component on the actual stack, not a transferable player counter. One charge per world tick across sweep/BC multi-target callbacks. Pulse consumes charge before applying secondary damage, has an80tick per-weapon AND per-owner cooldown, and at default affects at most8 hostiles within spherical radius and sight. Damage is2HP custom magic, armor-bypassing, never category-elixir amplified; no recursive charge/heal, player/allied/NPC damage, block damage, fire or explosion. The primary victim is excluded from the secondary list so normal hurt frames cannot silently swallow its pulse damage. No global scans or per-tick enchantment work. State follows the physical sword on normal transfer; distinct swords never exchange charges.

Feedback: four enchanted-hit particles for Bane, five snowflakes/quiet high chime for Frost, four red dust particles on actual healing, one charge particle and twelve compact ring particles/quiet resonance on pulse. ServerWorld sends ordinary nearby-player vanilla particle/sound events; no new particle entity, shader or mandatory client library.

## Configuration and persistence

Existing config/witchersbestiary.properties gains enchantments.* server balance keys, appending only missing defaults and preserving user values/comments. Restart required; no client-controlled settings. Bane bounded0–15%, heal0–2HP, frost10–80ticks with cooldown at least maxDuration+10, Blood cooldown40–1200, Rune thresholds2–8/radius1–4/damage0–4/cooldown60–1200/targets1–12. Nonfinite/malformed numbers safely fall back. Defaults are in EnchantmentSettings.DEFAULTS. Player heal/pulse and victim frost readiness persist in ordinary entity NBT; weapon charges/readiness/last-hit tick persist in the component. Vanilla enchantments persist through vanilla item NBT.

## Compatibility and tests

Narrow sword tag includes vanilla swords and53 optional, required:false, locally audited Simply Swords blade IDs. Broad SS sword tags include hammers/axes and were deliberately not imported. Active one-hand/two-hand/unique blades are eligible; inactive decorative weapons, ordinary tools and non-sword SS weapons do not acquire these enchants. No original monster/sword models or third-party assets were changed/copied.

One full suite:108 tests executed, all94 prior regression tests passed; five of13 new sword tests initially failed. Necessary focused repeats resolved actual test-fixture issues (permanently creative vanilla mock helper, login protection, BC default vanilla-sweep suppression, daytime-burning/despawning zombie fixtures). Final BC focused run14/14 including harness; subsequent no-optional focused run15/15 including the additional PvP-off/pulse-player test and harness. Coverage includes all tiers, exact Bane/oil/elixir composition, actual anvil/table pools and512 book-generation seeds, all compatibility pairs, shields/misses/indirect rejection, native sweep, cooldown gaps, friendlies, Rune thresholds/radii/exact2HP/eight-target cap/no recursion, weapon/NBT serialization, contract Katakan/progressive stat binding and official NPC/PvP fists. Prior wallet/merchant/alchemy/contract/duel regressions remain passing. No repeated full regression after minor fixes.

Evidence: build/stage21f-comprehensive.log, focused5.log, without-optional.log; Gradle build PASS and strict13 production mixin/refmap checks PASS in without-optional.log. Release source sets exclude all test controllers. Native BC2.3.2 + SimplySwords1.63.0, dedicated/two clients/restart campaign is pending final result; do not claim it passed yet. First run011719 reached native enchanting on both clients, then screenshot filename failed on a Windows colon; test-only filename sanitization fixed.

## Reproduction / limits

Java21 C:/Program Files/Java/jdk-21. Focused tests: ./gradlew.bat runGametest build -Pstage19fProfile=A -PgameTestClass=SwordEnchantmentTests --console=plain; append -Pstage21dCompat for local BC profile. Production harness: ./gradlew.bat stage20ePrepareLaunch remapStage20gServerHarness remapStage20gClientHarness -Pstage19fProfile=A; python tools/prepare-stage21f-production.py; set STAGE20G_PRODUCTION=1 and STAGE21F_NETWORK=1, then python tools/run-stage20e-network.py. Dependency preparation reads only existing local caches/ignored test copies. Do not compile while native JVMs are loading. Fresh worlds/logs/screenshots stay under ignored build/. No protected game installation, destructive cleanup, push, release or21G work.

Compatibility is specific to audited local versions, not every weapon mod, protection plugin or modpack. Custom damage is scoped to actual PlayerEntity.attack; arbitrary mod ability/player-labelled periodic damage cannot borrow enchantment procs. Broader ecosystem/human long-play balancing remains outside bounded integration evidence.
