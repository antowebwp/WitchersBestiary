# Cascade 1.21.1 research

The experiment starts at Stage 22J's `a8de233`. The comparison uses that branch's existing Grapeshot VFX as the control. The in-progress Arcane FX V2 renderer has its own checkpoint.

## Distribution and API

- [Modrinth release oSSE53mh](https://modrinth.com/mod/cascademc/version/oSSE53mh) publishes `1.0.0+1.21.1`; the project has separate 1.0.0 builds for 26.1, 26.2 and 26.3.
- [Fabric metadata at the 1.21.1 tag](https://github.com/ChocoBooyy/cascade/blob/v1.0.0%2B1.21.1/fabric/src/main/resources/fabric.mod.json) requires Minecraft 1.21.1, Java 21+, Loader 0.16+ and Fabric API. The loader JAR bundles the engine. The prototype uses Loom `modImplementation 'maven.modrinth:cascademc:oSSE53mh'` behind `-PcascadeExperiment`; the regular Fabric manifest has no dependency on Cascade.
- [MIT license](https://github.com/ChocoBooyy/cascade/blob/v1.0.0%2B1.21.1/LICENSE) permits modification and redistribution with the copyright and permission notice included in copies or substantial portions.
- [Emitter API](https://github.com/ChocoBooyy/cascade/blob/v1.0.0%2B1.21.1/common/src/main/java/dev/chocoboy/cascade/VfxEmitter.java) provides burst and continuous emission, animated atlas cells, size/opacity/color curves, drag, curl, gravity, collision, sub-emitters, ribbon trails and cube, shard, block and item meshes. Multiple emitters can build an explosion, dust cloud or shock front. The visual design and terrain or entity anchoring still belong to the caller.
- [Sequencing API](https://github.com/ChocoBooyy/cascade/blob/v1.0.0%2B1.21.1/common/src/main/java/dev/chocoboy/cascade/VfxSequence.java) supports delays and parallel branches. [SDF API](https://github.com/ChocoBooyy/cascade/blob/v1.0.0%2B1.21.1/common/src/main/java/dev/chocoboy/cascade/VfxSdf.java) supports raymarched spheres, boxes and tori. [Cast light](https://github.com/ChocoBooyy/cascade/blob/v1.0.0%2B1.21.1/common/src/main/java/dev/chocoboy/cascade/client/LightSplat.java) draws an additive pool on the ground and does not affect block lighting.
- [Fabric renderer](https://github.com/ChocoBooyy/cascade/blob/v1.0.0%2B1.21.1/fabric/src/main/java/dev/chocoboy/cascade/fabric/client/CascadeFabricClient.java) uses Fabric render events, registers soft-particle shaders and has a separate optional offscreen bloom pass. External shader packs are not required. Sodium/Iris combinations need separate runtime tests, especially bloom, soft depth and SDF. GeckoLib is present in the Witcher's Bestiary client classpath; Cascade does not declare it or call its API.
- [Render manager](https://github.com/ChocoBooyy/cascade/blob/v1.0.0%2B1.21.1/common/src/main/java/dev/chocoboy/cascade/client/VfxRenderManager.java) retires effects on tick, caps active effects at 256, sorts within 96 blocks by distance and uses a soft ceiling of 60,000 submitted primitives. The ceiling is not a GPU fill-rate budget. Bloom can redraw effects and raymarching costs fragments. GPU compute requires OpenGL 4.3 and is opt-in. Keep those paths off for initial RX 570 benchmarks and compare frame times in overlapping encounters before setting production caps.

## Original comparison at a2a4578

From the experiment worktree, run `./gradlew.bat -PcascadeExperiment runCascadeReview`. The client creates an isolated flat world, makes two actual Grapeshot throws against the same wall and takes frame sequences. The test mod switches the second impact's visual hook to a stock Cascade atlas and six bounded emitter layers. The mixin and review client are excluded from the normal mod JAR. The control is the Stage 22J effect; this is an A/B comparison of recipes as well as renderers.

## Original result

The Fabric 1.21.1 client started with Cascade 1.0.0 and GeckoLib 4.8.4. Both real throw and impact sequences completed. The paired frame at +5 client ticks is saved as `build/cascade-review/grapeshot-comparison.png`. Cascade's flash, ring, shard silhouettes and sparks read more clearly than Stage 22J. The orange cubes still form a conspicuous arch, and the smoke needs more separation from the flash for production. This run leaves OpenGL compute, bloom and raymarching off. It does not establish frame-time impact, performance on RX 570 or compatibility with Sodium/Iris.

Recommendation B as a gated direction. Keep Arcane FX responsible for effect identity, gameplay cues, anchoring, lifetime and fallback. Use Cascade optionally for short composite bursts if the next test confirms frame-time and render-stack compatibility. Keep long-lived gas and suppression anchored to real entities. No production adoption yet.

## Grapeshot volume rework

The first recipe sent all six emitters at once. Cascade's hemisphere sampler places particles on a dome shell, and identically timed cubes move outward from that shell. Its solid mesh path draws overlapping orange cubes with depth writes. The apparent orange arc came from their collective silhouette. The glow and smoke used camera-facing quads, the sparks used ribbon trails, and the ring was its own emitter. The renderer groups by material tier with additive glow after alpha; the smoke started small and dark in the same volume as the flash.

The revised test recipe draws exposed faces of independently oriented, procedurally colored voxel lobes. Four lobes form the flash, five expand in asymmetrical directions and six rise and disperse as smoke. Cascade still supplies the timed ring, two sizes of rotating metallic shards and short spark trails. The previous recipe remains available for the first shot of the same test run. Only the client test mod contains these changes.

Run `./gradlew.bat -PcascadeExperiment runCascadeReview` for the revised comparison. The test uses a small flat world and a stone platform. It throws four real Grapeshots, once with the previous recipe and once per camera angle with the revised recipe. Framebuffer captures are keyed to the first client tick observing each collision cue. The reduced server tick rate gives the client time to render each sample. The comparison grids and GIF live under build/cascade-volume-review. The scene checks the silhouette and timing; production modpack performance and Iris/Sodium compatibility are outside this run.

## Experimental signs and bombs integration

This branch's remapped dev build requires the separate Cascade Fabric release `1.0.0+1.21.1` (Modrinth version `oSSE53mh`). The game mod owns bomb visual cues, sign packets, culling and effect lifetimes. Cascade retains Grapeshot's approved ring, shards and sparks. The approved voxel recipe is shared by the game and its comparison harness.

The existing cast packets drive Igni, Fire Breath and Aard. The existing entity tracker drives Dragon gas and Moon Dust suppression marks. Entity removal immediately takes attached visuals out of drawing and bounded client maps expire their instances. Client rendering uses a procedural pixel palette and shaded faces, and does not require bloom or a shader pack. `-Dwitchersbestiary.fxLowDetail=true` uses reduced detail for Moon Dust, Samum and Dragon's Dream even up close. They also reduce detail beyond 22 blocks. This is not a frame-time profile for RX 570.

Run `./gradlew.bat runArcaneV2Review` to record actual framebuffer sequences in `build/arcane-fx-v2-review`. The test code is excluded from the remapped game mod.

## RC1 validation

Fire Breath locally reanchors its visible mesh to the caster's interpolated eye and heading. The server packet still bounds the reach and lifetime. A client collision ray can shorten that reach at a wall. Remote casters keep their server-supplied geometry. The server's contact checks, concentration and damage are unchanged.

Arcane FX schedules the four approved Grapeshot emitter layers on the receiving client. Cascade still simulates and draws the ring, both shard sizes and the sparks. Emitters are scoped to a client world, culled on the same 48-block range as the voxel effect, and capped at 48. The case report counts actual Cascade particles separately from Arcane FX sign and bomb instances. The PC benchmark samples render-to-render intervals at 960 by 540 with the frame rate capped at 120; it measures the test scenes rather than worst-case modpack performance.

- `./gradlew.bat build remapJar runGametest -PgameTestClass=IgniBreathTests,BombInteractionTests,SignFormsTests`
- `./gradlew.bat runArcaneV2Review` records the final framebuffer sequences and frame-time CSV. Assemble the requested images with `python tools/assemble-arcane-fx-rc1.py`.
- `./gradlew.bat stage20ePrepareLaunch` plus `python tools/run-arcane-fx-rc1-network.py` starts a dedicated loopback server and two native clients.
- `./gradlew.bat remapArcaneRc1Harness stage20ePrepareLaunch` plus `python tools/run-arcane-fx-rc1-compat.py <directory-with-original-renderer-jars>` exercises original Sodium 0.6.13 and Iris 1.8.8 through Fabric production mappings. Loom 1.7.4 cannot directly consume their newer Loom metadata.

Cascade is a required separate dependency. The Arcane FX JAR bundles no Cascade binaries or sources. Cascade itself is MIT licensed. The approval comparison and the standalone client test assets remain outside the game mod artifact.
