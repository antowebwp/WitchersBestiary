# 0.20.0 RC1 release checklist

Status: RC1 BUILT — VALIDATION PENDING.

- [x]44/44 GameTests; Gradle build; ZIP/metadata/harness exclusion.
- [x]Local artifact0.20.0-rc1.
- [x]Final two-client contract/reward/restart journal PASS.
- [ ]Basilisk actual controls/turns/rejoin/Nether/lava/chunk lifecycle.
- [ ]Pinned optional modpack combat and animations; exact SkillTree configuration supplied.
- [ ]Natural multi-seed graveyard count, terrain boundaries and old-world reopening.
- [ ]Manual RU/EN UI, survival balance and long-session performance.

Manual network: two players accept different Ghoul contracts; verify private clues; logout one while peer advances; respawn and Nether roundtrip; finish both and replay claim after reconnect/restart; unload/reload target; discard another target and confirm no reward and ability to accept a new instance.

Manual basilisk: weaken/tame with MoltenHeart, saddle, mount empty hand, forward/back/turn; logout and remount same UUID; leave chunks and return; transfer to Nether safely and ride across lava. Verify owner-only mount and no duplicates.

Do not publish until pending checks reviewed. No push/release authorized.
