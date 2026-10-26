package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.*;
import com.witchersbestiary.config.ModConfig;
import com.witchersbestiary.entity.ai.CombatMotion;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** One persistent hitbox; finite aerial circuit, committed dive, damage-forced grounding. */
public final class GriffinEntity extends HostileEntity implements GeoEntity,CategorizedMonster,Stunnable {
 private static final net.minecraft.entity.data.TrackedData<Boolean> STUN=net.minecraft.entity.data.DataTracker.registerData(GriffinEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
 private static final net.minecraft.entity.data.TrackedData<Boolean> AIR=net.minecraft.entity.data.DataTracker.registerData(GriffinEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
 private static final net.minecraft.entity.data.TrackedData<Boolean> RAGE=net.minecraft.entity.data.DataTracker.registerData(GriffinEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
 private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
 private final CombatMotion motion=new CombatMotion();
 public enum Action { NONE,CLAW_ATTACK,BITE,WING_ATTACK,ROAR,TAKEOFF,CIRCLE,DIVE_WARNING,DIVE,LANDING,STAGGER }
 private Action action=Action.NONE;
 private int actionTick,attackCooldown=20,flightCooldown=180,airTicks,vulnerableTicks,attackChoice;
 private float aerialDamage;
 private boolean contact,forcedLanding;
 private Vec3d direction=Vec3d.ZERO,landingPoint=Vec3d.ZERO,divePoint=Vec3d.ZERO;
 public GriffinEntity(EntityType<? extends HostileEntity> type,World world){super(type,world);experiencePoints=35;}
 @Override protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder b){super.initDataTracker(b);b.add(STUN,false);b.add(AIR,false);b.add(RAGE,false);}
 @Override public boolean trackedStun(){return dataTracker.get(STUN);}
 @Override public void setTrackedStun(boolean v){dataTracker.set(STUN,v);}
 @Override public MonsterCategory getMonsterCategory(){return MonsterCategory.HYBRID;}
 @Override public int getLimitPerChunk(){return 1;}
 // PathAwareEntity also checks this during natural spawning; HostileEntity rejects bright open ground.
 @Override public float getPathfindingFavor(BlockPos pos,WorldView world){return 0;}
 public boolean isFlying(){return dataTracker.get(AIR);}
 public boolean isEnraged(){return dataTracker.get(RAGE);}
 public Action getAction(){return action;}
 public int getVulnerableTicks(){return vulnerableTicks;}
 public float getAerialDamage(){return aerialDamage;}
 public int getAirTicks(){return airTicks;}
 @Override protected void initGoals(){targetSelector.add(1,new RevengeGoal(this));targetSelector.add(2,new ActiveTargetGoal<>(this,PlayerEntity.class,0,true,false,null));goalSelector.add(6,new WanderAroundFarGoal(this,.7){@Override public boolean canStart(){return getTarget()==null&&action==Action.NONE&&!isFlying()&&super.canStart();}});}
 @Override public CombatState getCombatState(){return Stun.active(this)?CombatState.STUNNED:action==Action.NONE?getTarget()==null?CombatState.IDLE:attackCooldown>0?CombatState.ATTACK_RECOVERY:CombatState.CHASE:CombatState.SPECIAL_ABILITY;}
 @Override public void cancelCombatForStun(){if(isFlying())vulnerableTicks=60;dataTracker.set(AIR,false);setNoGravity(false);action=Action.NONE;attackCooldown=20;flightCooldown=Math.max(flightCooldown,300);getNavigation().stop();setVelocity(Vec3d.ZERO);}
 private void start(Action next){action=next;actionTick=0;contact=false;getNavigation().stop();setVelocity(Vec3d.ZERO);String clip=switch(next){case CIRCLE->null;case DIVE_WARNING->"roar";case DIVE->"dive_attack";default->next.name().toLowerCase(java.util.Locale.ROOT);};if(clip!=null)triggerAnim("action",clip);}
 private void finish(){action=Action.NONE;attackCooldown=ModConfig.attackTicks(isEnraged()?7:11);stopTriggeredAnim("action",null);}
 private boolean ready(){return !getWorld().isClient&&isAlive()&&!isAiDisabled()&&!Stun.active(this)&&action==Action.NONE&&getTarget()!=null&&getTarget().isAlive();}
 /** Open wings require room before a takeoff. Every touched chunk must already be loaded. */
 public boolean hasFlightClearance(){
  var box=getBoundingBox().expand(3.1,0,3.1).offset(0,1,0).stretch(0,4,0);var w=getWorld();
  if(!w.isSkyVisible(getBlockPos().up(3))||!w.getWorldBorder().contains(box))return false;
  for(int x=MathHelper.floor(box.minX)>>4;x<=MathHelper.floor(box.maxX)>>4;x++)for(int z=MathHelper.floor(box.minZ)>>4;z<=MathHelper.floor(box.maxZ)>>4;z++)if(!w.isChunkLoaded(new BlockPos(x<<4,getBlockY(),z<<4)))return false;
  return w.isSpaceEmpty(this,box)&&!w.containsFluid(box);
 }
 public boolean beginTakeoff(){if(!ready()||flightCooldown>0||!isOnGround()||!canSee(getTarget())||squaredDistanceTo(getTarget())>400||!hasFlightClearance())return false;landingPoint=getPos();aerialDamage=0;airTicks=0;forcedLanding=false;dataTracker.set(AIR,true);setNoGravity(true);flightCooldown=ModConfig.abilityTicks(300);start(Action.TAKEOFF);playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL,.9f,1.3f);return true;}
 public boolean beginGroundAttack(Action next){if(!ready()||isFlying()||attackCooldown>0||!java.util.List.of(Action.CLAW_ATTACK,Action.BITE,Action.WING_ATTACK).contains(next)||!canSee(getTarget())||(!CombatMotion.meleeHeight(this,getTarget())||CombatMotion.distance(this,getTarget())>2.8))return false;direction=Vec3d.fromPolar(0,getYaw());start(next);return true;}
 private boolean hit(LivingEntity target,float damage,double reach,double sector,double knockback){if(!CombatMotion.contact(this,target,direction,reach,sector))return false;boolean hit=target.damage(getDamageSources().mobAttack(this),damage);if(hit&&knockback>0)target.takeKnockback(knockback,getX()-target.getX(),getZ()-target.getZ());return hit||target.isBlocking();}
 private boolean flyStep(Vec3d step){if(!CombatMotion.safeStep(this,step,false))return false;setVelocity(step);velocityModified=true;return true;}
 private boolean safeLanding(Vec3d pos){var w=getWorld();var box=getBoundingBox().offset(pos.subtract(getPos()));if(!w.getWorldBorder().contains(box))return false;for(int x=MathHelper.floor(box.minX)>>4;x<=MathHelper.floor(box.maxX)>>4;x++)for(int z=MathHelper.floor(box.minZ)>>4;z<=MathHelper.floor(box.maxZ)>>4;z++)if(!w.isChunkLoaded(new BlockPos(x<<4,MathHelper.floor(pos.y),z<<4)))return false;for(BlockPos foot:BlockPos.iterate(BlockPos.ofFloored(pos).add(-1,-1,-1),BlockPos.ofFloored(pos).add(1,-1,1))){if(!w.isChunkLoaded(foot)||!w.getBlockState(foot).isSolidBlock(w,foot))return false;}return w.isSpaceEmpty(this,box)&&!w.containsFluid(box);}
 /** Lure requests reuse the existing collision-checked landing state; combat always takes precedence. */
 public boolean approachLureLanding(Vec3d point){if(getTarget()!=null||!isFlying()||Stun.active(this)||!safeLanding(point))return false;landingPoint=point;beginLanding(false);return true;}
 private Vec3d findLanding(){if(safeLanding(landingPoint))return landingPoint;var w=getWorld();for(int ring=0;ring<=12;ring+=3)for(int i=0;i<(ring==0?1:8);i++){var q=BlockPos.ofFloored(getX()+Math.cos(i*Math.PI/4)*ring,getY(),getZ()+Math.sin(i*Math.PI/4)*ring);if(!w.isChunkLoaded(q))continue;Vec3d p=new Vec3d(q.getX()+.5,w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,q.getX(),q.getZ()),q.getZ()+.5);if(Math.abs(p.y-getY())<20&&safeLanding(p))return p;}return null;}
 private void beginLanding(boolean forced){if(action==Action.LANDING){forcedLanding|=forced;return;}forcedLanding|=forced;Vec3d point=findLanding();if(point==null){touchDown();return;}landingPoint=point;start(Action.LANDING);}
 private void touchDown(){dataTracker.set(AIR,false);setNoGravity(false);setVelocity(Vec3d.ZERO);airTicks=0;flightCooldown=ModConfig.abilityTicks(forcedLanding?360:260);if(forcedLanding){vulnerableTicks=60;start(Action.STAGGER);}else finish();}
 private void aerialTick(LivingEntity target){
  if(++airTicks>180&&action!=Action.LANDING)beginLanding(false);
  if((target==null||!target.isAlive())&&action!=Action.LANDING)beginLanding(false);
  if(!isFlying())return;
  actionTick++;getNavigation().stop();
  switch(action){
   case TAKEOFF->{if(!flyStep(new Vec3d(0,.17,0))){beginLanding(false);break;}if(actionTick>=24)start(Action.CIRCLE);}
   case CIRCLE->{double angle=(age*.045)+(getId()%8)*Math.PI/4;Vec3d centre=target.getPos();if(centre.squaredDistanceTo(landingPoint)>256){beginLanding(false);break;}Vec3d goal=centre.add(Math.cos(angle)*5.5,4.3,Math.sin(angle)*5.5);Vec3d delta=goal.subtract(getPos());Vec3d horizontal=delta.multiply(1,0,1);if(horizontal.lengthSquared()>.28*.28)horizontal=horizontal.normalize().multiply(.28);direction=CombatMotion.face(this,goal,7);if(!flyStep(horizontal.add(0,MathHelper.clamp(delta.y*.12,-.16,.16),0))){beginLanding(false);break;}if(actionTick>=55)start(Action.DIVE_WARNING);}
   case DIVE_WARNING->{setVelocity(Vec3d.ZERO);if(actionTick<=10)direction=CombatMotion.face(this,motion.lead(target,10,1.4),5);if(actionTick==10)divePoint=motion.lead(target,10,1.4).add(0,.3,0);if(actionTick>=20){start(Action.DIVE);direction=divePoint.subtract(getPos()).normalize();}}
   case DIVE->{Vec3d step=direction.multiply(isEnraged()?.9:.75);if(!contact)contact=hit(target,16,2.6,.05,1.2);if(!flyStep(step)||actionTick>=22||getY()<=divePoint.y+.25){beginLanding(false);}}
   case LANDING->{if(actionTick>80){touchDown();break;}Vec3d delta=landingPoint.subtract(getPos());if(delta.horizontalLengthSquared()<1&&getY()<=landingPoint.y+.3&&safeLanding(landingPoint)){touchDown();break;}Vec3d h=delta.multiply(1,0,1);if(h.lengthSquared()>.32*.32)h=h.normalize().multiply(.32);double y=MathHelper.clamp(delta.y*.15,-.23,.18);if(!flyStep(h.add(0,y,0))){setVelocity(Vec3d.ZERO);if(actionTick%10==0){Vec3d next=findLanding();if(next!=null)landingPoint=next;}}}
   default->beginLanding(false);
  }
 }
 @Override public void tick(){super.tick();if(!(getWorld() instanceof ServerWorld)||!isAlive())return;if(attackCooldown>0)attackCooldown--;if(flightCooldown>0)flightCooldown--;if(vulnerableTicks>0)vulnerableTicks--;if(Stun.active(this)){cancelCombatForStun();return;}if(isAiDisabled()){if(action==Action.TAKEOFF)cancelCombatForStun();return;}var target=getTarget();motion.sample(this,target);setAttacking(target!=null&&target.isAlive());
  if(!isEnraged()&&getHealth()<getMaxHealth()*.3f){dataTracker.set(RAGE,true);getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(ModConfig.speed(.37));if(!isFlying()&&action==Action.NONE)start(Action.ROAR);}
  if(isFlying()){aerialTick(target);return;}
  if(action!=Action.NONE){actionTick++;getNavigation().stop();setVelocity(0,getVelocity().y,0);if(action==Action.STAGGER){if(actionTick>=30)finish();return;}if(action==Action.ROAR){if(actionTick>=22)finish();return;}if(target==null||!target.isAlive()){finish();return;}
   int windup=action==Action.CLAW_ATTACK?10:action==Action.BITE?14:18;
   if(actionTick<=windup-4)direction=CombatMotion.face(this,target.getPos(),action==Action.WING_ATTACK?8:14);
   if(action==Action.CLAW_ATTACK&&actionTick<6&&CombatMotion.distance(this,target)>2.2)CombatMotion.safeStep(this,direction.multiply(.14),true);
   if(!contact&&actionTick>=windup&&actionTick<=windup+2)contact=hit(target,action==Action.BITE?13:action==Action.WING_ATTACK?9:11,action==Action.WING_ATTACK?3.2:2.8,action==Action.WING_ATTACK?-.1:.25,action==Action.WING_ATTACK?1.2:.35);
   if(actionTick>=windup+12)finish();return;
  }
  if(target==null||!target.isAlive())return;direction=CombatMotion.face(this,target.getPos(),12);getLookControl().lookAt(target,30,30);
  if(flightCooldown==0&&beginTakeoff())return;
  if(attackCooldown==0&&CombatMotion.distance(this,target)<=2.65&&canSee(target)){Action next=switch(attackChoice++%3){case 0->Action.CLAW_ATTACK;case 1->Action.BITE;default->Action.WING_ATTACK;};if(beginGroundAttack(next))return;}
  if(age%6==0){if(CombatMotion.distance(this,target)>1.8||!CombatMotion.meleeHeight(this,target)||!canSee(target))getNavigation().startMovingTo(target,isEnraged()?1.2:1.1);else getNavigation().stop();}
 }
 public boolean hasTakeoffMeleeProtection(){return !getWorld().isClient&&isAlive()&&!isAiDisabled()&&!Stun.active(this)&&isFlying()&&action==Action.TAKEOFF&&actionTick<24;}
 public static boolean ordinaryMelee(DamageSource source){return source.getAttacker() instanceof LivingEntity&&source.getSource()==source.getAttacker()&&!source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE)&&!source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_EXPLOSION)&&!source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY)&&source.isIn(net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.DAMAGE_TYPE,net.minecraft.util.Identifier.of("witchersbestiary","takeoff_melee")));}
 @Override public void takeKnockback(double strength,double x,double z){if(!hasTakeoffMeleeProtection())super.takeKnockback(strength,x,z);}
 @Override public boolean damage(DamageSource source,float amount){if(hasTakeoffMeleeProtection()&&ordinaryMelee(source))return false;float before=getHealth();boolean hit=super.damage(source,vulnerableTicks>0?amount*1.3f:amount);if(hit&&isAlive()&&!getWorld().isClient){if(isFlying()){aerialDamage+=Math.max(0,before-getHealth());if(aerialDamage>=18)beginLanding(true);}else if(action==Action.NONE)triggerAnim("action","hurt");}return hit;}
 @Override public boolean handleFallDamage(float distance,float multiplier,DamageSource source){return false;}
 @Override public void onDeath(DamageSource source){dataTracker.set(AIR,false);setNoGravity(false);action=Action.NONE;stopTriggeredAnim("action",null);super.onDeath(source);}
 @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putBoolean("GriffinRage",isEnraged());n.putInt("GriffinFlightCooldown",flightCooldown);}
 @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);dataTracker.set(RAGE,n.getBoolean("GriffinRage"));getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(ModConfig.speed(isEnraged()?.37:.32));flightCooldown=n.contains("GriffinFlightCooldown")?Math.clamp(n.getInt("GriffinFlightCooldown"),40,600):180;dataTracker.set(AIR,false);setNoGravity(false);action=Action.NONE;attackCooldown=20;}
 @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
 @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"movement",4,s->s.setAndContinue(!isAlive()?RawAnimation.begin().thenPlayAndHold("animation.griffin.death"):RawAnimation.begin().thenLoop("animation.griffin."+(isFlying()?"fly":s.isMoving()?isAttacking()?"run":"walk":"idle")))));var a=new AnimationController<>(this,"action",0,s->PlayState.STOP);for(String n:new String[]{"claw_attack","bite","wing_attack","roar","takeoff","landing","dive_attack","hurt","stagger"})a.triggerableAnim(n,RawAnimation.begin().thenPlay("animation.griffin."+n));c.add(a);}
 public static DefaultAttributeContainer.Builder createAttributes(){return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,180).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,11).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,ModConfig.speed(.32)).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,.85).add(EntityAttributes.GENERIC_FOLLOW_RANGE,48).add(EntityAttributes.GENERIC_STEP_HEIGHT,1);}
}
