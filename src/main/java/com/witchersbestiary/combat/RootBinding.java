package com.witchersbestiary.combat;

import com.witchersbestiary.registry.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.*;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public final class RootBinding {
    private record Hold(Vec3d anchor,long until,long immuneUntil){}
    private static final Map<UUID,Hold> HOLDS=new HashMap<>();
    private RootBinding(){}
    public static boolean bind(LivingEntity entity){return bind(entity,30);}
    public static boolean bind(LivingEntity entity,int duration){
        if(!(entity.getWorld() instanceof ServerWorld world)||!entity.isAlive())return false;
        var old=HOLDS.get(entity.getUuid());if(old!=null&&world.getTime()<old.immuneUntil)return false;
        HOLDS.put(entity.getUuid(),new Hold(entity.getPos(),world.getTime()+duration,world.getTime()+Math.max(100,duration+20)));
        entity.addStatusEffect(new StatusEffectInstance(ModEffects.ROOT_BOUND,duration,0));entity.setVelocity(Vec3d.ZERO);entity.velocityModified=true;
        if(entity instanceof ServerPlayerEntity p)p.networkHandler.requestTeleport(p.getX(),p.getY(),p.getZ(),p.getYaw(),p.getPitch());return true;
    }
    public static boolean active(LivingEntity entity){return entity.isAlive()&&entity.hasStatusEffect(ModEffects.ROOT_BOUND);}
    public static Vec3d anchor(LivingEntity e){var h=HOLDS.get(e.getUuid());return h==null?e.getPos():h.anchor;}
    public static void tick(LivingEntity e){
        if(!(e.getWorld() instanceof ServerWorld world))return;
        var h=HOLDS.get(e.getUuid());if(h==null)return;
        if(!e.isAlive()||world.getTime()>=h.immuneUntil){HOLDS.remove(e.getUuid());e.removeStatusEffect(ModEffects.ROOT_BOUND);return;}
        if(world.getTime()>=h.until){e.removeStatusEffect(ModEffects.ROOT_BOUND);return;}
        if(!active(e))return;
        e.setPosition(h.anchor);e.setVelocity(Vec3d.ZERO);e.velocityModified=true;
        if(e.age%5==0)world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,Blocks.DARK_OAK_LOG.getDefaultState()),e.getX(),e.getY()+.2,e.getZ(),10,.35,.25,.35,0);
    }
    public static void cleanup(long time){HOLDS.entrySet().removeIf(e->time>=e.getValue().immuneUntil);}
    public static void clear(){HOLDS.clear();}
}
