package com.witchersbestiary.client.signs;

import com.witchersbestiary.signs.SignPackets;
import com.witchersbestiary.client.vfx.VfxManager;
import com.witchersbestiary.signs.SignType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import java.util.LinkedHashMap;

final class SignGeometry {
    private record Key(int owner, int sign, Vec3d center) {}
    private record Effect(SignPackets.Visual message, long start, long end) {}
    private static final LinkedHashMap<Key, Effect> EFFECTS = new LinkedHashMap<>();

    static void initialize() {
        VfxManager.initialize();
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> EFFECTS.clear());
        ClientPlayNetworking.registerGlobalReceiver(SignPackets.Visual.ID, (packet, ctx) -> ctx.client().execute(() -> {
            var world = ctx.client().world;
            if (world == null || SignType.byIndex(packet.sign()) == null) return;
            if (packet.sign() == SignType.IGNI.ordinal()) {
                VfxManager.igni(packet, world.getTime());
                return;
            }
            var key = new Key(packet.owner(), packet.sign(),
                    packet.sign() == SignType.YRDEN.ordinal() ? packet.from() : Vec3d.ZERO);
            if (!EFFECTS.containsKey(key) && EFFECTS.size() >= 64) EFFECTS.pollFirstEntry();
            long now = world.getTime();
            var previous = EFFECTS.get(key);
            boolean persistent = packet.sign() == SignType.YRDEN.ordinal();
            EFFECTS.put(key, new Effect(packet, persistent && previous != null && previous.end > now
                    ? previous.start : now, now + packet.ticks()));
        }));
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            var world = MinecraftClient.getInstance().world;
            if (world == null || EFFECTS.isEmpty()) return;
            long now = world.getTime();
            EFFECTS.values().removeIf(e -> e.end <= now);
            var camera = context.camera().getPos();
            var out = context.consumers().getBuffer(RenderLayer.getDebugQuads());
            var matrix = context.matrixStack().peek().getPositionMatrix();
            double time = now + context.tickCounter().getTickDelta(false);
            for (var effect : EFFECTS.values()) {
                var p = effect.message;
                if (camera.squaredDistanceTo(p.from()) > 48*48) continue;
                var origin = p.from().subtract(camera);
                var delta = p.to().subtract(p.from());
                double age = time-effect.start;
                double fade = Math.clamp((effect.end-time)/3,0,1);
                switch (SignType.byIndex(p.sign())) {
                    case AARD -> wave(out,matrix,origin,delta,p.form(),p.radius(),age,fade);
                    case YRDEN -> seal(out,matrix,origin.add(0,.029,0),p.radius(),p.form(),age,fade);
                    case AXII -> {
                        var target=world.getEntityById(p.owner());
                        if(target==null||!target.isAlive())continue;
                        var head=target.getLerpedPos(context.tickCounter().getTickDelta(false)).subtract(camera)
                                .add(0,target.getStandingEyeHeight()+.37,0);
                        var facing=camera.subtract(head.add(camera)).multiply(1,0,1).normalize();
                        var side=new Vec3d(-facing.z,0,facing.x);
                        var left=head.add(side.multiply(-.19)).add(0,.31,0);
                        var right=head.add(side.multiply(.19)).add(0,.31,0);
                        var tip=head.add(0,-.02,0);
                        int green=0xafff86;
                        int opacity=(int)(235*fade);
                        sigilStroke(out,matrix,left,right,green,opacity);
                        sigilStroke(out,matrix,right,tip,green,opacity);
                        sigilStroke(out,matrix,tip,left,green,opacity);
                        sigilStroke(out,matrix,head.add(0,.21,0),right.add(0,-.10,0).subtract(side.multiply(.055)),green,opacity);
                        for(int i=0;i<3;i++)
                            ring(out,matrix,head.add(0,-.13-i*.095,0),.40+Math.min(.33,target.getWidth()*.25)-i*.035,.035,23,age*.14+i*.8,
                                    i==0?0x56ff81:0x24d864,(int)((160-i*34)*fade));
                        if(age<5) tube(out,matrix,origin,head.subtract(origin),.017,.035,5,0x46ef83,(int)(95*fade));
                    }
                    default -> { }
                }
            }
        });
    }

    private static void sigilStroke(VertexConsumer out,Matrix4f m,Vec3d a,Vec3d b,int color,int alpha){
        tube(out,m,a,b.subtract(a),.019,.019,5,color,alpha);
    }

    private static void wave(VertexConsumer out,Matrix4f m,Vec3d o,Vec3d delta,int form,float arc,double age,double fade){
        if(delta.lengthSquared()<.001)return;
        double growth=Math.min(1,(age+1)/7);
        if(form==1){ring(out,m,o,delta.length()*growth,.18,36,0,0xe2ecf5,(int)(170*fade));return;}
        var forward=delta.normalize();
        var side=forward.crossProduct(new Vec3d(0,1,0)).normalize();
        if(side.lengthSquared()<.05)side=new Vec3d(1,0,0);
        var up=side.crossProduct(forward);
        for(int layer=0;layer<3;layer++){
            double r=delta.length()*growth*(1-layer*.15);
            for(int i=0;i<16;i++){
                double a=Math.toRadians(arc)*(i/8.0-1), b=Math.toRadians(arc)*((i+1)/8.0-1);
                var left=o.add(forward.multiply(Math.cos(a)*r)).add(side.multiply(Math.sin(a)*r));
                var right=o.add(forward.multiply(Math.cos(b)*r)).add(side.multiply(Math.sin(b)*r));
                double h=(.16+.09*layer)*(form==2?.75:1);
                quad(out,m,left.add(up.multiply(-h)),left.add(up.multiply(h)),right.add(up.multiply(h)),
                        right.add(up.multiply(-h)),0xd3e8f7,(int)((145-layer*35)*fade));
            }
        }
    }

    private static void seal(VertexConsumer out,Matrix4f m,Vec3d o,double r,int form,double age,double fade){
        int glow=(int)((172+28*Math.sin(age*.13))*fade);
        ring(out,m,o,r,.042,52,0,0xcb70ff,glow);
        ring(out,m,o,r*.84,.028,48,0,0x9d51ef,glow);
        ring(out,m,o,r*.57,.025,form==3?6:32,age*.024,0xbe78ff,glow);
        ring(out,m,o,r*.37,r*.32,48,0,0x7020ba,(int)(34*fade));
        int glyphs=form==2?8:12;
        for(int i=0;i<glyphs;i++){
            double angle=i*Math.PI*2/glyphs;
            var radial=new Vec3d(Math.cos(angle),0,Math.sin(angle));
            var tangent=new Vec3d(-radial.z,0,radial.x);
            var center=o.add(radial.multiply(r*.91));
            stroke(out,m,center.add(radial.multiply(-r*.043)),center.add(radial.multiply(r*.041)),.019,glow);
            stroke(out,m,center.add(tangent.multiply(-r*.032)).add(radial.multiply(r*.022)),center,.017,glow);
            if(i%2==0)stroke(out,m,center,center.add(tangent.multiply(r*.029)).add(radial.multiply(r*.036)),.016,glow);
        }
    }

    private static void stroke(VertexConsumer out,Matrix4f m,Vec3d a,Vec3d b,double width,int alpha){
        var d=b.subtract(a);var normal=new Vec3d(-d.z,0,d.x).normalize().multiply(width);
        quad(out,m,a.add(normal),b.add(normal),b.subtract(normal),a.subtract(normal),0xefbaff,alpha);
    }
    private static void ring(VertexConsumer out,Matrix4f m,Vec3d o,double r,double w,int n,double turn,int color,int alpha){
        for(int i=0;i<n;i++){
            double a=turn+i*Math.PI*2/n,b=turn+(i+1)*Math.PI*2/n;
            quad(out,m,point(o,a,r-w),point(o,a,r),point(o,b,r),point(o,b,r-w),color,alpha);
        }
    }
    private static Vec3d point(Vec3d o,double a,double r){return o.add(Math.cos(a)*r,0,Math.sin(a)*r);}
    private static void tube(VertexConsumer out,Matrix4f m,Vec3d o,Vec3d d,double a,double b,int n,int color,int alpha){
        var axis=d.normalize();var side=axis.crossProduct(new Vec3d(0,1,0)).normalize();
        if(side.lengthSquared()<.05)side=new Vec3d(1,0,0);
        var up=side.crossProduct(axis);
        for(int i=0;i<n;i++){
            double x=i*Math.PI*2/n,y=(i+1)*Math.PI*2/n;
            var u=side.multiply(Math.cos(x)).add(up.multiply(Math.sin(x)));
            var v=side.multiply(Math.cos(y)).add(up.multiply(Math.sin(y)));
            quad(out,m,o.add(u.multiply(a)),o.add(d).add(u.multiply(b)),o.add(d).add(v.multiply(b)),o.add(v.multiply(a)),color,alpha);
        }
    }
    private static void quad(VertexConsumer out,Matrix4f m,Vec3d a,Vec3d b,Vec3d c,Vec3d d,int color,int alpha){
        for(var p:new Vec3d[]{a,b,c,d}) out.vertex(m,(float)p.x,(float)p.y,(float)p.z)
                .color(color>>16&255,color>>8&255,color&255,alpha);
    }
}
