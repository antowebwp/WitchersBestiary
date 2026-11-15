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

## Reproduce

From the experiment worktree, run `./gradlew.bat -PcascadeExperiment runCascadeReview`. The client creates an isolated flat world, makes two actual Grapeshot throws against the same wall and takes frame sequences. The test mod switches the second impact's visual hook to a stock Cascade atlas and six bounded emitter layers. The mixin and review client are excluded from the normal mod JAR. The control is the Stage 22J effect; this is an A/B comparison of recipes as well as renderers.

## Observed result

The Fabric 1.21.1 client started with Cascade 1.0.0 and GeckoLib 4.8.4. Both real throw and impact sequences completed. The paired frame at +5 client ticks is saved as `build/cascade-review/grapeshot-comparison.png`. Cascade's flash, ring, shard silhouettes and sparks read more clearly than Stage 22J. The orange cubes still form a conspicuous arch, and the smoke needs more separation from the flash for production. This run leaves OpenGL compute, bloom and raymarching off. It does not establish frame-time impact, performance on RX 570 or compatibility with Sodium/Iris.

Recommendation B as a gated direction. Keep Arcane FX responsible for effect identity, gameplay cues, anchoring, lifetime and fallback. Use Cascade optionally for short composite bursts if the next test confirms frame-time and render-stack compatibility. Keep long-lived gas and suppression anchored to real entities. No production adoption yet.
