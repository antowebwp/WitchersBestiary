package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.CategorizedMonster;
import com.witchersbestiary.combat.CombatIllusions;
import net.minecraft.nbt.NbtCompound;
import com.witchersbestiary.combat.MonsterCategory;
import com.witchersbestiary.entity.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.EntityData;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Tall, fast necrophage. Combat copies share the renderer and AI but never run special abilities or loot. */
public final class FogletEntity extends HostileEntity implements GeoEntity, CategorizedMonster, com.witchersbestiary.combat.Stunnable {
    private static final net.minecraft.entity.data.TrackedData<Boolean> STUN_FLAG=net.minecraft.entity.data.DataTracker.registerData(FogletEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    @Override protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder){super.initDataTracker(builder);builder.add(STUN_FLAG,false);}
    @Override public boolean trackedStun(){return dataTracker.get(STUN_FLAG);}
    @Override public void setTrackedStun(boolean value){dataTracker.set(STUN_FLAG,value);}

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.foglet.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.foglet.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.foglet.run");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.foglet.death");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean illusion;
    private int illusionAge;
    private long illusionExpiry;
    private final CombatIllusions copies=new CombatIllusions();
    private int fogCooldown = 40;
    private int vanishCooldown = 70;
    private int illusionCooldown = 60;
    private int vanishTicks;

    public FogletEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        experiencePoints = 7;
    }

    private com.witchersbestiary.entity.ai.ResponsiveMeleeGoal melee;
    @Override public com.witchersbestiary.combat.CombatState getCombatState(){return com.witchersbestiary.combat.Stun.active(this)?com.witchersbestiary.combat.CombatState.STUNNED:melee==null?com.witchersbestiary.combat.CombatState.IDLE:melee.state();}
    @Override public void cancelCombatForStun(){if(melee!=null)melee.stop();}
    @Override protected void initGoals() {
        goalSelector.add(2, melee=new com.witchersbestiary.entity.ai.ResponsiveMeleeGoal(this,false));
        goalSelector.add(7, new WanderAroundFarGoal(this, 1.05));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 16));
        goalSelector.add(9, new LookAroundGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this,PlayerEntity.class,0,true,false,null));
    }

    @Override public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        if (illusion) {
            if (++illusionAge >= CombatIllusions.LIFETIME || illusionExpiry>0 && getWorld().getTime()>=illusionExpiry || getWorld().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) discard();
            return;
        }
        if (fogCooldown > 0) fogCooldown--;
        if (vanishCooldown > 0) vanishCooldown--;
        if (illusionCooldown > 0) illusionCooldown--;
        if (vanishTicks > 0 && --vanishTicks == 0) {
            removeStatusEffect(StatusEffects.INVISIBILITY);
            triggerAnim("action", "appear");
            emitFog(1.2);
        }
        if (com.witchersbestiary.combat.Stun.active(this)||!isAlive()||isAiDisabled()) return;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target.getWorld() != getWorld()) return;
        double distance = squaredDistanceTo(target);
        if (canSee(target) && fogCooldown == 0 && distance < 12 * 12) {
            createFogVeil(target);
            fogCooldown = com.witchersbestiary.config.ModConfig.abilityTicks(140);
        }
        if (canSee(target) && vanishCooldown == 0 && distance < 9 * 9 && tryVanishNear(target)) {
            vanishCooldown = com.witchersbestiary.config.ModConfig.abilityTicks(160);
        }
        if (canSee(target) && illusionCooldown == 0 && distance < 18 * 18) {
            summonIllusions(target);
            illusionCooldown = com.witchersbestiary.config.ModConfig.abilityTicks(220);
        }
    }

    /** Applies a short sight-obscuring veil and cloud burst around the target. */
    public void createFogVeil(LivingEntity target) {
        if (!(getWorld() instanceof ServerWorld world) || illusion || target.isRemoved()) return;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 70, 0, false, true, true));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 35, 0, false, true, true));
        world.spawnParticles(ParticleTypes.CLOUD, target.getX(), target.getBodyY(.55), target.getZ(),
                42, 1.0, .8, 1.0, .045);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getBodyY(.55), target.getZ(),
                18, .8, .65, .8, .015);
    }

    /** Teleports only to a dry, supported, collision-free block near the target. */
    public boolean tryVanishNear(LivingEntity target) {
        if (!(getWorld() instanceof ServerWorld world) || illusion || target.isRemoved()) return false;
        emitFog(.9);
        for (int attempt = 0; attempt < 18; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 3.2 + random.nextDouble() * 3.0;
            double x = target.getX() + Math.cos(angle) * radius;
            double z = target.getZ() + Math.sin(angle) * radius;
            int y = MathHelper.clamp(MathHelper.floor(target.getY()), world.getBottomY() + 1, world.getTopY() - 3);
            BlockPos feet = BlockPos.ofFloored(x, y, z);
            BlockPos below = feet.down();
            if (!world.getWorldBorder().contains(feet) || !world.isInBuildLimit(feet)
                    || !world.getBlockState(below).isSolidBlock(world, below)
                    || !world.getBlockState(feet).isAir()
                    || !world.getBlockState(feet.up()).isAir()
                    || !world.getFluidState(feet).isEmpty()
                    || !world.getFluidState(feet.up()).isEmpty()) continue;
            double dx = feet.getX() + .5 - getX();
            double dy = feet.getY() - getY();
            double dz = feet.getZ() + .5 - getZ();
            Box destination = getBoundingBox().offset(dx, dy, dz);
            if (!world.isSpaceEmpty(this, destination)) continue;
            requestTeleport(feet.getX() + .5, feet.getY(), feet.getZ() + .5);
            addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 28, 0, false, false, true));
            triggerAnim("action", "vanish");
            vanishTicks = 20;
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + .8, getZ(),
                    26, .55, .8, .55, .035);
            return true;
        }
        return false;
    }

    /** Creates up to two combat copies; persisted slots prevent accumulation and recursion. */
    public int summonIllusions(LivingEntity target) {
        if (!(getWorld() instanceof ServerWorld world) || illusion || target.isRemoved()) return 0;
        int count = Math.min(1 + random.nextInt(2),copies.available(world));
        int created = 0;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * (i + 1) / (count + 1.0) + random.nextDouble() * .3;
            double radius = 2.6 + random.nextDouble() * .8;
            FogletEntity copy = ModEntities.FOGLET.create(world);
            if (copy == null) continue;
            copy.refreshPositionAndAngles(getX() + Math.cos(angle) * radius, getY(),
                    getZ() + Math.sin(angle) * radius, getYaw(), 0);
            copy.setIllusion(true);
            copy.setAiDisabled(false);
            copy.setTarget(target);
            copy.setGlowing(false);
            copy.setHealth(Math.min(copy.getMaxHealth(), 8));
            if (world.spawnEntity(copy)) {created++;copies.add(copy,copy.illusionExpiry);}
            else copy.discard();
        }
        if (created > 0) {
            triggerAnim("action", "summon_illusions");
            world.spawnParticles(ParticleTypes.CLOUD, getX(), getY() + .8, getZ(),
                    30, 1.1, .7, 1.1, .04);
        }
        return created;
    }

    public boolean isIllusion() { return illusion; }

    public void setIllusion(boolean illusion) {
        this.illusion = illusion;
        if (illusion) {
            experiencePoints = 0;
            illusionAge = 0;
            illusionExpiry=getWorld().getTime()+CombatIllusions.LIFETIME;
            getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(3);
            setGlowing(false);
            setPersistent();
        }
    }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);nbt.putBoolean("CombatIllusion",illusion);nbt.putInt("IllusionAge",illusionAge);
        nbt.putLong("IllusionExpiry",illusionExpiry);copies.save(nbt);
    }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);setIllusion(nbt.getBoolean("CombatIllusion"));
        illusionAge=nbt.getInt("IllusionAge");if(nbt.contains("IllusionExpiry"))illusionExpiry=nbt.getLong("IllusionExpiry");
        if(illusion)setAiDisabled(false);copies.load(nbt);
    }

    private void emitFog(double spread) {
        if (getWorld() instanceof ServerWorld world)
            world.spawnParticles(ParticleTypes.CLOUD, getX(), getY() + .7, getZ(), 24,
                    spread, .6, spread, .025);
    }

    @Override protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        if (!illusion) super.dropLoot(damageSource, causedByPlayer);
    }


    @Override public boolean damage(DamageSource source, float amount) {
        boolean hit = super.damage(source, amount);
        if (hit && !getWorld().isClient && isAlive() && (melee==null || melee.state()==com.witchersbestiary.combat.CombatState.IDLE || melee.state()==com.witchersbestiary.combat.CombatState.CHASE)) triggerAnim("action", "hurt");
        return hit;
    }

    @Override public MonsterCategory getMonsterCategory() { return MonsterCategory.NECROPHAGE; }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state -> {
            if (getHealth() <= 0) return state.setAndContinue(DEATH);
            if (!state.isMoving()) return state.setAndContinue(IDLE);
            return state.setAndContinue(getVelocity().horizontalLengthSquared() > .035 ? RUN : WALK);
        }));
        AnimationController<FogletEntity> action = new AnimationController<>(this, "action", 0,
                state -> PlayState.STOP);
        for (String name : new String[]{"attack", "vanish", "appear", "summon_illusions", "hurt"})
            action.triggerableAnim(name, RawAnimation.begin().thenPlay("animation.foglet." + name));
        controllers.add(action);
    }

    public static DefaultAttributeContainer.Builder createFogletAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 45)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, com.witchersbestiary.config.ModConfig.speed(.42))
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.0);
    }

    public static boolean canSpawnInWetDark(EntityType<FogletEntity> type, ServerWorldAccess world,
                                             SpawnReason reason, BlockPos pos,
                                             net.minecraft.util.math.random.Random random) {
        return HostileEntity.canSpawnInDark(type, world, reason, pos, random)
                && world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) <= pos.getY() + 1;
    }
}
