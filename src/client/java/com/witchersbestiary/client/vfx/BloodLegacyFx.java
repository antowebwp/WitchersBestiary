package com.witchersbestiary.client.vfx;

import com.witchersbestiary.equipment.armory.BloodLegacyPulse;
import java.util.LinkedHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

public final class BloodLegacyFx {
    private static final int WAVE_TICKS = 18;
    private record Wave(Vec3d center, long start) {}
    private static final LinkedHashMap<Long, Wave> WAVES = new LinkedHashMap<>();

    private BloodLegacyFx() {}

    static void clear() { WAVES.clear(); }

    static void accept(ClientWorld world, BloodLegacyPulse.Cue cue) {
        if (world == null || WAVES.containsKey(cue.seed())) return;
        if (WAVES.size() >= 12) WAVES.pollFirstEntry();
        WAVES.put(cue.seed(), new Wave(cue.center(), world.getTime()));
        CascadeBursts.bloodWave(world, cue.center(), cue.seed(), false);
    }

    static void renderWaves(WorldRenderContext context, double time) {
        WAVES.values().removeIf(wave -> time - wave.start() >= WAVE_TICKS);
        if (WAVES.isEmpty()) return;
        var camera = context.camera().getPos();
        var mesh = new PixelMesh(context.consumers().getBuffer(RenderLayer.getDebugQuads()),
                context.matrixStack().peek().getPositionMatrix());
        for (var wave : WAVES.values()) {
            if (wave.center().squaredDistanceTo(camera) > 48 * 48) continue;
            double age = Math.max(0, time - wave.start());
            double progress = Math.min(1, age / 13);
            double radius = .35 + (BloodLegacyPulse.RADIUS - .35) * (1 - Math.pow(1 - progress, 2));
            double fade = 1 - age / WAVE_TICKS;
            var center = wave.center().subtract(camera);
            // A bright moving front, a broader suspended rim and a trailing ground ring.
            mesh.ring(center, radius, .028, 0xff514d, (int)(235 * fade), 52);
            mesh.ring(center.add(0,.11*fade,0), radius * .986, .056, 0xc6294c, (int)(135 * fade), 44);
            mesh.ring(center.add(0,.025,0), radius * .84, .04, 0x871332, (int)(150 * fade), 42);
            for (int i = 0; i < 22; i++) {
                double angle = i * Math.PI * 2 / 22;
                double extent = (.14 + .28 * (.5 + .5 * Math.sin(i * 5.9 + age * .36))) * fade;
                var edge = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                mesh.bar(edge, edge.add(-Math.cos(angle)*extent*.49, extent, -Math.sin(angle)*extent*.49),
                        .02, .021, 0xe02b46, (int)(156 * fade));
            }
        }
    }

    public static void renderBlade(MatrixStack matrices, VertexConsumerProvider buffers, double time) {
        var mesh = new PixelMesh(buffers.getBuffer(RenderLayer.getDebugQuads()), matrices.peek().getPositionMatrix());
        // Coordinates are in the baked Blood Legacy blade space, after the vanilla display transform.
        int glow = 118 + (int)(30 * Math.sin(time * .23));
        for (int edge : new int[]{-1, 1}) {
            var base = new Vec3d(edge * .051, -.34, 0);
            var shoulder = new Vec3d(edge * .051, 1.20, 0);
            var tip = new Vec3d(0, 1.48, 0);
            mesh.bar(base, shoulder, .014, .012, 0xff413f, glow);
            mesh.bar(shoulder, tip, .012, .011, 0xff6256, glow);
        }
        for (int wisp = 0; wisp < 3; wisp++) {
            double phase = time * .095 + wisp * 2.094;
            for (int segment = 0; segment < 13; segment++) {
                double a = segment / 13.0, b = (segment + 1) / 13.0;
                var from = wispPoint(a, phase);
                var to = wispPoint(b, phase);
                double width = .012 + .048 * Math.sin(Math.PI * (a + b) / 2);
                var across = new Vec3d(Math.cos(phase + a*8), 0, Math.sin(phase + a*8)).multiply(width);
                int alpha = (int)(53 * Math.sin(Math.PI * (a + b) / 2));
                feather(mesh, from.subtract(across), to.subtract(across), to, from, 0, alpha);
                feather(mesh, from, to, to.add(across), from.add(across), alpha, 0);
                mesh.bar(from, to, .009, .010, 0xe52b49, alpha + 10);
            }
        }
    }

    private static void feather(PixelMesh mesh, Vec3d a, Vec3d b, Vec3d c, Vec3d d, int edge, int inner) {
        vertex(mesh, a, edge);
        vertex(mesh, b, edge);
        vertex(mesh, c, inner);
        vertex(mesh, d, inner);
    }

    private static void vertex(PixelMesh mesh, Vec3d point, int alpha) {
        mesh.out().vertex(mesh.matrix(), (float)point.x, (float)point.y, (float)point.z)
                .color(158, 18, 47, alpha);
    }

    private static Vec3d wispPoint(double along, double phase) {
        double angle = along * 8 + phase;
        double spread = .072 + .083 * Math.sin(Math.PI * along);
        return new Vec3d(Math.cos(angle)*spread, -.34 + along*1.85, Math.sin(angle)*spread);
    }
}
