# Stage 22K–22M recovery

The isolated Minecraft review captured the alchemy GUI and a successful small mutagen craft. The focused AlchemyTableTests run passed with the Patchouli smoke test. The remapped build passed.

Remaining acceptance work: capture the smithing UI and output, verify consumption and actual chest acquisition, exercise each bomb in-game, and verify mutagen equipment persistence and live damage. The current bombs detonate at an aimed location. They do not yet fly as projectiles. The current ingredient click cycles the preview only. Server crafting still selects from the staged input slots before inventory. The GUI still needs a verified exact crafting count and an equipped-mutagen display.

## RC1 checkpoint

The client review captured two current frames, the alchemy table and a successful small mutagen craft. The recipe planner now applies a preferred item, allows alternatives to fill the remaining demand, and simulates the repeated craft transaction to count available crafts and output capacity. The targeted GameTest checks exact count and preferred consumption. Equipped mutagen attachments sync to the owning client and the alchemy GUI exposes equip-from-main-hand and removal.

The four bombs now launch vanilla ballistic projectiles with server impacts, distinct particles, and no terrain destruction. Moon Dust still reveals targets and Dragon's Dream ignites targets directly. Ability suppression, an ignitable cloud, and client impact verification remain pending. Also pending are the full armor-template gameplay checks, a client reconnect and damage matrix for equipped mutagens, and the complete four-frame sheets. The current client evidence is build/stage22km-review/alchemy-comparison.png. Focused tests and remapped build passed.
