package com.witchersbestiary.client.vfx;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

record PixelMesh(VertexConsumer out, Matrix4f matrix) {
    static final Vec3d UP = new Vec3d(0, 1, 0);
    private static final int[][] FACES = {{1,3,7,5},{0,4,6,2},{2,6,7,3},{0,1,5,4},{4,5,7,6},{0,2,3,1}};
    private static final double[] SHADES = {.87, .76, 1, .64, .93, .82};

    static Vec3d side(Vec3d axis) {
        return (Math.abs(axis.y) > .97 ? new Vec3d(1, 0, 0) : UP).crossProduct(axis).normalize();
    }

    void bar(Vec3d from, Vec3d to, double width, double height, int color, int alpha) {
        var axis = to.subtract(from).normalize();
        if (axis.lengthSquared() < .5 || width < .002 || height < .002 || alpha < 6) return;
        var side = side(axis).multiply(width);
        var depth = axis.crossProduct(side.normalize()).multiply(height);
        var corners = new Vec3d[8];
        for (int i = 0; i < 8; i++) corners[i] = ((i & 4) == 0 ? from : to)
                .add((i & 1) == 0 ? side.negate() : side)
                .add((i & 2) == 0 ? depth.negate() : depth);
        for (int i = 0; i < FACES.length; i++) {
            int red = (int)((color >> 16 & 255) * SHADES[i]);
            int green = (int)((color >> 8 & 255) * SHADES[i]);
            int blue = (int)((color & 255) * SHADES[i]);
            for (int index : FACES[i]) {
                var p = corners[index];
                out.vertex(matrix, (float)p.x, (float)p.y, (float)p.z).color(red, green, blue, alpha);
            }
        }
    }

    void cellFace(Vec3d base, Vec3d right, Vec3d up, Vec3d axis, int face, int color) {
        cellFace(base, right, up, axis, face, color, 255);
    }

    void cellFace(Vec3d base, Vec3d right, Vec3d up, Vec3d axis, int face, int color, int alpha) {
        double shade = SHADES[face];
        int red = (int)((color >> 16 & 255) * shade);
        int green = (int)((color >> 8 & 255) * shade);
        int blue = (int)((color & 255) * shade);
        for (int vertex : FACES[face]) {
            int r = vertex & 1, u = vertex >> 1 & 1, f = vertex >> 2 & 1;
            out.vertex(matrix, (float)(base.x + r*right.x + u*up.x + f*axis.x),
                    (float)(base.y + r*right.y + u*up.y + f*axis.y),
                    (float)(base.z + r*right.z + u*up.z + f*axis.z)).color(red, green, blue, alpha);
        }
    }

    void quad(Vec3d a, Vec3d b, Vec3d c, Vec3d d, int color, int alpha) {
        for (var p : new Vec3d[]{a, b, c, d}) {
            out.vertex(matrix, (float)p.x, (float)p.y, (float)p.z)
                    .color(color >> 16 & 255, color >> 8 & 255, color & 255, alpha);
        }
    }

    void pixel(Vec3d p, double size, int color, int alpha) {
        bar(p.add(0, -size, 0), p.add(0, size, 0), size, size, color, alpha);
    }

    void ring(Vec3d center, double radius, double width, int color, int alpha, int segments) {
        for (int i = 0; i < segments; i++) {
            double a = i * 2 * Math.PI / segments;
            double b = (i + .88) * 2 * Math.PI / segments;
            bar(center.add(Math.cos(a)*radius,0,Math.sin(a)*radius),
                    center.add(Math.cos(b)*radius,0,Math.sin(b)*radius), width, .026, color, alpha);
        }
    }

    static double unit(double n) {
        return Math.clamp(n, 0, 1);
    }
}
