package com.witchersbestiary.entity.monster;

import com.witchersbestiary.combat.CategorizedMonster;
import com.witchersbestiary.combat.MonsterCategory;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.data.*;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import com.witchersbestiary.signs.SignManager;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import static net.minecraft.server.command.CommandManager.*;

public final class HigherVampireEntity extends HostileEntity implements GeoEntity, CategorizedMonster {
    private static final int HUMAN = 0, CHANGING = 1, REVEAL = 2, HYBRID = 3, FINAL = 4, DEFEATED = 5, FRENZY_TRANSITION = 6;
    private static final int CHANGE_TICKS = 40, REVEAL_TICKS = 36, FRENZY_TICKS = 17;
    private static final TrackedData<Integer> STAGE = DataTracker.registerData(
            HigherVampireEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<String> CLIP = DataTracker.registerData(
            HigherVampireEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final EntityDimensions HYBRID_SIZE = EntityDimensions.changing(1.32f, 3.2f).withEyeHeight(2.84f);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final HigherVampireCombat combat = new HigherVampireCombat(this);
    private final ServerBossBar bossBar = new ServerBossBar(getDisplayName(), BossBar.Color.RED, BossBar.Style.PROGRESS);
    private static final TrackedData<Float> COCOON = DataTracker.registerData(
            HigherVampireEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final int COCOON_DURATION = 160;
    private int stageTicks, scaledPlayers, cocoonTicks;
    private boolean cocoonUsed;
    private long regenAt, reviveAt;
    private boolean sealable, huntBound;
    public boolean hasHuntBinding() { return huntBound; }
    public void markHuntBinding() { huntBound = true; }
    private static final TrackedData<Boolean> DEFEATED_HUMAN = DataTracker.registerData(
            HigherVampireEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public HigherVampireEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 600)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, .415)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 56)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, .72);
    }

    @Override protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this) {
            @Override public boolean canStart() { return !isDefeated() && !isCocooned() && super.canStart(); }
        });
        goalSelector.add(2, new WanderAroundFarGoal(this, 1) {
            @Override public boolean canStart() { return !transforming() && !isDefeated() && !isCocooned() && getTarget() == null && super.canStart(); }
            @Override public boolean shouldContinue() { return !transforming() && !isDefeated() && !isCocooned() && getTarget() == null && super.shouldContinue(); }
        });
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(STAGE, HUMAN);
        builder.add(CLIP, "");
        builder.add(DEFEATED_HUMAN, false);
        builder.add(COCOON, 0f);
    }

    public boolean isHybrid() { return dataTracker.get(STAGE) >= REVEAL && !dataTracker.get(DEFEATED_HUMAN); }
    public boolean hasRedEyes() { return !isHybrid() && (transforming() || !dataTracker.get(CLIP).isEmpty()); }
    public boolean isFinalPhase() { return dataTracker.get(STAGE) == FINAL; }
    public boolean canBeSealed() { return isDefeated() && sealable; }
    public boolean isDefeated() { return dataTracker.get(STAGE) == DEFEATED; }
    public float cocoonIntegrity() { return dataTracker.get(COCOON); }
    public boolean isCocooned() { return cocoonIntegrity() > 0; }
    public boolean isTransforming() { return transforming(); }
    public String activeCombatClip() { return dataTracker.get(CLIP); }
    void combatClip(String clip) { dataTracker.set(CLIP, clip); }

    private boolean transforming() {
        int stage = dataTracker.get(STAGE);
        return stage == CHANGING || stage == REVEAL || stage == FRENZY_TRANSITION;
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
                || isDefeated() || !hasHybridClearance()) return false;
        combat.cancel();
        stageTicks = 0;
        dataTracker.set(STAGE, CHANGING);
        return true;
    }

    private boolean hasHybridClearance() {
        return getWorld().isSpaceEmpty(this, HYBRID_SIZE.getBoxAt(getPos()));
    }

    boolean seekHybridClearance() {
        // Check a bounded set of nearby standing positions, then let navigation resolve the route.
        for (int radius : new int[]{2, 4, 7}) {
            for (int direction = 0; direction < 8; direction++) {
                double angle = direction * Math.PI / 4;
                Vec3d at = getPos().add(Math.cos(angle)*radius, 0, Math.sin(angle)*radius);
                var box = HYBRID_SIZE.getBoxAt(at);
                if (getWorld().isChunkLoaded(BlockPos.ofFloored(at))
                        && getWorld().getWorldBorder().contains(box)
                        && !getWorld().isSpaceEmpty(box.offset(0, -.15, 0))
                        && getWorld().isSpaceEmpty(this, box)
                        && getNavigation().startMovingTo(at.x, at.y, at.z, 1.1)) return true;
            }
        }
        return false;
    }

    @Override public void tick() {
        if (transforming() || isDefeated() || isCocooned()) {
            getNavigation().stop();
            setVelocity(0, Math.min(getVelocity().y, 0), 0);
        }
        super.tick();
        if (!(getWorld() instanceof ServerWorld world) || !isAlive()) return;
        if (isDefeated()) {
            if (sealable && world.getTime() >= reviveAt) {
                setHealth(getMaxHealth() * .20f);
                suppressRegeneration(40);
                dataTracker.set(STAGE, FINAL);
            }
            return;
        }
        if (transforming()) {
            tickTransformation(world);
            updateBossBar();
            return;
        }

        scaleForEncounter();
        boolean needsClearance = !isHybrid() && getHealth() <= getMaxHealth() * .65f && !isAiDisabled();
        if (needsClearance && beginTransformation()) {
            updateBossBar();
            return;
        }
        if (dataTracker.get(STAGE) == HYBRID && getHealth() <= getMaxHealth() * .25f) {
            combat.cancel();
            dataTracker.set(STAGE, FINAL);
            regenAt = Math.max(regenAt, world.getTime()+40);
            playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 1, .65f);
        }
        if (isFinalPhase() && !cocoonUsed && getHealth() <= getMaxHealth() * .15f) beginCocoon();
        if (isCocooned()) {
            tickCocoon(world);
            updateBossBar();
            return;
        }
        combat.tick(world, needsClearance);
        if (isFinalPhase() && getTarget() != null && world.getTime() >= regenAt) {
            regenAt = world.getTime()+40;
            if (!isOnFire()) heal(Math.max(0, Math.min(3, getMaxHealth()*.35f-getHealth())));
        }
        updateBossBar();
    }

    private float cocoonMaximum() { return Math.min(180, getMaxHealth() * .15f); }

    private void beginCocoon() {
        cocoonUsed = true;
        cocoonTicks = COCOON_DURATION;
        dataTracker.set(COCOON, 1f);
        combat.cancel();
        extinguish();
        setVelocity(Vec3d.ZERO);
        playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.1f, .66f);
    }

    private void tickCocoon(ServerWorld world) {
        int elapsed = COCOON_DURATION - --cocoonTicks;
        if (elapsed % 20 == 0)
            heal(Math.max(0, Math.min(getMaxHealth() * .02f, getMaxHealth() * .35f - getHealth())));
        if (elapsed % 5 == 0)
            world.spawnParticles(ParticleTypes.CRIMSON_SPORE, getX(), getBodyY(.55), getZ(), 8, .65, .75, .65, .013);
        if (cocoonTicks <= 0) endCocoon(world, false);
    }

    private void endCocoon(ServerWorld world, boolean broken) {
        cocoonTicks = 0;
        dataTracker.set(COCOON, 0f);
        suppressRegeneration(broken ? 80 : 40);
        combat.interrupt(broken ? 36 : 20);
        net.minecraft.particle.ParticleEffect shards = broken
                ? new net.minecraft.particle.BlockStateParticleEffect(ParticleTypes.BLOCK,
                        net.minecraft.block.Blocks.NETHER_WART_BLOCK.getDefaultState())
                : ParticleTypes.CRIMSON_SPORE;
        world.spawnParticles(shards,
                getX(), getBodyY(.5), getZ(), 35, .62, .76, .62, .11);
    }

    private void tickTransformation(ServerWorld world) {
        ++stageTicks;
        if (dataTracker.get(STAGE) == FRENZY_TRANSITION) {
            if (stageTicks >= FRENZY_TICKS) {
                dataTracker.set(STAGE, FINAL);
                regenAt = Math.max(regenAt, world.getTime() + 40);
            }
            return;
        }
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

    private void scaleForEncounter() {
        if (scaledPlayers != 0 || !(getTarget() instanceof ServerPlayerEntity)) return;
        scaledPlayers = Math.clamp(combat.players(32).size(), 1, 4);
        float ratio = getHealth()/getMaxHealth();
        applyEncounterScale();
        setHealth(ratio*getMaxHealth());
    }

    private void applyEncounterScale() {
        getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(switch (scaledPlayers) {
            case 2 -> 840;
            case 3 -> 1050;
            case 4 -> 1200;
            default -> 600;
        });
    }

    private void updateBossBar() {
        bossBar.setVisible(!isDefeated() && (getTarget() != null || transforming()));
        bossBar.setPercent(getHealth()/getMaxHealth());
        bossBar.setName(getDisplayName());
    }

    @Override public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        bossBar.addPlayer(player);
        updateBossBar();
    }

    @Override public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        bossBar.removePlayer(player);
    }

    public void interruptCombat(int ticks) {
        if (!getWorld().isClient && !isDefeated() && !transforming()) combat.interrupt(ticks);
    }

    public void suppressRegeneration(int ticks) {
        regenAt = Math.max(regenAt, getWorld().getTime()+ticks);
    }

    /** Applied at the final health write, after armor, resistance and all combat modifiers. */
    public float limitHealthAfterHit(float health) {
        int stage = dataTracker.get(STAGE);
        if (stage == HUMAN) return Math.max(health, Math.min(getHealth(), getMaxHealth() * .65f));
        if (stage == HYBRID) return Math.max(health, Math.min(getHealth(), getMaxHealth() * .25f));
        if (stage == FINAL && !cocoonUsed) return Math.max(health, Math.min(getHealth(), getMaxHealth() * .15f));
        return health;
    }

    @Override public boolean damage(DamageSource source, float amount) {
        if (isDefeated() || transforming()) return false;
        if (isCocooned()) {
            if (!(getWorld() instanceof ServerWorld world) || !Float.isFinite(amount) || amount <= 0
                    || !(source.getAttacker() instanceof ServerPlayerEntity)) return false;
            float remaining = cocoonIntegrity() - amount / cocoonMaximum();
            if (remaining <= 0) endCocoon(world, true);
            else dataTracker.set(COCOON, remaining);
            return true;
        }
        float before = getHealth();
        boolean hit = super.damage(source, amount);
        if (hit && !getWorld().isClient && !isDefeated()) {
            if (source.getAttacker() instanceof ServerPlayerEntity player && !player.isCreative()
                    && !player.isSpectator() && getTarget() == null && squaredDistanceTo(player) <= 56*56) {
                setTarget(player);
            }
            if (isFinalPhase() && !cocoonUsed && getHealth() <= getMaxHealth() * .15f) beginCocoon();
            if (source.isOf(SignManager.FIRE)) suppressRegeneration(105);
            else if (before-getHealth() >= 16) suppressRegeneration(65);
        }
        return hit;
    }

    @Override public void onDeath(DamageSource source) {
        if (getWorld().isClient || isDefeated()) return;
        sealable = isFinalPhase();
        reviveAt = getWorld().getTime() + 1800;
        if (sealable && getWorld() instanceof ServerWorld world)
            for (var player : world.getPlayers(p -> p.squaredDistanceTo(this) <= 256))
                player.sendMessage(Text.translatable("item.witchersbestiary.crimson_phylactery.ready"), true);
        dataTracker.set(DEFEATED_HUMAN, !isHybrid());
        dataTracker.set(STAGE, DEFEATED);
        // A living, inert entity keeps its UUID and its persistent defeated pose.
        setHealth(1);
        extinguish();
        combat.cancel();
        setTarget(null);
        updateBossBar();
    }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("HybridForm", isHybrid());
        if (transforming()) {
            nbt.putInt("TransitionStage", dataTracker.get(STAGE));
            nbt.putInt("TransitionTicks", stageTicks);
        }
        nbt.putBoolean("FinalPhase", isFinalPhase());
        nbt.putBoolean("Defeated", isDefeated());
        nbt.putBoolean("Sealable", sealable);
        nbt.putBoolean("VampireHuntBound", huntBound);
        nbt.putLong("ReviveAt", reviveAt);
        if (canBeSealed()) nbt.putLong("SealWindowRemaining", Math.max(0, reviveAt - getWorld().getTime()));
        nbt.putInt("EncounterPlayers", scaledPlayers);
        nbt.putLong("RegenerationAt", regenAt);
        nbt.putBoolean("CocoonUsed", cocoonUsed);
        nbt.putInt("CocoonTicks", cocoonTicks);
        nbt.putFloat("CocoonIntegrity", cocoonIntegrity());
    }

    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        scaledPlayers = Math.clamp(nbt.getInt("EncounterPlayers"), 0, 4);
        applyEncounterScale();
        regenAt = nbt.getLong("RegenerationAt");
        boolean hybrid = nbt.getBoolean("HybridForm");
        huntBound = nbt.getBoolean("VampireHuntBound");
        sealable = nbt.getBoolean("Sealable");
        reviveAt = nbt.getLong("ReviveAt");
        boolean defeated = nbt.getBoolean("Defeated");
        if (defeated && sealable) {
            long now = getWorld().getTime();
            long remaining = nbt.contains("SealWindowRemaining")
                    ? nbt.getLong("SealWindowRemaining") : reviveAt - now;
            reviveAt = now + (remaining > 0 ? Math.min(1800, remaining) : 1800);
        }
        dataTracker.set(DEFEATED_HUMAN, defeated && !hybrid);
        dataTracker.set(STAGE, defeated ? DEFEATED : hybrid ? (nbt.getBoolean("FinalPhase") ? FINAL : HYBRID) : HUMAN);
        int transition = nbt.getInt("TransitionStage");
        if (!defeated && ((!hybrid && transition == CHANGING) || (hybrid && (transition == REVEAL || transition == FRENZY_TRANSITION)))) {
            stageTicks = Math.clamp(nbt.getInt("TransitionTicks"), 0,
                    (transition == CHANGING ? CHANGE_TICKS : transition == REVEAL ? REVEAL_TICKS : FRENZY_TICKS)-1);
            dataTracker.set(STAGE, transition);
        }
        cocoonUsed = nbt.getBoolean("CocoonUsed");
        cocoonTicks = isFinalPhase() ? Math.clamp(nbt.getInt("CocoonTicks"), 0, COCOON_DURATION) : 0;
        float integrity = nbt.getFloat("CocoonIntegrity");
        dataTracker.set(COCOON, cocoonTicks > 0 && Float.isFinite(integrity) ? Math.clamp(integrity, 0f, 1f) : 0f);
        if (isCocooned()) cocoonUsed = true;
        setNoGravity(false);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        var controller = new AnimationController<>(this, "pose", 2, state -> {
            String form = isHybrid() ? "hybrid." : "human.";
            String clip = switch (dataTracker.get(STAGE)) {
                case CHANGING -> "transformation";
                case REVEAL -> "reveal";
                case FRENZY_TRANSITION -> "wing_spread";
                case DEFEATED -> isHybrid() ? "defeat" : "human_defeat";
                default -> isCocooned() ? "idle" : !activeCombatClip().isEmpty() ? activeCombatClip() : hurtTime > 0 && isAlive() ? "hurt"
                        : state.isMoving() ? (getVelocity().horizontalLengthSquared() > .038 ? "run" : "walk") : "idle";
            };
            var animation = RawAnimation.begin();
            String name = "animation.higher_vampire." + form + clip;
            return state.setAndContinue(switch (clip) {
                case "idle", "walk", "run", "flight_idle", "flight_move" -> animation.thenLoop(name);
                default -> animation.thenPlayAndHold(name);
            });
        });
        registrar.add(controller);
    }

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                dispatcher.register(literal("wbvampire").requires(source -> source.hasPermissionLevel(2))
                        .then(literal("bind").then(argument("target", EntityArgumentType.entity()).executes(context -> {
                            var player = context.getSource().getPlayerOrThrow();
                            return EntityArgumentType.getEntity(context, "target") instanceof HigherVampireEntity vampire
                                    && com.witchersbestiary.contracts.hunt.VampireHunt.bind(player, vampire) ? 1 : 0;
                        })))
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
