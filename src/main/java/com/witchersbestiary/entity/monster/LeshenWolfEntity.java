package com.witchersbestiary.entity.monster;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import java.util.UUID;

/** Призванный волк с ограниченным временем жизни и серверной связью с боссом. */
public final class LeshenWolfEntity extends WolfEntity {
    public static final int LIFETIME = 1200;
    private UUID leshenId;
    private int remainingTicks = LIFETIME;

    public LeshenWolfEntity(EntityType<? extends WolfEntity> type, World world) {
        super(type, world);
        setPersistent();
    }

    public void bindTo(LeshenEntity leshen) { leshenId = leshen.getUuid(); }

    public void followTarget(LivingEntity victim) {
        if (victim != null && victim.isAlive() && canTarget(victim)) {
            setAngryAt(victim.getUuid());
            setAngerTime(remainingTicks);
            setTarget(victim);
        } else {
            setTarget(null);
            setAngryAt(null);
            setAngerTime(0);
        }
    }

    @Override public void tick() {
        if (isAlive() && getWorld() instanceof ServerWorld world) {
            if (--remainingTicks <= 0 || leshenId == null) { discard(); return; }
            if (world.getEntity(leshenId) instanceof LeshenEntity leshen) {
                if (!leshen.isAlive() || !leshen.hasSummonedWolf(getUuid(), world.getTime())) { discard(); return; }
                if (getTarget() != leshen.getTarget() || age % 20 == 0) followTarget(leshen.getTarget());
            } else {
                if (getTarget() != null) followTarget(null);
                if (age % 20 == 19) { discard(); return; }
            }
        }
        super.tick();
    }

    @Override public void onDeath(net.minecraft.entity.damage.DamageSource source) {
        if (getWorld() instanceof ServerWorld world && leshenId != null
                && world.getEntity(leshenId) instanceof LeshenEntity leshen) leshen.onSummonedWolfDeath(getUuid());
        super.onDeath(source);
    }

    @Override public boolean canTarget(LivingEntity victim) {
        return !(victim instanceof LeshenEntity) && !(victim instanceof LeshenWolfEntity) && super.canTarget(victim);
    }
    @Override public ActionResult interactMob(PlayerEntity player, Hand hand) { return ActionResult.PASS; }
    @Override public boolean isBreedingItem(ItemStack stack) { return false; }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (leshenId != null) nbt.putUuid("LeshenOwner", leshenId);
        nbt.putInt("LeshenLifetime", remainingTicks);
    }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        leshenId = nbt.containsUuid("LeshenOwner") ? nbt.getUuid("LeshenOwner") : null;
        remainingTicks = Math.clamp(nbt.getInt("LeshenLifetime"), 0, LIFETIME);
    }
}
