package com.witchersbestiary.unicorn;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
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
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class UnicornEntity extends HorseEntity implements GeoEntity {
    private static final TrackedData<Integer> RECOVERY = DataTracker.registerData(UnicornEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> HEALING = DataTracker.registerData(UnicornEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Byte> GAIT = DataTracker.registerData(UnicornEntity.class, TrackedDataHandlerRegistry.BYTE);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private boolean waiting;
    private int castingTicks;
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
    }

    @Override protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new EscapeDangerGoal(this, 1.25) {
            @Override public boolean canStart() { return !isTame() && super.canStart(); }
        });
        goalSelector.add(3, new Goal() {
            { setControls(java.util.EnumSet.of(Control.MOVE)); }
            @Override public boolean canStart() {
                var owner = getOwner();
                return owner != null && owner.isAlive() && owner.getWorld() == getWorld()
                    && !waiting && !recovering() && castingTicks == 0 && !hasPassengers()
                    && squaredDistanceTo(owner) > 16 && squaredDistanceTo(owner) < 4096;
            }
            @Override public boolean shouldContinue() { return canStart(); }
            @Override public void tick() {
                if (age % 10 == 0 && getOwner() != null) getNavigation().startMovingTo(getOwner(), 1.15);
            }
            @Override public void stop() { getNavigation().stop(); }
        });
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
    public boolean waiting() { return waiting; }

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
        if (player.isSneaking() && stack.isEmpty()) {
            if (!getWorld().isClient) {
                waiting = !waiting;
                getNavigation().stop();
                player.sendMessage(Text.translatable("unicorn.witchersbestiary." + (waiting ? "wait" : "follow")), true);
            }
            return ActionResult.success(getWorld().isClient);
        }
        return super.interactMob(player, hand);
    }

    @Override public Text getDefaultName() {
        return isTame() ? Text.translatable("entity.witchersbestiary.unicorn.tamed") : super.getDefaultName();
    }

    @Override public boolean damage(DamageSource source, float amount) {
        return !recovering() && super.damage(source, amount);
    }

    boolean beginRecovery() {
        if (!isTame()) return false;
        setHealth(1);
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
        var position = getPos();
        double distance = lastMotionPosition == null ? 0 : position.subtract(lastMotionPosition).horizontalLength();
        lastMotionPosition = position;
        strideSpeed = strideSpeed * .55 + (distance < 2 ? distance : 0) * .45;
        dataTracker.set(GAIT, (byte)(recovering() || healing() || strideSpeed < .025 ? 0 : strideSpeed >= .17 ? 2 : 1));
        if (recovering()) {
            int remaining = dataTracker.get(RECOVERY) - 1;
            dataTracker.set(RECOVERY, remaining);
            if (remaining == 0) { setHealth(getMaxHealth()); extinguish(); }
            return;
        }
        if (castingTicks > 0 && --castingTicks == 0) dataTracker.set(HEALING, false);
        if (age % 100 == 0) world.spawnParticles(ParticleTypes.END_ROD, getX(), getBodyY(.95), getZ(), 1, .28, .12, .28, .001);
        if (age % 10 == 0) healOwner(world);
    }

    public boolean healOwner(ServerWorld world) {
        if (!isTame() || recovering() || castingTicks > 0 || hasPassengers() || !(getOwner() instanceof ServerPlayerEntity owner)
            || owner.getWorld() != world || !owner.isAlive() || owner.isSpectator() || owner.getHealth() >= owner.getMaxHealth()
            || squaredDistanceTo(owner) > 144 || !canSee(owner) || com.witchersbestiary.fist.FistFights.busy(owner)) return false;
        boolean defending = owner.getAttacker() != null && owner.age - owner.getLastAttackedTime() <= 100;
        boolean attacking = owner.getAttacking() != null && owner.age - owner.getLastAttackTime() <= 100;
        if ((!defending && !attacking) || !UnicornHealing.get(world.getServer()).reserve(owner.getUuid(), world.getServer().getOverworld().getTime())) return false;
        owner.heal(2.5f);
        castingTicks = 25;
        dataTracker.set(HEALING, true);
        getNavigation().stop();
        triggerAnim("gesture", "heal");
        world.spawnParticles(ParticleTypes.END_ROD, getX(), getBodyY(1.12), getZ(), 6, .19, .2, .19, .015);
        world.spawnParticles(ParticleTypes.CHERRY_LEAVES, owner.getX(), owner.getBodyY(.65), owner.getZ(), 7, .28, .32, .28, .012);
        return true;
    }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("UnicornWaiting", waiting);
        nbt.putInt("UnicornRecovery", dataTracker.get(RECOVERY));
    }

    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        waiting = nbt.getBoolean("UnicornWaiting");
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
