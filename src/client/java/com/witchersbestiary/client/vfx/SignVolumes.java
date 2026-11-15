package com.witchersbestiary.client.vfx;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

final class SignVolumes {
    private SignVolumes() {}

    private static final int GRID = 15;
    private static final int SLICES = 37;
    private static final int[][] NEIGHBORS = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
    private static final int[] FIRE = {0, 0xa63817, 0xee5b1c, 0xff931f, 0xffc34a, 0xffed91, 0xfff6c5};

    static void fire(VertexConsumer out, Matrix4f matrix, Vec3d origin, Vec3d delta,
                     int form, float spread, double age, double opacity) {
        var mesh = new PixelMesh(out, matrix);
        double length = delta.length();
        if (length < .12 || opacity < .02) return;
        var direction = delta.normalize();
        var right = PixelMesh.side(direction);
        var up = direction.crossProduct(right);
        boolean breath = form == 2;
        double reach = breath ? length - .045 : length * (.36 + .60 * PixelMesh.unit(age / 4));
        int slices = breath ? SLICES : 27;
        double cell = (breath ? .107 : .16) * Math.sqrt(opacity);
        double phase = age * 1.02;
        var cells = new byte[slices*GRID*GRID];
        for (int z = 0; z < slices; z++) {
            double depth = (z + .5) / slices;
            double growth = Math.sin(Math.PI * (.09 + .87*depth));
            double width = breath ? (.22 + .77*growth) * (.85 + .14*Math.sin(z*.36 - phase))
                    : (.21 + .78 * Math.pow(growth, 1.22)) * (.90 + .13*Math.sin(z*.58 - phase));
            double cX = .20 * Math.sin(z*.42 - phase*.53);
            double cY = .21 * Math.sin(z*.37 - phase*.69) + .19*depth;
            for (int y = 0; y < GRID; y++) {
                for (int x = 0; x < GRID; x++) {
                    double dx = (x - 7 - cX) / (width * (breath ? 3.70 : 5.00));
                    double dy = (y - 5 - cY) / (width * (breath ? 2.72 : 3.20));
                    double radius = dx*dx + dy*dy;
                    double theta = Math.atan2(dy, dx);
                    double flicker = Math.sin(z*.52 - phase + 3*theta);
                    double detail = Math.sin(z*.93 - phase*1.31 - 5*theta);
                    double crest = .44*Math.max(0, dy) * Math.max(0, Math.sin(z*.72 - phase*.96 + theta));
                    if (radius > .94 + .29*flicker + .17*detail + crest) continue;
                    double heat = 1 - .76*radius + .27*flicker + .13*detail;
                    cells[index(x,y,z)] = (byte)Math.clamp((int)(1 + 4.85*heat), 1, 6);
                }
            }
        }
        tongues(cells, slices, breath, age);
        var rs = right.multiply(cell);
        var us = up.multiply(cell);
        var ds = direction.multiply(reach/slices);
        for (int z = 0; z < slices; z++) for (int y = 0; y < GRID; y++) for (int x = 0; x < GRID; x++) {
            int color = cells[index(x,y,z)];
            if (color == 0) continue;
            var base = origin.add(rs.multiply(x-7.5)).add(us.multiply(y-5.5)).add(ds.multiply(z));
            for (int face = 0; face < NEIGHBORS.length; face++) {
                var n = NEIGHBORS[face];
                int nx = x+n[0], ny = y+n[1], nz = z+n[2];
                if (nx < 0 || nx >= GRID || ny < 0 || ny >= GRID || nz < 0 || nz >= slices
                        || cells[index(nx,ny,nz)] == 0)
                    mesh.cellFace(base,rs,us,ds,face,FIRE[color]);
            }
        }
        for (int i = 0; i < (breath ? 9 : 13); i++) {
            double t = (age * (.14 + i*.006) + i*.317) % 1;
            double theta = i * 2.39996;
            double offset = (breath ? .52 : .92) * (.45 + .55*Math.sin(Math.PI*t));
            var at = origin.add(direction.multiply(reach * t))
                    .add(right.multiply(offset*Math.cos(theta))).add(up.multiply(offset*Math.sin(theta)));
            mesh.pixel(at, .025 * opacity, i % 2 == 0 ? 0xffb633 : 0xffef8b, 244);
        }
    }

    private static void tongues(byte[] cells, int slices, boolean breath, double age) {
        int count = breath ? 7 : 8;
        for (int i = 0; i < count; i++) {
            double speed = .094 + .017 * (i % 4);
            double moving = (age * speed + i * .628) % 1;
            double focus = breath ? .09 + .82 * moving : .13 + .72 * ((i*.381 + age*.042) % 1);
            double pulse = Math.sin(Math.PI*moving);
            double height = (3.6 + 5.7*pulse) * Math.sin(Math.PI*(.14+.79*focus));
            double z0 = focus*(slices-1);
            double x0 = 7 + Math.sin(i*2.39996 + age*.27) * (breath ? 2.85 : 4.0);
            double lean = Math.sin(i*2.65 + age*.34) * 2;
            int rise = Math.max(3, (int)Math.ceil(height));
            for (int h = -1; h < rise; h++) {
                int y = 5 + h;
                double fraction = Math.max(0, h)/(double)rise;
                double width = (.26 + 1.7*(1-fraction))*(.68+.32*pulse);
                double depth = .43 + (breath ? 2.4 : 2.0)*(1-fraction);
                double cx = x0 + lean * fraction * fraction;
                double cz = z0 + (1.4 + 4.5*pulse) * Math.pow(fraction, 1.4);
                for (int z = Math.max(0,(int)(cz-depth)); z < Math.min(slices,(int)(cz+depth+1)); z++) {
                    for (int x = Math.max(0,(int)(cx-width)); x < Math.min(GRID,(int)(cx+width+1)); x++) {
                        double nx = (x-cx)/width, nz = (z-cz)/depth;
                        if (nx*nx + nz*nz > 1.14) continue;
                        int heat = (int)Math.clamp(6 - 4.4*fraction - .82*Math.abs(nx), 1, 6);
                        int slot = index(x,y,z);
                        cells[slot] = (byte)Math.max(cells[slot], heat);
                    }
                }
            }
        }
    }

    private static int index(int x, int y, int z) {
        return (z*GRID + y)*GRID + x;
    }

    static void aard(PixelMesh mesh, Vec3d origin, Vec3d delta, int form, float arc, double age, double opacity) {
        boolean circular = form == 1;
        var axis = circular ? new Vec3d(0, 0, 1) : delta.normalize();
        var right = PixelMesh.side(axis);
        var up = axis.crossProduct(right);
        double span = circular ? Math.PI : Math.toRadians(arc);
        double progress = Math.pow(PixelMesh.unit((age + 1) / 8), .76);
        if (form == 3) progress = 1 - .83 * progress;
        int segments = circular ? 27 : 12;
        for (int layer = 0; layer < 3; layer++) {
            double radius = delta.length() * progress * (1 - .15 * layer);
            for (int segment = 0; segment < segments; segment++) {
                double a = (-1 + 2.0 * segment / segments) * span;
                double b = a + 1.82 * span / segments;
                double flow = Math.sin(segment * 1.3 + layer * 1.73 - age * .72);
                double y = .24 * flow + .14 * layer;
                var base = origin.add(up.multiply(y));
                var from = base.add(axis.multiply(Math.cos(a)*radius)).add(right.multiply(Math.sin(a)*radius));
                var to = base.add(axis.multiply(Math.cos(b)*radius)).add(right.multiply(Math.sin(b)*radius));
                mesh.bar(from, to, .057 + .026*flow, .063 + .029*layer,
                        layer == 0 ? 0xe3f7ff : 0x86bce2, (int)((146 - 32*layer) * opacity));
                if (segment % 3 == layer) {
                    var wake = to.subtract(origin).normalize().multiply(.41 + .15 * flow);
                    mesh.bar(from.subtract(wake), from, .038, .055, 0xc3edff, (int)(140 * opacity));
                    mesh.pixel(from.add(0, -.51 + .10*flow, 0), .043, 0xc3bca7, (int)(126 * opacity));
                }
            }
        }
    }
}
