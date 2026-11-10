package com.witchersbestiary.client.signs;

import com.witchersbestiary.signs.SignPackets;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

final class QuenShellRenderer {
    private record Shell(int form, long start, long end) {}
    private static final Map<Integer, Shell> SHELLS = new HashMap<>();

    static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(SignPackets.QuenShell.ID, (packet, context) ->
            context.client().execute(() -> {
                var world = context.client().world;
                if (world == null) return;
                if (packet.ticks() == 0) {
                    SHELLS.remove(packet.entityId());
                    return;
                }
                long now = world.getTime();
                SHELLS.compute(packet.entityId(), (id, shell) ->
                    new Shell(packet.form(), shell == null || shell.end() <= now ? now : shell.start(), now + packet.ticks()));
            }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SHELLS.clear());
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE
            .register((client, world) -> SHELLS.clear());
        WorldRenderEvents.AFTER_ENTITIES.register(QuenShellRenderer::render);
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
            var vertices = context.consumers().getBuffer(RenderLayer.getDebugQuads());
            float fade = Math.min(1, Math.min((now - shell.start() + 1) / 4f, (shell.end() - now) / 3f));
            float radius = (shell.form() == 3 ? 1.16f : .84f) * (.94f + .06f * fade);
            float height = entity.getHeight() * .58f;
            int sides = entity.squaredDistanceTo(camera) < 16 * 16 ? 12 : 8;
            for (int band = 0; band < 4; band++) {
                double low = -Math.PI / 2 + band * Math.PI / 4;
                double high = low + Math.PI / 4;
                for (int side = 0; side < sides; side++) {
                    double left = side * Math.PI * 2 / sides;
                    double right = (side + 1) * Math.PI * 2 / sides;
                    int alpha = (int)((28 + ((band + side) % 2) * 21) * fade);
                    quad(vertices, matrix, radius, height, left, right, low, high, alpha);
                    quad(vertices, matrix, radius + .006f, height + .006f, left, right, high - .033, high, (int)(140 * fade));
                }
            }
            matrices.pop();
        }
    }

    private static void quad(VertexConsumer out, Matrix4f matrix, float r, float h,
            double left, double right, double low, double high, int alpha) {
        vertex(out, matrix, r, h, left, low, alpha);
        vertex(out, matrix, r, h, left, high, alpha);
        vertex(out, matrix, r, h, right, high, alpha);
        vertex(out, matrix, r, h, right, low, alpha);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, float r, float h, double angle, double latitude, int alpha) {
        double ring = r * Math.cos(latitude);
        out.vertex(matrix, (float)(ring * Math.cos(angle)), (float)(h * Math.sin(latitude)),
            (float)(ring * Math.sin(angle))).color(255, 197, 65, alpha);
    }
}
