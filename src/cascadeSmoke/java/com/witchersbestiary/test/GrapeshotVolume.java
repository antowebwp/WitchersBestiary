package com.witchersbestiary.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Original pixel materials on the exposed faces of small, staggered voxel volumes. */
final class GrapeshotVolume {
    private static final int[] FLASH = {0xfffcea, 0xffed9f, 0xffbc4e, 0xf87727};
    private static final int[] FIRE = {0xffdf76, 0xffa42e, 0xe95e1c, 0xa73b24};
    private static final int[] SMOKE = {0x716867, 0x594e50, 0x493f46, 0x3a3239};
    private static final int[][] NORMALS = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
    private static final int[][][] CORNERS = {
        {{1,0,0},{1,1,0},{1,1,1},{1,0,1}}, {{0,0,1},{0,1,1},{0,1,0},{0,0,0}},
        {{0,1,0},{0,1,1},{1,1,1},{1,1,0}}, {{0,0,1},{0,0,0},{1,0,0},{1,0,1}},
        {{1,0,1},{1,1,1},{0,1,1},{0,0,1}}, {{0,0,0},{0,1,0},{1,1,0},{1,0,0}}
    };
    private static final float[] SHADE = {.86f, .77f, 1, .60f, .93f, .81f};

    private record Face(int x, int y, int z, int side, int material) {}
    private record Lobe(Vec3d axis, Vec3d offset, double start, double end, double reach,
                        double cell, double roll, List<Face> faces, int[] palette) {}

    private final List<Lobe> lobes = new ArrayList<>();

    GrapeshotVolume() {
        var random = new Random(3141);
        add(random, new Vec3d(.82,.44,-.57), Vec3d.ZERO, 0, 3.2, .30, .101, FLASH);
        add(random, new Vec3d(-.46,.84,-.18), Vec3d.ZERO, 0, 2.9, .26, .092, FLASH);
        add(random, new Vec3d(.06,.57,.85), Vec3d.ZERO, .3, 3.7, .33, .079, FLASH);

        add(random, new Vec3d(-.77,.53,.30), new Vec3d(-.13,0,0), 1, 7.5, 1.21, .128, FIRE);
        add(random, new Vec3d(.58,.82,-.20), new Vec3d(.12,.06,0), 1.9, 8.5, 1.45, .122, FIRE);
        add(random, new Vec3d(.58,.38,.74), new Vec3d(.16,0,.14), 2.4, 8, 1.00, .115, FIRE);
        add(random, new Vec3d(-.20,.44,-.91), new Vec3d(-.14,-.04,-.19), 2.9, 9, 1.17, .112, FIRE);
        add(random, new Vec3d(-.30,.92,.16), new Vec3d(-.15,.02,.1), 3.3, 9.8, 1.05, .095, FIRE);

        add(random, new Vec3d(.09,.93,-.14), new Vec3d(0,.16,0), .25, 5.1, .22, .132, FLASH);

        add(random, new Vec3d(-.32,.94,.18), new Vec3d(-.35,.23,.11), 4, 22, 1.04, .183, SMOKE);
        add(random, new Vec3d(.07,.94,-.22), new Vec3d(-.06,.15,-.13), 4.6, 22.6, .96, .225, SMOKE);
        add(random, new Vec3d(.23,.96,-.11), new Vec3d(.22,.40,-.12), 5.5, 24, 1.42, .204, SMOKE);
        add(random, new Vec3d(.84,.45,.42), new Vec3d(.32,.15,.24), 6, 18, .62, .160, SMOKE);
        add(random, new Vec3d(-.67,.53,-.50), new Vec3d(-.29,.12,-.19), 7, 20, .77, .177, SMOKE);
        add(random, new Vec3d(.06,.87,.39), new Vec3d(.04,.27,.07), 8, 25.5, 1.36, .184, SMOKE);
    }

    private void add(Random random, Vec3d axis, Vec3d offset, double start, double end,
                     double reach, double cell, int[] palette) {
        lobes.add(new Lobe(axis.normalize(), offset, start, end, reach, cell,
                random.nextDouble() - .5, buildShape(random, palette == SMOKE), palette));
    }

    private static List<Face> buildShape(Random random, boolean smoke) {
        int sx = 7, sy = smoke ? 7 : 10, sz = 7;
        boolean[][][] filled = new boolean[sx][sy][sz];
        int[][][] materials = new int[sx][sy][sz];
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    double h = (y + .5) / sy;
                    double drift = smoke ? .45 * Math.sin(y) : 1.1 * h * h;
                    double width = smoke ? 2.85 : 2.75 - h * 1.6;
                    double radial = (Math.pow(x - 3 - drift, 2) + Math.pow(z - 3 + h, 2)) / (width * width);
                    double envelope = radial + Math.pow((h - .48) * (smoke ? 1.9 : 1.67), 2);
                    filled[x][y][z] = envelope + random.nextDouble() * .24 < 1;
                    materials[x][y][z] = Math.clamp((int)((radial * .75 + h * .58 + random.nextDouble() * .18) * 3), 0, 3);
                }
            }
        }
        List<Face> faces = new ArrayList<>();
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    if (!filled[x][y][z]) continue;
                    for (int side = 0; side < NORMALS.length; side++) {
                        var n = NORMALS[side];
                        int nx = x + n[0], ny = y + n[1], nz = z + n[2];
                        if (nx < 0 || nx >= sx || ny < 0 || ny >= sy || nz < 0 || nz >= sz || !filled[nx][ny][nz])
                            faces.add(new Face(x - 3, y - sy / 2, z - 3, side, materials[x][y][z]));
                    }
                }
            }
        }
        return List.copyOf(faces);
    }

    void render(WorldRenderContext context, Vec3d center, double age) {
        var matrix = context.matrixStack().peek().getPositionMatrix();
        var vertices = context.consumers().getBuffer(RenderLayer.getDebugQuads());
        for (var lobe : lobes) {
            double t = (age - lobe.start) / (lobe.end - lobe.start);
            if (t < 0 || t >= 1) continue;
            drawLobe(vertices, matrix, center.subtract(context.camera().getPos()), lobe, t);
        }
    }

    private static void drawLobe(VertexConsumer vertices, Matrix4f matrix, Vec3d center, Lobe lobe, double t) {
        boolean smoke = lobe.palette == SMOKE;
        double spread = 1 - (1 - t) * (1 - t);
        double development = Math.min(1, .48 + spread * 1.75);
        double tail = Math.clamp((1 - t) * (smoke ? 2.8 : 2.2), 0, 1);
        double size = lobe.cell * development * (smoke ? .85 + t * .42 : 1) * Math.sqrt(tail);
        var spine = lobe.axis.add(Math.sin(t * 4 + lobe.roll) * .19, smoke ? t * .24 : -.26 * t, 0).normalize();
        var side = spine.crossProduct(new Vec3d(0, 0, 1)).normalize()
                .rotateY((float)(lobe.roll * t * .44));
        var across = side.crossProduct(spine).normalize();
        side = spine.crossProduct(across).normalize();
        var base = center.add(lobe.offset).add(lobe.axis.multiply(lobe.reach * spread));
        double height = size * (smoke ? 1 + .16 * spread : 1 + .66 * spread);
        int alpha = smoke ? (int)(252 * Math.min(1, (1 - t) * 3.3)) : 255;
        for (var face : lobe.faces) {
            int grade = face.material;
            if (!smoke && t > .58) grade = Math.min(3, grade + 1);
            int color = lobe.palette[grade];
            float shade = SHADE[face.side];
            int red = (int)((color >> 16 & 255) * shade), green = (int)((color >> 8 & 255) * shade);
            int blue = (int)((color & 255) * shade);
            for (var corner : CORNERS[face.side]) {
                double x = (face.x + corner[0]) * size;
                double y = (face.y + corner[1]) * height;
                double z = (face.z + corner[2]) * size;
                vertices.vertex(matrix, (float)(base.x + side.x*x + spine.x*y + across.x*z),
                        (float)(base.y + side.y*x + spine.y*y + across.y*z),
                        (float)(base.z + side.z*x + spine.z*y + across.z*z)).color(red, green, blue, alpha);
            }
        }
    }
}
