# 3D asset policy

New items and decorations require real volume geometry, correct UVs and original textures. Do not replace geometry with generated/handheld flat sprites without explicit user permission. Reusing an existing volumetric parent is allowed. Use static Minecraft JSON for static assets; GeckoLib only when animation needs it.

Preserve accepted monster and sword resources. Keep editable sources and deterministic, narrowly scoped generators with the new assets. Do not embed third-party reference images, extracted game files or local absolute paths. Validate volume bounds, faces, UVs, texture references and relevant item display contexts. Review real client rendering; do not infer visual PASS from compilation alone.

For the notice board run tools/validate-notice-board.py. The private-history aggregate validator is not present in this clean public snapshot and is not restored. Diagnostic images/worlds/logs belong under ignored build/, outside public commits. Run targeted tests and Gradle build; do not run the full historical GameTest suite for visual-only work.
