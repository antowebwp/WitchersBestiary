package com.witchersbestiary.alchemy;

import com.witchersbestiary.signs.SignManager;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class DragonGas {
    private static final String GAS = "witchersbestiary.dragon_gas";
    private static final float RADIUS = 2.5f;

    private DragonGas() {}

    public static void release(ServerWorld world, ServerPlayerEntity owner, Vec3d center) {
        if (world.getEntitiesByType(EntityType.AREA_EFFECT_CLOUD, c -> c.getCommandTags().contains(GAS)).size() >= 32) return;
        var cloud = new AreaEffectCloudEntity(world, center.x, center.y, center.z);
        cloud.addCommandTag(GAS);
        cloud.setOwner(owner);
        cloud.setDuration(160);
        cloud.setWaitTime(0);
        cloud.setRadius(RADIUS);
        cloud.setParticleType(ParticleTypes.SNEEZE);
        world.spawnEntity(cloud);
    }

    public static int ignite(ServerPlayerEntity player, Vec3d start, Vec3d end, double arc) {
        var world = player.getServerWorld();
        var delta = end.subtract(start);
        double reach = delta.length();
        if (reach < .01) return 0;
        var axis = delta.normalize();
        int ignited = 0;
        for (var cloud : world.getEntitiesByType(EntityType.AREA_EFFECT_CLOUD, new Box(start, start).expand(reach + RADIUS),
                entity -> entity.getCommandTags().contains(GAS))) {
            var center = cloud.getPos().add(0, .35, 0);
            var offset = center.subtract(start);
            if (offset.length() > reach + RADIUS) continue;
            if (arc == 0) {
                double projection = Math.clamp(offset.dotProduct(axis), 0, reach);
                if (offset.subtract(axis.multiply(projection)).lengthSquared() > RADIUS * RADIUS) continue;
            } else if (offset.normalize().dotProduct(axis) < Math.cos(Math.toRadians(arc))) continue;
            if (!visible(world, player, start, center)) continue;
            cloud.discard();
            ignited++;
            world.spawnParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 54, 1.1, .45, 1.1, .05);
            world.spawnParticles(ParticleTypes.LAVA, center.x, center.y, center.z, 8, .7, .28, .7, .04);
            world.playSound(null, cloud.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                    net.minecraft.sound.SoundCategory.PLAYERS, .7f, 1.25f);
            for (var target : world.getEntitiesByClass(LivingEntity.class, new Box(center, center).expand(RADIUS),
                    entity -> SignManager.enemy(player, entity) && entity.squaredDistanceTo(center) <= RADIUS * RADIUS
                            && visible(world, player, center, entity.getEyePos()))) {
                target.damage(world.getDamageSources().playerAttack(player), 6);
                target.setOnFireFor(3);
            }
        }
        return ignited;
    }

    private static boolean visible(ServerWorld world, ServerPlayerEntity player, Vec3d start, Vec3d end) {
        return world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, player)).getType() == HitResult.Type.MISS;
    }
}
