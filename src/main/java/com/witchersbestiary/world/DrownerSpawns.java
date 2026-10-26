package com.witchersbestiary.world;

import com.witchersbestiary.combat.mixin.SpawnRestrictionAccessor;
import com.witchersbestiary.entity.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import java.util.List;

public final class DrownerSpawns {
    private DrownerSpawns(){}
    public static void initialize(){
        DrownerSpawnConfig.load();
        NaturalSpawns.register(ModEntities.DROWNER,DrownerSpawnConfig.weight(),3,5);
    }
}
