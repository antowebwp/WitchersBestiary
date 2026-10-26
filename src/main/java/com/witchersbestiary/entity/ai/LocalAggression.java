package com.witchersbestiary.entity.ai;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import java.util.UUID;

/** A single local broadcast per newly acquired player; recipients are marked before
 * receiving the target, so their next tick cannot relay the broadcast onward. */
public final class LocalAggression {
 private LocalAggression(){}
 public interface Member {UUID sharedGroupTarget();void sharedGroupTarget(UUID target);}
 public static void tick(MobEntity source){
  if(source.getWorld().isClient||source.age%20!=0||source.isAiDisabled()||!source.isAlive())return;
  if(!(source instanceof Member member))return;
  if(source.getTarget()==null){member.sharedGroupTarget(null);return;}
  if(source.getTarget() instanceof PlayerEntity p&&!p.getUuid().equals(member.sharedGroupTarget()))alert(source,p);
 }
 public static void alert(MobEntity source,PlayerEntity player){
  if(source.getWorld().isClient||player.isCreative()||player.isSpectator()||!player.isAlive()||!(source instanceof Member member))return;
  member.sharedGroupTarget(player.getUuid());if(source.isAlive())source.setTarget(player);
  int count=0;
  for(var ally:source.getWorld().getEntitiesByClass(MobEntity.class,source.getBoundingBox().expand(12),e->e!=source&&e.getClass()==source.getClass()&&e.isAlive()&&!e.isAiDisabled())){
   if(++count>4)break;
   if(ally instanceof Member recipient)recipient.sharedGroupTarget(player.getUuid());ally.setTarget(player);
  }
 }
}
