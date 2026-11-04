package com.witchersbestiary.fist;

import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.*;

/** Neutral territorial fighter. All combat effects are resolved on the server. */
public final class RockTrollEntity extends FistNpc {
 private java.util.UUID alertedTarget;
 private int attackTicks, recovery, lastAttack=-1, attack=-1;
 public RockTrollEntity(EntityType<? extends net.minecraft.entity.mob.PathAwareEntity> type,World world){super(type,world,2);}
 @Override public void configureBout(FistFightSession fight){super.configureBout(fight);fight.player.maxStamina=140;fight.player.stamina=140;fight.opponent.quickContact=8;fight.opponent.heavyContact=18;fight.opponent.quickLength=20;fight.opponent.heavyLength=36;fight.opponent.health=180;fight.opponent.maxStamina=140;fight.opponent.stamina=140;fight.opponent.heavyDamageMultiplier=1.4;}
 @Override public String fightPose(FistFightSession.Action action){return (action==FistFightSession.Action.QUICK||action==FistFightSession.Action.HEAVY?"wild_":"")+super.fightPose(action);}
 @Override public int winReward(){return 90;}
 @Override protected void initGoals(){super.initGoals();targetSelector.add(1,new net.minecraft.entity.ai.goal.RevengeGoal(this));}
 @Override protected ActionResult interactMob(PlayerEntity p,Hand hand){if(getTarget()!=null)return ActionResult.PASS;return super.interactMob(p,hand);}
 @Override public boolean damage(DamageSource source,float amount){if(!getWorld().isClient&&!FistFights.busy(this)&&pose().equals("block")&&source.getAttacker()!=null){var direction=source.getAttacker().getPos().subtract(getPos()).normalize();if(getRotationVec(1).dotProduct(direction)>.1)amount*=.3f;}return super.damage(source,amount);}
 @Override public void tick(){
  super.tick();if(getWorld().isClient)return;
  if(FistFights.busy(this)){attack=-1;attackTicks=0;recovery=30;setTarget(null);return;}
  var target=getTarget();if(target==null||!target.isAlive()||squaredDistanceTo(target)>1024){setTarget(null);alertedTarget=null;if(attack>=0)pose("idle",0);attack=-1;attackTicks=0;return;}
  if(!target.getUuid().equals(alertedTarget)){alertedTarget=target.getUuid();recovery=20;getNavigation().stop();pose("alert",20);}
  getLookControl().lookAt(target,25,25);
  if(recovery>0){recovery--;return;}
  if(attack<0){double d=squaredDistanceTo(target);if(d>144||d>16&&lastAttack==2){getNavigation().startMovingTo(target,.85);return;}int choice=random.nextInt(5);if(choice==lastAttack)choice=(choice+1)%5;attack=d>16?2:choice;lastAttack=attack;attackTicks=0;getNavigation().stop();pose(new String[]{"wild_quick","wild_heavy","throw","stomp","block"}[attack],0);}
  attackTicks++;
  int contact=attack==0?8:attack==1?18:attack==2?20:attack==3?22:30;
  if(attackTicks==contact&&attack!=4){
   if(attack==2){if(canSee(target)){var rock=new net.minecraft.entity.projectile.thrown.SnowballEntity(getWorld(),this){@Override protected void onEntityHit(net.minecraft.util.hit.EntityHitResult hit){super.onEntityHit(hit);if(!getWorld().isClient&&getOwner() instanceof LivingEntity owner)hit.getEntity().damage(getDamageSources().thrown(this,owner),8);}};rock.setItem(new net.minecraft.item.ItemStack(net.minecraft.item.Items.COBBLESTONE));var v=target.getEyePos().subtract(rock.getPos());rock.setVelocity(v.x,v.y+v.horizontalLength()*.15,v.z,1.2f,2);getWorld().spawnEntity(rock);}}
   else if(attack==3){for(var e:getWorld().getEntitiesByClass(LivingEntity.class,getBoundingBox().expand(4,1,4),e->e!=this&&!(e instanceof RockTrollEntity))){if(e.isOnGround()&&canSee(e)){e.damage(getDamageSources().mobAttack(this),8);e.takeKnockback(.8,getX()-e.getX(),getZ()-e.getZ());}}}
   else if(squaredDistanceTo(target)<16&&canSee(target)){target.damage(getDamageSources().mobAttack(this),attack==0?6:14);target.takeKnockback(attack==0?.3:1,getX()-target.getX(),getZ()-target.getZ());}
  }
  if(attackTicks>=contact+16){attack=-1;recovery=20+random.nextInt(20);pose("stance",0);}
 }
}
