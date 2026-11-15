# Project rules

Develop on the public-history dev/0.20.0 branch. Do not merge the separate archived development history or restore excluded third-party references/internal diagnostics. Do not push or publish without explicit user approval. Preserve the v0.19.0 release and tag.

Read docs/PROGRESS.md, docs/ROADMAP.md and the relevant stage report. For new visual assets follow docs/3D_ASSET_POLICY.md. Preserve accepted monster/sword models and unrelated game mechanics. Keep test worlds, screenshots and raw logs under ignored build/. Never modify the separate user game installation.

## Test disk usage

Use a named, isolated directory for each necessary world test and reuse it for related checks. At the end of a stage measure `build/`, preserve current JARs, useful screenshots, failure logs and recovery inputs, and remove only disposable artifacts owned by completed automated runs. Verify that associated processes have exited and resolve every deletion target inside the intended project test directory before deleting. Preserve user worlds, source, release backups, external caches and any unclear directory. Record reclaimed space and remaining `build/` size. Never bypass a deletion restriction.

## Bestiary-first content documentation

For each new or materially changed monster, sign or form, alchemical recipe, weapon, armor set, mutagen, item, or gameplay system, update its entry in the existing Bestiary in the same change. Explain availability, use, progression and relevant mechanics, with original illustrations or item models where appropriate. Decorative items can use a brief catalog entry. A mechanic is complete when players can find an accurate description in the book. Review the Patchouli entry and existing links, item tooltips, live recipe pages, EN/RU localization and agreement with actual mechanics. Preserve book entry IDs and unlock progress. Gate future investigation chapters by implemented progression before publishing them.

Future Higher Vampire entries follow the investigation: contract introduction, evidence, preparation, then sealing. Add stable entry IDs and unlock conditions when the matching milestones exist.
