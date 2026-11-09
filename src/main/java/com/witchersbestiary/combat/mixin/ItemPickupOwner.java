package com.witchersbestiary.combat.mixin;

import java.util.UUID;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemEntity.class)
public interface ItemPickupOwner {
    @Accessor("owner") UUID witchersbestiary$getPickupOwner();
}
