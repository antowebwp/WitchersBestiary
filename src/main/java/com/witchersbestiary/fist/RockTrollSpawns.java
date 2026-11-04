package com.witchersbestiary.fist;
import net.minecraft.entity.*;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.*;
import net.fabricmc.fabric.api.biome.v1.*;
public final class RockTrollSpawns {
 public static final TagKey<net.minecraft.world.biome.Biome> HABITATS=TagKey.of(RegistryKeys.BIOME,Identifier.of("witchersbestiary","rock_troll_habitats"));
 public static void initialize(){
  com.witchersbestiary.combat.mixin.SpawnRestrictionAccessor.witchersbestiary$register(FistRegistry.ROCK_TROLL,SpawnLocationTypes.ON_GROUND,Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,(type,w,reason,pos,r)->w.getBiome(pos).isIn(HABITATS)&&w.getBlockState(pos.down()).isSolidBlock(w,pos.down())&&w.getFluidState(pos).isEmpty()&&r.nextInt(8)==0);
  BiomeModifications.addSpawn(BiomeSelectors.tag(HABITATS),SpawnGroup.CREATURE,FistRegistry.ROCK_TROLL,1,1,1);
 }
}
