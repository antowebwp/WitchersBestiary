package com.witchersbestiary.world;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.KatakanEntity;
import com.witchersbestiary.combat.mixin.SpawnRestrictionAccessor;
import net.fabricmc.fabric.api.biome.v1.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.*;

public final class KatakanSpawns {
    private KatakanSpawns(){}
    public static boolean canSpawn(EntityType<KatakanEntity> type,ServerWorldAccess world,SpawnReason reason,BlockPos pos,net.minecraft.util.math.random.Random random){
        return NaturalSpawns.canSpawn(type,world,reason,pos,random);
    }
    public static void initialize(){

        NaturalSpawns.register(ModEntities.KATAKAN,1,1,1);
    }
}
