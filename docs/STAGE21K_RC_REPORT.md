# Stage 21K RC1

Status: PARTIAL. The local RC is ready for manual testing. Full gameplay acceptance is open.

- Artifact: `build/libs/witchersbestiary-0.21.0-rc1.jar`. SHA-256 `c3272ab0d0251b7c240c92d55b11c7d2d88a63188a26ec5bd5ad151a40ef2af6`. Embedded metadata reports `0.21.0-rc1`; all JSON resources parsed, archive has no test classes or `.tmp`/`.bak` entries, and all 13 release mixins passed build verification.
- The only product change is the RC version. Review of sign damage, sword prices and tournament rewards did not establish a reason for a balance change.
- Gradle build passed (`build/stage21k-build.log`). Full GameTests 136/137 (`build/stage21k-rc1.log`); unloaded survey failed its route assertion under concurrent startup. The focused Stage21I run passed 11/11 without changes to survey logic (`build/stage21k-focused.log`). Timing sensitivity remains unconfirmed.
- Packaged RC loaded a copy of the saved Stage21I world. Two clients verified restored contract/target identities and magic progress. Server saved all dimensions and exited (`build/stage21k-native-retry.log`, `run-20261109-112813`). The first native setup omitted its isolated harness jars; the successful retry restored those jars outside the release artifact.
- Better Combat 2.3.2, Simply Swords 1.63.0 and AdventureZ 1.4.24 ran in that profile. This verifies startup and saved-world coexistence. Spell Engine, Spell Power, Skill Tree, CTOV, Moog's Villager Structures, Ice and Fire and Biomes O' Plenty were not run with RC1.
- Prior Stage21 reports record gameplay evidence. RC1 still needs a fresh full contract, one genuinely played tournament, one full duel, GUI and model review, crafting, expensive sword purchase, five signs and Icy Grasp in play, and migration testing on a copy of a pre-Stage21 world. Earlier campaign assertions carried in the reload proof are historical, not fresh RC1 playthroughs.
- Local checkpoint `Stage 21K: 0.21.0 release candidate` on `dev/0.21.0`. User installation and saves were untouched.
