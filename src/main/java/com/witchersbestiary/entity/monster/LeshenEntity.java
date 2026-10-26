package com.witchersbestiary.entity.monster;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import com.witchersbestiary.entity.ai.LeshenMeleeGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import com.witchersbestiary.entity.ai.LeshenSummons;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import com.witchersbestiary.combat.CategorizedMonster;
import com.witchersbestiary.combat.CombatState;
import com.witchersbestiary.combat.Stun;
import com.witchersbestiary.combat.MonsterCategory;

public class LeshenEntity extends HostileEntity implements GeoEntity, CategorizedMonster, com.witchersbestiary.combat.Stunnable {
    private static final net.minecraft.entity.data.TrackedData<Boolean> STUN_FLAG=net.minecraft.entity.data.DataTracker.registerData(LeshenEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    @Override public boolean trackedStun(){return dataTracker.get(STUN_FLAG);}
    @Override public void setTrackedStun(boolean value){dataTracker.set(STUN_FLAG,value);}

    @Override public MonsterCategory getMonsterCategory() { return MonsterCategory.RELICT; }
    private static final TrackedData<Boolean> SECOND_PHASE = DataTracker.registerData(LeshenEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossBar bossBar = new ServerBossBar(getDisplayName(), BossBar.Color.GREEN, BossBar.Style.PROGRESS);
    private boolean clawContact;
    private int clawTicks;
    private int clawCooldown;
    private UUID clawTarget;
    private Vec3d clawDirection=Vec3d.ZERO;
    private int rootTicks;
    private int rootCooldown = 100;
    private Vec3d rootPosition;
    private Vec3d rootOrigin;
    private final LeshenSummons summons = new LeshenSummons();
    private int summonTicks;
    private int summonCooldown = 200;
    private int roarTicks;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.leshen.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.leshen.walk");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.leshen.death");

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state -> {
            state.getController().setAnimationSpeed(isSecondPhase() ? 1.2 : 1);
            return state.setAndContinue(isDead() ? DEATH : state.isMoving() ? WALK : IDLE);
        }));
        AnimationController<LeshenEntity> actions = new AnimationController<>(this, "action", 0, state -> {state.getController().setAnimationSpeed(isSecondPhase()?1.2:1);return PlayState.STOP;});
        for (String action : new String[]{"attack_claw", "attack_roots", "summon_wolves", "hurt", "roar"}) {
            actions.triggerableAnim(action, RawAnimation.begin().thenPlay("animation.leshen." + action));
        }
        controllers.add(actions);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }

    // Сервер задаёт момент удара; анимация клиента не управляет уроном.
    @Override public boolean tryAttack(Entity target) {
        if (getWorld().isClient || !isAlive() || com.witchersbestiary.combat.Stun.active(this) || roarTicks > 0 || clawTicks > 0 || rootTicks > 0 || summonTicks > 0 || clawCooldown > 0
                || !(target instanceof LivingEntity victim) || !victim.isAlive()
                || !canTarget(victim) || !isInAttackRange(victim) || !canSee(victim)) return false;
        clawTarget = victim.getUuid();
        clawDirection=Vec3d.fromPolar(0,getYaw());
        clawTicks = 16;clawContact=false;
        clawCooldown = com.witchersbestiary.config.ModConfig.attackTicks(isSecondPhase() ? 18 : 26);
        triggerAnim("action", "attack_claw");
        return true;
    }

    @Override public CombatState getCombatState(){if(Stun.active(this))return CombatState.STUNNED;if(roarTicks>0)return CombatState.PHASE_TRANSITION;if(rootTicks>0||summonTicks>0)return CombatState.SPECIAL_ABILITY;if(clawTicks>0)return clawTicks>10?CombatState.ATTACK_WINDUP:clawTicks==10?CombatState.ATTACK_ACTIVE:CombatState.ATTACK_RECOVERY;return getTarget()==null?CombatState.IDLE:CombatState.CHASE;}
    @Override public void cancelCombatForStun(){clawTicks=0;clawTarget=null;rootTicks=0;summonTicks=0;}
    @Override public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld world)) return;
        bossBar.setPercent(Math.max(0, getHealth() / getMaxHealth()));
        bossBar.setName(getDisplayName());
        if (!isAlive()) { clawTicks = 0; clawTarget = null; rootTicks = 0; summonTicks = 0; return; }
        if(com.witchersbestiary.combat.Stun.active(this)){cancelCombatForStun();return;}
        if (!isSecondPhase() && getHealth() < getMaxHealth() * 0.4f) {
            dataTracker.set(SECOND_PHASE, true);
            getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(com.witchersbestiary.config.ModConfig.speed(.36));
            clawTicks = 0;
            clawTarget = null;
            rootTicks = 0;
            summonTicks = 0;
            clawCooldown = Math.min(clawCooldown, 24);
            rootCooldown = Math.min(rootCooldown, 60);
            summonCooldown = Math.min(summonCooldown, 100);
            roarTicks = 30;
            bossBar.setColor(BossBar.Color.RED);
            triggerAnim("action", "roar");
            world.spawnParticles(ParticleTypes.SOUL, getX(), getY() + 2, getZ(), 60, 1.3, 1.5, 1.3, 0.08);
        }
        if (roarTicks > 0) { roarTicks--; getNavigation().stop(); return; }
        if (clawCooldown > 0) clawCooldown--;
        if (rootCooldown > 0) rootCooldown--;
        if (summonCooldown > 0) summonCooldown--;
        if (age % 20 == 0) summons.tick(world);
        if (summonTicks > 0) {
            getNavigation().stop();
            if (--summonTicks == 10) summons.summon(world, this, isSecondPhase() ? 3 : 2);
        } else if (!isAiDisabled() && age % 5 == 0 && getTarget() != null) {
            trySummonWolves();
        }
        if (rootTicks > 0) {
            getNavigation().stop();
            rootTicks--;
            if (rootTicks >= 10 && rootTicks % 4 == 0) {
                for(int i=1;i<=8;i++){Vec3d warning=rootOrigin.lerp(rootPosition,i/8.0);
                    world.spawnParticles(ParticleTypes.COMPOSTER,warning.x,warning.y+.1,warning.z,3,.35,.05,.35,0);}
            }
            if (rootTicks == 10) {
                world.spawnParticles(ParticleTypes.CRIT, rootPosition.x, rootPosition.y + 0.7,
                        rootPosition.z, 40, 1.2, 0.8, 1.2, 0.05);
                for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class,
                        new Box(rootOrigin, rootPosition).expand(.9, 1.5, .9),
                        victim -> victim != this && victim.isAlive() && canTarget(victim) && rootDistanceSquared(victim.getPos())<=.81)) {
                    if (canSee(victim) && victim.damage(getDamageSources().mobAttack(this), 8)) {
                        com.witchersbestiary.combat.RootBinding.bind(victim);
                    }
                }
            }
        } else if (!isAiDisabled() && age % 5 == 0 && getTarget() != null
                && squaredDistanceTo(getTarget()) > 16) {
            tryRootAttack(getTarget());
        }
        if (clawTicks > 0) {
            getNavigation().stop();
            clawTicks--;
            if(clawTicks>12&&world.getEntity(clawTarget) instanceof LivingEntity aiming)clawDirection=com.witchersbestiary.entity.ai.CombatMotion.face(this,aiming.getPos(),12);
            else com.witchersbestiary.entity.ai.CombatMotion.holdFacing(this,clawDirection);
            if (!clawContact && clawTicks <= 10 && clawTicks >= 8 && world.getEntity(clawTarget) instanceof LivingEntity victim
                    && victim.isAlive() && canTarget(victim) && isInAttackRange(victim) && canSee(victim)
                    && com.witchersbestiary.entity.ai.CombatMotion.inSector(this,victim,clawDirection,0)) {
                super.tryAttack(victim);clawContact=true;
            }
            if (clawTicks == 0) clawTarget = null;
        }
    }

    private double rootDistanceSquared(Vec3d position){
        Vec3d line=rootPosition.subtract(rootOrigin).multiply(1,0,1);double length=line.lengthSquared();
        double fraction=length==0?0:Math.clamp(position.subtract(rootOrigin).multiply(1,0,1).dotProduct(line)/length,0,1);
        return position.subtract(rootOrigin.add(line.multiply(fraction))).multiply(1,0,1).lengthSquared();
    }
    public boolean tryRootAttack(LivingEntity victim) {
        if (!(getWorld() instanceof ServerWorld) || !isAlive() || com.witchersbestiary.combat.Stun.active(this) || !victim.isAlive()
                || roarTicks > 0 || clawTicks > 0 || rootTicks > 0 || summonTicks > 0 || rootCooldown > 0
                || !canTarget(victim) || squaredDistanceTo(victim) > 256 || !canSee(victim)
                || !getWorld().getBlockState(victim.getBlockPos().down()).isSolidBlock(getWorld(), victim.getBlockPos().down())) return false;
        rootOrigin = getPos();
        rootPosition = victim.getPos();
        rootTicks = 30;
        rootCooldown = com.witchersbestiary.config.ModConfig.abilityTicks(isSecondPhase() ? 90 : 140);
        triggerAnim("action", "attack_roots");
        return true;
    }

    public boolean trySummonWolves() {
        if (!(getWorld() instanceof ServerWorld world) || !isAlive() || com.witchersbestiary.combat.Stun.active(this) || getTarget() == null
                || !getTarget().isAlive() || squaredDistanceTo(getTarget()) > 1600
                || roarTicks > 0 || clawTicks > 0 || rootTicks > 0 || summonTicks > 0 || summonCooldown > 0) return false;
        summons.tick(world);
        if (summons.size() >= LeshenSummons.MAX_WOLVES) return false;
        summonTicks = 30;
        summonCooldown = com.witchersbestiary.config.ModConfig.abilityTicks(isSecondPhase() ? 260 : 400);
        triggerAnim("action", "summon_wolves");
        return true;
    }

    public int getSummonedWolfCount() { return summons.size(); }

    @Override public void onDeath(DamageSource source) {
        if (getWorld() instanceof ServerWorld world) summons.clear(world);
        bossBar.clearPlayers();
        super.onDeath(source);
    }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("ClawCooldown", clawCooldown);
        nbt.putInt("RootCooldown", rootCooldown);
        nbt.putInt("SummonCooldown", summonCooldown);
        nbt.putBoolean("SecondPhase", isSecondPhase());
        summons.writeNbt(nbt);
    }

    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        clawCooldown = Math.clamp(nbt.getInt("ClawCooldown"), 0, 40);
        rootCooldown = Math.clamp(nbt.getInt("RootCooldown"), 0, 200);
        summonCooldown = Math.clamp(nbt.getInt("SummonCooldown"), 0, 600);
        summons.readNbt(nbt);
        dataTracker.set(SECOND_PHASE, nbt.getBoolean("SecondPhase"));
        getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(com.witchersbestiary.config.ModConfig.speed(isSecondPhase() ? .36 : .32));
        bossBar.setColor(isSecondPhase() ? BossBar.Color.RED : BossBar.Color.GREEN);
        // После загрузки замахи отменены, но их перезарядки сохраняются.
        clawTicks = rootTicks = summonTicks = roarTicks = 0;
        clawTarget = null;
    }

    @Override public boolean canTarget(LivingEntity victim) {
        return !(victim instanceof LeshenWolfEntity) && !(victim instanceof LeshenEntity) && super.canTarget(victim);
    }

    @Override public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);
        if (damaged && !getWorld().isClient && isAlive() && roarTicks == 0 && clawTicks == 0 && rootTicks == 0 && summonTicks == 0) triggerAnim("action", "hurt");
        return damaged;
    }

    @Override public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        bossBar.addPlayer(player);
    }

    @Override public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        bossBar.removePlayer(player);
    }

    public LeshenEntity(EntityType<? extends LeshenEntity> type, World world) {
        super(type, world);
        experiencePoints = 50;
        setPersistent();
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);builder.add(STUN_FLAG,false);
        builder.add(SECOND_PHASE, false);
    }

    public boolean isSecondPhase() { return dataTracker.get(SECOND_PHASE); }

    public static DefaultAttributeContainer.Builder createLeshenAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 200)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, com.witchersbestiary.config.ModConfig.speed(.32))
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1);
    }

    @Override protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new LeshenMeleeGoal(this));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.7));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 12));
        goalSelector.add(7, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this,PlayerEntity.class,0,true,false,null));
    }
}
