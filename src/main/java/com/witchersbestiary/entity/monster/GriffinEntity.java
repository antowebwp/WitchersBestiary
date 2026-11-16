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
 private static final net.minecraft.entity.data.TrackedData<Boolean> GLIDE=net.minecraft.entity.data.DataTracker.registerData(GriffinEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
 private static final net.minecraft.entity.data.TrackedData<Boolean> RAGE=net.minecraft.entity.data.DataTracker.registerData(GriffinEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
 private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
 private static final double BASE_SCALE=2;
 private final CombatMotion motion=new CombatMotion();
 public enum Action { NONE,CLAW_ATTACK,BITE,WING_ATTACK,WING_RIGHT,WING_SPIN,LUNGE,ROAR,TAKEOFF_PREPARE,TAKEOFF,AIR_CHASE,CIRCLE,DIVE_WARNING,DIVE,LANDING,STAGGER }
 private Action action=Action.NONE;
 private int actionTick,attackCooldown=20,flightCooldown=35,airTicks,vulnerableTicks,attackChoice,lungeCooldown;
 private float aerialDamage;
 private boolean contact,forcedLanding;
 private Vec3d direction=Vec3d.ZERO,landingPoint=Vec3d.ZERO,divePoint=Vec3d.ZERO;
 public GriffinEntity(EntityType<? extends HostileEntity> type,World world){super(type,world);experiencePoints=35;}
 @Override protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder b){super.initDataTracker(b);b.add(STUN,false);b.add(AIR,false);b.add(GLIDE,false);b.add(RAGE,false);}
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
 @Override public void cancelCombatForStun(){if(action!=Action.NONE)stopTriggeredAnim("action",null);if(isFlying())vulnerableTicks=60;dataTracker.set(AIR,false);dataTracker.set(GLIDE,false);setNoGravity(false);action=Action.NONE;attackCooldown=20;flightCooldown=Math.max(flightCooldown,300);getNavigation().stop();setVelocity(Vec3d.ZERO);}
 private void start(Action next){action=next;actionTick=0;contact=false;getNavigation().stop();setVelocity(Vec3d.ZERO);String clip=switch(next){case CIRCLE,AIR_CHASE->null;case DIVE_WARNING->"dive_warning";case DIVE->"dive_attack";case WING_ATTACK->"sweep_left";case WING_RIGHT->"sweep_right";default->next.name().toLowerCase(java.util.Locale.ROOT);};if(clip!=null)triggerAnim("action",clip);}
 private void finish(){action=Action.NONE;attackCooldown=ModConfig.attackTicks(isEnraged()?7:11);stopTriggeredAnim("action",null);}
 private boolean ready(){return !getWorld().isClient&&isAlive()&&!isAiDisabled()&&!Stun.active(this)&&action==Action.NONE&&getTarget()!=null&&getTarget().isAlive();}
 /** Open wings require room before a takeoff. Every touched chunk must already be loaded. */
 public boolean hasFlightClearance(){
  var box=getBoundingBox().expand(getScale()*3.6,0,getScale()*3.6).offset(0,1,0).stretch(0,8.5,0);var w=getWorld();
  if(!w.isSkyVisible(getBlockPos().up(3))||!w.getWorldBorder().contains(box))return false;
  for(int x=MathHelper.floor(box.minX)>>4;x<=MathHelper.floor(box.maxX)>>4;x++)for(int z=MathHelper.floor(box.minZ)>>4;z<=MathHelper.floor(box.maxZ)>>4;z++)if(!w.isChunkLoaded(new BlockPos(x<<4,getBlockY(),z<<4)))return false;
  return w.isSpaceEmpty(this,box)&&!w.containsFluid(box);
 }
 public boolean beginTakeoff(){if(!ready()||flightCooldown>0||!isOnGround()||!canSee(getTarget())||squaredDistanceTo(getTarget())>1600||!hasFlightClearance())return false;landingPoint=getPos();aerialDamage=0;airTicks=0;forcedLanding=false;flightCooldown=ModConfig.abilityTicks(300);start(Action.TAKEOFF_PREPARE);return true;}
 public boolean beginGroundAttack(Action next){if(!ready()||isFlying()||attackCooldown>0||!java.util.List.of(Action.CLAW_ATTACK,Action.BITE,Action.WING_ATTACK,Action.WING_RIGHT,Action.WING_SPIN,Action.LUNGE).contains(next)||!canSee(getTarget())||(!CombatMotion.meleeHeight(this,getTarget())||CombatMotion.distance(this,getTarget())>reach(next==Action.LUNGE?8.5:next==Action.WING_ATTACK||next==Action.WING_RIGHT||next==Action.WING_SPIN?3.9:2.8)))return false;direction=Vec3d.fromPolar(0,getYaw());start(next);return true;}
 private double reach(double base){return base*(1+Math.max(0,getScale()-1)*.65);}
 private boolean hit(LivingEntity target,float damage,double reach,double sector,double knockback){if(!CombatMotion.contact(this,target,direction,reach(reach),sector))return false;boolean hit=target.damage(getDamageSources().mobAttack(this),damage);if(hit&&knockback>0)target.takeKnockback(knockback,getX()-target.getX(),getZ()-target.getZ());return hit||target.isBlocking();}
 private boolean flyStep(Vec3d step){if(!CombatMotion.safeStep(this,step,false))return false;setVelocity(step);velocityModified=true;return true;}
 private boolean safeLanding(Vec3d pos){var w=getWorld();var box=getBoundingBox().offset(pos.subtract(getPos()));int support=Math.max(1,MathHelper.floor(getWidth()*.5));if(!w.getWorldBorder().contains(box))return false;for(int x=MathHelper.floor(box.minX)>>4;x<=MathHelper.floor(box.maxX)>>4;x++)for(int z=MathHelper.floor(box.minZ)>>4;z<=MathHelper.floor(box.maxZ)>>4;z++)if(!w.isChunkLoaded(new BlockPos(x<<4,MathHelper.floor(pos.y),z<<4)))return false;for(BlockPos foot:BlockPos.iterate(BlockPos.ofFloored(pos).add(-support,-1,-support),BlockPos.ofFloored(pos).add(support,-1,support))){if(!w.isChunkLoaded(foot)||!w.getBlockState(foot).isSolidBlock(w,foot))return false;}return w.isSpaceEmpty(this,box)&&!w.containsFluid(box);}
 /** Lure requests reuse the existing collision-checked landing state; combat always takes precedence. */
 public boolean approachLureLanding(Vec3d point){if(getTarget()!=null||!isFlying()||Stun.active(this)||!safeLanding(point))return false;landingPoint=point;beginLanding(false);return true;}
 private Vec3d findLanding(){if(safeLanding(landingPoint))return landingPoint;var w=getWorld();for(int ring=0;ring<=12;ring+=3)for(int i=0;i<(ring==0?1:8);i++){var q=BlockPos.ofFloored(getX()+Math.cos(i*Math.PI/4)*ring,getY(),getZ()+Math.sin(i*Math.PI/4)*ring);if(!w.isChunkLoaded(q))continue;Vec3d p=new Vec3d(q.getX()+.5,w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,q.getX(),q.getZ()),q.getZ()+.5);if(Math.abs(p.y-getY())<20&&safeLanding(p))return p;}return null;}
 private void beginLanding(boolean forced){if(action==Action.LANDING){forcedLanding|=forced;return;}forcedLanding|=forced;Vec3d point=findLanding();if(point==null){touchDown();return;}landingPoint=point;start(Action.LANDING);}
 private void touchDown(){if(getWorld() instanceof ServerWorld world&&safeLanding(landingPoint)&&getPos().squaredDistanceTo(landingPoint)<4){
  world.spawnParticles(new net.minecraft.particle.BlockStateParticleEffect(net.minecraft.particle.ParticleTypes.BLOCK,world.getBlockState(getBlockPos().down())),getX(),getY()+.12,getZ(),18,1,.08,1,.12);
 }dataTracker.set(AIR,false);dataTracker.set(GLIDE,false);setNoGravity(false);setVelocity(Vec3d.ZERO);airTicks=0;flightCooldown=ModConfig.abilityTicks(forcedLanding?220:100);if(forcedLanding){vulnerableTicks=60;start(Action.STAGGER);}else finish();}
 private void aerialTick(LivingEntity target){
  if(++airTicks>180&&action!=Action.LANDING)beginLanding(false);
  if((target==null||!target.isAlive())&&action!=Action.LANDING)beginLanding(false);
  if(!isFlying())return;
  actionTick++;getNavigation().stop();
  switch(action){
   case TAKEOFF->{
    if(!flyStep(new Vec3d(0,.46,0))){beginLanding(false);break;}
    if(actionTick>=20){stopTriggeredAnim("action",null);start(Action.AIR_CHASE);}
   }
   case AIR_CHASE,CIRCLE->{
    double orbit=airTicks*.105;
    Vec3d goal=target.getPos().add(Math.cos(orbit)*5.7,10,Math.sin(orbit)*5.7);
    Vec3d delta=goal.subtract(getPos());
    Vec3d horizontal=delta.multiply(1,0,1);
    if(horizontal.lengthSquared()>.54*.54)horizontal=horizontal.normalize().multiply(.54);
    double climb=MathHelper.clamp(delta.y*.18,-.27,.36);
    direction=CombatMotion.face(this,getPos().add(horizontal),12);
    dataTracker.set(GLIDE,airTicks%42>=21);
    if(!flyStep(horizontal.add(0,climb,0))
        &&!flyStep(horizontal.rotateY(.95f).add(0,.16,0))
        &&!flyStep(horizontal.rotateY(-.95f).add(0,.16,0))
        &&!flyStep(new Vec3d(0,.24,0))){beginLanding(false);break;}
    if(actionTick>=72){
     dataTracker.set(GLIDE,false);
     start(Action.DIVE_WARNING);
     playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL,1,.96f);
    }
   }
   case DIVE_WARNING->{setVelocity(0,-.03,0);if(actionTick<=7)direction=CombatMotion.face(this,motion.lead(target,10,1.4),5);if(actionTick==7)divePoint=motion.lead(target,10,1.4).add(0,.3,0);if(actionTick>=11){start(Action.DIVE);direction=divePoint.subtract(getPos()).normalize();}}
   case DIVE->{Vec3d step=direction.multiply(isEnraged()?1.05:.95);if(actionTick>=5&&!contact)contact=hit(target,15,2.5,.24,1.2);if(!flyStep(step)||actionTick>=22||getY()<=divePoint.y+.25){beginLanding(false);}}
   case LANDING->{if(actionTick>80){touchDown();break;}Vec3d delta=landingPoint.subtract(getPos());if(delta.horizontalLengthSquared()<1&&getY()<=landingPoint.y+.3&&safeLanding(landingPoint)){touchDown();break;}Vec3d h=delta.multiply(1,0,1);if(h.lengthSquared()>.32*.32)h=h.normalize().multiply(.32);double y=MathHelper.clamp(delta.y*.15,-.23,.18);if(!flyStep(h.add(0,y,0))){setVelocity(Vec3d.ZERO);if(actionTick%10==0){Vec3d next=findLanding();if(next!=null)landingPoint=next;}}}
   default->beginLanding(false);
  }
 }
 @Override public void tick(){super.tick();if(!(getWorld() instanceof ServerWorld)||!isAlive())return;if(attackCooldown>0)attackCooldown--;if(flightCooldown>0)flightCooldown--;if(vulnerableTicks>0)vulnerableTicks--;if(lungeCooldown>0)lungeCooldown--;if(Stun.active(this)){cancelCombatForStun();return;}if(isAiDisabled()){if(action!=Action.NONE||isFlying())cancelCombatForStun();return;}var target=getTarget();motion.sample(this,target);setAttacking(target!=null&&target.isAlive());
  if(!isEnraged()&&getHealth()<getMaxHealth()*.3f){dataTracker.set(RAGE,true);getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(ModConfig.speed(.37));if(!isFlying()&&action==Action.NONE)start(Action.ROAR);}
  if(isFlying()){aerialTick(target);return;}
  if(action!=Action.NONE){actionTick++;getNavigation().stop();setVelocity(0,getVelocity().y,0);if(action==Action.STAGGER){if(!isOnGround())actionTick=0;else if(actionTick>=30)finish();return;}if(action==Action.ROAR){if(actionTick>=22)finish();return;}if(target==null||!target.isAlive()){finish();return;}
   if(action==Action.TAKEOFF_PREPARE){
    direction=CombatMotion.face(this,target.getPos(),10);
    if(actionTick>=14){
     if(!isOnGround()||!hasFlightClearance()){finish();return;}
     dataTracker.set(AIR,true);
     setNoGravity(true);
     start(Action.TAKEOFF);
    }
    return;
   }
   int windup=switch(action){case CLAW_ATTACK->7;case BITE->11;case WING_ATTACK,WING_RIGHT,LUNGE->8;default->12;};
   if(actionTick<=windup-3)direction=CombatMotion.face(this,motion.lead(target,action==Action.LUNGE?7:0,1.8),22);
   else CombatMotion.holdFacing(this,direction);
   if(action==Action.LUNGE&&actionTick>=4&&actionTick<=10&&!contact){
    CombatMotion.safeStep(this,direction.multiply(.91),true);
    if(actionTick>=windup)contact=hit(target,10,2.5,.44,.66);
   }else if(actionTick==windup){
    if(action==Action.WING_SPIN||action==Action.WING_ATTACK||action==Action.WING_RIGHT){
     for(var victim:getWorld().getEntitiesByClass(PlayerEntity.class,getBoundingBox().expand(reach(4),1,reach(4)),this::canTarget)){
      boolean doubleSweep=action==Action.WING_SPIN;
      Vec3d sweep=direction.rotateY((float)(action==Action.WING_RIGHT?-.75:.75));
      if(CombatMotion.inSector(this,victim,direction,doubleSweep?.12:-.08)
          &&(doubleSweep||CombatMotion.inSector(this,victim,sweep,.46)))
       hit(victim,doubleSweep?11:9,doubleSweep?3.65:3.85,doubleSweep?.12:-.08,1.2);
     }
    }else hit(target,action==Action.BITE?13:7,2.8,.25,action==Action.BITE?.35:0);
   }
   if(action==Action.CLAW_ATTACK&&actionTick==windup+11){
    direction=CombatMotion.face(this,target.getPos(),25);
    hit(target,7,2.8,.25,.35);
   }
   if(actionTick>=windup+(action==Action.CLAW_ATTACK?16:action==Action.WING_SPIN?14:11))finish();return;
  }
  if(target==null||!target.isAlive())return;direction=CombatMotion.face(this,target.getPos(),12);getLookControl().lookAt(target,30,30);
  if(flightCooldown==0&&squaredDistanceTo(target)>81&&beginTakeoff())return;
  double distance=CombatMotion.distance(this,target);
  if(lungeCooldown==0&&distance>=reach(3)&&distance<=reach(8.5)&&(motion.retreatingFrom(this)||distance>reach(5.5))
      &&beginGroundAttack(Action.LUNGE)){lungeCooldown=50;return;}
  if(attackCooldown==0&&distance<=reach(3.75)&&canSee(target)){
   Action next=switch(attackChoice++%5){
    case 0->Action.CLAW_ATTACK;
    case 1->Action.BITE;
    case 2->Action.WING_ATTACK;
    case 3->Action.WING_RIGHT;
    default->Action.WING_SPIN;
   };
   if(distance>reach(2.8)&&(next==Action.CLAW_ATTACK||next==Action.BITE))next=Action.WING_ATTACK;
   if(beginGroundAttack(next))return;
  }
  if(age%4==0){
   if(distance>reach(1.8)||!CombatMotion.meleeHeight(this,target)||!canSee(target))getNavigation().startMovingTo(target,isEnraged()?1.2:1.1);
   else getNavigation().stop();
  }
 }
 /** Called only after a successful server-side Aard hit with its normal range and line-of-sight checks. */
 public void interruptDive(){
  if(getWorld().isClient||action!=Action.DIVE||!isFlying())return;
  dataTracker.set(AIR,false);
  dataTracker.set(GLIDE,false);
  setNoGravity(false);
  airTicks=0;
  forcedLanding=true;
  vulnerableTicks=65;
  flightCooldown=ModConfig.abilityTicks(220);
  start(Action.STAGGER);
  setVelocity(0,-.48,0);
 }
 public boolean hasTakeoffMeleeProtection(){return !getWorld().isClient&&isAlive()&&!isAiDisabled()&&!Stun.active(this)&&isFlying()&&action==Action.TAKEOFF&&actionTick<20;}
 public static boolean ordinaryMelee(DamageSource source){return source.getAttacker() instanceof LivingEntity&&source.getSource()==source.getAttacker()&&!source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE)&&!source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_EXPLOSION)&&!source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY)&&source.isIn(net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.DAMAGE_TYPE,net.minecraft.util.Identifier.of("witchersbestiary","takeoff_melee")));}
 @Override public void takeKnockback(double strength,double x,double z){if(!hasTakeoffMeleeProtection())super.takeKnockback(strength,x,z);}
 @Override public boolean damage(DamageSource source,float amount){if(hasTakeoffMeleeProtection()&&ordinaryMelee(source))return false;float before=getHealth();boolean hit=super.damage(source,vulnerableTicks>0?amount*1.3f:amount);if(hit&&isAlive()&&!getWorld().isClient){if(isFlying()){aerialDamage+=Math.max(0,before-getHealth());if(aerialDamage>=18)beginLanding(true);}else if(action==Action.NONE)triggerAnim("action","hurt");}return hit;}
 @Override public boolean handleFallDamage(float distance,float multiplier,DamageSource source){return false;}
 @Override public void onDeath(DamageSource source){dataTracker.set(AIR,false);dataTracker.set(GLIDE,false);setNoGravity(false);action=Action.NONE;stopTriggeredAnim("action",null);super.onDeath(source);}
 @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putBoolean("GriffinLargeRig",true);n.putBoolean("GriffinRage",isEnraged());n.putInt("GriffinFlightCooldown",flightCooldown);}
 @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);var scale=getAttributeInstance(EntityAttributes.GENERIC_SCALE);if(!n.getBoolean("GriffinLargeRig")&&scale.getBaseValue()==1)scale.setBaseValue(BASE_SCALE);dataTracker.set(RAGE,n.getBoolean("GriffinRage"));getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(ModConfig.speed(isEnraged()?.37:.32));flightCooldown=n.contains("GriffinFlightCooldown")?Math.clamp(n.getInt("GriffinFlightCooldown"),40,600):180;dataTracker.set(AIR,false);dataTracker.set(GLIDE,false);setNoGravity(false);action=Action.NONE;attackCooldown=20;}
 @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
 @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"movement",4,s->s.setAndContinue(!isAlive()?RawAnimation.begin().thenPlayAndHold("animation.griffin.death"):RawAnimation.begin().thenLoop("animation.griffin."+(isFlying()?dataTracker.get(GLIDE)?"glide":"fly":s.isMoving()?isAttacking()?"run":"walk":"idle")))));var a=new AnimationController<>(this,"action",0,s->PlayState.STOP);for(String n:new String[]{"claw_attack","bite","sweep_left","sweep_right","wing_spin","lunge","roar","takeoff_prepare","takeoff","dive_warning","landing","dive_attack","hurt","stagger"})a.triggerableAnim(n,RawAnimation.begin().thenPlay("animation.griffin."+n));c.add(a);}
 public static DefaultAttributeContainer.Builder createAttributes(){return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_SCALE,BASE_SCALE).add(EntityAttributes.GENERIC_MAX_HEALTH,180).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,11).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,ModConfig.speed(.32)).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,.85).add(EntityAttributes.GENERIC_FOLLOW_RANGE,48).add(EntityAttributes.GENERIC_STEP_HEIGHT,1);}
}
