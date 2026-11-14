package com.witchersbestiary.client.vfx;

import net.minecraft.client.render.*;

import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Closed, independently advected flame lobes with procedural color and opacity. */
final class FireVfx {
    static final RenderLayer LAYER = RenderLayer.getDebugQuads();
    private static final int RINGS = 7, SIDES = 9;
    private static final Vec3d UP = new Vec3d(0,1,0);

    static void render(VertexConsumer out, Matrix4f matrix, Vec3d origin, Vec3d delta,
                       int form, float arc, double age, double fade) {
        double reach = delta.length();
        if (reach < .04 || fade <= 0) return;
        boolean breath = form == 2;
        double front = reach * (breath ? 1 : Math.min(1,(age+1)/5));
        var axis = delta.normalize();
        var side = axis.crossProduct(UP).normalize();
        if (side.lengthSquared() < .1) side = new Vec3d(1,0,0);
        int rays = breath ? 1 : form == 1 ? 12 : 5;
        for (int ray=0; ray<rays; ray++) {
            double angle = form == 1 ? ray*Math.PI/6 : rays == 1 ? 0 :
                    Math.toRadians(arc)*(2*ray/(double)(rays-1)-1);
            var forward = axis.multiply(Math.cos(angle)).add(side.multiply(Math.sin(angle)));
            var right = forward.crossProduct(UP).normalize();
            if(right.lengthSquared()<.1)right=side;
            var up = right.crossProduct(forward);
            for (int layer=2; layer>=0; layer--) {
                int lobes=breath?5:3;
                for (int i=0;i<lobes;i++) {
                    double seed=ray*3.12+i*2.399+layer*1.37;
                    double flow=(i/(double)lobes + age*(breath?.075:.046)+layer*.046)%1;
                    double start=Math.max(0,flow-.18)*front;
                    double end=Math.min(front,start+front*(.31+.07*Math.sin(seed)));
                    double width=(breath?.32:.43+flow*.18);
                    width*=switch(layer){case 0->.52;case 1->.84;default->1.13;};
                    int alpha=(int)(fade*Math.min(1,(1-flow)*5)*(layer==0?242:layer==1?202:146));
                    mesh(out,matrix,origin,forward,right,up,start,end,width,age,seed,layer,alpha);
                }
            }
        }
    }

    private static void mesh(VertexConsumer out,Matrix4f matrix,Vec3d origin,Vec3d axis,Vec3d right,Vec3d up,
                             double start,double end,double width,double age,double seed,int layer,int alpha) {
        if(end-start<.03||alpha<5)return;

        Vec3d[] previous=null;
        for(int ring=0;ring<=RINGS;ring++) {
            double t=ring/(double)RINGS;
            double swirl=age*.40+seed+t*3.6;
            double shape=Math.pow(Math.sin(Math.PI*t),.68);
            var center=origin.add(axis.multiply(start+(end-start)*t))
                    .add(up.multiply(width*(.38*t*t+.19*Math.sin(swirl))*shape))
                    .add(right.multiply(width*.35*Math.cos(swirl)*shape));
            var current=new Vec3d[SIDES];
            for(int k=0;k<SIDES;k++) {
                double angle=k*Math.PI*2/SIDES;
                double radius=width*shape*(1+.25*Math.sin(3*angle+swirl)+.13*Math.cos(5*angle-swirl));
                current[k]=center.add(right.multiply(radius*Math.cos(angle))).add(up.multiply(radius*Math.sin(angle)));
            }
            if(previous!=null)for(int k=0;k<SIDES;k++) {
                int next=(k+1)%SIDES;
                vertex(out,matrix,previous[k],age+seed,layer,ring-1,k,alpha);
                vertex(out,matrix,current[k],age+seed,layer,ring,k,alpha);
                vertex(out,matrix,current[next],age+seed,layer,ring,k+1,alpha);
                vertex(out,matrix,previous[next],age+seed,layer,ring-1,k+1,alpha);
            }
            previous=current;
        }
    }
    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3d p,
                               double phase, int layer, int ring, int side, int alpha) {
        double t = ring / (double) RINGS;
        double angle = side * Math.PI * 2 / SIDES;
        double heat = .5 + .5 * Math.sin(phase * .9 - t * 8 + 2 * angle);
        double edge = Math.sin(Math.PI * t);
        int green = (int) (switch (layer) {
            case 0 -> 202 + 53 * heat;
            case 1 -> 87 + 108 * heat;
            default -> 25 + 59 * heat;
        });
        int blue = (int) (layer == 0 ? 67 + 174 * heat : 12 + 24 * heat);
        out.vertex(matrix, (float) p.x, (float) p.y, (float) p.z)
                .color(255, green, blue, (int) (alpha * edge * (.45 + .55 * heat)));
    }
}
