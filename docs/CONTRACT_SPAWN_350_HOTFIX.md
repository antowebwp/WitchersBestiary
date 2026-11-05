# Contract spawn 350 hotfix

Implementation checkpoint; combined Stage21I verification pending.

Procedural species no longer use natural biome/spawn rules. Safe full entity volume, supporting natural surface, fluids, border/height, occupied village POIs and nearby construction are checked. Board coordinates are passed explicitly; all new clue/lair points and spawn fallback stay within horizontal350 blocks. Two clues remain40–60 blocks apart, with bounded walkable surface sampling. Existing routes and legendary hunts retain their prior behavior.

Board acceptance uses a finite asynchronous survey: one future started per shared scheduler step, at most8 pending players,16 patches/job and1800 scheduler steps. Survey tickets expire after200 ticks and are explicitly released on patch changes, completion and cancellation; per-player ticket keys prevent interference. No future is awaited on a server tick. A complete route is saved before acceptance and HUD sync; physical clue rendering remains loaded-client-local. Closing/leaving the board cancels the pending request. Impossible terrain fails without storing an active contract or consuming its offer.

Existing real monster factory, rarity/profile, progressive attributes, binding UUID, AI, crowns and cancellation are reused. Nearby buildings are conservatively identified through natural floor/clearance, POI, block entity/plank/door/brick checks; arbitrary unmarked player stone structures cannot be identified perfectly.
