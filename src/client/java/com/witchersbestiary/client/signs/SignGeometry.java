package com.witchersbestiary.client.signs;

import com.witchersbestiary.signs.SignPackets;
import com.witchersbestiary.client.vfx.VfxManager;
import com.witchersbestiary.signs.SignType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import java.util.LinkedHashMap;

public final class SignGeometry {
    private record Key(int owner, int sign, Vec3d center) {}
    private record Effect(SignPackets.Visual message, long start, long end) {}
    private static final LinkedHashMap<Key, Effect> EFFECTS = new LinkedHashMap<>();

    static void initialize() {
        VfxManager.initialize();
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> EFFECTS.clear());
        ClientPlayNetworking.registerGlobalReceiver(SignPackets.Visual.ID, (packet, ctx) ->
                ctx.client().execute(() -> accept(packet)));
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            var world = MinecraftClient.getInstance().world;
            if (world == null || EFFECTS.isEmpty()) return;
            long now = world.getTime();
            EFFECTS.values().removeIf(e -> e.end <= now);
            var camera = context.camera().getPos();


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
                    case YRDEN -> seal(context.consumers().getBuffer(SignPixels.MATERIAL),matrix,origin.add(0,.029,0),p.radius(),p.form(),age,fade);
                    case AXII -> {
                        var target=world.getEntityById(p.owner());
                        if(target==null||!target.isAlive())continue;
                        var head=target.getLerpedPos(context.tickCounter().getTickDelta(false)).subtract(camera)
                                .add(0,target.getStandingEyeHeight()+.37,0);
                        var facing=head.negate().multiply(1,0,1).normalize();
                        var side=new Vec3d(-facing.z,0,facing.x);
                        var pixels = context.consumers().getBuffer(SignPixels.MATERIAL);
                        double size = .29 + .03 * Math.sin(age * .25);
                        var right = side.multiply(size);
                        var up = new Vec3d(0, size, 0);
                        int tint = p.form() >= 2 ? 0x79ff49 : 0xbaff83;
                        SignPixels.tile(pixels, matrix, 3, tint, (int)(250 * fade),
                                head.subtract(right).add(up), head.add(right).add(up),
                                head.add(right).subtract(up), head.subtract(right).subtract(up));
                        for (int i = 0; i <= p.form(); i++) {
                            double phase = age * .14 + i * 1.57;
                            var orbit = head.add(Math.cos(phase) * .43, -.18 + i * .095, Math.sin(phase) * .43);
                            var mark = side.multiply(.095);
                            var rise = new Vec3d(0, .08, 0);
                            SignPixels.tile(pixels, matrix, 3, 0x53ec66, (int)(175 * fade),
                                    orbit.subtract(mark).add(rise), orbit.add(mark).add(rise),
                                    orbit.add(mark).subtract(rise), orbit.subtract(mark).subtract(rise));
                        }
                    }
                    default -> { }
                }
            }
        });
    }

    public static void accept(SignPackets.Visual packet) {
        var world = MinecraftClient.getInstance().world;
        if (world == null || SignType.byIndex(packet.sign()) == null) return;
        if (packet.sign() == SignType.IGNI.ordinal()) {
            VfxManager.igni(packet, world.getTime());
            return;
        }
        if (packet.sign() == SignType.AARD.ordinal()) {
            VfxManager.aard(packet, world.getTime());
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
    }

    private static void seal(VertexConsumer out, Matrix4f matrix, Vec3d origin, double radius,
                             int form, double age, double fade) {
        disc(out, matrix, origin, radius, 0, 0xcf82ff, (int)(245 * fade));
        int tint = switch (form) {
            case 1 -> 0xec9cff;
            case 2 -> 0xb38eff;
            case 3 -> 0xfaabff;
            default -> 0xac64ff;
        };
        double pulse = .5 + .5 * Math.sin(age * .16);
        disc(out, matrix, origin.add(0, .008, 0), radius * (.68 + form * .045),
                age * (form == 3 ? -.05 : .025), tint, (int)((155 + 80 * pulse) * fade));
        disc(out, matrix, origin.add(0, .017, 0), radius * (.31 + .035 * pulse),
                -age * .044, tint, (int)(195 * fade));
    }

    private static void disc(VertexConsumer out, Matrix4f matrix, Vec3d o, double r,
                             double angle, int tint, int alpha) {
        double x = Math.cos(angle) * r, z = Math.sin(angle) * r;
        SignPixels.tile(out, matrix, 1, tint, alpha,
                o.add(-x-z, 0, x-z), o.add(x-z, 0, x+z),
                o.add(x+z, 0, z-x), o.add(z-x, 0, -x-z));
    }
}
