package com.witchersbestiary.combat.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnLocation;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpawnRestriction.class)
public interface SpawnRestrictionAccessor {
    @Invoker("register")
    static <T extends MobEntity> void witchersbestiary$register(EntityType<T> type,SpawnLocation location,
            Heightmap.Type heightmap,SpawnRestriction.SpawnPredicate<T> predicate){throw new AssertionError();}
}
