package com.witchersbestiary.combat;

import com.witchersbestiary.registry.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

/** Synchronized, expiring status, scoped to this mod's monsters. No permanent AI disabling. */
public final class Stun {
    private Stun() { }
    public static boolean active(LivingEntity entity) {
        return entity instanceof CategorizedMonster && entity.isAlive() && (entity.getWorld().isClient ? entity instanceof Stunnable s && s.trackedStun() : entity.hasStatusEffect(ModEffects.STUNNED));
    }
    public static void apply(LivingEntity entity, int ticks) {
        if (entity.getWorld().isClient || !(entity instanceof CategorizedMonster) || !entity.isAlive()) return;
        entity.addStatusEffect(new StatusEffectInstance(ModEffects.STUNNED, Math.clamp(ticks, 1, 100), 0, false, false, true));
        if (entity instanceof Stunnable interrupted) {interrupted.cancelCombatForStun();interrupted.setTrackedStun(true);}
        if(entity instanceof software.bernie.geckolib.animatable.GeoEntity geo)geo.stopTriggeredAnim("action",null);
        stop(entity);
    }
    public static void sync(LivingEntity entity) {
        if(!entity.getWorld().isClient && entity instanceof Stunnable s && s.trackedStun()!=active(entity))s.setTrackedStun(active(entity));
    }
    public static void stop(LivingEntity entity) {
        if (entity instanceof MobEntity mob) { mob.getNavigation().stop(); mob.setJumping(false); mob.setForwardSpeed(0); mob.setSidewaysSpeed(0); }
        entity.setVelocity(Vec3d.ZERO);
        entity.velocityModified = true;
    }
}
