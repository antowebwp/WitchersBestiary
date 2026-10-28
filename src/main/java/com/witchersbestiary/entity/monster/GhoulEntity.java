package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.CategorizedMonster;
import com.witchersbestiary.combat.MonsterCategory;
import com.witchersbestiary.entity.ai.GhoulLeapGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Fast, hunched necrophage with deliberate melee and leap attack cooldowns. */
public final class GhoulEntity extends HostileEntity implements com.witchersbestiary.entity.ai.LocalAggression.Member,GeoEntity, CategorizedMonster, com.witchersbestiary.combat.Stunnable {
    private static final net.minecraft.entity.data.TrackedData<Boolean> STUN_FLAG=net.minecraft.entity.data.DataTracker.registerData(GhoulEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    @Override protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder){super.initDataTracker(builder);builder.add(STUN_FLAG,false);}
    @Override public boolean trackedStun(){return dataTracker.get(STUN_FLAG);}
    @Override public void setTrackedStun(boolean value){dataTracker.set(STUN_FLAG,value);}

    public com.witchersbestiary.contracts.procedural.ProceduralCombat procedural;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ghoul.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.ghoul.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.ghoul.run");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.ghoul.death");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public java.util.UUID huntContract;
    public int huntLeap, huntRage, huntFeed;
    public boolean huntRageUsed;
    public float huntHealed;
    private int leapCooldown;
    private boolean pouncing;
    public void preparePounce(){pouncing=true;leapCooldown=com.witchersbestiary.config.ModConfig.abilityTicks(55+random.nextInt(21));triggerAnim("action","leap");}
    public void finishPounce(){pouncing=false;}

    public GhoulEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        experiencePoints = 5;
    }

    private com.witchersbestiary.entity.ai.ResponsiveMeleeGoal melee;
    @Override public com.witchersbestiary.combat.CombatState getCombatState(){return com.witchersbestiary.combat.Stun.active(this)?com.witchersbestiary.combat.CombatState.STUNNED:pouncing?com.witchersbestiary.combat.CombatState.SPECIAL_ABILITY:melee==null?com.witchersbestiary.combat.CombatState.IDLE:melee.state();}
    @Override public void cancelCombatForStun(){pouncing=false;if(melee!=null)melee.stop();}
    @Override protected void initGoals() {
        goalSelector.add(0, new com.witchersbestiary.entity.ai.GravePounceGoal(this));
        goalSelector.add(1, new GhoulLeapGoal(this));
        goalSelector.add(2, melee=new com.witchersbestiary.entity.ai.ResponsiveMeleeGoal(this,true));
        goalSelector.add(7, new WanderAroundFarGoal(this, .9));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 12));
        goalSelector.add(9, new LookAroundGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this,PlayerEntity.class,0,true,false,null));
    }

    @Override public void tick() {
        super.tick();com.witchersbestiary.entity.ai.LocalAggression.tick(this);
        if (leapCooldown > 0) leapCooldown--;
        com.witchersbestiary.contracts.hunt.GraveDevourer.tick(this);
    }

    public boolean canLeap() { return (huntContract==null||procedural!=null&&!procedural.profile.has(com.witchersbestiary.contracts.procedural.ContractAbility.CHARGE)) && !com.witchersbestiary.combat.Stun.active(this) && leapCooldown == 0 && isOnGround() && getTarget() != null; }

    @Override public MonsterCategory getMonsterCategory() { return MonsterCategory.NECROPHAGE; }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state -> {
            if (getHealth() <= 0) return state.setAndContinue(DEATH);
            if (!state.isMoving()) return state.setAndContinue(IDLE);
            return state.setAndContinue(getVelocity().horizontalLengthSquared() > .035 ? RUN : WALK);
        }));
        AnimationController<GhoulEntity> action = new AnimationController<>(this, "action", 0,
                state -> PlayState.STOP);
        action.triggerableAnim("attack", RawAnimation.begin().thenPlay("animation.ghoul.attack"));
        action.triggerableAnim("leap", RawAnimation.begin().thenPlay("animation.ghoul.leap"));
        action.triggerableAnim("hurt", RawAnimation.begin().thenPlay("animation.ghoul.hurt"));
        controllers.add(action);
    }


    @Override public boolean damage(DamageSource source, float amount) {
        boolean hit = super.damage(source, amount);if(hit&&source.getAttacker() instanceof PlayerEntity p)com.witchersbestiary.entity.ai.LocalAggression.alert(this,p);
        if (hit && !getWorld().isClient && isAlive() && (melee==null || melee.state()==com.witchersbestiary.combat.CombatState.IDLE || melee.state()==com.witchersbestiary.combat.CombatState.CHASE)) triggerAnim("action", "hurt");
        return hit;
    }

    @Override public boolean tryAttack(net.minecraft.entity.Entity victim){
        float before=victim instanceof net.minecraft.entity.LivingEntity l?l.getHealth()+l.getAbsorptionAmount():0;
        boolean hit=super.tryAttack(victim);
        if(hit&&victim instanceof net.minecraft.entity.LivingEntity l&&l.getHealth()+l.getAbsorptionAmount()<before){com.witchersbestiary.contracts.hunt.GraveDevourer.feed(this);com.witchersbestiary.contracts.procedural.ProceduralHunts.hit(this,l);}
        return hit;
    }
    @Override public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound n){super.writeCustomDataToNbt(n);if(procedural!=null)n.put("WbProcedural",procedural.write());com.witchersbestiary.contracts.hunt.GraveDevourer.write(this,n);}
    @Override public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound n){super.readCustomDataFromNbt(n);procedural=n.contains("WbProcedural")?com.witchersbestiary.contracts.procedural.ProceduralCombat.read(n.getCompound("WbProcedural")):null;com.witchersbestiary.contracts.hunt.GraveDevourer.read(this,n);}

    public static DefaultAttributeContainer.Builder createGhoulAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, com.witchersbestiary.config.ModConfig.speed(.40))
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.0);
    }

    private java.util.UUID sharedTarget;
    public java.util.UUID sharedGroupTarget(){return sharedTarget;}
    public void sharedGroupTarget(java.util.UUID target){sharedTarget=target;}
    @Override public int getLimitPerChunk(){return com.witchersbestiary.config.ModConfig.groupMaximum("ghoul",4);}
}
