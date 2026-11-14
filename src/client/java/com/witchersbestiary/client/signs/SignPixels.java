package com.witchersbestiary.client.signs;

import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

final class SignPixels {
    static final RenderLayer MATERIAL = RenderLayer.getEntityTranslucentEmissive(
            Identifier.of("witchersbestiary", "textures/effect/sign_pixels.png"));

    static void tile(VertexConsumer out, Matrix4f matrix, int tile, int color, int alpha,
                     Vec3d a, Vec3d b, Vec3d c, Vec3d d) {
        float u = (tile * 32 + .5f) / 128, end = (tile * 32 + 31.5f) / 128;
        vertex(out, matrix, color, alpha, a, u, .015625f);
        vertex(out, matrix, color, alpha, b, end, .015625f);
        vertex(out, matrix, color, alpha, c, end, .984375f);
        vertex(out, matrix, color, alpha, d, u, .984375f);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, int color, int alpha, Vec3d p, float u, float v) {
        out.vertex(matrix, (float) p.x, (float) p.y, (float) p.z)
                .color(color >> 16 & 255, color >> 8 & 255, color & 255, alpha).texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(0, 1, 0);
    }
}
