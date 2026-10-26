package com.witchersbestiary.entity.ai;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.world.World;
/** Vanilla flying paths, without the loose corner-cutting intended for small birds. */
public final class SpectralNavigation extends BirdNavigation {
 public SpectralNavigation(MobEntity mob,World world){super(mob,world);setCanPathThroughDoors(false);setCanEnterOpenDoors(true);setCanSwim(false);}
 private net.minecraft.util.math.Vec3d waypoint(){
  var point=currentPath.getNodePosition(entity);var below=net.minecraft.util.math.BlockPos.ofFloored(point).down();
  if(!entity.getWorld().getBlockState(below).isSolidBlock(entity.getWorld(),below))return point;
  var hover=point.add(0,.75,0);
  return entity.getWorld().isSpaceEmpty(entity,entity.getBoundingBox().offset(hover.subtract(entity.getPos())))?hover:point;
 }
 @Override public void tick(){super.tick();if(!isIdle()){var at=waypoint();entity.getMoveControl().moveTo(at.x,at.y,at.z,speed);}}
 @Override protected void continueFollowingPath(){
  var at=waypoint();
  if(Math.abs(entity.getX()-at.x)<.12&&Math.abs(entity.getZ()-at.z)<.12&&Math.abs(entity.getY()-at.y)<.25)currentPath.next();
  checkTimeouts(getPos());
 }
}
