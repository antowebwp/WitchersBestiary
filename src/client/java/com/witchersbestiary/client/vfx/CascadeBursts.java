package com.witchersbestiary.client.vfx;

import com.witchersbestiary.alchemy.BombVfx;
import dev.chocoboy.cascade.Vfx;
import dev.chocoboy.cascade.client.ParticleBurstEffect;
import dev.chocoboy.cascade.client.RenderedEffect;
import dev.chocoboy.cascade.client.VfxFrame;
import dev.chocoboy.cascade.client.VfxRenderManager;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.ParticleSystem;
import dev.chocoboy.cascade.engine.effect.SpriteId;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Cascade CPU backend for short supplementary particles, scoped to the receiving world. */
final class CascadeBursts {
    private static final int MAX_EMITTERS = 48;
    private static final List<Burst> ACTIVE = new ArrayList<>();
    private static Frustum frustum;

    private record Layer(int tick, double rise, EmitterSpec spec) {}
    private static final List<Layer> LAYERS = List.of(
            new Layer(2, .04, Vfx.emitter().shape(ShapeSpec.ring(.46f)).count(23).lifetime(7).cube()
                    .blend(BlendMode.ALPHA).speed(.23f).size(.046f,.026f,Easings.LINEAR)
                    .gradient(Easings.LINEAR,0xc6aa82,0x887264).alpha(.63f,0,Easings.EASE_OUT_QUAD).spec()),
            new Layer(3, .24, Vfx.emitter().shape(ShapeSpec.hemisphere(.18f)).count(13).lifetime(20).shard()
                    .spin(3.5f).size(.095f,.051f,Easings.EASE_IN_QUAD).speed(.21f).gravity(0,-.023f,0)
                    .drag(.03f).gradient(Easings.LINEAR,0xd6c4b3,0x877e80,0x524b53)
                    .alpha(1,0,Easings.EASE_IN_QUAD).spec()),
            new Layer(3, .24, Vfx.emitter().shape(ShapeSpec.hemisphere(.13f)).count(19).lifetime(13)
                    .sprite(SpriteId.SPARK).trail(2).stretch(2.2f).speed(.29f).gravity(0,-.019f,0)
                    .size(.043f,.010f,Easings.LINEAR).gradient(Easings.LINEAR,0xffe899,0xd46d22)
                    .alpha(1,0,Easings.EASE_OUT_QUAD).spec()),
            new Layer(5, .24, Vfx.emitter().shape(ShapeSpec.hemisphere(.22f)).count(9).lifetime(17).shard()
                    .spin(5).size(.059f,.019f,Easings.LINEAR).speed(.30f).gravity(0,-.026f,0)
                    .gradient(Easings.LINEAR,0xb5acac,0x514c58).alpha(1,0,Easings.EASE_IN_QUAD).spec()));

    private CascadeBursts() {}

    private static final EmitterSpec FIRE_SPARKS = Vfx.emitter().shape(ShapeSpec.hemisphere(.30f)).count(24).lifetime(13)
            .sprite(SpriteId.SPARK).trail(2).stretch(1.7f).speed(.24f).gravity(0,-.016f,0)
            .size(.038f,.010f,Easings.LINEAR).gradient(Easings.LINEAR,0xfff7ab,0xff7526)
            .alpha(.9f,0,Easings.EASE_OUT_QUAD).spec();
    private static final EmitterSpec FIRE_SMOKE = Vfx.emitter().shape(ShapeSpec.ring(.75f)).count(12).lifetime(17)
            .cube().blend(BlendMode.ALPHA).speed(.095f).gravity(0,.005f,0).drag(.13f)
            .size(.08f,.22f,Easings.EASE_OUT_QUAD).gradient(Easings.LINEAR,0x9a6851,0x40363a)
            .alpha(.42f,0,Easings.EASE_OUT_QUAD).spec();

    static void fireburst(ClientWorld world, Vec3d position, long seed, boolean coarse) {
        spawn(world, position.add(0, .18, 0), FIRE_SPARKS, seed, coarse);
        spawn(world, position.add(0, .39, 0), FIRE_SMOKE, seed + 1, coarse);
    }

    private static void spawn(ClientWorld world, Vec3d position, EmitterSpec spec, long seed, boolean coarse) {
        if (ACTIVE.size() >= MAX_EMITTERS) return;
        var simulation = spec.build(new Random(seed), null, coarse ? .55f : 1);
        var burst = new Burst(world, position, world.getTime() + 24, simulation,
                new ParticleBurstEffect(position, simulation, spec.render(), null, 0));
        ACTIVE.add(burst);
        VfxRenderManager.get().spawn(burst);
    }

    static void tick(ClientWorld world, BombVfx.Cue cue, long age, boolean coarse) {
        for (int i = 0; i < LAYERS.size(); i++) {
            var layer = LAYERS.get(i);
            if (layer.tick != age || ACTIVE.size() >= MAX_EMITTERS) continue;
            var position = cue.center().add(0, layer.rise, 0);
            spawn(world, position, layer.spec, cue.seed() + i, coarse);
        }
    }

    static void prune(ClientWorld world) {
        ACTIVE.removeIf(burst -> burst.world != world || burst.end <= world.getTime());
    }

    static void clear() {
        ACTIVE.clear();
        frustum = null;
    }

    static void setFrustum(Frustum current) {
        frustum = current;
    }

    static int emitters() {
        return ACTIVE.size();
    }

    static int particles() {
        return ACTIVE.stream().mapToInt(burst -> burst.simulation.particles().size()).sum();
    }

    private record Burst(ClientWorld world, Vec3d position, long end, ParticleSystem simulation,
                         RenderedEffect delegate) implements RenderedEffect {
        @Override public boolean tick() {
            if (world == MinecraftClient.getInstance().world && !delegate.tick()) return false;
            ACTIVE.remove(this);
            return true;
        }

        @Override public void render(VfxFrame frame) {
            if (world == MinecraftClient.getInstance().world && position.squaredDistanceTo(frame.cameraPos()) <= 48*48
                    && (frustum == null || frustum.isVisible(new Box(position, position).expand(6)))) delegate.render(frame);
        }

        @Override public int drawCount() {
            return delegate.drawCount();
        }
    }
}
