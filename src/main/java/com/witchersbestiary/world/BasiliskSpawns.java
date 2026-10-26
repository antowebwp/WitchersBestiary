package com.witchersbestiary.world;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.InfernalBasiliskEntity;
import com.witchersbestiary.combat.mixin.SpawnRestrictionAccessor;
import com.witchersbestiary.config.ModConfig;
import net.fabricmc.fabric.api.biome.v1.*;
import net.minecraft.entity.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;

public final class BasiliskSpawns {
    public static final TagKey<net.minecraft.world.biome.Biome> HABITATS=TagKey.of(RegistryKeys.BIOME,Identifier.of("witchersbestiary","basilisk_habitats"));
    public static boolean canSpawn(EntityType<InfernalBasiliskEntity> type,ServerWorldAccess access,SpawnReason reason,BlockPos pos,Random random){
        var world=access.toServerWorld();if(world.getRegistryKey()!=World.NETHER||world.getDifficulty()==Difficulty.PEACEFUL||!ModConfig.spawnAllowed("infernal_basilisk",1,true,random)||!access.getBiome(pos).isIn(HABITATS))return false;
        var box=new Box(pos.getX()-3.5,pos.getY(),pos.getZ()-3.5,pos.getX()+4.5,pos.getY()+3,pos.getZ()+4.5);
        if(!world.getWorldBorder().contains(box))return false;
        for(int x=MathHelper.floor(box.minX)>>4;x<=MathHelper.floor(box.maxX)>>4;x++)for(int z=MathHelper.floor(box.minZ)>>4;z<=MathHelper.floor(box.maxZ)>>4;z++)if(!world.isChunkLoaded(new BlockPos(x<<4,pos.getY(),z<<4)))return false;
        if(!world.isSpaceEmpty(box)||world.containsFluid(box)||!access.getBlockState(pos.down()).isSolidBlock(access,pos.down()))return false;
        boolean lava=false;
        for(int dx=-8;dx<=8&&!lava;dx+=2)for(int dz=-8;dz<=8&&!lava;dz+=2){BlockPos p=pos.add(dx,-1,dz);if(world.isChunkLoaded(p)&&world.getFluidState(p).isIn(FluidTags.LAVA)&&world.getBlockState(p.up()).isAir())lava=true;}
        return lava&&world.getEntitiesByClass(InfernalBasiliskEntity.class,new Box(pos).expand(96),e->e.isAlive()&&!e.isTamed()).isEmpty();
    }
    public static void initialize(){
        SpawnRestrictionAccessor.witchersbestiary$register(ModEntities.INFERNAL_BASILISK,SpawnLocationTypes.ON_GROUND,Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,BasiliskSpawns::canSpawn);
        int weight=ModConfig.spawnWeight("infernal_basilisk",1,true);
        if(weight>0)BiomeModifications.addSpawn(BiomeSelectors.tag(HABITATS),SpawnGroup.MONSTER,ModEntities.INFERNAL_BASILISK,weight,1,1);
    }
}
