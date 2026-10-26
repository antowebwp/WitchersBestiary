package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animation.*;

/** Ordinary specter shares the bounded pursuit/copy lifecycle, but has its own warned abilities. */
public final class WraithEntity extends NoonwraithEntity {
    private int stepCooldown=240, stepWindup, whisperCooldown=80, whisperWindup, reactionTicks;
    private Vec3d idlePoint;
    public WraithEntity(EntityType<? extends HostileEntity> type,World world){super(type,world);experiencePoints=8;stepCooldown=200+random.nextInt(121);}
    @Override protected boolean healingRetreatEnabled(){return false;}
    @Override protected boolean ambientSparkles(){return false;}
    @Override protected boolean combatHopEnabled(){return false;}
    @Override protected int illusionLimit(){return 2;}
    @Override protected float illusionDamage(){return 2.4f;}
    @Override protected double chaseSpeed(){return .32;}
    @Override protected boolean doubleStrike(){return false;}
    @Override protected boolean combatPaused(){return stepWindup>0||whisperWindup>0||reactionTicks>0;}
    @Override public CombatState getCombatState(){return Stun.active(this)?CombatState.STUNNED:stepWindup>0||whisperWindup>0?CombatState.SPECIAL_ABILITY:super.getCombatState();}
    @Override public void cancelCombatForStun(){super.cancelCombatForStun();stepWindup=whisperWindup=0;setInvisible(false);}
    @Override protected void afterPhase(boolean completed){reactionTicks=completed?16:8;}
    @Override public boolean tryGhostDodge(){return false;}
    public int getStepCooldown(){return stepCooldown;}
    public boolean isStepping(){return isRetreating();}
    public boolean beginSpectralStep(){
        if(getWorld().isClient||isIllusion()||Stun.active(this)||!isAlive()||stepCooldown>0||combatPaused()||getTarget()==null||!getTarget().isAlive())return false;
        if(!beginPhase(false))return false;
        stepCooldown=280+random.nextInt(121);return true;
    }
    @Override public boolean scream(){
        if(getWorld().isClient||isIllusion()||Stun.active(this)||whisperCooldown>0||isRetreating()||combatPaused()||getTarget()==null||!getTarget().isAlive())return false;
        whisperCooldown=400+random.nextInt(101);whisperWindup=12;suspendAttack(18);
        playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,.45f,.7f);triggerAnim("action","scream");cue(8);return true;
    }
    private void cue(int count){if(getWorld() instanceof ServerWorld w)w.spawnParticles(ParticleTypes.SMOKE,getX(),getY()+1,getZ(),count,.35,.5,.35,.015);}
    @Override public void tick(){
        super.tick();if(!(getWorld() instanceof ServerWorld world))return;
        if(!isAlive()){stepWindup=whisperWindup=0;setInvisible(false);return;}
        if(isIllusion())return;
        if(stepCooldown>0)stepCooldown--;if(whisperCooldown>0)whisperCooldown--;if(reactionTicks>0)reactionTicks--;
        if(Stun.active(this)||isAiDisabled())return;
        if(isRetreating())return;
        if(whisperWindup>0){if(--whisperWindup==0){var target=getTarget();if(target!=null&&target.isAlive()&&squaredDistanceTo(target)<=49&&canSee(target))target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS,30,0));}return;}
        var target=getTarget();if(target!=null&&target.isAlive()){if(stepCooldown==0&&squaredDistanceTo(target)<256)beginSpectralStep();}
        else{
            if(age%100==0){idlePoint=getPos().add(random.nextDouble()*6-3,0,random.nextDouble()*6-3);if(!safeDestination(idlePoint))idlePoint=null;}
            if(idlePoint!=null&&age%20==0&&getNavigation().isIdle())getNavigation().startMovingTo(idlePoint.x,idlePoint.y,idlePoint.z,.7);
            if(age%180==0)playSound(SoundEvents.ENTITY_PHANTOM_AMBIENT,.35f,.65f);
        }
    }
    @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putInt("SpectralStepCooldown",stepCooldown);n.putInt("WhisperCooldown",whisperCooldown);}
    @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);stepCooldown=Math.clamp(n.getInt("SpectralStepCooldown"),0,400);whisperCooldown=Math.clamp(n.getInt("WhisperCooldown"),0,500);stepWindup=whisperWindup=0;reactionTicks=8;setInvisible(false);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new AnimationController<>(this,"movement",3,state->state.setAndContinue(RawAnimation.begin().thenLoop("animation.wraith."+(!isAlive()?"death":state.isMoving()?getTarget()!=null?"chase":"float":"idle")))));
        var action=new AnimationController<>(this,"action",0,state->PlayState.STOP);
        for(String name:new String[]{"attack","teleport","summon_illusions","scream","hurt","vanish","appear"})action.triggerableAnim(name,RawAnimation.begin().thenPlay("animation.wraith."+name));controllers.add(action);
    }
    public static DefaultAttributeContainer.Builder createWraithAttributes(){return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,45).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,6).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,.32).add(EntityAttributes.GENERIC_FOLLOW_RANGE,32).add(EntityAttributes.GENERIC_FLYING_SPEED,com.witchersbestiary.config.ModConfig.speed(.22));}
    @Override public int getLimitPerChunk(){return com.witchersbestiary.config.ModConfig.groupMaximum("wraith",3);}
}
