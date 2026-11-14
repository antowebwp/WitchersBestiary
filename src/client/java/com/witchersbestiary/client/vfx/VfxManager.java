package com.witchersbestiary.client.vfx;

import com.witchersbestiary.signs.SignPackets;
import java.util.LinkedHashMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientPlayerEntity;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class VfxManager {
    private static final int MAX_EFFECTS = 12;
    private static final double MAX_DISTANCE_SQUARED = 48 * 48;
    private static final LinkedHashMap<Key, Instance> ACTIVE = new LinkedHashMap<>();

    private static Instance preview;

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
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> dispatcher.register(
                literal("wbarcanefx")
                        .requires(source -> FabricLoader.getInstance().isDevelopmentEnvironment())
                        .then(literal("preview")
                                .executes(context -> startPreview(context.getSource().getPlayer(), 2))
                                .then(literal("igni")
                                        .executes(context -> startPreview(context.getSource().getPlayer(), 0))))
                        .then(literal("stop").executes(context -> {
                            preview = null;
                            return 1;
                        }))));
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            ACTIVE.clear();
            preview = null;
        });
        ClientTickEvents.END_WORLD_TICK.register(world -> {
            long now = world.getTime();
            ACTIVE.values().removeIf(instance -> instance.timeline.end <= now);
            if (preview != null && preview.timeline.end <= now) preview = null;
        });
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            double time = context.world().getTime() + context.tickCounter().getTickDelta(false);
            for (var instance : ACTIVE.values()) render(context, instance, time);
            if (preview != null) render(context, preview, time);
        });
    }

    private static void render(WorldRenderContext context, Instance instance, double time) {
        var transform = instance.transform;
        var camera = context.camera().getPos();
        double opacity = instance.timeline.opacity(time);
        if (opacity <= 0 || camera.squaredDistanceTo(transform.origin) > MAX_DISTANCE_SQUARED) return;
        if (context.frustum() != null && !context.frustum().isVisible(transform.bounds)) return;
        instance.emitter.render(context.consumers().getBuffer(instance.material),
                context.matrixStack().peek().getPositionMatrix(), transform.origin.subtract(camera),
                transform.direction, instance.form, instance.spread, time - instance.timeline.start, opacity);
    }

    private static int startPreview(ClientPlayerEntity player, int form) {
        var forward = player.getRotationVec(1);
        var from = player.getEyePos().add(forward.multiply(1.2)).add(0, -.4, 0);
        var to = from.add(forward.multiply(5));
        long now = player.getWorld().getTime();
        preview = fire(from, to, now, now + (form == 2 ? 200 : 9), form, form == 2 ? .45f : 27f);
        return 1;
    }

    public static void igni(SignPackets.Visual packet, long now) {
        var key = new Key(packet.owner(), packet.form() == 2);
        var previous = ACTIVE.get(key);
        if (previous == null && ACTIVE.size() >= MAX_EFFECTS) ACTIVE.pollFirstEntry();
        long start = key.channelled && previous != null && previous.timeline.end > now
                ? previous.timeline.start : now;
        ACTIVE.put(key, fire(packet.from(), packet.to(), start, now + packet.ticks(), packet.form(), packet.radius()));
    }

    private static Instance fire(Vec3d from, Vec3d to, long start, long end, int form, float spread) {
        var direction = to.subtract(from);
        double padding = form == 2 ? 1 : direction.length() + 1;
        var transform = new Transform(from, direction, new Box(from, to).expand(padding));
        return new Instance(transform, new Timeline(start, end), FireVfx::render, FireVfx.LAYER, form, spread);
    }
}
