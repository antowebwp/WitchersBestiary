# Stage21E — Alchemy overhaul and Arcane Merchant

Status: COMPLETED — implemented and locally verified. Local0.21.0-dev, dev/0.21.0, 2026-11-04–05. Base d08e65b and Stage21A–D/contract reliability work preserved. No Stage21F, push, publication or user-game installation changes.

## Existing alchemy, complete recipe access

The existing `witchersbestiary:alchemy_workbench`, BlockItem, WorkshopInventory BlockEntity and all four saved slot indices remain unchanged. No migration or replacement block is necessary. Existing crafting recipes still work. The new table browser obtains actual crafting entries from RecipeManager: six existing elixirs, eight existing oils and three existing ingredient refinements,17 recipes total. Two refinements output wraith essence;17 is not a count of distinct new items. [Registered-content audit](STAGE21E_ALCHEMY_AUDIT.md).

The300×238 GUI has a category-cycle button (Elixirs/Oils/Components), paginated recipe list with full-name tooltips, output icon/name/count, actual category bonus/duration or oil charges, grouped ingredient counts and missing-material tooltips, Brew button, original three stored inputs/output and normal player inventory. It fits Minecraft's normal320×240 minimum scaled viewport. RU/EN reviewed at actual GUI3/2 respectively. Duplicate essence entries display once with the correct combined requirement. Numeric bonuses are first, so truncation cannot turn25% into2. Hovering the output shows existing full item tooltips/targets. The same planning function supplies the client availability preview; only the server commits.

AlchemyRecipes stages a copied inventory: original three table inputs plus36 main player slots. A bounded augmenting-path matcher handles overlapping tags/alternatives without greedy false rejection or exponential search. The actual recipe matches/craft/remainder functions determine the result. Output max-count/component compatibility and all remainder capacity are checked first; only then are input/player/output changes committed together. No dropped remainder or partial ingredient debit. Screen buttons require the current live handler, table availability, valid player, unchanged recipe-ID list and a four-tick click interval. Broken/removed/replaced table entities fail the existing canPlayerUse check. Normal inventory drag and Shift-click behavior remains server-controlled; output insertion is prohibited. Closing/logging out retains the old BlockEntity contents; concurrent players share the existing output buffer.

No elixir, oil, effect, level, toxicity or recipe-unlock gate added. Existing application and duration are preserved. The25% category multiplier is merely exported as the same1.25 constant for accurate descriptions; all damage comparisons retain the same value. Inventory oils retain existing15%/40-charge behavior, including the existing legacy coating path. Clear Mind remains the already-registered Fiend-hypnosis protection.

## One merchant entity and one crown market

`witchersbestiary:arcane_merchant` / Arcane Merchant / Таинственный торговец is an original PathAwareEntity + GeckoLib model, not a villager retexture. Editable deterministic source: tools/generate-arcane-merchant.py.12 bones,37 volume cubes,256px original material atlas, hood/face/beard, dark leather/cloak, gloves/boots, belt flasks, satchel and metal details. Idle, walking, trading clips plus actual player-facing head control work with installed GeckoLib4.8.4. Actual front/side/back/motion frames and trade GUI reviewed. Physical height2.05 matches the hood geometry. Static validator checks volume sizes, UV/atlas bounds, parent references and animation bones/clips.

Modes share entity, offers, GUI and purchase service:

- VILLAGE: permanent, saved home, bounded wandering/return navigation and customer look; no ordinary despawn. Existing shared village structure tag and structure-origin identity support vanilla and CTOV without a single-template dependency. The saved MerchantLedger claims a UUID once, so revisits, second players, chunk reload and restarts cannot create duplicates. Loaded-only safe search has12 attempts,9 candidates each,1200-tick retry spacing. Radius expands from5..21 toward outskirts, at most57 blocks. Schema2 accepts Schema1; only unclaimed old failed searches receive one SearchVersion2 retry window. Existing UUID claims stay untouched.
- WANDERING: slow travel/pauses, one currently loaded wandering merchant per Overworld, one scheduled attempt per12000 ticks with12 loaded-only40..64-block candidates near an alive player. Saved48000-loaded-tick lifetime; no expiry while trading or within48 blocks of a player. Normal chunk unload preserves entity/offers/lifetime. Three limited rare offers are selected once using the entity UUID and saved, not rerolled by opening GUI.

Both searches require natural solid/fluid-free floor, sufficient empty collision space, no path support, entity overlap or world-border violation. They never generate/force-load chunks. No stall/camp blocks are placed: the permitted NPC-only fallback avoids damage to player buildings, roads and third-party structures. Village merchants are not automatically replaced after death/unknown absence; this fail-closed claim rule prevents duplication of unloaded entities.

Merchant GUI shows mode, item icons/counts, authoritative crown price, shared stock, balance, purchase button and specific insufficient-funds/out-of-stock/space feedback. Payment supports physical crowns plus existing main/offhand wallets and offhand coins; wallet bounds/storage format remain Stage21A's. Purchase preflights copies, debits exactly the live server price, checks delivery capacity and commits inventory/stock synchronously. No buyback, free payout, XP or trophy market. Packet sync ID + random session UUID + monotonic sequence bind requests to the invoking player's live nearby merchant screen; exact replay spends/delivers once. Invalid distance/death/dimension/removal closes access. Multiple viewers can trade; last stock cannot be duplicated.

## Economy

Stage21A converts an emerald reward unit to10 crowns. Existing novice/experienced/champion fights pay12/28/60 and the troll90; a basic Ghoul contract's four emerald units become40. Basic component prices are fractions of these rewards; ready consumables cost more than acquiring common components and brewing. Rare items are limited and never required for contracts. No resale loop exists.

| Ordinary offer, one item per purchase | Crowns | Initial stock |
|---|---:|---:|
| Glass bottle |2|16|
| Red mushroom |3|12|
| Bone dust |5|12|
| Drowner essence |12|8|
| Ghoul claw |10|8|
| Necrophage oil |52|4|
| Necrophage elixir |64|3|

Village stock is replenished to at least8 per offer every48000 server-world ticks. Wandering merchants add three of: wraith essence24, katakan essence30, hybrid essence26, ogroid essence26, cursed oil72, vampire elixir90 crowns; each rare offer has2 units and no restock. All are existing registered content; no legendary trophy or future-stage item is sold.

## Validation

- One combined run: ALL94 GameTests PASS, build/stage21e-comprehensive.log. Existing89 scenarios retained. No repeated full regression.
- Related alchemy:3/3 (two scenarios +smoke), build/stage21e-final-alchemy-build.log. All17 actual recipes, exact result/consumption, no-input replay rejection, blocked-output rollback, shared table isolation, original NBT roundtrip, forged index and distance rejection.
- Related merchant:5/5 (four scenarios +smoke), build/stage21e-ctov-search-fix.log. Mixed physical/multiple/main/offhand wallets, insufficient-funds/full-bag rollback, shared last stock, valid token/sequence replay, foreign session/distance, saved mode/price/stock/UUID claims, natural-surface hazards and versioned failed-claim recovery. Expanded wandering rare-offer roundtrip PASS in build/stage21e-merchant-final.log (5/5): mode, all10 offer IDs/prices and depleted rare stock retained exactly.
- Gradle build and12 release mixin/refmap checks PASS; latest release/harness build in build/stage21e-ctov-search-fix.log. Static tools/validate-arcane-merchant.py PASS37 volumes/UV/three clips.
- Real localhost dedicated server and two native Better Combat clients, actual GUI2 EN at960×640 / GUI3 RU at1280×900. Native main campaign build/stage20e-network/run-20261104-233512/complete.json: both100-crown inventories (2 physical+98 wallet) bought oil52 ->48 each, shared stock4->2, second insufficient purchase refused, SAME table produced exactly two Cursed elixirs, normal output Shift-click, saved wallet/inventory/stock/block survived restart.
- Normal-world vanilla discovery: build/stage20e-network/run-20261104-234137/complete.json. Actual village928,0,-288, durable key minecraft:overworld/minecraft:village_plains/-77309411270. Production wandering spawn894,102,-327. Restart and two-player revisit retained one village merchant. Only wandering waiting timer was accelerated for review; entities/terrain were not injected in natural phases.
- CTOV3.6.3 +Lithostitched1.6.3 verified using the actual remapped release JAR and original optional jars. Original named-development Lithostitched mixin failure was a test-launch namespace issue; no third-party patch/disabled mixin used. The first dense CTOV search exhausted12 inner-centre candidates. The improved versioned search was tested on a COPY of that failed saved world, preserving its record: key minecraft:overworld/ctov:small/village_plains/231928234031, village752,0,864. Automatic village spawn succeeded on the outskirts; automatic wandering spawn807,74,883 and two-player/restart no-duplicate checks passed. Review retry clocks were accelerated; actual production placement/safety/claim logic created both entities.
- CTOV recovered campaign build/stage20e-network/run-20261105-000640 performed natural gates, restart, real RU/EN purchases/brewing, and another restart. Its last GUI test initially requested a now-distant walking merchant and was correctly refused. Related final replay used a COPY of that saved world and positioned viewers near the merchant: build/stage20e-network/run-20261105-001605/complete.json +build/stage21e-final-native.log PASS all consolidated gates, exact persisted inventories/stock, merchant reopening and read-only post-restart recipe inspection. Final RU/EN grouped-ingredient/bonus/disabled-Brew screens and mode/stock market screens inspected. Test fixes did not bypass purchase, crafting or distance validation.

## Reproduction

Java21, all artifacts/test worlds/logs remain ignored under build/. Keep current native JVMs stopped before rebuilding dev class directories.

```powershell
python tools/prepare-duel-compat.py
./gradlew.bat runGametest build stage20ePrepareLaunch -Pstage19fProfile=A -Pstage21dCompat --console=plain
# Related-only selectors: -PgameTestClass=AlchemyTableTests or MerchantTests
$env:STAGE21E_NETWORK='1'
python tools/run-stage20e-network.py
```

For original CTOV/Lithostitched production compatibility, build/remap the isolated network harnesses with remapStage20gServerHarness/remapStage20gClientHarness; run tools/prepare-stage21e-production.py. It uses already-local ignored copies/caches, never the user game installation. Set STAGE20G_PRODUCTION=1, STAGE21E_NETWORK=1, STAGE21E_NATURAL=1, STAGE21E_CTOV=1. An optional STAGE21E_RECOVER_NATURAL_WORLD copies a verified isolated test world for existing-village migration proof; STAGE21E_RECOVER_WORLD copies a finished alchemy campaign and its baselines for related GUI/persistence replay. Sources are retained and paths are restricted to this repository's ignored test-world tree.

## Compatibility limits and retained files

Actual vanilla and CTOV small-plains natural generation are verified. Other CTOV villages use the existing expandable notice_board_villages structure tag and the same algorithm; each template is not separately visited. Moog's Villager Structures is not confirmed/classified automatically; only verified village structures should be added to the shared tag. The unrelated local MoogsVoyager statue is not treated as a village. No blanket compatibility with arbitrary structure mods is claimed.

Original user game/saves, accepted monster/sword assets, Stage21A wallet, contract rewards/progressive difficulty and Stage21B–D mechanics preserved. No Stage21F started. Test evidence retained; no new disk cleanup claimed. Previous automatic review rejected test-world deletion (blocked by policy), and it was not retried through another mechanism.

Final release artifact SHA256: `4185e7eb08574ff65d7f79474a59b1ef3e9142f6685e9d394f587d436dcc104c`. Version0.21.0-dev, alchemy/merchant resources included; test harness and optional mod jars excluded. Existing crafting definitions and accepted fighter/troll resources unchanged; RU/EN new-key parity PASS. Final local checkpoint message: `Stage 21E: alchemy overhaul and arcane merchant`.
