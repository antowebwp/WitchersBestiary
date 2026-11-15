package com.witchersbestiary.client.vfx;

import com.witchersbestiary.alchemy.BombVfx;
import net.minecraft.util.math.Vec3d;

final class BombVolumes {
    private static final int[] MOON = {0xf1eaff, 0xc6b3e0, 0xa393ca, 0x8276a9};
    private static final int[] GAS = {0xd4d56a, 0xb4be48, 0x8f9e3a, 0x707e31};
    private static final int[] SMOKE = {0x7f6457, 0x6f5551, 0x514951, 0x403840};
    private static final int[] FIRE = {0xffef9f, 0xffb332, 0xe96a1d, 0xaa3c20};

    private BombVolumes() {}

    static void render(PixelMesh mesh, BombVfx.Cue cue, Vec3d center, double age, double remaining, boolean coarse) {
        double phase = (cue.seed() & 255) * .0245;
        switch (cue.type()) {
            case MOON_DUST -> cloud(mesh, center, age, remaining, phase, MOON, false, coarse);
            case GAS -> cloud(mesh, center, age, remaining, phase, GAS, true, coarse);
            case SAMUM -> samum(mesh, center.add(0,.34,0), age, phase, coarse);
            case IGNITION -> ignition(mesh, center, age, phase, coarse);
            case SUPPRESSION -> suppression(mesh, center, age, remaining);
            default -> { }
        }
    }

    private static void cloud(PixelMesh mesh, Vec3d center, double age, double remaining, double phase,
                              int[] colors, boolean gas, boolean coarse) {
        double grow = Math.sqrt(PixelMesh.unit((age + 1) / (gas ? 20 : 12)));
        double fade = PixelMesh.unit(remaining / (gas ? 18 : 14));
        int total = coarse ? 11 : 21;
        for (int i = 0; i < total; i++) {
            double swirl = phase + i*2.39996 + age*(.021 + (i % 4)*.006);
            double radius = (gas ? 2.22 : 2.04) * grow * Math.sqrt((i+.2)/total);
            double lift = gas ? .43 + .22*Math.sin(age*.11 + i*1.2)
                    : .11 + .66*Math.sin(Math.PI*PixelMesh.unit((age+i*.22)/37)) * (1 - i/(double)total);
            var p = center.add(Math.cos(swirl)*radius, lift, Math.sin(swirl)*radius);
            double size = (gas ? .24 : .18) + .093*Math.sin(i*3.3 + age*.14);
            puff(mesh, p, size * (.61 + grow*.61), colors, i, (int)(fade*(gas ? 147 : 109)), swirl, coarse);
            if (!gas && !coarse) {
                double motes = (age*.14 + i*.61) % 1;
                var mote = p.add(Math.cos(i)*motes*.47, .52*(1-motes), Math.sin(i)*motes*.47);
                mesh.pixel(mote, .024, 0xf7eaff, (int)(212*fade));
            }
        }
        if (gas) mesh.ring(center.add(0,.045,0), 2.35*grow, .025, 0xc8cb56, (int)(112*fade), coarse ? 14 : 28);
    }

    private static void puff(PixelMesh mesh, Vec3d p, double size, int[] colors, int index, int alpha, double angle, boolean coarse) {
        if (!coarse) {
            GrapeshotVolume.puff(mesh, p, size, angle, colors, index, alpha);
            return;
        }
        mesh.bar(p.add(0,-size*.68,0), p.add(0,size*.72,0), size*1.36, size, colors[index%4], alpha);
        mesh.bar(p.add(size*.3,0,-size*.28), p.add(size*.16,size*1.33,-size*.31),
                size*.75, size*.67, colors[(index+1)%4], alpha);

    }

    private static void samum(PixelMesh mesh, Vec3d center, double age, double phase, boolean coarse) {
        double flash = PixelMesh.unit((6.5 - age) / 5);
        mesh.pixel(center, (.24 + age*.045)*flash, 0xfff9d2, 255);
        int rays = coarse ? 8 : 15;
        for (int i = 0; i < rays; i++) {
            double theta = phase + i*2.39996;
            var direction = new Vec3d(Math.cos(theta), (i % 7 - 2)*.29, Math.sin(theta)).normalize();
            double distance = (.20 + age*.24) * (1 + .38*Math.sin(i*4.2));
            var root = center.add(direction.multiply(distance*.34));
            var middle = center.add(direction.multiply(distance));
            var tip = center.add(direction.multiply(distance+.31*flash));
            mesh.bar(root,middle,.062*flash,.046*flash, i%3 == 0 ? 0xffd16a : 0xfff7d4, 255);
            mesh.bar(middle,tip,.027*flash,.024*flash,0xffde8b,255);
            mesh.pixel(tip.add(direction.multiply(.14)), .027*flash, 0xffefa4, 255);
        }
    }

    private static void suppression(PixelMesh mesh, Vec3d center, double age, double remaining) {
        int alpha = (int)(214*PixelMesh.unit(remaining/9));
        double angle = age*.075;
        var right = new Vec3d(Math.cos(angle),0,Math.sin(angle));
        var top = center.add(0,.23,0);
        var bottom = center.add(0,-.19,0);
        var left = center.subtract(right.multiply(.18));
        var edge = center.add(right.multiply(.18));
        mesh.bar(top,left,.028,.028,0xefc9ff,alpha);
        mesh.bar(left,bottom,.028,.028,0xefc9ff,alpha);
        mesh.bar(bottom,edge,.028,.028,0xefc9ff,alpha);
        mesh.bar(edge,top,.028,.028,0xefc9ff,alpha);
        mesh.bar(left.add(0,-.17,0),edge.add(0,.19,0),.031,.031,0xbc8de8,alpha);
    }

    private static void ignition(PixelMesh mesh, Vec3d center, double age, double phase, boolean coarse) {
        int tongues = coarse ? 12 : 23;
        for (int i = 0; i < tongues; i++) {
            double theta = phase + i*2.39996;
            double radial = Math.sqrt((i+.4)/tongues);
            double t = (age - radial*4.4) / (7 + i%3);
            double f = Math.sin(Math.PI*PixelMesh.unit(t));
            var root = center.add(Math.cos(theta)*2.19*radial,-.23,Math.sin(theta)*2.19*radial);
            if (t > 0 && t < 1) {
                double height = (.76 + (i%5)*.24)*Math.sqrt(f);
                var mid = root.add(Math.cos(theta)*.17*f,height*.58,Math.sin(theta)*.17*f);
                var tip = mid.add(Math.cos(theta)*.21*f,height*.44,Math.sin(theta)*.21*f);
                mesh.bar(root,mid,.16*f,.13*f,FIRE[i%3],255);
                mesh.bar(mid,tip,.081*f,.071*f,FIRE[2+i%2],255);
                mesh.pixel(mid.add(0,.31+.51*t,0),.034*f,0xffd05d,255);
            }
            double smoke = (age - 6 - radial*3)/22;
            if (smoke > 0 && smoke < 1 && i%2 == 0) {
                var rise = root.add(Math.cos(theta)*smoke*.42,.54 + smoke*1.7,Math.sin(theta)*smoke*.42);
                puff(mesh,rise,(.15+.31*smoke)*PixelMesh.unit((1-smoke)*4),
                        SMOKE,i,(int)(181*PixelMesh.unit((1-smoke)*3)),theta + smoke,coarse);
            }
        }
        if (age < 12) mesh.ring(center.add(0,-.29,0),.34 + age*.23,.05,0xe89137,
                (int)(156*PixelMesh.unit((12-age)/8)),coarse ? 12 : 24);
    }
}
