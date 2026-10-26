package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.*;
import com.witchersbestiary.entity.ai.CombatMotion;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.EnumSet;

/** Ground mini-boss; movement uses vanilla navigation/collision, damage uses discrete windows. */
public final class WerewolfEntity extends HostileEntity implements GeoEntity, CategorizedMonster, InterruptibleAttack, Stunnable {
    private static final net.minecraft.entity.data.TrackedData<Boolean> STUN_FLAG=net.minecraft.entity.data.DataTracker.registerData(WerewolfEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    @Override public boolean trackedStun(){return dataTracker.get(STUN_FLAG);}
    @Override public void setTrackedStun(boolean value){dataTracker.set(STUN_FLAG,value);}

    public enum Attack { NONE, CLAWS, BITE, LEAP, CHARGE, DODGE, HOWL, RAGE }
    private static final TrackedData<Integer> PHASE=DataTracker.registerData(WerewolfEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> HUMAN=DataTracker.registerData(WerewolfEntity.class,TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> TRANSFORM=DataTracker.registerData(WerewolfEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private boolean transformTarget;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private Attack attack=Attack.NONE;
    private int actionAge,combatCooldown,biteCooldown,leapCooldown,chargeCooldown,dodgeCooldown,interruptCooldown;
    private int stunTicks,recoveryTicks,rageTicks,rageHeals;
    private Vec3d direction=Vec3d.ZERO;
    private boolean rushHit;
    public WerewolfEntity(EntityType<? extends HostileEntity> type,World world){super(type,world);experiencePoints=30;applyForm(shouldBeHuman(world));}
    @Override protected void initDataTracker(DataTracker.Builder builder){super.initDataTracker(builder);builder.add(STUN_FLAG,false);builder.add(PHASE,1);builder.add(HUMAN,false);builder.add(TRANSFORM,0);}
    @Override protected void initGoals(){
        goalSelector.add(1,new Goal(){
            private int repath;
            {setControls(EnumSet.of(Control.MOVE,Control.LOOK));}
            @Override public boolean shouldRunEveryTick(){return true;}
            @Override public boolean canStart(){return !isHumanForm()&&!isTransforming()&&getTarget()!=null&&getTarget().isAlive();}
            @Override public boolean shouldContinue(){return canStart();}
            @Override public void stop(){getNavigation().stop();}
            @Override public void tick(){
                var target=getTarget();if(target==null)return;
                getLookControl().lookAt(target,30,30);
                if(attack!=Attack.NONE||stunTicks>0||Stun.active(WerewolfEntity.this)||recoveryTicks>0){getNavigation().stop();return;}
                double d=squaredDistanceTo(target);
                if(combatCooldown==0 && canSee(target) && d<7){startAttack(random.nextFloat()<.25f&&biteCooldown==0?Attack.BITE:Attack.CLAWS);return;}
                if(combatCooldown==0 && canSee(target) && isOnGround() && d>=16&&d<=81){
                    if(d>=36&&random.nextBoolean()&&leapCooldown==0&&startAttack(Attack.LEAP))return;
                    if(chargeCooldown==0&&startAttack(Attack.CHARGE))return;
                }
                if(--repath<=0){repath=6;getNavigation().startMovingTo(target,getPhase()==1?1.1:1.3);}
            }
        });
        goalSelector.add(7,new WanderAroundFarGoal(this,.8));
        goalSelector.add(8,new LookAtEntityGoal(this,PlayerEntity.class,16));
        targetSelector.add(1,new ActiveTargetGoal<>(this,PlayerEntity.class,0,true,false,null));
    }
    private static boolean shouldBeHuman(World world){long time=Math.floorMod(world.getTimeOfDay(),24000);return world.getDimension().hasSkyLight()&&!world.getDimension().hasFixedTime()&&(time<13000||time>=23000);}
    public boolean isHumanForm(){return dataTracker.get(HUMAN);}
    public boolean isTransforming(){return dataTracker.get(TRANSFORM)>0;}
    private void applyForm(boolean human){
        float fraction=getMaxHealth()>0?getHealth()/getMaxHealth():1;
        dataTracker.set(HUMAN,human);getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(human?20:120);com.witchersbestiary.combat.ProgressiveDifficulty.refresh(this);
        getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(human?.22:com.witchersbestiary.config.ModConfig.speed(.43));
        getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(human?0:9);setHealth(Math.clamp(getMaxHealth()*fraction,0f,getMaxHealth()));
        if(human){setTarget(null);removeStatusEffect(StatusEffects.SPEED);}calculateDimensions();
    }
    private void tickForm(){
        if(isTransforming()){
            int remaining=dataTracker.get(TRANSFORM)-1;dataTracker.set(TRANSFORM,remaining);getNavigation().stop();setVelocity(0,getVelocity().y,0);
            if(remaining==0)applyForm(transformTarget);return;
        }
        if(age==1||age%20==0){boolean day=shouldBeHuman(getWorld());
            if(day!=isHumanForm()){
                transformTarget=day;dataTracker.set(TRANSFORM,30);attack=Attack.NONE;actionAge=0;getNavigation().stop();setTarget(null);
                triggerAnim("action","transform");playSound(SoundEvents.ENTITY_WOLF_HOWL,1,.7f);
                ((net.minecraft.server.world.ServerWorld)getWorld()).spawnParticles(net.minecraft.particle.ParticleTypes.POOF,getX(),getY()+1,getZ(),18,.4,.8,.4,.02);
            }
        }
    }
    @Override public boolean canTarget(net.minecraft.entity.LivingEntity target){return !isHumanForm()&&!isTransforming()&&super.canTarget(target);}
    @Override public EntityDimensions getBaseDimensions(EntityPose pose){return isHumanForm()?EntityDimensions.changing(.6f,1.95f):super.getBaseDimensions(pose);}
    @Override public void onTrackedDataSet(TrackedData<?> data){super.onTrackedDataSet(data);if(HUMAN.equals(data))calculateDimensions();}
    public int getPhase(){return dataTracker.get(PHASE);}
    public Attack getAttack(){return attack;}
    public boolean isRecovering(){return recoveryTicks>0;}
    public boolean isStunned(){return stunTicks>0;}
    public int getRageHeals(){return rageHeals;}
    private boolean raging(){return getPhase()==3&&getHealth()<30;}
    @Override public CombatState getCombatState(){if(isTransforming()||attack==Attack.HOWL||attack==Attack.RAGE)return CombatState.PHASE_TRANSITION;if(isStunned()||Stun.active(this))return CombatState.STUNNED;if(attack==Attack.CLAWS)return actionAge<6?CombatState.ATTACK_WINDUP:actionAge==6||actionAge==16||getPhase()>=2&&actionAge==26?CombatState.ATTACK_ACTIVE:CombatState.ATTACK_RECOVERY;if(attack!=Attack.NONE)return CombatState.SPECIAL_ABILITY;if(isRecovering())return CombatState.ATTACK_RECOVERY;return getTarget()==null?CombatState.IDLE:CombatState.CHASE;}
    @Override public void cancelCombatForStun(){attack=Attack.NONE;actionAge=0;combatCooldown=Math.max(combatCooldown,8);}
    @Override public void tick(){
        super.tick();if(getWorld().isClient)return;
        if(!isAlive()){attack=Attack.NONE;rageTicks=0;return;}
        if(Stun.active(this)){if(stunTicks>0)stunTicks--;if(combatCooldown>0)combatCooldown--;cancelCombatForStun();return;}
        tickForm();if(isHumanForm()||isTransforming())return;
        if(combatCooldown>0)combatCooldown--;if(biteCooldown>0)biteCooldown--;if(leapCooldown>0)leapCooldown--;
        if(chargeCooldown>0)chargeCooldown--;if(dodgeCooldown>0)dodgeCooldown--;if(interruptCooldown>0)interruptCooldown--;
        if(recoveryTicks>0)recoveryTicks--;
        if(stunTicks>0){stunTicks--;getNavigation().stop();setVelocity(0,getVelocity().y,0);return;}
        if(getPhase()<2&&getHealth()<=72)dataTracker.set(PHASE,2);
        if(getPhase()<3&&getHealth()<30){dataTracker.set(PHASE,3);rageTicks=240;rageHeals=0;begin(Attack.HOWL);addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,80,0));}
        if(rageTicks>0){
            if(getHealth()>=30){rageTicks=0;removeStatusEffect(StatusEffects.SPEED);}
            else if(--rageTicks%60==0&&rageHeals<4){heal(1);rageHeals++;}
        }
        tickAttack();
    }
    public boolean startAttack(Attack next){
        if(getWorld().isClient||isHumanForm()||isTransforming()||!isAlive()||attack!=Attack.NONE||combatCooldown>0||stunTicks>0||Stun.active(this)||recoveryTicks>0)return false;
        if(getTarget()==null||!getTarget().isAlive())return false;
        if(next==Attack.BITE&&biteCooldown>0 || next==Attack.LEAP&&(leapCooldown>0||!isOnGround())
                ||next==Attack.CHARGE&&(chargeCooldown>0||!isOnGround())||next==Attack.DODGE&&dodgeCooldown>0)return false;
        if(next==Attack.NONE||next==Attack.HOWL||next==Attack.RAGE)return false;
        direction=getTarget().getPos().subtract(getPos()).multiply(1,0,1).normalize();
        if(next==Attack.DODGE){
            Vec3d side=new Vec3d(-direction.z,0,direction.x).multiply(random.nextBoolean()?1:-1);
            Vec3d end=getPos().add(side.multiply(2));var feet=BlockPos.ofFloored(end);
            if(!getWorld().isSpaceEmpty(this,getBoundingBox().offset(end.subtract(getPos())))||getWorld().containsFluid(getBoundingBox().offset(end.subtract(getPos())))
                    ||!getWorld().getBlockState(feet.down()).isSolidBlock(getWorld(),feet.down()))return false;
            direction=side;dodgeCooldown=com.witchersbestiary.config.ModConfig.abilityTicks(70);
        }
        if(next==Attack.BITE)biteCooldown=com.witchersbestiary.config.ModConfig.abilityTicks(60);if(next==Attack.LEAP)leapCooldown=com.witchersbestiary.config.ModConfig.abilityTicks(100);if(next==Attack.CHARGE)chargeCooldown=com.witchersbestiary.config.ModConfig.abilityTicks(90);
        begin(next);return true;
    }
    private void begin(Attack next){
        attack=next;actionAge=0;rushHit=false;getNavigation().stop();
        triggerAnim("action",switch(next){case CLAWS->getPhase()>=2?"claw_combo":"claw_attack";default->next.name().toLowerCase(java.util.Locale.ROOT);});
        if(next==Attack.LEAP||next==Attack.CHARGE)playSound(SoundEvents.ENTITY_WOLF_GROWL,1,.65f);
        if(next==Attack.HOWL)playSound(SoundEvents.ENTITY_WOLF_HOWL,2,.65f);
    }
    private void tickAttack(){
        if(attack==Attack.NONE)return;
        actionAge++;
        if(attack==Attack.HOWL){if(actionAge>=22)begin(Attack.RAGE);return;}
        if(attack==Attack.RAGE){if(actionAge>=10)finish(false);return;}
        var target=getTarget();if(target==null||!target.isAlive()){finish(false);return;}
        if(attack==Attack.CLAWS&&(actionAge<5||actionAge>9&&actionAge<15||actionAge>19&&actionAge<25)||attack==Attack.BITE&&actionAge<7){
            direction=CombatMotion.face(this,target.getPos(),20);
            if(CombatMotion.distance(this,target)>2)CombatMotion.safeStep(this,direction.multiply(.16),true);
        }
        switch(attack){
            case CLAWS->{if(actionAge==6)hit(9,2.7);if(actionAge==16)hit(11,2.7);
                if(getPhase()>=2&&actionAge==26)hit(10,2.7);if(actionAge>=(getPhase()>=2?34:24))finish(false);}
            case BITE->{if(actionAge==10&&hit(13,2.1))target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,35,0));if(actionAge>=24)finish(false);}
            case LEAP->{
                if(actionAge==1){setVelocity(direction.multiply(-.22).add(0,.16,0));velocityModified=true;}
                if(actionAge==14){setVelocity(direction.multiply(.65).add(0,.45,0));velocityModified=true;}
                if(actionAge>=18&&actionAge<=32&&!rushHit)rushHit=leapContact();
                if(actionAge>=36)finish(!rushHit);
            }
            case CHARGE->{
                if(actionAge>10){if(!CombatMotion.safeStep(this,direction.multiply(.75),true)){finish(!rushHit);break;}
                    setVelocity(direction.x*.75,getVelocity().y,direction.z*.75);velocityModified=true;
                    if(!rushHit)rushHit=hit(13,2.1);}
                if(actionAge>=26)finish(!rushHit);
            }
            case DODGE->{if(horizontalCollision||actionAge>=7)finish(false);else{setVelocity(direction.x*.4,getVelocity().y,direction.z*.4);velocityModified=true;}}
            default->{}
        }
    }
    private boolean leapContact(){
        var target=getTarget();if(target==null||!canSee(target))return false;
        var delta=target.getPos().subtract(getPos()).multiply(1,0,1);
        if(delta.lengthSquared()>2.2*2.2||direction.dotProduct(delta.normalize())<.1)return false;
        // A falling predator's body can overlap the player's upper body while their feet are far apart.
        var box=getBoundingBox();var victim=target.getBoundingBox();
        if(victim.maxY<box.minY-.4||victim.minY>box.maxY+.4)return false;
        return target.damage(getDamageSources().mobAttack(this),14*(raging()?1.2f:1));
    }
    private boolean hit(float damage,double reach){
        var target=getTarget();if(target==null||squaredDistanceTo(target)>reach*reach||!canSee(target))return false;
        Vec3d delta=target.getPos().subtract(getPos()).multiply(1,0,1).normalize();
        if(direction.dotProduct(delta)<.1)return false;
        return target.damage(getDamageSources().mobAttack(this),damage*(raging()?1.2f:1));
    }
    private void finish(boolean missed){
        boolean counter=attack==Attack.DODGE;attack=Attack.NONE;combatCooldown=counter?2:com.witchersbestiary.config.ModConfig.attackTicks(missed?16:getPhase()==1?8:getPhase()==2?6:4);
        if(missed)recoveryTicks=20;setVelocity(0,getVelocity().y,0);
    }
    @Override public boolean canInterruptAttack(){return isAlive()&&interruptCooldown==0&&(attack==Attack.LEAP&&actionAge<14||attack==Attack.CHARGE&&actionAge<=10);}
    @Override public void interruptAttack(){
        if(!canInterruptAttack())return;attack=Attack.NONE;stunTicks=18;Stun.apply(this,18);interruptCooldown=160;combatCooldown=30;
        getNavigation().stop();setVelocity(0,getVelocity().y,0);triggerAnim("action","hurt");
    }
    @Override public boolean damage(DamageSource source,float amount){
        float before=getHealth();boolean hit=super.damage(source,recoveryTicks>0?amount*1.25f:amount);
        if(hit&&isAlive()&&!getWorld().isClient){
            boolean interrupted=AttackInterruptions.attempt(this,source,before-getHealth());
            if(!interrupted&&attack==Attack.NONE&&source.getAttacker() instanceof PlayerEntity&&random.nextFloat()<.12f)startAttack(Attack.DODGE);
            if(!interrupted&&attack==Attack.NONE)triggerAnim("action","hurt");
        }return hit;
    }
    @Override public MonsterCategory getMonsterCategory(){return MonsterCategory.CURSED;}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar registrar){
        registrar.add(new AnimationController<>(this,"movement",4,state->state.setAndContinue(!isAlive()?RawAnimation.begin().thenPlayAndHold("animation.werewolf.death"):
                RawAnimation.begin().thenLoop("animation.werewolf."+(!state.isMoving()?"idle":getVelocity().horizontalLengthSquared()>.035?"run":"walk")))));
        var action=new AnimationController<>(this,"action",0,state->PlayState.STOP);
        for(String name:new String[]{"claw_attack","claw_combo","bite","leap","charge","dodge","howl","rage","hurt","transform"})action.triggerableAnim(name,RawAnimation.begin().thenPlay("animation.werewolf."+name));
        registrar.add(action);
    }
    @Override public void writeCustomDataToNbt(NbtCompound n){
        super.writeCustomDataToNbt(n);n.putBoolean("HumanForm",isHumanForm());n.putInt("TransformTicks",dataTracker.get(TRANSFORM));n.putBoolean("TransformTarget",transformTarget);n.putInt("WerewolfPhase",getPhase());n.putInt("RageTicks",rageTicks);n.putInt("RageHeals",rageHeals);
        n.putInt("BiteCooldown",biteCooldown);n.putInt("LeapCooldown",leapCooldown);n.putInt("ChargeCooldown",chargeCooldown);
        n.putInt("DodgeCooldown",dodgeCooldown);n.putInt("InterruptCooldown",interruptCooldown);n.putInt("CombatCooldown",combatCooldown);
    }
    @Override public void readCustomDataFromNbt(NbtCompound n){
        super.readCustomDataFromNbt(n);applyForm(n.getBoolean("HumanForm"));dataTracker.set(TRANSFORM,MathHelper.clamp(n.getInt("TransformTicks"),0,30));transformTarget=n.getBoolean("TransformTarget");dataTracker.set(PHASE,MathHelper.clamp(n.getInt("WerewolfPhase"),1,3));
        rageTicks=MathHelper.clamp(n.getInt("RageTicks"),0,240);rageHeals=MathHelper.clamp(n.getInt("RageHeals"),0,4);
        biteCooldown=n.getInt("BiteCooldown");leapCooldown=n.getInt("LeapCooldown");chargeCooldown=n.getInt("ChargeCooldown");
        dodgeCooldown=n.getInt("DodgeCooldown");interruptCooldown=n.getInt("InterruptCooldown");combatCooldown=n.getInt("CombatCooldown");
        attack=Attack.NONE;actionAge=0;combatCooldown=Math.max(combatCooldown,20);
    }
    public static DefaultAttributeContainer.Builder createAttributes(){return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,120).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,9)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED,com.witchersbestiary.config.ModConfig.speed(.43)).add(EntityAttributes.GENERIC_FOLLOW_RANGE,40)
            .add(EntityAttributes.GENERIC_STEP_HEIGHT,1).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,.35);}
}
