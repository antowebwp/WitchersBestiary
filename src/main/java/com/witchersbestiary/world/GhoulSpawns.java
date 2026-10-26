package com.witchersbestiary.world;

import com.witchersbestiary.entity.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;

/** Natural overworld spawn: vanilla hostile-light rules make these night and cave spawns. */
public final class GhoulSpawns {
    private GhoulSpawns() { }
    public static void initialize(){

        NaturalSpawns.register(ModEntities.GHOUL,12,2,4);
    }
}
