package com.witchersbestiary.entity.ai;

import com.witchersbestiary.entity.monster.GhoulEntity;
import com.witchersbestiary.combat.*;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.sound.*;
import java.util.EnumSet;

/** Bounded, loaded-only ballistic corridor check. No auxiliary path search or chunk tickets. */
public final class GravePounceGoal extends Goal {
 private final GhoulEntity g;private int ticks,nextProbe;private boolean hit;private Vec3d velocity=Vec3d.ZERO,facing=Vec3d.ZERO;
 public GravePounceGoal(GhoulEntity g){this.g=g;setControls(EnumSet.of(Control.MOVE,Control.LOOK,Control.JUMP));}
 public boolean safeTrajectory(){
  var target=g.getTarget();if(!(g.getWorld() instanceof ServerWorld w)||target==null||Math.abs(target.getY()-g.getY())>1)return false;
  var delta=target.getPos().subtract(g.getPos()).multiply(1,0,1);double distance=delta.length();if(distance<3||distance>7)return false;
  facing=delta.normalize();velocity=facing.multiply(distance/12);velocity=new Vec3d(velocity.x,.48,velocity.z);
  double y=0,vertical=.48;
  for(int i=1;i<=14;i++){
   var offset=facing.multiply(Math.min(distance,distance*i/12));y+=vertical;vertical=(vertical-.08)*.98;
   var box=g.getBoundingBox().offset(offset.x,Math.max(.11,y),offset.z).expand(.1);
   for(int x=MathHelper.floor(box.minX)>>4;x<=MathHelper.floor(box.maxX)>>4;x++)for(int z=MathHelper.floor(box.minZ)>>4;z<=MathHelper.floor(box.maxZ)>>4;z++)if(w.getChunkManager().getWorldChunk(x,z)==null)return false;
   if(!w.isSpaceEmpty(g,box))return false;
  }
  var landing=BlockPos.ofFloored(g.getPos().add(delta));return g.canSee(target)&&w.getFluidState(landing).isEmpty()&&w.getBlockState(landing.down()).isSolidBlock(w,landing.down());
 }
 @Override public boolean canStart(){if(g.huntContract==null||g.huntLeap>0||!g.isOnGround()||Stun.active(g)||g.getTarget()==null||!g.getTarget().isAlive()||g.age<nextProbe)return false;nextProbe=g.age+10;return safeTrajectory();}
 @Override public boolean shouldContinue(){return ticks<34&&g.isAlive()&&!Stun.active(g)&&g.getTarget()!=null&&g.getTarget().isAlive();}
 @Override public boolean shouldRunEveryTick(){return true;}
 @Override public void start(){ticks=0;hit=false;g.huntLeap=200;g.getNavigation().stop();if(g.getWorld() instanceof ServerWorld w)w.spawnParticles(net.minecraft.particle.ParticleTypes.ANGRY_VILLAGER,g.getX(),g.getY()+1.4,g.getZ(),5,.3,.2,.3,0);g.getWorld().playSound(null,g.getBlockPos(),SoundEvents.ENTITY_RAVAGER_ATTACK,SoundCategory.HOSTILE,.7f,.7f);}
 @Override public void tick(){
  ++ticks;g.getNavigation().stop();
  if(ticks<14){g.getLookControl().lookAt(g.getTarget(),30,30);g.setVelocity(0,g.getVelocity().y,0);return;}
  if(ticks==14){if(!safeTrajectory()){ticks=34;return;}g.preparePounce();g.setVelocity(velocity);g.velocityModified=true;}
  if(ticks>14){CombatMotion.holdFacing(g,facing);if(g.horizontalCollision){g.setVelocity(0,g.getVelocity().y,0);ticks=34;return;}if(!hit&&CombatMotion.contact(g,g.getTarget(),facing,1.7,.3)){g.tryAttack(g.getTarget());hit=true;}if(ticks>20&&g.isOnGround())ticks=34;}
 }
 @Override public void stop(){g.finishPounce();ticks=34;}
}
