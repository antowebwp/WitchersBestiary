package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.*;
import com.witchersbestiary.entity.ai.CombatMotion;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Server owns combat windows; tracked guard/rage are also available to renderers. */
public final class EndregaWarriorEntity extends HostileEntity implements CategorizedMonster,GeoEntity,Stunnable {
 private static final net.minecraft.entity.data.TrackedData<Boolean> STUN=net.minecraft.entity.data.DataTracker.registerData(EndregaWarriorEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
 private static final net.minecraft.entity.data.TrackedData<Boolean> GUARD=net.minecraft.entity.data.DataTracker.registerData(EndregaWarriorEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
 private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
 public enum Action { NONE,BITE,SLASH,COMBO,INTERCEPT,DODGE,GUARD }
 private Action action=Action.NONE;
 private int actionTick,cooldown=15,guardCooldown=100,dodgeCooldown=70,vulnerableTicks,orbit=1;
 private boolean lastHit,windowContact;
 private int interceptCooldown;
 private final CombatMotion motion=new CombatMotion();
 private Vec3d direction=Vec3d.ZERO;
 public EndregaWarriorEntity(EntityType<? extends HostileEntity> type,World world){super(type,world);experiencePoints=14;}
 @Override protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder b){super.initDataTracker(b);b.add(GUARD,false);b.add(STUN,false);}
 @Override public boolean trackedStun(){return dataTracker.get(STUN);}
 @Override public void setTrackedStun(boolean value){dataTracker.set(STUN,value);}
 @Override public void cancelCombatForStun(){finish();actionTick=0;getNavigation().stop();setVelocity(Vec3d.ZERO);}
 @Override public MonsterCategory getMonsterCategory(){return MonsterCategory.INSECTOID;}
 @Override public int getLimitPerChunk(){return 3;}
 public boolean isEnraged(){return getHealth()<getMaxHealth()*.35f;}
 public boolean isGuarding(){return dataTracker.get(GUARD);}
 public Action getAction(){return action;}
 public int getVulnerableTicks(){return vulnerableTicks;}
 @Override public CombatState getCombatState(){return Stun.active(this)?CombatState.STUNNED:action!=Action.NONE?CombatState.SPECIAL_ABILITY:cooldown>0?CombatState.ATTACK_RECOVERY:getTarget()==null?CombatState.IDLE:CombatState.CHASE;}
 @Override protected void initGoals(){
  targetSelector.add(1,new RevengeGoal(this));targetSelector.add(2,new ActiveTargetGoal<>(this,PlayerEntity.class,0,true,false,null));
  goalSelector.add(5,new WanderAroundFarGoal(this,.7){@Override public boolean canStart(){return getTarget()==null&&action==Action.NONE&&super.canStart();}});
 }
 private void face(LivingEntity target,float speed){direction=CombatMotion.face(this,target.getPos(),speed); }
 private double distance(LivingEntity target){return CombatMotion.distance(this,target);}
 public boolean beginAction(Action next){
  var t=getTarget();if(getWorld().isClient||!isAlive()||isAiDisabled()||Stun.active(this)||action!=Action.NONE||t==null||!t.isAlive()||!canSee(t)||next==Action.NONE)return false;
  if((next==Action.BITE||next==Action.SLASH||next==Action.COMBO)&&(cooldown>0||distance(t)>2.7||!CombatMotion.meleeHeight(this,t)))return false;
  if(next==Action.GUARD&&guardCooldown>0||next==Action.DODGE&&dodgeCooldown>0)return false;
  if(next==Action.INTERCEPT&&(cooldown>0||interceptCooldown>0||distance(t)<3||distance(t)>7||!isOnGround()))return false;
  face(t,0);action=next;actionTick=0;lastHit=windowContact=false;getNavigation().stop();dataTracker.set(GUARD,next==Action.GUARD);
  if(next==Action.INTERCEPT)interceptCooldown=100;
  if(next==Action.GUARD)guardCooldown=180;if(next==Action.DODGE){dodgeCooldown=110;orbit=-orbit;}
  triggerAnim("action",next.name().toLowerCase(java.util.Locale.ROOT));return true;
 }
 private boolean strike(LivingEntity t,float damage,double reach,boolean poison){
  if(!CombatMotion.contact(this,t,direction,reach,.45))return false;
  boolean hit=t.damage(getDamageSources().mobAttack(this),damage);
  if(hit&&poison)t.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,isEnraged()?80:60,0),this);
  return hit;
 }
 private boolean windowStrike(LivingEntity t,float damage,double reach,boolean poison){
  if(windowContact||!CombatMotion.contact(this,t,direction,reach,.45))return false;
  windowContact=true;return strike(t,damage,reach,poison);
 }
 private void finish(){action=Action.NONE;dataTracker.set(GUARD,false);stopTriggeredAnim("action",null);cooldown=com.witchersbestiary.config.ModConfig.attackTicks(isEnraged()?8:14);}
 private void safeStep(Vec3d step){CombatMotion.safeStep(this,step,true); }
 @Override public void tick(){
  super.tick();if(getWorld().isClient||!isAlive())return;
  if(interceptCooldown>0)interceptCooldown--;if(cooldown>0)cooldown--;if(guardCooldown>0)guardCooldown--;if(dodgeCooldown>0)dodgeCooldown--;if(vulnerableTicks>0)vulnerableTicks--;
  if(isAiDisabled()||Stun.active(this)){if(action!=Action.NONE)finish();getNavigation().stop();return;}
  var t=getTarget();motion.sample(this,t);setAttacking(t!=null&&t.isAlive());
  if(t==null||!t.isAlive()){if(action!=Action.NONE)finish();return;}
  if(action!=Action.NONE){
   getNavigation().stop();setVelocity(0,getVelocity().y,0);actionTick++;
   // Last four ticks before a bite/lunge are committed. Combo has three distinct reads.
   if(action==Action.SLASH&&actionTick<6||action==Action.BITE&&actionTick<10||action==Action.COMBO&&(actionTick<5||actionTick>9&&actionTick<16||actionTick>20&&actionTick<28))face(t,12);
   if(action==Action.INTERCEPT&&actionTick<=6)direction=CombatMotion.face(this,motion.lead(t,8,1.25),16);
   if((action==Action.SLASH&&actionTick<6||action==Action.BITE&&actionTick<10)&&distance(t)>2.3)safeStep(direction.multiply(.12));
   switch(action){
    case BITE->{if(!windowContact&&actionTick>=14&&actionTick<=16)lastHit=windowStrike(t,8,2.7,true);if(actionTick==17&&!lastHit)vulnerableTicks=22;if(actionTick>=26)finish();}
    case SLASH->{if(!windowContact&&actionTick>=8&&actionTick<=10)lastHit=windowStrike(t,6,2.6,false);if(actionTick>=18){if(lastHit&&distance(t)<3.2&&canSee(t)){action=Action.COMBO;actionTick=0;lastHit=windowContact=false;triggerAnim("action","combo");}else finish();}}
    case COMBO->{if(actionTick==8)strike(t,5,2.6,false);if(actionTick==19)strike(t,5,2.6,false);if(actionTick>=29&&actionTick<=32)safeStep(direction.multiply(.22));if(actionTick==33)lastHit=strike(t,isEnraged()?12:10,2.9,true);if(actionTick==35&&!lastHit)vulnerableTicks=24;if(actionTick>=44)finish();}
    case INTERCEPT->{
     if(actionTick>=9&&actionTick<=16&&!CombatMotion.safeStep(this,direction.multiply(isEnraged()?.55:.48),true)){vulnerableTicks=24;finish();break;}
     if(!windowContact&&actionTick>=10&&actionTick<=18)lastHit=windowStrike(t,7,2.5,true);
     if(actionTick>=26){if(lastHit&&distance(t)<=2.8){action=Action.COMBO;actionTick=0;lastHit=windowContact=false;triggerAnim("action","combo");}else {vulnerableTicks=24;finish();}}
    }
    case DODGE->{if(actionTick>=3&&actionTick<=8)safeStep(new Vec3d(direction.z,0,-direction.x).multiply(orbit*.27));if(actionTick>=15)finish();}
    case GUARD->{face(t,5);if(actionTick>=32)finish();}
    default->{}
   }return;
  }
  face(t,isEnraged()?16:12);double d=distance(t);
  if(!CombatMotion.meleeHeight(this,t)){if(age%5==0)getNavigation().startMovingTo(t,isEnraged()?1.24:1.05);return;}
  if(d>=3&&d<=7&&motion.retreatingFrom(this)&&beginAction(Action.INTERCEPT))return;
  if(age%45==0&&random.nextBoolean())orbit=-orbit;
  if(d<3.3&&guardCooldown==0&&cooldown>0&&beginAction(Action.GUARD))return;
  if(d<2.9&&dodgeCooldown==0&&cooldown>0&&random.nextInt(12)==0&&beginAction(Action.DODGE))return;
  if(d<=2.55&&cooldown==0&&canSee(t)){int roll=random.nextInt(isEnraged()?4:6);if(beginAction(roll<2?Action.COMBO:roll==2?Action.BITE:Action.SLASH))return;}
  if(age%5==0){
   if(d>4.5)getNavigation().startMovingTo(t,isEnraged()?1.24:1.05);
   // Navigation accepts a waypoint within roughly half this wide mob's body.
   // Aim inside contact distance when ready, rather than orbiting outside reach.
   else {Vec3d radial=getPos().subtract(t.getPos()).multiply(1,0,1).normalize();double radius=cooldown==0?.4:2.4;Vec3d tangent=new Vec3d(-radial.z,0,radial.x).multiply(orbit*(cooldown==0?.45:isEnraged()?1.8:1.3));Vec3d goal=t.getPos().add(radial.multiply(radius)).add(tangent);getNavigation().startMovingTo(goal.x,goal.y,goal.z,isEnraged()?1.2:.9);}
  }
 }
 @Override public boolean damage(DamageSource source,float amount){
  if(vulnerableTicks>0)amount*=1.3f;
  if(isGuarding()&&source.getAttacker()!=null){Vec3d from=source.getAttacker().getPos().subtract(getPos()).multiply(1,0,1).normalize();if(from.dotProduct(Vec3d.fromPolar(0,getYaw()))>.25)amount*=.4f;}
  boolean hit=super.damage(source,amount);if(hit&&isAlive()&&action==Action.NONE)triggerAnim("action","hurt");return hit;
 }
 @Override public void onDeath(DamageSource source){dataTracker.set(GUARD,false);action=Action.NONE;stopTriggeredAnim("action",null);super.onDeath(source);}
 @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
 @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){
  c.add(new AnimationController<>(this,"movement",3,s->s.setAndContinue(RawAnimation.begin().thenLoop("animation.endrega_warrior."+(!isAlive()?"death":s.isMoving()?isAttacking()?"run":"walk":"idle")))));
  var a=new AnimationController<>(this,"action",0,s->PlayState.STOP);for(String n:new String[]{"bite","slash","combo","intercept","dodge","guard","hurt"})a.triggerableAnim(n,RawAnimation.begin().thenPlay("animation.endrega_warrior."+n));c.add(a);
 }
 public static DefaultAttributeContainer.Builder createAttributes(){return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,76).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,6).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,com.witchersbestiary.config.ModConfig.speed(.37)).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,.65).add(EntityAttributes.GENERIC_FOLLOW_RANGE,36).add(EntityAttributes.GENERIC_STEP_HEIGHT,1);}
}
