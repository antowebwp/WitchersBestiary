package com.witchersbestiary.alchemy;


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

    private static void send(ServerPlayerEntity player, Cue cue) {
        if (ServerPlayNetworking.canSend(player, Cue.ID)) ServerPlayNetworking.send(player, cue);
    }
}
