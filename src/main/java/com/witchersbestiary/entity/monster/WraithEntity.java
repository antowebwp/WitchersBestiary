package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.*;
import com.witchersbestiary.signs.SignManager;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
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
public final class WraithEntity extends NoonwraithEntity implements Ownable {
    private int stepCooldown=240, stepWindup, whisperCooldown=80, whisperWindup, reactionTicks;
    private Vec3d idlePoint;
    private UUID summoner;
    private long summonExpiry;

    public void bindSummon(ServerPlayerEntity owner, long expiry) {
        summoner = owner.getUuid();
        summonExpiry = expiry;
        experiencePoints = 0;
        targetSelector.clear(goal -> true);
        getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(30);
        getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(7);
        setHealth(30);
        setPersistent();
    }

    public UUID getOwnerUuid() { return summoner; }
    public boolean isSummoned() { return summoner != null; }

    @Override public ServerPlayerEntity getOwner() {
        return summoner != null && getWorld() instanceof ServerWorld world
                ? world.getServer().getPlayerManager().getPlayer(summoner) : null;
    }

    @Override public boolean shouldSave() { return summoner == null && super.shouldSave(); }
    @Override protected boolean isDisallowedInPeaceful() { return summoner == null && super.isDisallowedInPeaceful(); }

    public boolean permitsAttack(LivingEntity target) {
        if (summoner == null) return true;
        var owner = getOwner();
        return owner != null && owner.getWorld() == getWorld() && SignManager.allowed(owner)
                && getWorld().getTime() < summonExpiry && !(target instanceof net.minecraft.entity.player.PlayerEntity)
                && !(target instanceof WraithEntity wraith && wraith.summoner != null)
                && SignManager.enemy(owner, target);
    }

    @Override public void setTarget(LivingEntity target) {
        if (target == null || permitsAttack(target)) super.setTarget(target);
    }

    @Override public int summonIllusions() { return summoner == null ? super.summonIllusions() : 0; }
    @Override protected void dropLoot(DamageSource source, boolean player) {
        if (summoner == null) super.dropLoot(source, player);
    }

    @Override protected void idleFlight() {
        var owner = getOwner();
        if (owner == null || squaredDistanceTo(owner) <= 9) super.idleFlight();
        else if (age % 10 == 0) pursueFlight(owner);
    }

    private boolean prepareSummonTick(ServerWorld world) {
        var owner = getOwner();
        if (owner == null || owner.getWorld() != world || !SignManager.allowed(owner)
                || world.getTime() >= summonExpiry || squaredDistanceTo(owner) > 48 * 48) {
            discard();
            return false;
        }
        var target = getTarget();
        if (target != null && (!permitsAttack(target) || target.squaredDistanceTo(owner) > 18 * 18)) {
            setTarget(null);
        }
        if (isAlive() && age % 10 == 0 && getTarget() == null) {
            var nearby = world.getEntitiesByClass(HostileEntity.class, getBoundingBox().expand(12),
                    mob -> permitsAttack(mob) && mob.squaredDistanceTo(owner) <= 18 * 18 && canSee(mob));
            nearby.stream().min(Comparator.comparingDouble(this::squaredDistanceTo)).ifPresent(this::setTarget);
        }
        return true;
    }
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
        if(summoner!=null||getWorld().isClient||isIllusion()||com.witchersbestiary.alchemy.MoonDust.active(this)||Stun.active(this)||!isAlive()||stepCooldown>0||combatPaused()||getTarget()==null||!getTarget().isAlive())return false;
        if(!beginPhase(false))return false;
        stepCooldown=280+random.nextInt(121);return true;
    }
    @Override public boolean scream(){
        if(summoner!=null||getWorld().isClient||isIllusion()||com.witchersbestiary.alchemy.MoonDust.active(this)||Stun.active(this)||whisperCooldown>0||isRetreating()||combatPaused()||getTarget()==null||!getTarget().isAlive())return false;
        whisperCooldown=400+random.nextInt(101);whisperWindup=12;suspendAttack(18);
        playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,.45f,.7f);triggerAnim("action","scream");cue(8);return true;
    }
    private void cue(int count){if(getWorld() instanceof ServerWorld w)w.spawnParticles(ParticleTypes.SMOKE,getX(),getY()+1,getZ(),count,.35,.5,.35,.015);}
    @Override public void tick(){
        if (summoner != null && getWorld() instanceof ServerWorld serverWorld && !prepareSummonTick(serverWorld)) return;
        super.tick();if(!(getWorld() instanceof ServerWorld world))return;
        if (summoner != null) {
            if (isAlive() && age % 10 == 0)
                world.spawnParticles(ParticleTypes.SOUL, getX(), getBodyY(.5), getZ(), 2, .2, .35, .2, .01);
            return;
        }
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
