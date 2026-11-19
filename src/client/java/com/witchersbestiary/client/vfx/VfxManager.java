package com.witchersbestiary.client.vfx;

import com.witchersbestiary.signs.SignPackets;
import com.witchersbestiary.signs.SignType;
import com.witchersbestiary.alchemy.BombVfx;
import com.witchersbestiary.equipment.armory.BloodLegacyPulse;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.render.RenderLayer;
import java.util.LinkedHashMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.HitResult;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class VfxManager {
    private static final int MAX_EFFECTS = 12;
    private static final double MAX_DISTANCE_SQUARED = 48 * 48;
    private static final LinkedHashMap<Key, Instance> ACTIVE = new LinkedHashMap<>();

    private static final int MAX_BOMBS = 24;
    private static final boolean LOW_DETAIL = Boolean.getBoolean("witchersbestiary.fxLowDetail");
    private static final GrapeshotVolume GRAPESHOT = new GrapeshotVolume();
    private static final LinkedHashMap<BombKey, BombInstance> BOMBS = new LinkedHashMap<>();

    private record BombKey(BombVfx.Kind type, int entity, long burst) {}
    private record BombInstance(BombVfx.Cue cue, long start, long end) {}
    private static final int MAX_RITUALS = 40;
    private record RitualKey(SignType sign, int entity, Vec3d center, boolean impact) {}
    private record Ritual(SignPackets.Visual packet, long start, long end) {}
    private static final LinkedHashMap<RitualKey, Ritual> RITUALS = new LinkedHashMap<>();

    private static long nextBurst;
    private static Instance preview;

    private record Key(int owner, boolean channelled, boolean aard) {}
    private record Transform(Vec3d origin, Vec3d direction, Box bounds) {}
    private record Timeline(long start, long end) {
        double opacity(double now) {
            return Math.clamp((end - now) / 2, 0, 1);
        }
    }

    private interface Emitter {
        void render(net.minecraft.client.render.VertexConsumer vertices, org.joml.Matrix4f matrix,
                    Vec3d origin, Vec3d direction, int form, float spread, double age, double opacity);
    }

    private record Instance(Transform transform, Timeline timeline, Emitter emitter,
                            net.minecraft.client.render.RenderLayer material, int form, float spread) {}

    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> dispatcher.register(
                literal("wbarcanefx")
                        .requires(source -> FabricLoader.getInstance().isDevelopmentEnvironment())
                        .then(literal("preview")
                                .executes(context -> startPreview(context.getSource().getPlayer(), 2))
                                .then(literal("igni")
                                        .executes(context -> startPreview(context.getSource().getPlayer(), 0))))
                        .then(literal("stop").executes(context -> {
                            preview = null;
                            return 1;
                        }))));
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientTickEvents.END_WORLD_TICK.register(world -> {
            long now = world.getTime();
            CascadeBursts.prune(world);
            var player = MinecraftClient.getInstance().player;
            if (player != null) for (var instance : BOMBS.values()) {
                var cue = instance.cue;
                double distance = player.squaredDistanceTo(cue.center());
                if (cue.type() == BombVfx.Kind.GRAPESHOT && distance <= MAX_DISTANCE_SQUARED)
                    CascadeBursts.tick(world, cue, now - instance.start, LOW_DETAIL || distance > 22*22);
            }
            RITUALS.entrySet().removeIf(entry -> {
                var key = entry.getKey();
                var effect = entry.getValue();
                if (effect.end <= now) return true;
                if (key.center != null) return false;
                var entity = world.getEntityById(key.entity);
                return now - effect.start > 3 && (entity == null || !entity.isAlive());
            });
            ACTIVE.values().removeIf(instance -> instance.timeline.end <= now);
            BOMBS.values().removeIf(fx -> fx.end <= now || fx.cue.entityId() >= 0
                    && now - fx.start > 4 && world.getEntityById(fx.cue.entityId()) == null);
            if (preview != null && preview.timeline.end <= now) preview = null;
        });
        ClientPlayNetworking.registerGlobalReceiver(BombVfx.Cue.ID, (cue, context) -> {
            var world = context.client().world;
            if (world != null) acceptBomb(cue, world.getTime());
        });
        ClientPlayNetworking.registerGlobalReceiver(BloodLegacyPulse.Cue.ID,
                (cue, context) -> BloodLegacyFx.accept(context.client().world, cue));
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (context.matrixStack() == null || context.consumers() == null) return;
            double time = context.world().getTime() + context.tickCounter().getTickDelta(false);
            CascadeBursts.setFrustum(context.frustum());
            for (var entry : ACTIVE.entrySet()) render(context, entry.getValue(), time, entry.getKey());
            if (preview != null) render(context, preview, time, null);
            for (var entry : RITUALS.entrySet()) renderRitual(context, entry.getKey(), entry.getValue(), time);
            for (var fx : BOMBS.values()) renderBomb(context, fx, time);
            BloodLegacyFx.renderWaves(context, time);
        });
    }

    private static void clear() {
        ACTIVE.clear();
        RITUALS.clear();
        BOMBS.clear();
        CascadeBursts.clear();
        BloodLegacyFx.clear();
        preview = null;
    }

    private static void acceptBomb(BombVfx.Cue cue, long now) {
        int duration = switch (cue.type()) {
            case GRAPESHOT -> 26;
            case MOON_DUST -> 48;
            case SAMUM -> 7;
            case GAS -> 160;
            case IGNITION -> 34;
            case SUPPRESSION -> 120;
        };
        if (cue.ticks() > 0) duration = Math.min(duration, cue.ticks());
        var key = new BombKey(cue.type(), cue.entityId(), cue.entityId() < 0 ? nextBurst++ : 0);
        if (!BOMBS.containsKey(key) && BOMBS.size() >= MAX_BOMBS) BOMBS.pollFirstEntry();
        BOMBS.put(key, new BombInstance(cue, now, now + duration));
    }

    private static void renderBomb(WorldRenderContext context, BombInstance fx, double time) {
        var cue = fx.cue;
        var center = cue.center();
        if (cue.entityId() >= 0) {
            var entity = context.world().getEntityById(cue.entityId());
            if (entity == null || entity.isRemoved()) return;
            center = entity.getPos();
            if (cue.type() == BombVfx.Kind.SUPPRESSION) center = center.add(0, entity.getHeight()+.37, 0);
        }
        var camera = context.camera().getPos();
        double distance = camera.squaredDistanceTo(center);
        if (distance > MAX_DISTANCE_SQUARED || fx.end <= time) return;
        if (context.frustum() != null && !context.frustum().isVisible(new Box(center, center).expand(5))) return;
        double age = time - fx.start;
        if (cue.type() == BombVfx.Kind.GRAPESHOT) {
            GRAPESHOT.render(context, center.add(0,.45,0), age);
            return;
        }
        var mesh = new PixelMesh(context.consumers().getBuffer(RenderLayer.getDebugQuads()),
                context.matrixStack().peek().getPositionMatrix());
        BombVolumes.render(mesh, cue, center.subtract(camera), age, fx.end - time, LOW_DETAIL || distance > 22*22);
    }

    public static void ritual(SignPackets.Visual packet, long now) {
        var sign = SignType.byIndex(packet.sign());
        boolean impact = packet.form() >= 4;
        var center = impact || sign == SignType.YRDEN ? packet.from() : null;
        var key = new RitualKey(sign, packet.owner(), center, impact);
        var previous = RITUALS.get(key);
        long start = previous != null && previous.end > now && !impact ? previous.start : now;
        putRitual(key, new Ritual(packet, start, now + packet.ticks()));
    }

    public static void quen(SignPackets.QuenShell packet, long now) {
        var key = new RitualKey(SignType.QUEN, packet.entityId(), null, false);
        if (packet.ticks() == 0) {
            RITUALS.remove(key);
            return;
        }
        ritual(new SignPackets.Visual(packet.entityId(), SignType.QUEN.ordinal(), packet.form(), packet.ticks(),
                Vec3d.ZERO, Vec3d.ZERO, 2), now);
    }

    public static void ice(int entity, int ticks, long now) {
        putRitual(new RitualKey(null, entity, null, false), new Ritual(null, now, now + Math.clamp(ticks, 1, 60)));
    }

    private static void putRitual(RitualKey key, Ritual ritual) {
        if (!RITUALS.containsKey(key) && RITUALS.size() >= MAX_RITUALS) RITUALS.pollFirstEntry();
        RITUALS.put(key, ritual);
    }

    private static void renderRitual(WorldRenderContext context, RitualKey key, Ritual effect, double time) {
        var entity = key.center == null ? context.world().getEntityById(key.entity) : null;
        if (key.center == null && (entity == null || !entity.isAlive())) return;
        var point = entity == null ? key.center : entity.getLerpedPos(context.tickCounter().getTickDelta(false));
        var camera = context.camera().getPos();
        double distance = camera.squaredDistanceTo(point);
        if (time >= effect.end || distance > MAX_DISTANCE_SQUARED) return;
        var packet = effect.packet;
        double radius = packet == null ? 2 : Math.max(1.5, packet.radius());
        if (context.frustum() != null && !context.frustum().isVisible(new Box(point, point).expand(radius + 1))) return;
        var mesh = new PixelMesh(context.consumers().getBuffer(RenderLayer.getDebugQuads()),
                context.matrixStack().peek().getPositionMatrix());
        var origin = point.subtract(camera);
        double age = time - effect.start;
        double fade = Math.clamp((effect.end - time) / (key.sign == null ? 9 : 3), 0, 1);
        if (key.sign == null) {
            SignRituals.ice(mesh, origin.add(0, .04, 0), Math.min(1.6, .35 + entity.getWidth() * .55), age, fade);
        } else if (key.impact) {
            SignRituals.reaction(mesh, origin, packet.to().subtract(packet.from()), packet.form() - 4,
                    key.sign == SignType.QUEN, age, fade);
        } else if (key.sign == SignType.YRDEN) {
            var foot = context.world().raycast(new RaycastContext(point.add(0, .2, 0), point.add(0, -.25, 0),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, context.camera().getFocusedEntity()));
            if (foot.getType() != HitResult.Type.MISS)
                SignRituals.ward(mesh, foot.getPos().subtract(camera).add(0, .045, 0), packet.form(), packet.radius(), age, fade);
        } else if (key.sign == SignType.QUEN) {
            SignRituals.shell(mesh, origin, packet.form(), age, fade,
                    context.camera().getFocusedEntity() == entity && !context.camera().isThirdPerson());
        } else if (key.sign == SignType.AXII) {
            var head = origin.add(0, entity.getStandingEyeHeight() + .41, 0);
            SignRituals.mind(mesh, head, packet.from().subtract(camera), packet.form(), age, fade);
        }
    }

    public static void aard(SignPackets.Visual packet, long now) {
        var key = new Key(packet.owner(), false, true);
        if (!ACTIVE.containsKey(key) && ACTIVE.size() >= MAX_EFFECTS) ACTIVE.pollFirstEntry();
        var delta = packet.to().subtract(packet.from());
        var origin = packet.from();
        var bounds = new Box(origin, origin).expand(delta.length() + 1);
        ACTIVE.put(key, new Instance(new Transform(origin, delta, bounds),
                new Timeline(now, now + packet.ticks()),
                (out, matrix, center, direction, form, spread, age, opacity) ->
                        {
                            var mesh = new PixelMesh(out, matrix);
                            if (form == 0) SignVolumes.aard(mesh, center, direction, form, spread, age, opacity);
                            else SignRituals.air(mesh, center, direction, form, age, opacity);
                        },
                RenderLayer.getDebugQuads(), packet.form(), packet.radius()));
    }

    public record Counts(int signs, int bombs, int cascadeEmitters, int cascadeParticles) {}

    public static Counts counts() {
        return new Counts(ACTIVE.size() + RITUALS.size(), BOMBS.size(), CascadeBursts.emitters(), CascadeBursts.particles());
    }

    private static Transform localBreath(WorldRenderContext context, Transform packet, Key key) {
        var player = MinecraftClient.getInstance().player;
        if (key == null || !key.channelled || player == null || key.owner != player.getId()) return packet;
        float delta = context.tickCounter().getTickDelta(false);
        var eye = player.getCameraPosVec(delta).add(0, -.12, 0);
        var aim = player.getRotationVec(delta);
        double reach = packet.direction.length();
        var requested = eye.add(aim.multiply(reach + .35));
        var hit = context.world().raycast(new RaycastContext(eye, requested, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, player));
        double distance = eye.distanceTo(hit.getPos());
        if (hit.getType() != HitResult.Type.MISS) {
            double normal = Math.abs(aim.dotProduct(Vec3d.of(hit.getSide().getVector())));
            distance = Math.max(0, distance - Math.min(.38, .28 * Math.sqrt(Math.max(0, 1 - normal*normal)) / Math.max(.05, normal)));
        }
        var from = eye.add(aim.multiply(Math.min(.35, distance)));
        var to = eye.add(aim.multiply(distance));
        return new Transform(from, to.subtract(from), new Box(from, to).expand(1));
    }

    private static void render(WorldRenderContext context, Instance instance, double time, Key key) {
        var transform = localBreath(context, instance.transform, key);
        var camera = context.camera().getPos();
        double opacity = instance.timeline.opacity(time);
        if (opacity <= 0 || camera.squaredDistanceTo(transform.origin) > MAX_DISTANCE_SQUARED) return;
        if (context.frustum() != null && !context.frustum().isVisible(transform.bounds)) return;
        instance.emitter.render(context.consumers().getBuffer(instance.material),
                context.matrixStack().peek().getPositionMatrix(), transform.origin.subtract(camera),
                transform.direction, instance.form, instance.spread, time - instance.timeline.start, opacity);
    }

    private static int startPreview(ClientPlayerEntity player, int form) {
        var forward = player.getRotationVec(1);
        var from = player.getEyePos().add(forward.multiply(1.2)).add(0, -.4, 0);
        var to = from.add(forward.multiply(5));
        long now = player.getWorld().getTime();
        preview = fire(from, to, now, now + (form == 2 ? 200 : 9), form, form == 2 ? .45f : 27f);
        return 1;
    }

    public static void igni(SignPackets.Visual packet, long now) {
        var key = new Key(packet.owner(), packet.form() == 2, false);
        var previous = ACTIVE.get(key);
        if (previous == null && ACTIVE.size() >= MAX_EFFECTS) ACTIVE.pollFirstEntry();
        long start = key.channelled && previous != null && previous.timeline.end > now
                ? previous.timeline.start : now;
        ACTIVE.put(key, fire(packet.from(), packet.to(), start, now + packet.ticks(), packet.form(), packet.radius()));
        var client = MinecraftClient.getInstance();
        if (packet.form() == 1 && client.player != null && client.world != null) {
            double distance = client.player.squaredDistanceTo(packet.from());
            if (distance <= MAX_DISTANCE_SQUARED)
                CascadeBursts.fireburst(client.world, packet.from().add(0, -.32, 0),
                        now ^ packet.owner(), LOW_DETAIL || distance > 22 * 22);
        }
    }

    private static Instance fire(Vec3d from, Vec3d to, long start, long end, int form, float spread) {
        var direction = to.subtract(from);
        double padding = form == 2 ? 1 : direction.length() + 1;
        var transform = new Transform(from, direction, new Box(from, to).expand(padding));
        if (form == 1 || form == 3)
            return new Instance(transform, new Timeline(start, end), (out, matrix, center, delta, shape, ignored, age, fade) ->
                            SignRituals.flame(new PixelMesh(out, matrix), center, delta, shape, age, fade),
                    RenderLayer.getDebugQuads(), form, spread);
        return new Instance(transform, new Timeline(start, end), SignVolumes::fire, RenderLayer.getDebugQuads(), form, spread);
    }
}
