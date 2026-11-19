package com.witchersbestiary.equipment.armory;

import com.witchersbestiary.entity.monster.HigherVampireEntity;
import com.witchersbestiary.signs.SignManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class BloodLegacyPulse {
    public static final double RADIUS = 5;
    private static final RegistryKey<DamageType> DAMAGE =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of("witchersbestiary", "blood_wave"));

    private BloodLegacyPulse() {}

    public record Cue(Vec3d center, long seed) implements CustomPayload {
        public static final Id<Cue> ID = new Id<>(Identifier.of("witchersbestiary", "blood_wave_fx"));
        public static final PacketCodec<RegistryByteBuf, Cue> CODEC = PacketCodec.of(
                (cue, buf) -> { buf.writeVec3d(cue.center()); buf.writeLong(cue.seed()); },
                buf -> new Cue(buf.readVec3d(), buf.readLong()));
        @Override public Id<Cue> getId() { return ID; }
    }

    static void initialize() {
        PayloadTypeRegistry.playS2C().register(Cue.ID, Cue.CODEC);
    }

    static void fire(ServerPlayerEntity player) {
        var world = player.getServerWorld();
        Vec3d origin = player.getPos();
        var floor = world.raycast(new RaycastContext(origin.add(0, .12, 0), origin.add(0, -1.4, 0),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        Vec3d center = floor.getType() == HitResult.Type.BLOCK ? floor.getPos() : origin;
        var cue = new Cue(center.add(0, .065, 0), world.random.nextLong());
        for (var viewer : PlayerLookup.around(world, origin, 48)) {
            if (ServerPlayNetworking.canSend(viewer, Cue.ID)) ServerPlayNetworking.send(viewer, cue);
        }
        var block = BlockPos.ofFloored(origin);
        world.playSound(null, block, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, .78f, .82f);
        world.playSound(null, block, SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, .52f, 1.12f);

        var source = new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DAMAGE), player);
        var targets = world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(RADIUS),
                target -> target.squaredDistanceTo(origin) <= RADIUS * RADIUS
                        && SignManager.enemy(player, target) && player.canSee(target));
        for (var target : targets) {
            if (!target.damage(source, target.getMaxHealth() * .02f) || !target.isAlive()) continue;
            boolean heavy = target.getWidth() > 1.5 || target.getMaxHealth() >= 200
                    || target.getType().isIn(SignManager.CONTROL_IMMUNE) || target instanceof HigherVampireEntity;
            var direction = origin.subtract(target.getPos());
            if (direction.horizontalLengthSquared() < 1e-6) direction = Vec3d.fromPolar(0, player.getYaw()).negate();
            target.takeKnockback(heavy ? .18 : .95, direction.x, direction.z);
            target.velocityModified = true;
        }
    }
}
