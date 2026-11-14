package com.witchersbestiary.client.vfx;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

final class FireVfx {
    static final RenderLayer LAYER = RenderLayer.getEntityTranslucentEmissive(
            Identifier.of("witchersbestiary", "textures/effect/igni_pixel_fire.png"));
    private static final Vec3d UP = new Vec3d(0, 1, 0);

    static void render(VertexConsumer out, Matrix4f matrix, Vec3d origin, Vec3d delta,
                       int form, float arc, double age, double fade) {
        double reach = delta.length();
        if (reach < .04 || fade <= 0) return;
        var axis = delta.normalize();
        var side = axis.crossProduct(UP).normalize();
        if (side.lengthSquared() < .1) side = new Vec3d(1, 0, 0);

        if (form == 2) {
            stream(out, matrix, origin, axis, side, reach, age, fade);
            return;
        }

        int rays = form == 1 ? 10 : 7;
        double growth = Math.clamp((age + 1) / 4, .05, 1);
        for (int i = 0; i < rays; i++) {
            double fraction = 2.0 * i / (rays - 1) - 1;
            double angle = form == 1 ? i * Math.PI * 2 / rays : fraction * Math.toRadians(arc);
            var direction = axis.multiply(Math.cos(angle)).add(side.multiply(Math.sin(angle)));
            var right = direction.crossProduct(UP).normalize();
            if (right.lengthSquared() < .1) right = side;
            double length = reach * growth * (.68 + .3 * Math.cos(fraction * 1.45));
            var end = origin.add(direction.multiply(length)).add(0, .15 + .23 * (i % 3), 0);
            var width = right.multiply(.74).add(UP.multiply(i % 2 == 0 ? .73 : -.73));
            ribbon(out, matrix, origin, end, width, age + i * 2, i % 2, fade);
            ribbon(out, matrix, origin, end, width.crossProduct(direction), age + i * 2 + 3, 1 - i % 2, fade);
        }
    }

    private static void stream(VertexConsumer out, Matrix4f matrix, Vec3d origin, Vec3d axis,
                               Vec3d side, double reach, double age, double fade) {
        var up = side.crossProduct(axis);
        var end = origin.add(axis.multiply(reach));
        for (int i = 0; i < 3; i++) {
            double angle = i * Math.PI / 3 + .23;
            var width = up.multiply(Math.cos(angle) * .79).add(side.multiply(Math.sin(angle) * .79));
            ribbon(out, matrix, origin, end, width, age + i * 3, i % 2, fade);
        }
        for (int i = 0; i < 5; i++) {
            double travel = (age * (.065 + i * .003) + i * .213) % 1;
            var start = origin.add(axis.multiply(reach * travel))
                    .add(up.multiply(.21 + travel * .42))
                    .add(side.multiply(Math.sin(i * 2.4) * .30));
            var tip = start.add(axis.multiply(Math.min(reach * (1 - travel), .31 + i * .05)))
                    .add(up.multiply(.08));
            var width = up.add(side.multiply(i % 2 == 0 ? 1 : -1)).multiply(.11);
            ribbon(out, matrix, start, tip, width, age + i * 3, 1, fade * Math.min(1, (1 - travel) * 6));
        }
    }

    private static void ribbon(VertexConsumer out, Matrix4f matrix, Vec3d start, Vec3d end, Vec3d width,
                               double age, int variant, double fade) {
        int frame = Math.floorMod((int) Math.floor(age / 1.5), 8);
        float left = (frame * 64 + .5f) / 512, right = (frame * 64 + 63.5f) / 512;
        float top = (variant * 32 + .5f) / 64, bottom = (variant * 32 + 31.5f) / 64;
        int alpha = (int) (255 * fade);
        var rootWidth = width.multiply(.46);
        vertex(out, matrix, start.add(rootWidth), left, top, alpha);
        vertex(out, matrix, start.subtract(rootWidth), left, bottom, alpha);
        vertex(out, matrix, end.subtract(width), right, bottom, alpha);
        vertex(out, matrix, end.add(width), right, top, alpha);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3d point, float u, float v, int alpha) {
        out.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(255, 255, 255, alpha).texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(0, 1, 0);
    }
}
