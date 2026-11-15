# Stage 22K–22M recovery

The isolated Minecraft review captured the alchemy GUI and a successful small mutagen craft. The focused AlchemyTableTests run passed with the Patchouli smoke test. The remapped build passed.

Remaining acceptance work: capture the smithing UI and output, verify consumption and actual chest acquisition, exercise each bomb in-game, and verify mutagen equipment persistence and live damage. The current bombs detonate at an aimed location. They do not yet fly as projectiles. The current ingredient click cycles the preview only. Server crafting still selects from the staged input slots before inventory. The GUI still needs a verified exact crafting count and an equipped-mutagen display.

## RC1 checkpoint

The client review captured two current frames, the alchemy table and a successful small mutagen craft. The recipe planner now applies a preferred item, allows alternatives to fill the remaining demand, and simulates the repeated craft transaction to count available crafts and output capacity. The targeted GameTest checks exact count and preferred consumption. Equipped mutagen attachments sync to the owning client and the alchemy GUI exposes equip-from-main-hand and removal.

The four bombs now launch vanilla ballistic projectiles with server impacts, distinct particles, and no terrain destruction. Moon Dust still reveals targets and Dragon's Dream ignites targets directly. Ability suppression, an ignitable cloud, and client impact verification remain pending. Also pending are the full armor-template gameplay checks, a client reconnect and damage matrix for equipped mutagens, and the complete four-frame sheets. The current client evidence is build/stage22km-review/alchemy-comparison.png. Focused tests and remapped build passed.

## RC2 checkpoint — PARTIAL

Moon Dust prevents selected supernatural actions for 120 ticks and reveals targets. Dragon Gas lasts 160 ticks, is capped at 32 loaded clouds per world, and is removed when ignited by Igni cone or breath. Targeted tests passed. The client review verified an actual Cat chestplate upgrade and one template consumed. The two partial sheets are alchemy-rc2.png and smithing-rc2.png under build/stage22km-review.

Still pending: four live bomb scenarios and dedicated FX frames, end-to-end mutagen reconnect and damage matrix, chest and merchant acquisition, mastercrafted smithing and enchantment retention, and the two full PNG sheets. The existing build and review logs are build/stage22km-rc2-final.log and build/stage22km-rc2-client.log.
