package com.witchersbestiary.unicorn;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class UnicornEntity extends HorseEntity implements GeoEntity {
    private static final TrackedData<Integer> RECOVERY = DataTracker.registerData(UnicornEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> HEALING = DataTracker.registerData(UnicornEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Byte> GAIT = DataTracker.registerData(UnicornEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Byte> BEHAVIOR = DataTracker.registerData(UnicornEntity.class, TrackedDataHandlerRegistry.BYTE);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private static final int MAX_SPEED_LEVEL = 25;
    private static final Identifier RIDING_SPEED = Identifier.of("witchersbestiary", "unicorn_riding_speed");
    private int speedLevel = 1;
    private double ridingDistance;
    private int lastRidingCredit = Integer.MIN_VALUE;
    private enum Behavior { FOLLOW, WAIT, GUARD }
    private static final Behavior[] BEHAVIORS = Behavior.values();
    private BlockPos guardPoint;
    private Goal guardAttackGoal, guardReturnGoal, followGoal;
    private int castingTicks;
    private long combatUntil, combatHealAt, damageHealAfter;
    private net.minecraft.util.math.Vec3d lastMotionPosition;
    private double strideSpeed;

    public UnicornEntity(EntityType<? extends HorseEntity> type, World world) {
        super(type, world);
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(RECOVERY, 0);
        builder.add(HEALING, false);
        builder.add(GAIT, (byte)0);
        builder.add(BEHAVIOR, (byte) Behavior.FOLLOW.ordinal());
    }

    @Override protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this) {
            @Override public boolean canStart() { return !waiting() && super.canStart(); }
        });
        goalSelector.add(1, new EscapeDangerGoal(this, 1.25) {
            @Override public boolean canStart() { return !isTame() && super.canStart(); }
        });
        guardAttackGoal = new Goal() {
            private net.minecraft.entity.mob.HostileEntity attacker;
            private int cooldown;
            { setControls(java.util.EnumSet.of(Control.MOVE, Control.LOOK)); }

            @Override public boolean canStart() {
                var owner = getOwner();
                if (behavior() != Behavior.GUARD || guardPoint == null || recovering() || castingTicks > 0
                        || hasPassengers() || owner == null || owner.age - owner.getLastAttackedTime() > 100
                        || !(owner.getAttacker() instanceof net.minecraft.entity.mob.HostileEntity hostile)
                        || !eligible(hostile)) return false;
                attacker = hostile;
                return true;
            }

            private boolean eligible(net.minecraft.entity.mob.HostileEntity target) {
                return target.isAlive() && target.getWorld() == getWorld()
                        && target.squaredDistanceTo(Vec3d.ofBottomCenter(guardPoint)) <= 100
                        && squaredDistanceTo(target) <= 144;
            }

            @Override public boolean shouldContinue() {
                return behavior() == Behavior.GUARD && guardPoint != null && !hasPassengers()
                        && !recovering() && castingTicks == 0 && attacker != null && eligible(attacker);
            }

            @Override public boolean shouldRunEveryTick() { return true; }
            @Override public void tick() {
                if (!shouldContinue()) return;
                getLookControl().lookAt(attacker, 25, 25);
                if (cooldown > 0) cooldown--;
                if (squaredDistanceTo(attacker) <= 6.25 && canSee(attacker)) {
                    getNavigation().stop();
                    if (cooldown == 0 && tryAttack(attacker)) cooldown = 20;
                } else if (age % 10 == 0) getNavigation().startMovingTo(attacker, 1.17);
            }
            @Override public void stop() {
                attacker = null;
                getNavigation().stop();
            }
        };
        goalSelector.add(2, guardAttackGoal);

        guardReturnGoal = new Goal() {
            private int repath;
            { setControls(java.util.EnumSet.of(Control.MOVE)); }

            private boolean canReturn(double threshold) {
                return canUseCommand(Behavior.GUARD) && guardPoint != null
                        && squaredDistanceTo(Vec3d.ofBottomCenter(guardPoint)) > threshold;
            }

            @Override public boolean canStart() { return canReturn(9); }
            @Override public boolean shouldContinue() { return canReturn(4); }
            @Override public void start() { repath = 0; }
            @Override public boolean shouldRunEveryTick() { return true; }

            @Override public void tick() {
                if (!shouldContinue() || repath-- > 0) return;
                repath = 9;
                var anchor = Vec3d.ofBottomCenter(guardPoint);
                getNavigation().startMovingTo(anchor.x, anchor.y, anchor.z, 1.12);
            }

            @Override public void stop() { getNavigation().stop(); }
        };
        goalSelector.add(3, guardReturnGoal);

        followGoal = new Goal() {
            private int repath;
            { setControls(java.util.EnumSet.of(Control.MOVE)); }

            @Override public boolean canStart() {
                var owner = followOwner();
                return owner != null && squaredDistanceTo(owner) >= 64;
            }

            @Override public boolean shouldContinue() {
                var owner = followOwner();
                return owner != null && squaredDistanceTo(owner) > 16;
            }

            @Override public void start() {
                repath = 0;
            }

            @Override public boolean shouldRunEveryTick() { return true; }

            @Override public void tick() {
                var owner = followOwner();
                if (owner == null || squaredDistanceTo(owner) <= 16 || repath-- > 0) return;
                repath = 9;
                double distance = squaredDistanceTo(owner);
                var path = getNavigation().findPathTo(owner.getBlockPos(), 0);
                if (path != null) {
                    double speed = path.reachesTarget() ? (distance > 256 ? 1.42 : 1.18) : 1.28;
                    getNavigation().startMovingAlong(path, speed);
                }
            }

            @Override public void stop() {
                repath = 0;
                getNavigation().stop();
            }
        };
        goalSelector.add(3, followGoal);
        goalSelector.add(5, new WanderAroundFarGoal(this, .75) {
            @Override public boolean canStart() { return !isTame() && super.canStart(); }
        });
        goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 7));
        goalSelector.add(8, new LookAroundGoal(this));
    }

    @Override public boolean isBreedingItem(ItemStack stack) { return false; }
    @Override public boolean canBreedWith(net.minecraft.entity.passive.AnimalEntity other) { return false; }
    public boolean recovering() { return dataTracker.get(RECOVERY) > 0; }
    public boolean healing() { return dataTracker.get(HEALING); }
    public byte gait() { return dataTracker.get(GAIT); }
    private Behavior behavior() { return BEHAVIORS[dataTracker.get(BEHAVIOR)]; }
    public boolean waiting() { return behavior() == Behavior.WAIT; }

    private boolean canUseCommand(Behavior mode) {
        return behavior() == mode && isTame() && !recovering() && castingTicks == 0 && !hasPassengers();
    }

    private net.minecraft.entity.LivingEntity followOwner() {
        if (!canUseCommand(Behavior.FOLLOW)) return null;
        var owner = getOwner();
        return owner != null && owner.isAlive() && !owner.isSpectator() && owner.getWorld() == getWorld() ? owner : null;
    }

    private void stopCommandMovement() {
        for (var goal : goalSelector.getGoals()) {
            if (goal.getGoal() == guardAttackGoal || goal.getGoal() == guardReturnGoal || goal.getGoal() == followGoal)
                goal.stop();
        }
        getNavigation().stop();
        getMoveControl().moveTo(getX(), getY(), getZ(), 0);
        setForwardSpeed(0);
        setSidewaysSpeed(0);
    }

    private final com.witchersbestiary.entity.PetFeeding feedingCooldown = new com.witchersbestiary.entity.PetFeeding();
    @Override public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (recovering()) return ActionResult.PASS;
        if (!isTame()) {
            if (!stack.isOf(Items.GOLDEN_CARROT) && !stack.isOf(Items.GOLDEN_APPLE)) return ActionResult.PASS;
            if (getWorld() instanceof ServerWorld world) {
                boolean apple = stack.isOf(Items.GOLDEN_APPLE);
                if (!player.isCreative()) stack.decrement(1);
                if (apple || getRandom().nextInt(3) == 0) {
                    bondWithPlayer(player);
                    getNavigation().stop();
                    triggerAnim("gesture", "tame");
                    world.spawnParticles(ParticleTypes.CHERRY_LEAVES, getX(), getBodyY(.8), getZ(), 9, .45, .3, .45, .01);
                }
            }
            return ActionResult.success(getWorld().isClient);
        }
        if (!player.getUuid().equals(getOwnerUuid())) return ActionResult.PASS;
        if (player.isSneaking() && stack.getItem() instanceof UnicornWhistle whistle)
            return whistle.useOnEntity(stack, player, this, hand);
        if (!player.isSneaking()) {
            var feeding = feedingCooldown.interact(this, player, hand, getOwnerUuid(),
                food -> food.isOf(Items.GOLDEN_APPLE) ? 8 : food.isOf(Items.GOLDEN_CARROT) ? 6 :
                    food.isOf(Items.APPLE) || food.isOf(Items.CARROT) ? 4 : 0);
            if (feeding != null) return feeding;
        }
        if (player.isSneaking() && hand == Hand.MAIN_HAND && stack.isEmpty()) {
            if (!getWorld().isClient) {
                Behavior next = switch (behavior()) {
                    case FOLLOW -> Behavior.WAIT;
                    case WAIT -> Behavior.GUARD;
                    case GUARD -> Behavior.FOLLOW;
                };
                stopCommandMovement();
                guardPoint = next == Behavior.GUARD ? getBlockPos() : null;
                dataTracker.set(BEHAVIOR, (byte) next.ordinal());
                if (player instanceof ServerPlayerEntity owner) UnicornWhistle.cancelPending(owner, this);
                player.sendMessage(Text.translatable("unicorn.witchersbestiary." + next.name().toLowerCase(java.util.Locale.ROOT)), true);
                player.sendMessage(speedStatus(), false);
            }
            return ActionResult.success(getWorld().isClient);
        }
        if (!player.isSneaking() && hand == Hand.MAIN_HAND && stack.isEmpty() && isSaddled()) {
            if (hasPassengers() || player.hasVehicle()) return ActionResult.PASS;
            if (!getWorld().isClient) {
                stopCommandMovement();
                if (!player.startRiding(this)) return ActionResult.FAIL;
            }
            return ActionResult.success(getWorld().isClient);
        }
        if (!player.isSneaking() && stack.isOf(Items.SADDLE) && !isSaddled() && canBeSaddled()) {
            if (!getWorld().isClient) {
                saddle(stack.copyWithCount(1), net.minecraft.sound.SoundCategory.NEUTRAL);
                if (!player.isCreative()) stack.decrement(1);
            }
            return ActionResult.success(getWorld().isClient);
        }
        return super.interactMob(player, hand);
    }

    @Override public Text getDefaultName() {
        return isTame() ? Text.translatable("entity.witchersbestiary.unicorn.tamed") : super.getDefaultName();
    }

    @Override public boolean damage(DamageSource source, float amount) {
        if (recovering()) return false;
        boolean hit = super.damage(source, amount);
        if (hit && !recovering() && isAlive() && isTame() && getWorld() instanceof ServerWorld world) {
            long now = world.getTime();
            if (source.getAttacker() instanceof net.minecraft.entity.LivingEntity) enterCombat(now);
            damageHealAfter = now + 60;
        }
        return hit;
    }

    @Override public boolean tryAttack(net.minecraft.entity.Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && isTame() && isAlive() && !recovering() && getWorld() instanceof ServerWorld world)
            enterCombat(world.getTime());
        return hit;
    }

    private void enterCombat(long now) {
        if (combatUntil <= now) combatHealAt = now + 100;
        combatUntil = now + 200;
    }

    private void tickCombatHealing(ServerWorld world) {
        long now = world.getTime();
        float maximum = getMaxHealth();
        float before = getHealth();
        if (!isAlive() || !isTame() || now >= combatUntil || now < combatHealAt
                || now < damageHealAfter || before > maximum * .9f) return;
        combatHealAt = now + 100;
        heal(maximum * .01f);
        if (getHealth() <= before) return;
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getBodyY(.7), getZ(), 3, .25, .18, .25, .01);
        world.spawnParticles(ParticleTypes.WAX_ON, getX(), getBodyY(.7), getZ(), 2, .22, .18, .22, .01);
    }

    boolean beginRecovery() {
        if (!isTame()) return false;
        setHealth(1);
        combatUntil = combatHealAt = damageHealAfter = 0;
        dataTracker.set(RECOVERY, 600);
        castingTicks = 0;
        dataTracker.set(HEALING, false);
        removeAllPassengers();
        getNavigation().stop();
        if (getOwner() instanceof ServerPlayerEntity owner) owner.sendMessage(Text.translatable("unicorn.witchersbestiary.recovery"), true);
        return true;
    }

    @Override public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld world)) return;
        if (isTame()) UnicornWhistleState.get(world.getServer()).update(this, false);
        var position = getPos();
        double distance = lastMotionPosition == null ? 0 : position.subtract(lastMotionPosition).horizontalLength();
        lastMotionPosition = position;
        strideSpeed = strideSpeed * .55 + (distance < 2 ? distance : 0) * .45;
        dataTracker.set(GAIT, (byte)(recovering() || strideSpeed < .025 ? 0 : strideSpeed >= .17 ? 2 : 1));
        if (recovering()) {
            int remaining = dataTracker.get(RECOVERY) - 1;
            dataTracker.set(RECOVERY, remaining);
            if (remaining == 0) { setHealth(getMaxHealth()); extinguish(); }
            return;
        }
        tickCombatHealing(world);
        if (castingTicks > 0 && --castingTicks == 0) dataTracker.set(HEALING, false);
        if (age % 100 == 0) world.spawnParticles(ParticleTypes.END_ROD, getX(), getBodyY(.95), getZ(), 1, .28, .12, .28, .001);
        if (getOwner() instanceof ServerPlayerEntity owner) {
            refreshMountedRegeneration(owner);
            healOwner(world, owner);
        }
    }

    private void refreshMountedRegeneration(ServerPlayerEntity owner) {
        if (!isTame() || !owner.isAlive() || owner.isSpectator() || owner.getVehicle() != this) return;
        var regeneration = owner.getStatusEffect(StatusEffects.REGENERATION);
        // Leave time for vanilla's 50-tick Regeneration I pulse before renewing.
        if (regeneration == null || (regeneration.getAmplifier() == 0
                && !regeneration.isInfinite() && regeneration.getDuration() <= 40)) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0, true, false, true));
        }
    }

    public boolean healOwner(ServerWorld world) {
        return getOwner() instanceof ServerPlayerEntity owner && healOwner(world, owner);
    }

    private boolean healOwner(ServerWorld world, ServerPlayerEntity owner) {
        if (!isTame() || recovering() || castingTicks > 0
            || owner.getWorld() != world || !owner.isAlive() || owner.isSpectator() || owner.getHealth() >= owner.getMaxHealth()
            || squaredDistanceTo(owner) > 144 || (owner.getVehicle() != this && !canSee(owner)) || com.witchersbestiary.fist.FistFights.busy(owner)) return false;
        if (!UnicornHealing.get(world.getServer()).reserve(owner.getUuid(), world.getServer().getOverworld().getTime())) return false;
        owner.heal(4);
        castingTicks = 25;
        dataTracker.set(HEALING, true);
        getNavigation().stop();
        triggerAnim("gesture", "heal");
        Vec3d horn = getPos().add(Vec3d.fromPolar(0, getHeadYaw()).multiply(.64));
        world.spawnParticles(ParticleTypes.END_ROD, horn.x, getBodyY(1.12), horn.z, 6, .19, .2, .19, .015);
        world.spawnParticles(ParticleTypes.CHERRY_LEAVES, owner.getX(), owner.getBodyY(.65), owner.getZ(), 7, .28, .32, .28, .012);
        return true;
    }

    private static int distanceForLevel(int level) {
        return 96 + 32 * (level - 1);
    }

    private Text speedStatus() {
        var status = Text.translatable("unicorn.witchersbestiary.speed", speedLevel, MAX_SPEED_LEVEL);
        if (speedLevel < MAX_SPEED_LEVEL) {
            status.append(" | ").append(Text.translatable("unicorn.witchersbestiary.speed_progress",
                (int) ridingDistance, distanceForLevel(speedLevel)));
        }
        double speed = Math.round(strideSpeed * 200) / 10.0;
        status.append(" | ").append(Text.translatable("unicorn.witchersbestiary.current_speed", speed));
        if (speedLevel < MAX_SPEED_LEVEL) {
            int filled = Math.clamp((int)(10 * ridingDistance / distanceForLevel(speedLevel)), 0, 10);
            double nextBonus = 100 * (ridingBonus(speedLevel + 1) - ridingBonus(speedLevel));
            status.append(" ").append("▰".repeat(filled)).append("▱".repeat(10 - filled));
            status.append(" | ").append(Text.translatable("unicorn.witchersbestiary.next_bonus",
                    String.format(java.util.Locale.ROOT, "%.2f", nextBonus)));
        }
        return status;
    }

    // Called only after vanilla accepts a ridden vehicle move.
    public void recordRiddenMove(ServerPlayerEntity rider, Vec3d displacement) {
        if (!(getWorld() instanceof ServerWorld world) || recovering() || speedLevel == MAX_SPEED_LEVEL
            || !isTame() || !rider.getUuid().equals(getOwnerUuid()) || rider.isSpectator()
            || getControllingPassenger() != rider || !isOnGround()) return;

        int tick = world.getServer().getTicks();
        if (tick == lastRidingCredit) return;
        double distance = displacement.horizontalLength();
        // Vanilla mounted ground motion can exceed the raw movement attribute per tick.
        double limit = getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 2.1 + .025;
        if (!(distance >= .02 && distance <= limit) || Math.abs(displacement.y) > 1) return;

        Vec3d input = new Vec3d(rider.sidewaysSpeed * .5, 0, rider.forwardSpeed)
            .rotateY((float) -Math.toRadians(rider.getYaw()));
        if (displacement.dotProduct(input) <= .25 * distance * input.length()) return;

        lastRidingCredit = tick;
        ridingDistance += distance;
        if (ridingDistance >= distanceForLevel(speedLevel)) {
            ridingDistance -= distanceForLevel(speedLevel);
            speedLevel++;
            if (speedLevel == MAX_SPEED_LEVEL) ridingDistance = 0;
            updateRidingSpeed();
            rider.sendMessage(Text.translatable("unicorn.witchersbestiary.speed_level_up",
                speedLevel + "/" + MAX_SPEED_LEVEL), true);
        }
    }

    private double ridingBonus(int level) {
        return Math.max(0, Math.min(.45 * (level - 1) / (MAX_SPEED_LEVEL - 1),
                .49 / getAttributeBaseValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) - 1));
    }

    private void updateRidingSpeed() {
        var speed = getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        speed.removeModifier(RIDING_SPEED);
        double bonus = ridingBonus(speedLevel);
        if (bonus > 0) speed.addTemporaryModifier(new EntityAttributeModifier(
            RIDING_SPEED, bonus, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("UnicornSpeedLevel", speedLevel);
        nbt.putDouble("UnicornRidingDistance", ridingDistance);
        nbt.putBoolean("UnicornWaiting", waiting());
        nbt.putString("UnicornBehavior", behavior().name());
        if (guardPoint != null) nbt.putLong("UnicornGuardPoint", guardPoint.asLong());
        nbt.putInt("UnicornRecovery", dataTracker.get(RECOVERY));
    }

    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        speedLevel = Math.clamp(nbt.getInt("UnicornSpeedLevel"), 1, MAX_SPEED_LEVEL);
        double savedDistance = nbt.getDouble("UnicornRidingDistance");
        ridingDistance = speedLevel < MAX_SPEED_LEVEL && Double.isFinite(savedDistance)
            && savedDistance >= 0 && savedDistance < distanceForLevel(speedLevel) ? savedDistance : 0;
        updateRidingSpeed();
        Behavior saved = switch (nbt.getString("UnicornBehavior")) {
            case "GUARD" -> Behavior.GUARD;
            case "WAIT" -> Behavior.WAIT;
            case "FOLLOW" -> Behavior.FOLLOW;
            default -> nbt.getBoolean("UnicornWaiting") ? Behavior.WAIT : Behavior.FOLLOW;
        };
        dataTracker.set(BEHAVIOR, (byte) saved.ordinal());
        guardPoint = saved == Behavior.GUARD
                ? (nbt.contains("UnicornGuardPoint") ? BlockPos.fromLong(nbt.getLong("UnicornGuardPoint")) : getBlockPos())
                : null;
        dataTracker.set(RECOVERY, Math.clamp(nbt.getInt("UnicornRecovery"), 0, 600));
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "gait", 5, state -> state.setAndContinue(RawAnimation.begin().thenLoop(
            "animation.unicorn." + (gait() == 0 ? "idle" : gait() == 2 ? "run" : "walk")))));
        controllers.add(new AnimationController<>(this, "hair", 3, state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.unicorn.hair"))));
        controllers.add(new AnimationController<>(this, "gesture", 4, state -> PlayState.STOP)
            .triggerableAnim("heal", RawAnimation.begin().thenPlay("animation.unicorn.heal"))
            .triggerableAnim("tame", RawAnimation.begin().thenPlay("animation.unicorn.tame")));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }
}
