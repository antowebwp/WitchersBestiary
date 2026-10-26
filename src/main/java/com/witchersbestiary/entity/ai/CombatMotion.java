package com.witchersbestiary.entity.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.*;

/** Server combat geometry and bounded motion. Pathfinding remains responsible for long routes. */
public final class CombatMotion {
 private LivingEntity tracked;
 private Vec3d previous=Vec3d.ZERO, velocity=Vec3d.ZERO;
 private int sampleAge=-1;
 public void sample(MobEntity mob, LivingEntity target) {
  if(target==null){tracked=null;velocity=Vec3d.ZERO;return;}
  if(sampleAge==mob.age)return;
  Vec3d delta=target.getPos().subtract(previous).multiply(1,0,1);
  // Teleports and a change of target are never interpreted as running velocity.
  velocity=tracked==target&&sampleAge==mob.age-1&&delta.lengthSquared()<.36?delta:Vec3d.ZERO;
  tracked=target;previous=target.getPos();sampleAge=mob.age;
 }
 public Vec3d lead(LivingEntity target, double ticks, double cap) {
  Vec3d offset=velocity.multiply(ticks);
  if(offset.lengthSquared()>cap*cap)offset=offset.normalize().multiply(cap);
  return target.getPos().add(offset);
 }
 public boolean retreatingFrom(MobEntity mob) {return tracked!=null&&velocity.dotProduct(tracked.getPos().subtract(mob.getPos()).multiply(1,0,1).normalize())>.045;}
 public static Vec3d face(MobEntity mob, Vec3d point, float maxDegrees) {
  Vec3d d=point.subtract(mob.getPos());
  float desired=(float)(Math.atan2(d.z,d.x)*180/Math.PI)-90;
  float yaw=MathHelper.stepUnwrappedAngleTowards(mob.getYaw(),desired,maxDegrees);
  mob.setYaw(yaw);mob.setBodyYaw(yaw);mob.setHeadYaw(yaw);
  return Vec3d.fromPolar(0,yaw);
 }
 public static double distance(MobEntity mob, LivingEntity target) {
  Box b=target.getBoundingBox();
  return Math.hypot(MathHelper.clamp(mob.getX(),b.minX,b.maxX)-mob.getX(),MathHelper.clamp(mob.getZ(),b.minZ,b.maxZ)-mob.getZ());
 }
 public static boolean contact(MobEntity mob, LivingEntity target, Vec3d facing, double reach, double dot) {
  if(target==null||!target.isAlive()||distance(mob,target)>reach||!mob.canSee(target))return false;
  if(!meleeHeight(mob,target))return false;
  return inSector(mob,target,facing,dot);
 }
 public static boolean inSector(MobEntity mob,LivingEntity target,Vec3d facing,double dot){
  Vec3d delta=target.getPos().subtract(mob.getPos()).multiply(1,0,1);
  return delta.lengthSquared()<.01||delta.normalize().dotProduct(facing)>=dot;
 }
 /** The windup must use the same vertical reach as its eventual contact. */
 public static boolean meleeHeight(MobEntity mob,LivingEntity target){
  return target.getBoundingBox().maxY>=mob.getY()&&target.getY()<=mob.getBoundingBox().maxY+.4;
 }
 /** Validate quarter-block samples, including native step clearance. Movement itself stays vanilla. */
 public static boolean safeStep(MobEntity mob, Vec3d step, boolean support) {
  int count=Math.max(1,(int)Math.ceil(step.length()/.25));double rise=0;
  Box previous=mob.getBoundingBox();var world=mob.getWorld();
  if(!loaded(mob,previous))return false;
  boolean centreAtStart=support&&centreSupported(mob,previous);
  for(int i=1;i<=count;i++) {
   Vec3d offset=step.multiply((double)i/count);Box box=mob.getBoundingBox().offset(offset).offset(0,rise,0);
   if(!loaded(mob,box))return false;
   if(!world.isSpaceEmpty(mob,box)){
    if(!support||!mob.isOnGround()||Math.abs(step.y)>1.0e-6)return false;
    var heights=new java.util.TreeSet<Double>();
    for(var shape:world.getBlockCollisions(mob,box)){
     double height=shape.getMax(Direction.Axis.Y)-mob.getBoundingBox().minY;
     if(height>rise+1.0e-6&&height<=mob.getStepHeight()+1.0e-6)heights.add(height);
    }
    boolean found=false;
    for(double height:heights){
     var raised=mob.getBoundingBox().offset(offset).offset(0,height,0);var upward=previous.stretch(0,height-rise,0);
     if(loaded(mob,raised)&&loaded(mob,upward)&&world.isSpaceEmpty(mob,upward)&&world.isSpaceEmpty(mob,raised)&&supported(mob,raised)){
      box=raised;rise=height;found=true;break;
     }
    }
    if(!found)return false;
   }
   if(support&&(!supported(mob,box)||rise==0&&centreAtStart&&!centreSupported(mob,box)))return false;
   previous=box;
  }
  // No teleport, vertical impulse or forced jump: Entity.move resolves the real step.
  mob.setVelocity(step.x,mob.getVelocity().y,step.z);mob.velocityModified=true;return true;
 }
 private static boolean centreSupported(MobEntity mob,Box box){
  double x=(box.minX+box.maxX)*.5,z=(box.minZ+box.maxZ)*.5;
  return mob.getWorld().getBlockCollisions(mob,new Box(x-.01,box.minY-.05,z-.01,x+.01,box.minY,z+.01)).iterator().hasNext();
 }
 private static boolean supported(MobEntity mob,Box box){
  return mob.getWorld().getBlockCollisions(mob,new Box(box.minX,box.minY-.05,box.minZ,box.maxX,box.minY,box.maxZ)).iterator().hasNext();
 }
 private static boolean loaded(MobEntity mob,Box box){
  var world=mob.getWorld();if(!world.getWorldBorder().contains(box))return false;
  for(int x=MathHelper.floor(box.minX)>>4;x<=MathHelper.floor(box.maxX)>>4;x++)
   for(int z=MathHelper.floor(box.minZ)>>4;z<=MathHelper.floor(box.maxZ)>>4;z++)
    if(!world.isChunkLoaded(new BlockPos(x<<4,MathHelper.floor(box.minY),z<<4)))return false;
  return true;
 }
 /** Navigation must not turn a committed attack after its telegraph. */
 public static void holdFacing(MobEntity mob, Vec3d facing) {
  if(facing.horizontalLengthSquared()<1.0e-8)return;
  float yaw=(float)(Math.atan2(facing.z,facing.x)*180/Math.PI)-90;
  mob.setYaw(yaw);mob.setBodyYaw(yaw);mob.setHeadYaw(yaw);
 }
 public CombatMotion() {}
}
