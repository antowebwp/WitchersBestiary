package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.CategorizedMonster;
import com.witchersbestiary.combat.MonsterCategory;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.data.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import static net.minecraft.server.command.CommandManager.*;

public final class HigherVampireEntity extends HostileEntity implements GeoEntity, CategorizedMonster {
    private static final int HUMAN = 0, CHANGING = 1, REVEAL = 2, HYBRID = 3;
    private static final int CHANGE_TICKS = 40, REVEAL_TICKS = 36;
    private static final TrackedData<Integer> STAGE = DataTracker.registerData(
            HigherVampireEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final EntityDimensions HYBRID_SIZE = EntityDimensions.changing(1.32f, 3.2f).withEyeHeight(2.84f);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int stageTicks;

    public HigherVampireEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 80)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, .26);
    }

    @Override protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new WanderAroundFarGoal(this, 1) {
            @Override public boolean canStart() { return !transforming() && super.canStart(); }
            @Override public boolean shouldContinue() { return !transforming() && super.shouldContinue(); }
        });
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(STAGE, HUMAN);
    }

    public boolean isHybrid() { return dataTracker.get(STAGE) >= REVEAL; }
    public boolean hasRedEyes() { return dataTracker.get(STAGE) == CHANGING; }
    private boolean transforming() {
        int stage = dataTracker.get(STAGE);
        return stage == CHANGING || stage == REVEAL;
    }

    @Override public MonsterCategory getMonsterCategory() { return MonsterCategory.VAMPIRE; }

    @Override public EntityDimensions getBaseDimensions(EntityPose pose) {
        return isHybrid() ? HYBRID_SIZE : super.getBaseDimensions(pose);
    }

    @Override public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (STAGE.equals(data)) calculateDimensions();
    }

    public boolean beginTransformation() {
        if (!(getWorld() instanceof ServerWorld) || !isAlive() || transforming() || isHybrid()
                || !hasHybridClearance()) return false;
        getNavigation().stop();
        stageTicks = 0;
        dataTracker.set(STAGE, CHANGING);
        return true;
    }

    private boolean hasHybridClearance() {
        return getWorld().isSpaceEmpty(this, HYBRID_SIZE.getBoxAt(getPos()));
    }

    @Override public void tick() {
        if (transforming() && isAlive()) {
            getNavigation().stop();
            setVelocity(0, getVelocity().y, 0);
        }
        super.tick();
        if (!(getWorld() instanceof ServerWorld world) || !isAlive() || !transforming()) return;
        ++stageTicks;
        if (dataTracker.get(STAGE) == CHANGING && stageTicks >= CHANGE_TICKS) {
            stageTicks = 0;
            if (!hasHybridClearance()) {
                dataTracker.set(STAGE, HUMAN);
                return;
            }
            dataTracker.set(STAGE, REVEAL);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 36, .5, .7, .5, .045);
        } else if (dataTracker.get(STAGE) == REVEAL) {
            if (stageTicks == 19) playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 1.1f, .78f);
            if (stageTicks >= REVEAL_TICKS) dataTracker.set(STAGE, HYBRID);
        }
    }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("HybridForm", isHybrid());
    }

    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        dataTracker.set(STAGE, nbt.getBoolean("HybridForm") ? HYBRID : HUMAN);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        var controller = new AnimationController<>(this, "pose", 2, state -> {
            String form = isHybrid() ? "hybrid." : "human.";
            String clip = switch (dataTracker.get(STAGE)) {
                case CHANGING -> "transformation";
                case REVEAL -> "reveal";
                default -> !isAlive() && isHybrid() ? "defeat" : hurtTime > 0 && isAlive() ? "hurt"
                        : state.isMoving() ? (getVelocity().horizontalLengthSquared() > .038 ? "run" : "walk") : "idle";
            };
            var animation = RawAnimation.begin();
            String name = "animation.higher_vampire." + form + clip;
            return state.setAndContinue(switch (clip) {
                case "idle", "walk", "run" -> animation.thenLoop(name);
                default -> animation.thenPlayAndHold(name);
            });
        });
        for (String clip : new String[]{"claw_attack_left", "claw_attack_right", "claw_combo", "dodge"}) {
            controller.triggerableAnim("human." + clip,
                    RawAnimation.begin().thenPlay("animation.higher_vampire.human." + clip));
        }
        for (String clip : new String[]{"claw_left", "claw_right", "double_claw", "bite", "roar",
                "wing_spread", "wing_fold", "wing_attack", "jump", "land", "flight_idle", "flight_move"}) {
            controller.triggerableAnim("hybrid." + clip,
                    RawAnimation.begin().thenPlay("animation.higher_vampire.hybrid." + clip));
        }
        registrar.add(controller);
    }

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                dispatcher.register(literal("wbvampire").requires(source -> source.hasPermissionLevel(2))
                        .then(literal("transform").then(argument("target", EntityArgumentType.entity())
                                .executes(context -> {
                                    if (EntityArgumentType.getEntity(context, "target") instanceof HigherVampireEntity vampire
                                            && vampire.beginTransformation()) return 1;
                                    context.getSource().sendError(net.minecraft.text.Text.literal(
                                            "Select a living human-form Higher Vampire with room to transform."));
                                    return 0;
                                })))));
    }
}
