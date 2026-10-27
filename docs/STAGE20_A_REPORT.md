# Stage 20A — Witcher Contracts core foundation

Development version: **0.20.0-dev**. Branch: **dev/0.20.0**, based directly on public v0.19.0 / f4096eedaefecb2fd8d38a7ea24f05e863bc55b2. The public release/tag remain unchanged. No push or publication for this stage. The separate original development history and its verified bundle/restore copy are preserved; incompatible histories were not merged and excluded reference/diagnostic materials were not restored.

## Architecture

Common/server package `com.witchersbestiary.contracts`:

- `ContractDefinition`: immutable validated JSON definition, reward, optional eligibility and lifetime.
- `ContractDefinitions`: server-data reload listener; atomically replaces an immutable registry after parsing. Invalid entries are logged with resource/pack identity and excluded.
- `ContractInstance`: identity, owner UUID, definition/reward snapshot, creation/deadline, state and credited-death UUID ledger.
- `ContractState`: overworld PersistentState shared across dimensions, instance and owner indexes, schema and quarantine.
- `Contracts`: server events, eligibility/ownership validation, developer commands and integration facade.
- `ContractRewards`: complete inventory preflight and once-only claim transition/delivery on the server thread.

No client-only dependency, networking/UI, board, tracking effect, new monster, variant, currency or spawn/loot/combat change is added.

## Lifecycle and data

`AVAILABLE -> ACCEPTED -> COMPLETED -> REWARDED`. AVAILABLE/ACCEPTED can also become CANCELLED or EXPIRED; terminal states cannot be reopened. Completion requires exactly the required number of unique qualifying kills. A kill cannot advance a completed instance or produce repeated completion. The developer assign command creates and immediately accepts a fresh instance; Stage 20B can expose availability/acceptance separately using the domain transition API.

An instance stores unique instance UUID, definition ID/snapshot, player UUID, monster Identifier, difficulty tier, progress/required progress, reward configuration, creation epoch milliseconds, optional duration/deadline and state. COMPLETED/REWARDED encode completion and claim status without independent contradictory booleans. Deadlines use server wall-clock time, including offline time. Expiration is lazy at assignment/inspection or relevant kill; no background scanner. COMPLETED rewards do not expire. Multiple accepted contracts for the same target each receive the matching kill. Up to 16 accepted/completed-unclaimed instances per player; expired entries do not consume this limit.

## Persistence

Overworld `data/witchersbestiary_contracts.dat`, schema **1**, via Minecraft PersistentState. Active, completed, cancelled, expired and rewarded instances remain owned and retained; credited UUIDs preserve deduplication after reload. Every event/service mutation marks state dirty. Normal world saving/shutdown persists it alongside player data. In-memory owner index is rebuilt on load.

Accepted conditions and rewards are snapshotted: editing a JSON definition does not change an existing bargain. Removing or invalidating its live definition pauses kill/claim operations; cancellation remains possible. Restoring the same ID re-enables its snapshotted bargain. Missing registry items/entities in an old snapshot cause the record to be quarantined and preserved verbatim, not deleted. A later restart with those registries restored retries decoding. Unknown store schema is preserved read-only; no silent reset or attempted migration. Future schema migrations need explicit implementations.

Persistence uses vanilla saves, not a transaction spanning contract state and player files. Graceful save/restart is supported; abrupt process/power failure between separate vanilla saves can roll back progress or delivery. Crash-atomic exactly-once delivery is not claimed. Keep world backups. Archived contracts/ledgers are retained without automatic pruning; a future retention policy must preserve claimed-state safety.

## Definition format

Datapack files: `data/<namespace>/contracts/<path>.json`; ID is derived from namespace/path. Optional explicit `id` must agree with that resource ID. Use `name` and `description` as translation keys or display text. Required fields: target, required, difficulty, reward. Optional eligibility: minimum_level, dimension; optional duration_millis (0/no deadline, maximum 365 days).

Validation: existing WB MONSTER target only, positive integral kills up to 10,000; tier 1..5; minimum level 0..1,000; experience 0..100,000; at most 32 item entries, each 1..2,304 items; rewards restricted to emeralds or registered WB items. Fractional integer fields and out-of-range values are rejected. Emeralds may be supplied as reward.emeralds, materials as reward.items entries with item/count. These limits are validation policies, not new balance changes to existing systems.

Examples (three internal templates; developer access only):

| ID suffix | Target | Kills | Tier | Reward |
|---|---|---:|---:|---|
| drowner_cleanup | drowner | 3 | 1 | 30 XP, 2 emeralds, 1 drowner essence |
| ghoul_hunt | ghoul | 4 | 2 | 60 XP, 4 emeralds, 1 ghoul claw |
| foglet_hunt | foglet | 2 | 3 | 90 XP, 6 emeralds, 1 foglet essence |

All IDs use the witchersbestiary namespace. RU/EN keys are included. Normal `/reload` reloads definitions. No accepted instance is regenerated by reload.

## Kill integration

Fabric ServerLivingEntityEvents.AFTER_DEATH only. Victim must actually be dead; DamageSource attacker must resolve to a ServerPlayerEntity on the same server. This covers direct and player-owned projectile damage. No fallback to last attacker: unattributed environmental deaths, unrelated monsters and other players' contracts are ignored. Pet-only or indirect trap kills without a reliable player attacker are deliberately excluded. The ledger deduplicates UUIDs within each contract; no global entity scan, chunk loading or AI modification. Owner index avoids scanning other players' contract archives, but a player's own retained history is still iterated.

## Rewards

`Contracts.claim` resolves the stored contract and current definition availability, then validates owner and COMPLETED state. `ContractRewards` checks that the instance belongs to the supplied store. It plans all inventory merges/splits using copies before any state mutation. If the entire item reward will not fit in the 36-slot main inventory, reject with no XP/items delivered and keep the claim pending. No ground drops, partial grants or deletion. After a successful plan, transition once to REWARDED, mark dirty, apply inventory and XP, synchronize the screen handler. Repeated calls and loaded REWARDED records reject further claims. Shared contract/player save crash limitations are described above.

## Developer commands

All subcommands require permission level 2. Ordinary survival players cannot assign or claim via this test interface. Console can list, assign and inspect; claim/cancel act on the executing player, with ownership checks.

```
/wbcontracts list
/wbcontracts assign <online-player> witchersbestiary:drowner_cleanup
/wbcontracts inspect <online-player>
/wbcontracts claim <instance-uuid>
/wbcontracts cancel <instance-uuid>
```

Inspection shows ID, definition ID, state, progress/required and unavailable-definition flag. Definition ID suggestions are provided. These commands are a developer test interface, not the final player flow.

## Validation

Focused source set contains only the new contract tests; no prior full GameTest suite was restored. Run `./gradlew runGametest --offline --console=plain` with Java 21. Fabric additionally discovers Patchouli's own smoke-test. Tests use real server events/damage sources with mock server players; this is not a two-human multiplayer session.

Checks cover state transitions, unique completion, cancellation/expiration, NBT roundtrip, compressed NBT disk roundtrip, owner index reconstruction, progress/claim persistence, direct/projectile/environment attribution, unrelated targets, multiplayer UUID isolation, repeated death callbacks, permission denial, wrong-owner operations, incomplete/full-inventory/repeated claims, XP/emerald/WB material delivery, invalid/missing definitions, paused instances, reload snapshots, corrupt-record preservation and future-schema protection.

Initial focused run: **8 contract tests + 1 Patchouli test PASS**, 4.382 seconds reported test execution, Gradle task 49 seconds. Final focused/build results recorded below after completion. No real stop/relaunch persistence test or native client UI test; disk/NBT deserialization covers persisted state. Mock clients emit pre-existing Cardinal Components handshake disconnect warnings, but server-side assertions complete. Expected errors for deliberately invalid/future-schema records occur in negative tests. No full GameTest, long survival soak or release publication.

## Stage 20B integration points

Use validated definitions for notice-board availability; persist AVAILABLE instances, validate eligibility before acceptance, then mark the store dirty after successful domain mutations. Future networking must resolve the sender UUID and invoke ownership-checked services on the server thread; never trust client progress/reward snapshots. Introduce explicit transactions/migrations/retention as requirements expand. No Stage 20B implementation is included.

## Final checkpoint results

27 October 2026. Final focused run: **8 contract tests + 1 Patchouli test PASS**, 3.762 seconds reported execution; Gradle run 40 seconds. Final `build` PASS in **26 seconds**, release mixin/refmap checks PASS. JAR ZIP check PASS, version 0.20.0-dev, three definitions present, no contract test classes packaged.

Artifact: `build/libs/witchersbestiary-0.20.0-dev.jar`

Size: 8005827 bytes. SHA-256: `4ecadb9ed0a84f05f81fc87d0388aaba7f5b86ecee8411850cb974fa7702a793`.

Raw local logs/XML remain under ignored build/ and are not committed to the public-history branch.
