package com.witchersbestiary.lure;

import com.witchersbestiary.entity.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public enum LureKind {
 LESHEN("leshen_lure"),FIEND("fiend_lure"),GRIFFIN("griffin_lure");
 public final String id;
 LureKind(String id){this.id=id;}
 public EntityType<? extends PathAwareEntity> type(){return switch(this){case LESHEN->ModEntities.LESHEN;case FIEND->ModEntities.FIEND;case GRIFFIN->ModEntities.GRIFFIN;};}
 private boolean tagged(ServerWorld world,BlockPos pos,String id){return world.getBiome(pos).isIn(TagKey.of(RegistryKeys.BIOME,Identifier.of("witchersbestiary",id)));}
 public String habitatError(ServerWorld world,BlockPos pos){
  if(world.getRegistryKey()!=World.OVERWORLD)return "dimension";
  if(world.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return "peaceful";
  if(this==LESHEN){if(!tagged(world,pos,"leshen_habitats"))return "forest";if(world.isDay()&&world.getLightLevel(pos)>7)return "dark";}
  if(this==FIEND&&!tagged(world,pos,"fiend_wilderness")&&!tagged(world,pos,"forests"))return "forest";
  if(this==GRIFFIN&&(!tagged(world,pos,"griffin_ranges")||pos.getY()<80||!world.isSkyVisible(pos)||world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ())>pos.getY()+1))return "highland";
  return null;
 }
}
