package com.witchersbestiary.entity.projectile;

import com.witchersbestiary.alchemy.WitcherBombItem;
import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.registry.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public final class WitcherBombEntity extends ThrownItemEntity {
    public WitcherBombEntity(EntityType<? extends ThrownItemEntity> type, World world) {
        super(type, world);
    }

    public WitcherBombEntity(World world, LivingEntity owner) {
        super(ModEntities.WITCHER_BOMB, owner, world);
    }

    @Override protected Item getDefaultItem() {
        return ModItems.BOMBS.getFirst();
    }

    @Override protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (getWorld() instanceof ServerWorld world && !isRemoved()) {
            if (getOwner() instanceof ServerPlayerEntity owner && getStack().getItem() instanceof WitcherBombItem bomb)
                bomb.detonate(world, owner, hit.getPos().subtract(getVelocity().normalize().multiply(.04)));
            discard();
        }
    }

    @Override public void tick() {
        super.tick();
        if (!getWorld().isClient && age >= 100) discard();
    }
}
