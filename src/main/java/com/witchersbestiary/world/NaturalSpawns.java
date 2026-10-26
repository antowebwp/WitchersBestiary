package com.witchersbestiary.world;

import com.witchersbestiary.config.ModConfig;
import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.*;
import com.witchersbestiary.combat.mixin.SpawnRestrictionAccessor;
import net.fabricmc.fabric.api.biome.v1.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.fluid.Fluids;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;

/** Only vanilla spawn attempts invoke these bounded checks; no chunk scans or tick handlers. */
public final class NaturalSpawns {
    private NaturalSpawns(){}
    private static final java.util.Map<EntityType<?>,Integer> BASE_WEIGHTS=new java.util.HashMap<>();
    private static final SpawnLocation WETLAND_LOCATION=(world,pos,type)->SpawnLocationTypes.IN_WATER.isSpawnPositionOk(world,pos,type)||SpawnLocationTypes.ON_GROUND.isSpawnPositionOk(world,pos,type);
    public static boolean boss(EntityType<?> t){return t==ModEntities.LESHEN||t==ModEntities.NOONWRAITH||t==ModEntities.FIEND||t==ModEntities.GRIFFIN;}
    public static String habitat(EntityType<?> t){
        if(t==ModEntities.WATER_HAG)return "water_hag_habitats";
        if(t==ModEntities.GRIFFIN)return "griffin_ranges";
        if(t==ModEntities.ENDREGA_WARRIOR)return "endrega_habitats";
        if(t==ModEntities.FIEND)return "fiend_wilderness";
        if(t==ModEntities.NEKKER)return "nekker_habitats";
        if(t==ModEntities.BONECRUSHER)return "ogroid_ranges";
        if(t==ModEntities.DROWNER)return "wetlands";
        if(t==ModEntities.FOGLET||t==ModEntities.WRAITH)return "haunted";
        if(t==ModEntities.NOONWRAITH)return "fields";
        if(t==ModEntities.LESHEN)return "leshen_habitats";
        if(t==ModEntities.WEREWOLF)return "forests";
        return "hunting_grounds";
    }
    /** Only the ordinary natural-spawn pipeline calls these rules; scripted reasons bypass it. */
    public static boolean specterWindow(EntityType<?> type,long dayTime){long t=Math.floorMod(dayTime,24000);return type==ModEntities.NOONWRAITH?t<12000:type!=ModEntities.WRAITH||t>=13000&&t<23000;}
    public static double noonAdmission(long dayTime){long t=Math.floorMod(dayTime,24000);return t>=12000?0:t>=4000&&t<8000?.2:.05;}
    public static boolean canSpawn(EntityType<? extends HostileEntity> type,ServerWorldAccess access,SpawnReason reason,BlockPos pos,Random random){
        var world=access.toServerWorld();boolean boss=boss(type);
        boolean spectral=type==ModEntities.NOONWRAITH||type==ModEntities.WRAITH;
        boolean natural=reason==SpawnReason.NATURAL||reason==SpawnReason.CHUNK_GENERATION;
        if(spectral&&!natural){var specialBox=new Box(pos.getX()+.5-type.getWidth()/2,pos.getY(),pos.getZ()+.5-type.getWidth()/2,pos.getX()+.5+type.getWidth()/2,pos.getY()+type.getHeight(),pos.getZ()+.5+type.getWidth()/2);return world.getDifficulty()!=Difficulty.PEACEFUL&&pos.getY()>world.getBottomY()&&specialBox.maxY<world.getTopY()&&world.getWorldBorder().contains(specialBox)&&loaded(world,specialBox)&&world.isSpaceEmpty(specialBox);}
        if(spectral&&!specterWindow(type,world.getTimeOfDay()))return false;
        if(type==ModEntities.NOONWRAITH&&random.nextDouble()>=noonAdmission(world.getTimeOfDay()))return false;
        if(com.witchersbestiary.ward.SpiritWardBlock.blocks(world,type,reason,pos))return false;
        if(!ModConfig.spawnAllowed(Registries.ENTITY_TYPE.getId(type).getPath(),BASE_WEIGHTS.getOrDefault(type,1),boss,random)||world.getRegistryKey()!=World.OVERWORLD||world.getDifficulty()==Difficulty.PEACEFUL||!world.getWorldBorder().contains(pos)||pos.getY()<=world.getBottomY()||pos.getY()+type.getHeight()>=world.getTopY())return false;
        var tag=TagKey.of(RegistryKeys.BIOME,Identifier.of("witchersbestiary",habitat(type)));
        if(!access.getBiome(pos).isIn(tag))return false;
        var box=new Box(pos.getX()+.5-type.getWidth()/2,pos.getY(),pos.getZ()+.5-type.getWidth()/2,pos.getX()+.5+type.getWidth()/2,pos.getY()+type.getHeight(),pos.getZ()+.5+type.getWidth()/2);
        if(!world.getWorldBorder().contains(box)||!loaded(world,box)||!world.isSpaceEmpty(box))return false;
        if(type==ModEntities.LESHEN){
            var crown=box.expand(.9,0,.9).stretch(0,.5,0);
            if(!world.getWorldBorder().contains(crown)||!loaded(world,crown)||!world.isSpaceEmpty(crown)||world.containsFluid(crown))return false;
            for(BlockPos q:BlockPos.iterate(pos.add(-1,-1,-1),pos.add(1,4,1))){
                var state=access.getBlockState(q);
                if(state.isIn(net.minecraft.registry.tag.BlockTags.LOGS)||state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES))return false;
            }
        }
        if(type==ModEntities.FIEND){
            var spawn=world.getSpawnPos();double dx=pos.getX()-spawn.getX(),dz=pos.getZ()-spawn.getZ();var silhouette=box.expand(.55,0,.55);
            if(dx*dx+dz*dz<96*96||!world.getWorldBorder().contains(silhouette)||!loaded(world,silhouette)||!world.isSpaceEmpty(silhouette)||world.isNearOccupiedPointOfInterest(pos,2)||world.getStructureAccessor().getStructureContaining(pos,net.minecraft.registry.tag.StructureTags.VILLAGE).hasChildren())return false;
            for(BlockPos foot:BlockPos.iterate(pos.add(-1,-1,-1),pos.add(1,-1,1))){var state=world.getBlockState(foot);if(!state.isSolidBlock(world,foot)||state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)||state.isIn(net.minecraft.registry.tag.BlockTags.LOGS))return false;}
        }
        if(type==ModEntities.GRIFFIN){
            if(pos.getY()<80||!world.isSkyVisible(pos))return false;
            var flight=box.expand(3.1,0,3.1).stretch(0,5,0);
            if(!world.getWorldBorder().contains(flight))return false;
            for(int cx=MathHelper.floor(flight.minX)>>4;cx<=MathHelper.floor(flight.maxX)>>4;cx++)for(int cz=MathHelper.floor(flight.minZ)>>4;cz<=MathHelper.floor(flight.maxZ)>>4;cz++)if(!world.isChunkLoaded(new BlockPos(cx<<4,pos.getY(),cz<<4)))return false;
            if(!world.isSpaceEmpty(flight)||world.containsFluid(flight))return false;
            for(BlockPos foot:BlockPos.iterate(pos.add(-1,-1,-1),pos.add(1,-1,1))){var state=world.getBlockState(foot);if(!state.isSolidBlock(world,foot)||state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)||state.isIn(net.minecraft.registry.tag.BlockTags.LOGS))return false;}
        }else if(type==ModEntities.NOONWRAITH){
            if(!world.isDay()||!access.isSkyVisible(pos)||access.getLightLevel(pos)<12)return false;
        }else{
            if(type!=ModEntities.DROWNER&&type!=ModEntities.WATER_HAG&&!HostileEntity.canSpawnInDark(type,access,reason,pos,random))return false;
            if(type!=ModEntities.FIEND&&type!=ModEntities.WATER_HAG&&type!=ModEntities.DROWNER&&world.isDay()&&(type!=ModEntities.KATAKAN&&type!=ModEntities.BONECRUSHER&&type!=ModEntities.NEKKER||access.isSkyVisible(pos)||pos.getY()>40))return false;
        }
        if(type==ModEntities.WATER_HAG){
            if(!waterHagShore(access,pos)||access.getFluidState(pos.up()).isOf(Fluids.WATER))return false;
            if(!access.getBlockState(pos.down()).isSolidBlock(access,pos.down()))return false;
        }else if(type==ModEntities.DROWNER){
            if(access.getFluidState(pos).isOf(Fluids.WATER)&&access.getFluidState(pos.up()).isOf(Fluids.WATER)&&!waterExit(world,type,pos))return false;
            if(!access.getFluidState(pos).isOf(Fluids.WATER)||!access.getFluidState(pos.up()).isOf(Fluids.WATER)){
                if(!access.getFluidState(pos).isEmpty()||!access.getBlockState(pos.down()).isSolidBlock(access,pos.down()))return false;
                boolean shore=false;
                for(int dx=-4;dx<=4&&!shore;dx++)for(int dz=-4;dz<=4;dz++){
                    if(Math.abs(dx)+Math.abs(dz)>4)continue;
                    if(access.getFluidState(pos.add(dx,0,dz)).isOf(Fluids.WATER)||access.getFluidState(pos.add(dx,-1,dz)).isOf(Fluids.WATER)){shore=true;break;}
                }
                if(!shore)return false;
            }
        }else if(!access.getFluidState(pos).isEmpty()||!access.getFluidState(pos.up()).isEmpty()||!access.getBlockState(pos.down()).isSolidBlock(access,pos.down()))return false;
        for(BlockPos q:BlockPos.iterate(pos.down().add(-1,0,-1),pos.up((int)Math.ceil(type.getHeight())).add(1,0,1))){
            var state=access.getBlockState(q);if(state.isOf(Blocks.LAVA)||state.isOf(Blocks.FIRE)||state.isOf(Blocks.SOUL_FIRE)||state.isOf(Blocks.MAGMA_BLOCK)||state.isOf(Blocks.CAMPFIRE)||state.isOf(Blocks.CACTUS)||state.isOf(Blocks.POWDER_SNOW))return false;
        }
        int cap=type==ModEntities.WATER_HAG?ModConfig.groupMaximum("water_hag",2):type==ModEntities.ENDREGA_WARRIOR?ModConfig.groupMaximum("endrega_warrior",3):type==ModEntities.DROWNER?ModConfig.groupMaximum("drowner",5):type==ModEntities.GHOUL?ModConfig.groupMaximum("ghoul",4):type==ModEntities.WRAITH?ModConfig.groupMaximum("wraith",2):type==ModEntities.NEKKER?ModConfig.groupMaximum("nekker",5):type==ModEntities.FOGLET?ModConfig.groupMaximum("foglet",2):boss?1:type==ModEntities.KATAKAN||type==ModEntities.WEREWOLF||type==ModEntities.BONECRUSHER?1:6;double radius=type==ModEntities.FIEND?128:boss?96:type==ModEntities.BONECRUSHER?64:48;
        int nearby=world.getOtherEntities(null,new Box(pos).expand(radius),e->e.isAlive()&&e.getType()==type&&(!(e instanceof NoonwraithEntity n)||!n.isIllusion())).size();
        if(type==ModEntities.WATER_HAG&&nearby>0&&random.nextInt(4)!=0)return false;
        return nearby<cap;
    }
    public static <T extends HostileEntity> void register(EntityType<T> type,int weight,int min,int max){
        BASE_WEIGHTS.put(type,weight);
        SpawnRestrictionAccessor.witchersbestiary$register(type,(type==ModEntities.DROWNER||type==ModEntities.WATER_HAG)?WETLAND_LOCATION:SpawnLocationTypes.ON_GROUND,Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,NaturalSpawns::canSpawn);
        String id=Registries.ENTITY_TYPE.getId(type).getPath();int adjusted=ModConfig.spawnWeight(id,weight,boss(type));
        var group=ModConfig.groupSize(id,min,max);
        if(adjusted>0)BiomeModifications.addSpawn(BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME,Identifier.of("witchersbestiary",habitat(type)))),SpawnGroup.MONSTER,type,adjusted,group.min(),group.max());
    }
    private static boolean loaded(net.minecraft.server.world.ServerWorld world,Box box){
        for(int x=MathHelper.floor(box.minX)>>4;x<=MathHelper.floor(box.maxX)>>4;x++)for(int z=MathHelper.floor(box.minZ)>>4;z<=MathHelper.floor(box.maxZ)>>4;z++)if(!world.isChunkLoaded(new BlockPos(x<<4,MathHelper.floor(box.minY),z<<4)))return false;
        return true;
    }
    private static boolean waterExit(net.minecraft.server.world.ServerWorld world,EntityType<?> type,BlockPos from){
        for(int r=2;r<=8;r+=2)for(int i=0;i<8;i++){
            int x=from.getX()+(int)Math.round(Math.cos(i*Math.PI/4)*r),z=from.getZ()+(int)Math.round(Math.sin(i*Math.PI/4)*r);var sample=new BlockPos(x,from.getY(),z);if(!world.isChunkLoaded(sample))continue;
            for(int dy=-2;dy<=2;dy++){var exit=sample.up(dy);var support=exit.down();if(!world.getBlockState(support).isSolidBlock(world,support)||!world.getFluidState(support).isEmpty())continue;
            var box=new Box(exit.getX()+.5-type.getWidth()/2,exit.getY(),exit.getZ()+.5-type.getWidth()/2,exit.getX()+.5+type.getWidth()/2,exit.getY()+type.getHeight(),exit.getZ()+.5+type.getWidth()/2);
            if(world.getWorldBorder().contains(box)&&world.isSpaceEmpty(box)&&!world.containsFluid(box))return true;}
        }return false;
    }
    public static boolean waterHagShore(WorldAccess world,BlockPos pos){
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(Math.abs(x)+Math.abs(z)<=3){
            var q=pos.add(x,0,z);if(world.isChunkLoaded(q)&&(world.getFluidState(q).isOf(Fluids.WATER)||world.getFluidState(q.down()).isOf(Fluids.WATER)))return true;
        }return false;
    }
    public static void initialize(){register(ModEntities.WATER_HAG,4,1,2);register(ModEntities.GRIFFIN,1,1,1);register(ModEntities.ENDREGA_WARRIOR,5,1,3);register(ModEntities.FIEND,1,1,1);register(ModEntities.NEKKER,12,3,5);register(ModEntities.BONECRUSHER,2,1,1);register(ModEntities.WRAITH,5,1,2);register(ModEntities.LESHEN,1,1,1);}
}
