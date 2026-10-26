package com.witchersbestiary.combat;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.world.NaturalSpawns;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Bounded grace for wild monsters; permanent hunt bosses use vanilla PersistenceRequired. */
public final class MonsterDespawnPolicy {
 public static final int SOFT_DISTANCE=64,HARD_DISTANCE=192,COMBAT_GRACE=600;
 private MonsterDespawnPolicy(){}
 public static boolean applies(MobEntity mob){return mob instanceof CategorizedMonster;}
 public static boolean rare(MobEntity mob){var type=mob.getType();return NaturalSpawns.boss(type)||type==ModEntities.GRAVE_HAG||type==ModEntities.KATAKAN||type==ModEntities.WEREWOLF||type==ModEntities.BONECRUSHER;}
 public static int idleTicks(MobEntity mob){return rare(mob)?2400:1200;}
 public static boolean engaged(MobEntity mob){
  var target=mob.getTarget();
  return target!=null&&target.isAlive()&&!target.isRemoved()&&target.getWorld()==mob.getWorld()
   &&(!(target instanceof PlayerEntity p)||!p.isCreative()&&!p.isSpectator())
   &&(mob.squaredDistanceTo(target)<=128*128||!mob.getNavigation().isIdle());
 }
}
