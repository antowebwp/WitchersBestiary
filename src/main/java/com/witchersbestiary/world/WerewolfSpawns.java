package com.witchersbestiary.world;

import com.witchersbestiary.config.ModConfig;
import com.witchersbestiary.combat.mixin.SpawnRestrictionAccessor;
import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.WerewolfEntity;
import net.fabricmc.fabric.api.biome.v1.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.util.List;

public final class WerewolfSpawns {
    private WerewolfSpawns() { }
    public static boolean canSpawn(EntityType<WerewolfEntity> type,ServerWorldAccess world,SpawnReason reason,BlockPos pos,net.minecraft.util.math.random.Random random){
        return NaturalSpawns.canSpawn(type,world,reason,pos,random);
    }
    public static void initialize(){

        NaturalSpawns.register(ModEntities.WEREWOLF,ModConfig.werewolfSpawnWeight(),1,1);
    }
}
