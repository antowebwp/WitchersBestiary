package com.witchersbestiary.client.vfx;

import net.minecraft.util.math.Vec3d;

final class SignRituals {
    private static final double TURN = Math.PI * 2;
    private static final double GOLDEN = 2.39996323;

    private SignRituals() {}

    static void flame(PixelMesh mesh, Vec3d origin, Vec3d direction, int form, double age, double fade) {
        double progress = PixelMesh.unit((age + .7) / 8);
        var axis = direction.multiply(1, 0, 1).normalize();
        var side = PixelMesh.side(axis);
        double reach = direction.length() * (.12 + .78 * Math.sqrt(progress));
        int[] heat = {0xb72d16, 0xf65220, 0xff8b23, 0xffc953, 0xfff6b0};
        int alpha = (int)(250 * fade);
        int bands = form == 1 ? 2 : 3;
        int cells = form == 1 ? 54 : 27;
        for (int band = 0; band < bands; band++) {
            for (int i = 0; i < cells; i++) {
                double u = i / (cells - 1.0);
                double angle = form == 1 ? u * TURN : (u - .5) * 1.22;
                double turbulence = Math.sin(angle * 7 - age * .48 + band * 1.63)
                        + .55 * Math.sin(angle * 13 + age * .27);
                if (turbulence < -.93 && (band == 0 || i % 5 == 0)) continue;
                var ray = form == 1 ? polar(angle, 1)
                        : axis.multiply(Math.cos(angle)).add(side.multiply(Math.sin(angle)));
                double distance = reach * (.72 - .22 * band + .063 * turbulence);
                var base = origin.add(ray.multiply(distance)).add(0, form == 1 ? -.42 : .07, 0);
                double height = (.30 + Math.max(0, turbulence + .15) * .55) * (1 - .48 * progress);
                double width = Math.min(.21, .085 + distance * (form == 1 ? .047 : .031)) * fade;
                for (int j = 0; j < 6; j++) {
                    double f = j / 5.0;
                    var at = base.add(0, height * f, 0).add(ray.multiply(.36 * f * f));
                    double size = width * (1.1 - .82 * f);
                    mesh.pixel(at, size, heat[Math.max(0, 4 - j - band)], alpha);
                }
                if (i % 6 == band) {
                    var tip = base.add(ray.multiply(.34 + progress * .59)).add(0, .22 + height, 0);
                    mesh.pixel(tip, .029 + .018 * fade, heat[3], alpha);
                }
            }
        }
        if (form == 1 && progress < .74) {
            for (int i = 0; i < 19; i++) {
                var spark = polar(i * GOLDEN, progress * (i % 5) * .22);
                mesh.pixel(origin.add(spark).add(0, -.31 + (i % 7) * .11, 0),
                        .105 * (1 - progress), heat[3 + i % 2], alpha);
            }
            mesh.ring(origin.add(0, -.46, 0), reach * .80, .019, heat[2], alpha / 3, 30);
        }
    }

    static void air(PixelMesh mesh, Vec3d origin, Vec3d direction, int form, double age, double fade) {
        double t = PixelMesh.unit((age + .5) / 8);
        var axis = direction.normalize();
        var side = PixelMesh.side(axis);
        var up = axis.crossProduct(side);
        if (form == 3) {
            for (int i = 0; i < 45; i++) {
                double z = (i / 45.0 + 1 - t) % 1;
                double a = i * .57 + age * .82;
                double r = .16 + 1.46 * z;
                var p = origin.add(axis.multiply(z * direction.length()))
                        .add(side.multiply(r * Math.cos(a))).add(up.multiply(r * Math.sin(a) * .65));
                mesh.bar(p, p.add(axis.multiply(-.32)).add(side.multiply(Math.sin(a) * .19))
                        .add(up.multiply(-Math.cos(a) * .18)), .027, .036, i % 4 == 0 ? 0xf3fbff : 0x9adaf3, (int)(215 * fade));
            }
            return;
        }
        int rings = form == 1 ? 3 : 4;
        for (int i = 0; i < rings; i++) {
            double travel = Math.max(.06, t - i * .12);
            double radius = form == 1 ? direction.length() * travel : .25 + .75 * travel;
            var center = form == 1 ? origin.add(0, -.78 + i * .12, 0)
                    : origin.add(axis.multiply(direction.length() * travel));
            for (int j = 0; j < 24; j++) {
                double a = j * TURN / 24;
                double b = a + .19;
                var from = form == 1 ? center.add(polar(a, radius))
                        : center.add(side.multiply(Math.cos(a) * radius)).add(up.multiply(Math.sin(a) * radius * .75));
                var to = form == 1 ? center.add(polar(b, radius))
                        : center.add(side.multiply(Math.cos(b) * radius)).add(up.multiply(Math.sin(b) * radius * .75));
                mesh.bar(from, to, (.046 - i * .007), .027, i == 0 ? 0xdbf8ff : 0x87bedb, (int)((190 - 32 * i) * fade));
                if (j % 4 == 0) mesh.bar(from, from.add(axis.multiply(-.31 - travel * .4)),
                        .026, .028, 0xd7eaf6, (int)(153 * fade));
            }
        }
    }

    static void ward(PixelMesh mesh, Vec3d feet, int form, double radius, double age, double fade) {
        double r = radius * (.43 + .57 * PixelMesh.unit((age + 1) / 7));
        int tint = switch (form) { case 1 -> 0xda7fee; case 2 -> 0xa68cff; case 3 -> 0xfb9cff; default -> 0xbb71ff; };
        int alpha = (int)(218 * fade);
        mesh.ring(feet, r, .033, tint, alpha, 32);
        mesh.ring(feet.add(0, .035, 0), r * .64, .024, 0xdeb3ff, alpha, 16);
        int count = form == 2 ? 3 : 6;
        for (int n = 0; n < count; n++) {
            double a = n * TURN / count;
            var outer = feet.add(polar(a, r * .89));
            var inner = feet.add(polar(a, r * .73));
            for (int side = -1; side <= 1; side += 2) {
                var mark = feet.add(polar(a + side * .14, r * .78));
                mesh.bar(inner, mark.add(0, .065, 0), .027, .028, tint, alpha);
                mesh.bar(mark, outer.add(0, .055, 0), .023, .023, 0xf4c9ff, alpha);
            }
            double rise = .12 + .20 * (.5 + .5 * Math.sin(age * .32 + n));
            mesh.pixel(outer.add(0, rise, 0), .039, 0xe4b0ff, alpha);
            if (form == 1 || form == 3)
                mesh.bar(inner, inner.add(0, rise * (form == 1 ? 1.7 : 2.4), 0), .023, .023, tint, alpha);
        }
        if (form == 3) mesh.ring(feet.add(0, .08, 0), r * (.13 + .43 * (age % 24) / 24),
                .038, 0xf9b6ff, alpha, 18);
    }

    static void shell(PixelMesh mesh, Vec3d feet, int form, double age, double fade, boolean local) {
        double scale = .65 + .35 * PixelMesh.unit((age + 1) / 6);
        int alpha = (int)(151 * fade);
        if (form == 3) {
            mesh.ring(feet.add(0, .08, 0), 1.95 * scale, .034, 0xffce52, alpha, 28);
            mesh.ring(feet.add(0, .19, 0), 1.56 * scale, .023, 0xffe79b, alpha, 24);
        }
        for (int i = 0; i < 8; i++) {
            double a = i * TURN / 8 + age * .017;
            var base = feet.add(polar(a, (form == 3 ? 1.94 : .79) * scale));
            if (form == 3) {
                mesh.bar(base, base.add(0, .38 + .09 * Math.sin(age * .2 + i), 0), .037, .04, 0xffd16c, alpha);
                continue;
            }
            if (local) continue;
            var waist = feet.add(polar(a + .13, .96 * scale)).add(0, .98, 0);
            var apex = feet.add(polar(a + .34, .53 * scale)).add(0, 1.88, 0);
            var nextWaist = feet.add(polar(a + TURN / 8 + .13, .96 * scale)).add(0, .98, 0);
            var nextApex = feet.add(polar(a + TURN / 8 + .34, .53 * scale)).add(0, 1.88, 0);
            mesh.quad(waist, nextWaist, nextApex, apex, 0xeda331, alpha / 6);
            mesh.bar(base.add(0, .28, 0), waist, .035, .026, 0xe8a72f, alpha);
            mesh.bar(waist, apex, form == 2 ? .055 : .033, .023, 0xffdf79, alpha);
            mesh.bar(waist, feet.add(polar(a + .56, .96 * scale)).add(0, 1.15, 0), .026, .023,
                    form == 1 ? 0xffac38 : 0xffe49c, alpha);
            mesh.pixel(apex, .038, 0xffdc71, alpha);
        }
        if (form < 3) mesh.ring(feet.add(0, .13, 0), .91 * scale, .025, 0xf4bd4c, alpha, 20);
    }

    static void reaction(PixelMesh mesh, Vec3d origin, Vec3d delta, int form, boolean quen, double age, double fade) {
        double t = PixelMesh.unit(age / 9);
        int color = quen ? 0xffde77 : 0xe1a2ff;
        int alpha = (int)(238 * fade);
        mesh.ring(origin.add(0, -.4 + t * .4, 0), .22 + t * (quen && form == 1 ? 2.3 : .76), .036, color, alpha, 20);
        for (int i = 0; i < 12; i++) {
            var ray = polar(i * GOLDEN, .25 + t * (quen ? 1.2 : .63));
            var p = origin.add(ray).add(0, Math.sin(i * 3) * (t + .18), 0);
            mesh.bar(p, p.add(ray.multiply(.19)).add(0, .12, 0), .037, .037, color, alpha);
        }
        if (quen && form == 2 && delta.lengthSquared() > .1) {
            for (int i = 0; i < 7; i++) {
                var p = origin.add(delta.multiply(PixelMesh.unit(t + i * .035))).add(0, Math.sin(i * 2.2) * .14, 0);
                mesh.pixel(p, .066 - i * .006, 0xffe393, alpha);
            }
        }
    }

    static void mind(PixelMesh mesh, Vec3d head, Vec3d caster, int form, double age, double fade) {
        int edges = switch (form) { case 0 -> 3; case 1 -> 5; case 2 -> 4; default -> 6; };
        int tint = form >= 2 ? 0x85ef67 : 0xbcf28b;
        int alpha = (int)(230 * fade);
        double pulse = .31 + .045 * Math.sin(age * .28);
        var side = PixelMesh.side(head.multiply(-1, 0, -1).normalize());
        for (int i = 0; i < edges; i++) {
            double a = i * TURN / edges, b = a + TURN / edges;
            var p = head.add(side.multiply(Math.sin(a) * pulse)).add(0, Math.cos(a) * pulse, 0);
            var q = head.add(side.multiply(Math.sin(b) * pulse)).add(0, Math.cos(b) * pulse, 0);
            mesh.bar(p, q, .025, .026, tint, alpha);
            mesh.pixel(p, .035, 0xe8e989, alpha);
            var orbit = head.add(polar(a + age * .17, .47)).add(0, -.17 + .12 * Math.sin(a * 2 + age * .12), 0);
            mesh.pixel(orbit, .044, tint, alpha);
        }
        if (age < 9) for (int i = 0; i < 12; i++) {
            double t = PixelMesh.unit(age / 8 - i * .038);
            var p = caster.lerp(head, t).add(0, Math.sin(t * Math.PI) * .41, 0);
            mesh.pixel(p, .021 + i * .001, tint, alpha * i / 12);
        }
    }

    static void ice(PixelMesh mesh, Vec3d feet, double size, double age, double fade) {
        double growth = PixelMesh.unit((age + 1) / 7);
        int alpha = (int)(247 * fade);
        mesh.ring(feet, .89 * size, .027, 0xb5ecff, alpha, 24);
        for (int i = 0; i < 9; i++) {
            var root = feet.add(polar(GOLDEN * i, size * (.45 + .08 * (i % 5))));
            double h = (.46 + .15 * (i % 5)) * growth;
            for (int n = 0; n < 7; n++) {
                double step = n / 7.0;
                var p = root.add(0, h * step, 0).add(polar(i * GOLDEN, step * .22));
                mesh.pixel(p, (.125 - .096 * step) * (.5 + .5 * growth) * fade,
                        n < 2 ? 0x467db7 : n < 5 ? 0x81cdeb : 0xcfedff, alpha);
            }
            double scatter = (age * .033 + i * .17) % 1;
            mesh.pixel(root.add(0, .13 + scatter * .95, 0).add(polar(i * GOLDEN, scatter * .23)),
                    .029 * fade, 0xc9f4ff, alpha);
        }
    }

    private static Vec3d polar(double angle, double radius) {
        return new Vec3d(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
    }
}
