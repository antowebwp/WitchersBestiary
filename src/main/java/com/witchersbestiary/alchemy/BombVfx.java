package com.witchersbestiary.alchemy;

import dev.chocoboy.cascade.Vfx;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.SpriteId;
import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.Easings;

import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/** Server-authoritative visual cues. The client owns geometry, animation and budgets. */
public final class BombVfx {
    public enum Kind { GRAPESHOT, MOON_DUST, SAMUM, GAS, IGNITION, SUPPRESSION }
    private static final Kind[] TYPES = Kind.values();
    static final DustParticleEffect GAS = new DustParticleEffect(new Vector3f(.53f, .62f, .09f), .01f);

    private BombVfx() {}

    public record Cue(Kind type, Vec3d center, int entityId, int ticks, long seed) implements CustomPayload {
        public static final Id<Cue> ID = new Id<>(Identifier.of("witchersbestiary", "bomb_fx_v2"));
        public static final PacketCodec<RegistryByteBuf, Cue> CODEC = PacketCodec.of((cue, buf) -> {
            buf.writeEnumConstant(cue.type);
            buf.writeVec3d(cue.center);
            buf.writeVarInt(cue.entityId);
            buf.writeVarInt(cue.ticks);
            buf.writeLong(cue.seed);
        }, buf -> new Cue(buf.readEnumConstant(Kind.class), buf.readVec3d(), buf.readVarInt(),
                buf.readVarInt(), buf.readLong()));

        @Override public Id<Cue> getId() { return ID; }
    }

    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(Cue.ID, Cue.CODEC);
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (entity instanceof AreaEffectCloudEntity cloud && DragonGas.isGas(cloud)) {
                int remaining = cloud.getDuration() - cloud.age;
                if (remaining > 0) send(player, new Cue(Kind.GAS, cloud.getPos(), cloud.getId(),
                        remaining, cloud.getUuid().getLeastSignificantBits()));
            } else if (entity instanceof LivingEntity target) {
                int remaining = MoonDust.remaining(target);
                if (remaining > 0) send(player, marker(target, remaining));
            }
        });
    }

    static void impact(ServerWorld world, Vec3d center, int type) {
        broadcast(world, new Cue(TYPES[type], center, -1, 0, world.random.nextLong()));
        if (type == 0) revised(world, center);
        if (type == 0) world.playSound(null, BlockPos.ofFloored(center), SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                SoundCategory.PLAYERS, .7f, 1.3f);
    }

    static void ignition(ServerWorld world, Vec3d center) {
        broadcast(world, new Cue(Kind.IGNITION, center, -1, 0, world.random.nextLong()));
    }

    static void suppression(ServerWorld world, LivingEntity target, int ticks) {
        var cue = marker(target, ticks);
        for (var player : PlayerLookup.tracking(target)) send(player, cue);
        if (target instanceof ServerPlayerEntity player) send(player, cue);
    }

    private static Cue marker(LivingEntity target, int ticks) {
        return new Cue(Kind.SUPPRESSION, target.getPos(), target.getId(), ticks,
                target.getUuid().getLeastSignificantBits());
    }

    private static void broadcast(ServerWorld world, Cue cue) {
        for (var player : PlayerLookup.around(world, cue.center, 48)) send(player, cue);
    }

    private static void revised(ServerWorld world, Vec3d position) {
        var center = position.add(0, .24, 0);
        var wave = Vfx.emitter().shape(ShapeSpec.ring(.46f)).count(23).lifetime(7).cube()
                .blend(BlendMode.ALPHA).speed(.23f).size(.046f, .026f, Easings.LINEAR)
                .gradient(Easings.LINEAR, 0xc6aa82, 0x887264).alpha(.63f, 0, Easings.EASE_OUT_QUAD);
        var shards = Vfx.emitter().shape(ShapeSpec.hemisphere(.18f)).count(13).lifetime(20).shard()
                .spin(3.5f).size(.095f, .051f, Easings.EASE_IN_QUAD).speed(.21f).gravity(0, -.023f, 0)
                .drag(.03f).gradient(Easings.LINEAR, 0xd6c4b3, 0x877e80, 0x524b53)
                .alpha(1, 0, Easings.EASE_IN_QUAD);
        var chips = Vfx.emitter().shape(ShapeSpec.hemisphere(.22f)).count(9).lifetime(17).shard()
                .spin(5).size(.059f, .019f, Easings.LINEAR).speed(.30f).gravity(0, -.026f, 0)
                .gradient(Easings.LINEAR, 0xb5acac, 0x514c58).alpha(1, 0, Easings.EASE_IN_QUAD);
        var sparks = Vfx.emitter().shape(ShapeSpec.hemisphere(.13f)).count(19).lifetime(13)
                .sprite(SpriteId.SPARK).trail(2).stretch(2.2f).speed(.29f).gravity(0, -.019f, 0)
                .size(.043f, .010f, Easings.LINEAR).gradient(Easings.LINEAR, 0xffe899, 0xd46d22)
                .alpha(1, 0, Easings.EASE_OUT_QUAD);
        Vfx.at(world).delay(2).emit(position.add(0, .04, 0), wave)
                .delay(1).emit(center, shards).emit(center, sparks)
                .delay(2).emit(center, chips).play();
    }

    private static void send(ServerPlayerEntity player, Cue cue) {
        if (ServerPlayNetworking.canSend(player, Cue.ID)) ServerPlayNetworking.send(player, cue);
    }
}
