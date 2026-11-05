> 2026-11-05 combined hotfix supersedes lesson-recipe instructions: all five signs are now freely learned in the existing Bestiary. Legacy item IDs remain save-compatible. See COMBINED_HOTFIX_REPORT.md for geometry, native two-player/restart evidence and targeted 42/42 checks.

# Stage21G — COMPLETE
All mandatory implementation and available checks completed. See STAGE21G_REPORT.md for mechanics, controls, recipes, state policies, compatibility and evidence.

Branch dev/0.21.0; base777ec33, intermediate544d843; final checkpoint is the commit titled "Stage 21G: five witcher signs and concentration system" (git log -1). Preserve prior21A–F and stable releases. No21H/push/release requested.

Verified: full115/115 once, final focused29/29 + build +13 release mixin/refmap checks; actual dedicated/two native clients and restart, all5 R/G signs, GUI2 EN/GUI3 RU, HUD persistence/drag/corners/scale, remapping and one held-key request. Final artifact native control run-20261105-122225. Source package excludes test controllers; JAR hash in report. No outstanding mandatory mechanics or known failing checks. SkillTree was unavailable and NOT TESTED; no promised optional integration.

Future changes: run only directly affected checks unless new evidence justifies broad regression. Use Java21 C:/Program Files/Java/jdk-21; focused command in report. Native scripts keep worlds/logs/PNGs under ignored build and must not touch E:/Minecraft/game. Cleanup policy refusals remain respected; no destructive reset/clean/delete. No active test JVMs should be left running after final control.
