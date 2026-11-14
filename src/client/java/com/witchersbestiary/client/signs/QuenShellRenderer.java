package com.witchersbestiary.client.signs;

import com.witchersbestiary.signs.SignPackets;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public final class QuenShellRenderer {
    private record Shell(int form, long start, long end) {}
    private static final Map<Integer, Shell> SHELLS = new HashMap<>();

    static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(SignPackets.QuenShell.ID, (packet, context) ->
                context.client().execute(() -> accept(packet)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SHELLS.clear());
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE
            .register((client, world) -> SHELLS.clear());
        WorldRenderEvents.AFTER_ENTITIES.register(QuenShellRenderer::render);
    }

    public static void accept(SignPackets.QuenShell packet) {
        var world = MinecraftClient.getInstance().world;
        if (world == null) return;
        if (packet.ticks() == 0) {
            SHELLS.remove(packet.entityId());
            return;
        }
        long now = world.getTime();
        SHELLS.compute(packet.entityId(), (id, shell) ->
                new Shell(packet.form(), shell == null || shell.end() <= now ? now : shell.start(),
                        now + packet.ticks()));
    }

    private static void render(WorldRenderContext context) {
        var client = MinecraftClient.getInstance();
        if (client.world == null || SHELLS.isEmpty()) return;
        long now = client.world.getTime();
        SHELLS.values().removeIf(shell -> shell.end() <= now);
        var camera = context.camera().getPos();
        for (var entry : SHELLS.entrySet()) {
            var entity = client.world.getEntityById(entry.getKey());
            if (entity == null || !entity.isAlive() || entity.squaredDistanceTo(camera) > 40 * 40
                || context.camera().getFocusedEntity() == entity && !context.camera().isThirdPerson()) continue;
            var shell = entry.getValue();
            var pos = entity.getLerpedPos(context.tickCounter().getTickDelta(false)).subtract(camera);
            var matrices = context.matrixStack();
            matrices.push();
            matrices.translate(pos.x, pos.y + entity.getHeight() * .5, pos.z);
            var matrix = matrices.peek().getPositionMatrix();
            var vertices = context.consumers().getBuffer(SignPixels.MATERIAL);
            float fade = Math.min(1, Math.min((now - shell.start() + 1) / 4f, (shell.end() - now) / 3f));
            float radius = (shell.form() == 3 ? 1.16f : .84f) * (.94f + .06f * fade);
            float height = entity.getHeight() * .58f;
            int sides = shell.form() == 1 ? 8 : 12;
            double age = now - shell.start() + context.tickCounter().getTickDelta(false);
            int tint = switch (shell.form()) {
                case 1 -> 0xffe891;
                case 2 -> 0xffa431;
                case 3 -> 0xffde72;
                default -> 0xffca55;
            };
            for (int band = 0; band < 4; band++) {
                double low = -Math.PI / 2 + band * Math.PI / 4;
                double high = low + Math.PI / 4;
                for (int side = 0; side < sides; side++) {
                    double left = (side + age * .024) * Math.PI * 2 / sides;
                    double right = (side + 1 + age * .024) * Math.PI * 2 / sides;
                    int alpha = (int)((145 + 65 * Math.sin(age * .20 - band + side * .7)) * fade);
                    SignPixels.tile(vertices, matrix, 2, tint, alpha,
                            point(radius, height, left, high), point(radius, height, right, high),
                            point(radius, height, right, low), point(radius, height, left, low));
                }
            }
            matrices.pop();
        }
    }

    private static Vec3d point(float radius, float height, double angle, double latitude) {
        double ring = radius * Math.cos(latitude);
        return new Vec3d(ring * Math.cos(angle), height * Math.sin(latitude), ring * Math.sin(angle));
    }
}
