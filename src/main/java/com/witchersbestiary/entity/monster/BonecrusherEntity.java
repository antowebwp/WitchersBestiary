package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.*;
import com.witchersbestiary.entity.ai.CombatMotion;
import com.witchersbestiary.config.ModConfig;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Finite, warned attacks. Paths are refreshed six ticks apart; impact checks retain line of sight. */
public final class BonecrusherEntity extends HostileEntity implements CategorizedMonster,GeoEntity,Stunnable {
    private static final net.minecraft.entity.data.TrackedData<Boolean> STUN=net.minecraft.entity.data.DataTracker.registerData(BonecrusherEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    private static final net.minecraft.entity.data.TrackedData<Boolean> RAGE=net.minecraft.entity.data.DataTracker.registerData(BonecrusherEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    private static final Identifier RAGE_SPEED=Identifier.of("witchersbestiary","bonecrusher_rage");
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public enum Action { NONE, CLUB_SWING, OVERHEAD_SMASH, GROUND_SLAM, CHARGE, ROAR, STAGGER }
    private Action action=Action.NONE;
    private int actionTick,attackCooldown,heavyCooldown=80,chargeCooldown=100,vulnerableTicks;
    private Vec3d chargeDirection=Vec3d.ZERO,attackDirection=Vec3d.ZERO;
    private boolean contact;
    public BonecrusherEntity(EntityType<? extends HostileEntity> type,World world){super(type,world);experiencePoints=20;}
    @Override protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder b){super.initDataTracker(b);b.add(STUN,false);b.add(RAGE,false);}
    @Override public boolean trackedStun(){return dataTracker.get(STUN);}
    @Override public void setTrackedStun(boolean v){dataTracker.set(STUN,v);}
    public boolean isEnraged(){return dataTracker.get(RAGE);}
    public Action getAction(){return action;}
    public int getVulnerableTicks(){return vulnerableTicks;}
    @Override public MonsterCategory getMonsterCategory(){return MonsterCategory.OGROID;}
    @Override protected void initGoals(){
        targetSelector.add(1,new ActiveTargetGoal<>(this,PlayerEntity.class,0,true,false,null));
        goalSelector.add(5,new WanderAroundFarGoal(this,.65){@Override public boolean canStart(){return getTarget()==null&&action==Action.NONE&&super.canStart();}});
        goalSelector.add(6,new LookAtEntityGoal(this,PlayerEntity.class,12));
    }
    @Override public CombatState getCombatState(){return Stun.active(this)?CombatState.STUNNED:action!=Action.NONE?CombatState.SPECIAL_ABILITY:attackCooldown>0?CombatState.ATTACK_RECOVERY:getTarget()==null?CombatState.IDLE:CombatState.CHASE;}
    @Override public void cancelCombatForStun(){action=Action.NONE;actionTick=0;attackCooldown=12;getNavigation().stop();setVelocity(Vec3d.ZERO);}
    private void start(Action a){if(getTarget()!=null){attackDirection=getTarget().getPos().subtract(getPos()).multiply(1,0,1).normalize();float yaw=(float)(MathHelper.atan2(attackDirection.z,attackDirection.x)*180/Math.PI)-90;setYaw(yaw);setBodyYaw(yaw);setHeadYaw(yaw);}action=a;actionTick=0;contact=false;getNavigation().stop();setVelocity(Vec3d.ZERO);triggerAnim("action",a.name().toLowerCase(java.util.Locale.ROOT));if(a==Action.CHARGE||a==Action.ROAR)playSound(SoundEvents.ENTITY_RAVAGER_ROAR,1.2f,.7f);}
    public boolean beginHeavyAttack(){if(getWorld().isClient||!isAlive()||isAiDisabled()||Stun.active(this)||action!=Action.NONE||heavyCooldown>0||getTarget()==null)return false;heavyCooldown=ModConfig.abilityTicks(240+random.nextInt(81));start(random.nextBoolean()?Action.OVERHEAD_SMASH:Action.GROUND_SLAM);return true;}
    public boolean beginCharge(){var t=getTarget();if(getWorld().isClient||!isAlive()||isAiDisabled()||Stun.active(this)||action!=Action.NONE||chargeCooldown>0||t==null||!t.isAlive()||!canSee(t)||squaredDistanceTo(t)<25||squaredDistanceTo(t)>169)return false;chargeCooldown=ModConfig.abilityTicks((int)((300+random.nextInt(101))*(isEnraged()?.8:1)));chargeDirection=t.getPos().subtract(getPos()).multiply(1,0,1).normalize();start(Action.CHARGE);return true;}
    private boolean hit(LivingEntity t,float damage,double range,double knockback){
        if(!t.isAlive()||!canSee(t)||squaredDistanceTo(t)>range*range||Math.abs(t.getY()-getY())>3)return false;
        if(t.damage(getDamageSources().mobAttack(this),damage)){t.takeKnockback(knockback,getX()-t.getX(),getZ()-t.getZ());return true;}return false;
    }
    private void recover(boolean heavy){if(heavy&&!contact){vulnerableTicks=28;start(Action.STAGGER);}else{action=Action.NONE;attackCooldown=ModConfig.attackTicks(isEnraged()?16:22);}}
    @Override public void tick(){
        super.tick();if(!(getWorld() instanceof ServerWorld world)||!isAlive())return;
        if(attackCooldown>0)attackCooldown--;if(heavyCooldown>0)heavyCooldown--;if(chargeCooldown>0)chargeCooldown--;if(vulnerableTicks>0)vulnerableTicks--;
        if(Stun.active(this)){cancelCombatForStun();return;}if(isAiDisabled())return;
        if(!isEnraged()&&getHealth()<getMaxHealth()*.35f){dataTracker.set(RAGE,true);applyRage();start(Action.ROAR);}
        var target=getTarget();
        if(action!=Action.NONE){
            getNavigation().stop();actionTick++;setVelocity(0,getVelocity().y,0);
            int windup=isEnraged()?11:14,heavyWindup=isEnraged()?20:24;
            switch(action){
                case CLUB_SWING->{if(target!=null&&actionTick<windup-4)attackDirection=CombatMotion.face(this,target.getPos(),9);if(!contact&&actionTick>=windup&&actionTick<=windup+2&&target!=null&&target.getPos().subtract(getPos()).multiply(1,0,1).normalize().dotProduct(attackDirection)>.25)contact=hit(target,(float)getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE),3.7,.6);if(actionTick>=windup+14)recover(false);}
                case OVERHEAD_SMASH,GROUND_SLAM->{
                    if(actionTick==heavyWindup){
                        var floor=getBlockPos().down();world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,world.getBlockState(floor)),getX(),getY()+.1,getZ(),50,2,.15,2,.1);playSound(SoundEvents.ENTITY_GENERIC_EXPLODE.value(),.7f,.6f);
                        for(PlayerEntity p:world.getEntitiesByClass(PlayerEntity.class,getBoundingBox().expand(4),p->!p.isCreative()&&!p.isSpectator()))contact|=hit(p,p==target?16:10,4.3,1.2);
                    }
                    int pulse=actionTick-heavyWindup;
                    if(pulse>=0&&pulse<=4&&pulse%2==0)for(int i=0;i<18;i++){double angle=i*Math.PI/9,radius=1.3+pulse*.6;world.spawnParticles(ParticleTypes.POOF,getX()+Math.cos(angle)*radius,getY()+.15,getZ()+Math.sin(angle)*radius,1,.03,.03,.03,.01);}
                    if(actionTick>=heavyWindup+12)recover(true);
                }
                case CHARGE->{
                    if(actionTick>18){
                        Vec3d step=chargeDirection.multiply(.65);Box next=getBoundingBox().offset(step);
                        if(!CombatMotion.safeStep(this,step,true)){recover(true);break;}
                        setVelocity(step.x,getVelocity().y,step.z);velocityModified=true;
                        if(!contact)for(PlayerEntity p:world.getEntitiesByClass(PlayerEntity.class,next.expand(.35),p->!p.isCreative()&&!p.isSpectator()))if(hit(p,12,3.2,1.4)){contact=true;break;}
                    }
                    if(actionTick>=34)recover(true);
                }
                case STAGGER->{if(actionTick>=28)recover(false);}
                case ROAR->{if(actionTick>=24)recover(false);}
                default->{}
            }
            return;
        }
        if(target==null||!target.isAlive())return;
        getLookControl().lookAt(target,30,30);CombatMotion.face(this,target.getPos(),9);
        double distance=squaredDistanceTo(target);
        if(distance>14){if(beginCharge())return;if(age%6==0)getNavigation().startMovingTo(target,ModConfig.speed(1));}
        else if(canSee(target)&&Math.abs(target.getY()-getY())<=3){getNavigation().stop();if(attackCooldown==0){if(!beginHeavyAttack())start(Action.CLUB_SWING);}}
        else if(age%6==0)getNavigation().startMovingTo(target,ModConfig.speed(1));
    }
    private void applyRage(){var speed=getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);speed.removeModifier(RAGE_SPEED);if(isEnraged())speed.addTemporaryModifier(new EntityAttributeModifier(RAGE_SPEED,.2,EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));}
    @Override public boolean damage(DamageSource source,float amount){boolean hit=super.damage(source,vulnerableTicks>0?amount*1.25f:amount);if(hit&&isAlive()&&action==Action.NONE)triggerAnim("action","hurt");return hit;}
    @Override public void onDeath(DamageSource source){action=Action.NONE;super.onDeath(source);}
    @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putBoolean("BonecrusherRage",isEnraged());n.putInt("HeavyCooldown",heavyCooldown);n.putInt("ChargeCooldown",chargeCooldown);}
    @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);dataTracker.set(RAGE,n.getBoolean("BonecrusherRage"));applyRage();heavyCooldown=Math.clamp(n.getInt("HeavyCooldown"),0,320);chargeCooldown=Math.clamp(n.getInt("ChargeCooldown"),0,400);action=Action.NONE;attackCooldown=20;}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){
        c.add(new AnimationController<>(this,"movement",4,s->{s.setControllerSpeed(isEnraged()?1.2f:1);return s.setAndContinue(RawAnimation.begin().thenLoop("animation.bonecrusher."+(!isAlive()?"death":s.isMoving()?isEnraged()?"run":"walk":"idle")));}));
        var action=new AnimationController<>(this,"action",0,s->{s.setControllerSpeed(isEnraged()?1.15f:1);return PlayState.STOP;});for(String name:new String[]{"club_swing","overhead_smash","ground_slam","charge","roar","stagger","hurt"})action.triggerableAnim(name,RawAnimation.begin().thenPlay("animation.bonecrusher."+name));c.add(action);
    }
    public static DefaultAttributeContainer.Builder createAttributes(){return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,120).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,12).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,.30).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,.85).add(EntityAttributes.GENERIC_FOLLOW_RANGE,40).add(EntityAttributes.GENERIC_STEP_HEIGHT,1);}
}
