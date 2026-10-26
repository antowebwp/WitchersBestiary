package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.*;
import com.witchersbestiary.config.ModConfig;
import com.witchersbestiary.entity.ai.CombatMotion;
import com.witchersbestiary.registry.ModEffects;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Heavy quadruped. Committed attacks have finite windows; gaze is checked only at hypnosis impact. */
public final class FiendEntity extends HostileEntity implements CategorizedMonster,GeoEntity,Stunnable {
 private static final net.minecraft.entity.data.TrackedData<Boolean> STUN=net.minecraft.entity.data.DataTracker.registerData(FiendEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
 private static final net.minecraft.entity.data.TrackedData<Boolean> RAGE=net.minecraft.entity.data.DataTracker.registerData(FiendEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
 private static final net.minecraft.entity.data.TrackedData<Boolean> EYE=net.minecraft.entity.data.DataTracker.registerData(FiendEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
 private static final Identifier RAGE_SPEED=Identifier.of("witchersbestiary","fiend_rage");
 private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
 public enum Action { NONE, CLAW_ATTACK, HORN_ATTACK, SWEEP, CHARGE_PREPARATION, CHARGE, HYPNOSIS, ENRAGE }
 private Action action=Action.NONE;private int actionTick,attackCooldown,chargeCooldown=120,hypnosisCooldown=160;
 private final CombatMotion motion=new CombatMotion();
 private int flankTicks,sweepCooldown;
 private Vec3d direction=Vec3d.ZERO;private boolean contact,hornNext;
 public FiendEntity(EntityType<? extends HostileEntity> type,World world){super(type,world);experiencePoints=30;}
 @Override protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder b){super.initDataTracker(b);b.add(STUN,false);b.add(RAGE,false);b.add(EYE,false);}
 @Override public boolean trackedStun(){return dataTracker.get(STUN);}
 @Override public void setTrackedStun(boolean v){dataTracker.set(STUN,v);}
 public boolean isEnraged(){return dataTracker.get(RAGE);}
 public boolean isHypnotizing(){return dataTracker.get(EYE);}
 public Action getAction(){return action;}
 public int getChargeCooldown(){return chargeCooldown;}
 public int getHypnosisCooldown(){return hypnosisCooldown;}
 @Override public int getLimitPerChunk(){return 1;}
 @Override public MonsterCategory getMonsterCategory(){return MonsterCategory.RELICT;}
 @Override protected void initGoals(){targetSelector.add(1,new RevengeGoal(this));targetSelector.add(2,new ActiveTargetGoal<>(this,PlayerEntity.class,0,true,false,null));goalSelector.add(5,new WanderAroundFarGoal(this,.65){@Override public boolean canStart(){return getTarget()==null&&action==Action.NONE&&super.canStart();}});goalSelector.add(6,new LookAtEntityGoal(this,PlayerEntity.class,14));}
 @Override public CombatState getCombatState(){return Stun.active(this)?CombatState.STUNNED:action!=Action.NONE?CombatState.SPECIAL_ABILITY:attackCooldown>0?CombatState.ATTACK_RECOVERY:getTarget()==null?CombatState.IDLE:CombatState.CHASE;}
 @Override public void cancelCombatForStun(){action=Action.NONE;actionTick=0;dataTracker.set(EYE,false);attackCooldown=12;getNavigation().stop();setVelocity(Vec3d.ZERO);}
 private boolean ready(){return !getWorld().isClient&&isAlive()&&!isAiDisabled()&&!Stun.active(this)&&action==Action.NONE&&getTarget()!=null&&getTarget().isAlive();}
 // Turn the whole quadruped, including the authoritative attack direction.
 private void face(LivingEntity target,float degrees){
  direction=CombatMotion.face(this,target.getPos(),degrees);
 }
 private double edgeDistanceSquared(LivingEntity target){return MathHelper.square(CombatMotion.distance(this,target));}
 private void start(Action value){var target=getTarget();if(target!=null)face(target,0);action=value;actionTick=0;contact=false;getNavigation().stop();setVelocity(Vec3d.ZERO);dataTracker.set(EYE,value==Action.HYPNOSIS);triggerAnim("action",value.name().toLowerCase(java.util.Locale.ROOT));}
 public boolean beginCharge(){if(!ready()||chargeCooldown>0||squaredDistanceTo(getTarget())<25||squaredDistanceTo(getTarget())>324||!canSee(getTarget()))return false;chargeCooldown=ModConfig.abilityTicks((int)((240+random.nextInt(121))*(isEnraged()?.7:1)));start(Action.CHARGE_PREPARATION);playSound(SoundEvents.ENTITY_RAVAGER_ROAR,1.3f,.55f);return true;}
 public boolean beginHypnosis(){if(!ready()||hypnosisCooldown>0||squaredDistanceTo(getTarget())>196||!canSee(getTarget()))return false;hypnosisCooldown=ModConfig.abilityTicks(360+random.nextInt(141));start(Action.HYPNOSIS);playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,.75f,.7f);return true;}
 public boolean beginHornAttack(){if(!ready()||attackCooldown>0||edgeDistanceSquared(getTarget())>3.2*3.2||!CombatMotion.meleeHeight(this,getTarget())||!canSee(getTarget()))return false;start(Action.HORN_ATTACK);return true;}
 public Vec3d thirdEyePosition(){return getPos().add(Vec3d.fromPolar(0,getYaw()).multiply(1.8)).add(0,2.3,0);}
 public boolean hypnosisAffects(PlayerEntity p){
  if(!p.isAlive()||p.isSpectator()||p.isCreative()||squaredDistanceTo(p)>196||!canSee(p))return false;
  Vec3d eye=thirdEyePosition(),toward=eye.subtract(p.getEyePos()).normalize();
  if(p.getRotationVec(1).dotProduct(toward)<.75)return false;
  return getWorld().raycast(new RaycastContext(p.getEyePos(),eye,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p)).getType()==net.minecraft.util.hit.HitResult.Type.MISS;
 }
 public boolean applyHypnosis(PlayerEntity p){if(getWorld().isClient||!hypnosisAffects(p))return false;boolean protectedMind=p.hasStatusEffect(ModEffects.CLEAR_MIND);p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,protectedMind?15:60,0));p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,protectedMind?12:50,0));return true;}
 private boolean impact(LivingEntity t,float damage,double range,double force){if(!CombatMotion.contact(this,t,direction,range,.2))return false;boolean hit=t.damage(getDamageSources().mobAttack(this),damage);if(hit)t.takeKnockback(force,getX()-t.getX(),getZ()-t.getZ());return hit||t.isBlocking();}
 private void finish(){action=Action.NONE;dataTracker.set(EYE,false);stopTriggeredAnim("action",null);attackCooldown=ModConfig.attackTicks(isEnraged()?6:9);}
 private void wallStagger(){finish();Stun.apply(this,40);triggerAnim("action","stagger");playSound(SoundEvents.ENTITY_RAVAGER_STUNNED,1,.65f);}
 @Override public void tick(){
  super.tick();if(!(getWorld() instanceof ServerWorld world)||!isAlive())return;
  if(attackCooldown>0)attackCooldown--;if(chargeCooldown>0)chargeCooldown--;if(hypnosisCooldown>0)hypnosisCooldown--;
  if(Stun.active(this)){cancelCombatForStun();Stun.stop(this);return;}if(isAiDisabled())return;
  var target=getTarget();motion.sample(this,target);if(sweepCooldown>0)sweepCooldown--;setAttacking(target!=null&&target.isAlive());
  if(!isEnraged()&&getHealth()<getMaxHealth()*.35f){dataTracker.set(RAGE,true);applyRage();start(Action.ENRAGE);playSound(SoundEvents.ENTITY_RAVAGER_ROAR,1.5f,.5f);}
  if(target!=null){double side=Vec3d.fromPolar(0,getYaw()).dotProduct(target.getPos().subtract(getPos()).multiply(1,0,1).normalize());flankTicks=edgeDistanceSquared(target)<16&&side<.25?Math.min(30,flankTicks+1):action!=Action.NONE||attackCooldown>0?flankTicks:Math.max(0,flankTicks-1);}
  if(action!=Action.NONE){
   if(target==null||!target.isAlive()){finish();return;}
   getNavigation().stop();actionTick++;setVelocity(0,getVelocity().y,0);
   // Claws track until the swing; horns/charge commit early enough to sidestep.
   if(action==Action.CLAW_ATTACK&&actionTick<=attackTick()-2)face(target,14);
   else if(action==Action.HORN_ATTACK&&actionTick<=hornTick()-6)face(target,10);
   else if(action==Action.CHARGE_PREPARATION&&actionTick<=14)direction=CombatMotion.face(this,motion.lead(target,8,1.3),6);
   else if(action==Action.SWEEP&&actionTick<=12)face(target,12);
   else if(action==Action.HYPNOSIS&&actionTick<20)face(target,8);
   if(action==Action.CLAW_ATTACK&&actionTick<attackTick()-2&&edgeDistanceSquared(target)>2.6*2.6)CombatMotion.safeStep(this,direction.multiply(.12),true);
   switch(action){
    case CLAW_ATTACK->{int windup=attackTick();if(!contact&&actionTick>=windup&&actionTick<=windup+2)contact=impact(target,14,3.2,.6);if(actionTick>=windup+10)finish();}
    case HORN_ATTACK->{int windup=hornTick();if(!contact&&actionTick>=windup&&actionTick<=windup+2)contact=impact(target,18,3.4,1.25);if(actionTick>=windup+14)finish();}
    case SWEEP->{int windup=isEnraged()?13:16;if(!contact&&actionTick>=windup&&actionTick<=windup+2)contact=impact(target,12,3.4,.85);if(actionTick>=(isEnraged()?25:30))finish();}
    case CHARGE_PREPARATION->{if(actionTick%5==0)world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,world.getBlockState(getBlockPos().down())),getX(),getY()+.1,getZ(),12,1,.1,1,.02);if(actionTick>=22){action=Action.CHARGE;actionTick=0;triggerAnim("action","charge");}}
    case CHARGE->{if(actionTick<=6)direction=CombatMotion.face(this,target.getPos(),1.5f);Vec3d step=direction.multiply(isEnraged()?.92:.8);Box next=getBoundingBox().offset(step);if(horizontalCollision||!CombatMotion.safeStep(this,step,true)){wallStagger();break;}setVelocity(step.x,getVelocity().y,step.z);velocityModified=true;if(!contact)for(var p:world.getEntitiesByClass(PlayerEntity.class,next.expand(.25),e->!e.isCreative()&&!e.isSpectator()))if(impact(p,22,2.8,1.6)){contact=true;break;}if(actionTick>=22)finish();}
    case HYPNOSIS->{if(actionTick==24){for(var p:world.getEntitiesByClass(PlayerEntity.class,getBoundingBox().expand(14),e->!e.isSpectator()))applyHypnosis(p);var eye=thirdEyePosition();world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,eye.x,eye.y,eye.z,14,.3,.3,.3,.02);}if(actionTick>=40)finish();}
    case ENRAGE->{if(actionTick>=30)finish();}
    default->{}
   }return;
  }
  if(target==null||!target.isAlive())return;
  if(flankTicks>=5&&attackCooldown==0&&sweepCooldown==0&&CombatMotion.meleeHeight(this,target)&&canSee(target)){start(Action.SWEEP);sweepCooldown=100;flankTicks=0;return;}
  face(target,12);getLookControl().lookAt(target,30,30);double d=squaredDistanceTo(target);
  if(attackCooldown==0&&beginHypnosis())return;
  if(edgeDistanceSquared(target)>3.05*3.05){if(beginCharge())return;if(age%6==0)getNavigation().startMovingTo(target,1.05);}
  else if(canSee(target)&&CombatMotion.meleeHeight(this,target)){getNavigation().stop();if(attackCooldown==0){if(hornNext)beginHornAttack();else start(Action.CLAW_ATTACK);hornNext=!hornNext;}}
  else if(age%6==0)getNavigation().startMovingTo(target,1.05);
 }
 private int attackTick(){return isEnraged()?8:10;}
 private int hornTick(){return isEnraged()?15:18;}
 private void applyRage(){var speed=getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);speed.removeModifier(RAGE_SPEED);if(isEnraged())speed.addTemporaryModifier(new EntityAttributeModifier(RAGE_SPEED,.2,EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));}
 @Override public boolean damage(DamageSource source,float amount){boolean hit=super.damage(source,amount);if(hit&&isAlive()&&action==Action.NONE&&!Stun.active(this))triggerAnim("action","hurt");return hit;}
 @Override public void onDeath(DamageSource source){action=Action.NONE;dataTracker.set(EYE,false);stopTriggeredAnim("action",null);super.onDeath(source);}
 @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putBoolean("FiendRage",isEnraged());n.putInt("FiendChargeCooldown",chargeCooldown);n.putInt("FiendHypnosisCooldown",hypnosisCooldown);}
 @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);dataTracker.set(RAGE,n.getBoolean("FiendRage"));applyRage();chargeCooldown=n.contains("FiendChargeCooldown")?Math.clamp(n.getInt("FiendChargeCooldown"),0,1080):120;hypnosisCooldown=n.contains("FiendHypnosisCooldown")?Math.clamp(n.getInt("FiendHypnosisCooldown"),0,1500):160;action=Action.NONE;dataTracker.set(EYE,false);attackCooldown=20;}
 @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
 @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"movement",4,state->{state.setControllerSpeed(isEnraged()?1.2f:1);return state.setAndContinue(RawAnimation.begin().thenLoop("animation.fiend."+(!isAlive()?"death":state.isMoving()?isAttacking()?"run":"walk":"idle")));}));var a=new AnimationController<>(this,"action",0,state->{state.setControllerSpeed(isEnraged()?1.2f:1);return PlayState.STOP;});for(String n:new String[]{"claw_attack","horn_attack","sweep","charge_preparation","hypnosis","enrage","roar","stagger","hurt"})a.triggerableAnim(n,RawAnimation.begin().thenPlay("animation.fiend."+n));a.triggerableAnim("charge",RawAnimation.begin().thenLoop("animation.fiend.charge"));c.add(a);}
 public static DefaultAttributeContainer.Builder createAttributes(){return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,220).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,14).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,ModConfig.speed(.31)).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,.95).add(EntityAttributes.GENERIC_FOLLOW_RANGE,48).add(EntityAttributes.GENERIC_STEP_HEIGHT,1);}
}
