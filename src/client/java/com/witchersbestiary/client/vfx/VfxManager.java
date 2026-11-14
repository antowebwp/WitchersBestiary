package com.witchersbestiary.client.vfx;

import com.witchersbestiary.signs.SignPackets;
import java.util.LinkedHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class VfxManager {
    private static final int MAX_EFFECTS = 12;
    private static final double MAX_DISTANCE_SQUARED = 48 * 48;
    private static final LinkedHashMap<Key, Instance> ACTIVE = new LinkedHashMap<>();

    private record Key(int owner, boolean channelled) {}
    private record Transform(Vec3d origin, Vec3d direction, Box bounds) {}
    private record Timeline(long start, long end) {
        double opacity(double now) {
            return Math.clamp((end - now) / 2, 0, 1);
        }
    }

    private interface Emitter {
        void render(net.minecraft.client.render.VertexConsumer vertices, org.joml.Matrix4f matrix,
                    Vec3d origin, Vec3d direction, int form, float spread, double age, double opacity);
    }

    private record Instance(Transform transform, Timeline timeline, Emitter emitter,
                            net.minecraft.client.render.RenderLayer material, int form, float spread) {}

    public static void initialize() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> ACTIVE.clear());
        ClientTickEvents.END_WORLD_TICK.register(world ->
                ACTIVE.values().removeIf(instance -> instance.timeline.end <= world.getTime()));
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            double time = context.world().getTime() + context.tickCounter().getTickDelta(false);
            var camera = context.camera().getPos();
            var matrix = context.matrixStack().peek().getPositionMatrix();
            for (var instance : ACTIVE.values()) {
                var transform = instance.transform;
                double opacity = instance.timeline.opacity(time);
                if (opacity <= 0 || camera.squaredDistanceTo(transform.origin) > MAX_DISTANCE_SQUARED) continue;
                if (context.frustum() != null && !context.frustum().isVisible(transform.bounds)) continue;
                instance.emitter.render(context.consumers().getBuffer(instance.material), matrix,
                        transform.origin.subtract(camera), transform.direction, instance.form,
                        instance.spread, time - instance.timeline.start, opacity);
            }
        });
    }

    public static void igni(SignPackets.Visual packet, long now) {
        var key = new Key(packet.owner(), packet.form() == 2);
        var previous = ACTIVE.get(key);
        if (previous == null && ACTIVE.size() >= MAX_EFFECTS) ACTIVE.pollFirstEntry();
        long start = key.channelled && previous != null && previous.timeline.end > now
                ? previous.timeline.start : now;
        var direction = packet.to().subtract(packet.from());
        double padding = key.channelled ? 1 : direction.length() + 1;
        var transform = new Transform(packet.from(), direction, new Box(packet.from(), packet.to()).expand(padding));
        ACTIVE.put(key, new Instance(transform, new Timeline(start, now + packet.ticks()),
                FireVfx::render, FireVfx.LAYER, packet.form(), packet.radius()));
    }
}
