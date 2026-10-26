package com.witchersbestiary.world;

import com.witchersbestiary.combat.mixin.SpawnRestrictionAccessor;
import com.witchersbestiary.entity.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import java.util.List;

/** Foglets prefer wetlands and dark forests, with vanilla light checks still enforced. */
public final class FogletSpawns {
    private FogletSpawns() { }
    public static void initialize(){

        NaturalSpawns.register(ModEntities.FOGLET,6,1,2);
    }
}
