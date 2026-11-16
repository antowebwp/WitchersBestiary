package com.witchersbestiary.test;

import com.witchersbestiary.unicorn.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;

public final class Stage22BBopSpawnTests implements FabricGameTest {
    @GameTest(templateName="witchersbestiary:village_board_arena",tickLimit=80)
    public void naturalUnicornAppearsInLavenderField(TestContext c){
        var log=org.slf4j.LoggerFactory.getLogger("witchersbestiary");
        if(!FabricLoader.getInstance().isModLoaded("biomesoplenty")){
            log.info("BOP scenario skipped because optional mod is absent");c.complete();return;
        }
        var world=c.getWorld();
        var registry=world.getRegistryManager().get(RegistryKeys.BIOME);
        for(String path:java.util.List.of("grassland","prairie","lavender_field","mystic_grove")){
            var id=Identifier.of("biomesoplenty",path);
            var biome=registry.get(id);
            c.assertTrue(biome!=null&&biome.getSpawnSettings().getSpawnEntries(SpawnGroup.CREATURE).getEntries().stream()
                .anyMatch(entry->entry.type==Unicorns.TYPE&&entry.minGroupSize==1&&entry.maxGroupSize==1),"Registered singleton spawn in "+id);
        }
        var lavender=Identifier.of("biomesoplenty","lavender_field");
        var biome=registry.getEntry(lavender).orElseThrow();
        var center=c.getAbsolutePos(new BlockPos(23,7,23));
        var chunk=world.getChunk(center);
        chunk.populateBiomes((x,y,z,sampler)->biome,world.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
        for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){
            var at=center.add(x,0,z);
            world.setBlockState(at.down(),Blocks.GRASS_BLOCK.getDefaultState());
            for(int y=0;y<3;y++)world.setBlockState(at.up(y),Blocks.AIR.getDefaultState());
        }
        var player=c.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);player.setNoGravity(true);
        player.refreshPositionAndAngles(center.getX()+32,center.getY(),center.getZ(),90,0);
        c.runAtTick(16,()->{
            c.assertTrue(player.interactionManager.getGameMode()==GameMode.SURVIVAL,"Survival observer");
            // BiomeAccess samples adjacent quart cells near chunk edges.
            for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++)
                world.getChunk((center.getX()>>4)+dx,(center.getZ()>>4)+dz).populateBiomes(
                        (x,y,z,sampler)->biome,world.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
            c.assertTrue(world.getBiome(center).matchesId(lavender),"Installed Lavender Field biome on test patch");
            var random=Random.create(22);boolean eligible=false;
            for(int i=0;i<32;i++)eligible|=SpawnRestriction.canSpawn(Unicorns.TYPE,world,SpawnReason.NATURAL,center,random);
            c.assertTrue(eligible,"Grass and clearance accepted in Lavender Field");
            world.setBlockState(center.up(),Blocks.STONE.getDefaultState());
            for(int i=0;i<16;i++)c.assertTrue(!SpawnRestriction.canSpawn(Unicorns.TYPE,world,SpawnReason.NATURAL,center,random),"Blocked surface rejected");
            world.setBlockState(center.up(),Blocks.AIR.getDefaultState());
            var found=new java.util.ArrayList<UnicornEntity>();
            // Use the actual NATURAL path. Veto other species to keep the fixture bounded.
            int attempts=0;
            for(;attempts<2048&&found.isEmpty();attempts++){
                SpawnHelper.spawnEntitiesInChunk(SpawnGroup.CREATURE,world,chunk,center,
                    (type,pos,source)->type==Unicorns.TYPE&&world.getBiome(pos).matchesId(lavender),
                    (mob,source)->{if(mob instanceof UnicornEntity unicorn)found.add(unicorn);});
            }
            c.assertTrue(found.size()==1&&!found.getFirst().isTame()&&world.getEntity(found.getFirst().getUuid())==found.getFirst(),"One wild unicorn actually spawned through SpawnHelper");
            c.assertTrue(player.squaredDistanceTo(found.getFirst())<96*96,"Survival observer near naturally spawned unicorn");
            log.info("BOP natural unicorn witnessed in {} at {} after {} spawner invocations",lavender,found.getFirst().getBlockPos(),attempts);
            found.getFirst().discard();player.discard();c.complete();
        });
    }
    @GameTest(templateName="witchersbestiary:village_board_arena",tickLimit=80)
    public void singletonNaturalSpawnInVanillaAndBopHabitats(TestContext c) {
        var center=c.getAbsolutePos(new BlockPos(23,7,23));
        var player=c.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        player.setNoGravity(true);
        player.refreshPositionAndAngles(center.getX()+32,center.getY(),center.getZ(),90,0);
        c.runAtTick(16,()->{
            natural(c,center,Identifier.ofVanilla("forest"),Unicorns.TYPE,Blocks.GRASS_BLOCK);
            natural(c,center,Identifier.ofVanilla("windswept_hills"),
                    com.witchersbestiary.fist.FistRegistry.ROCK_TROLL,Blocks.STONE);
            if(FabricLoader.getInstance().isModLoaded("biomesoplenty"))
                natural(c,center,Identifier.of("biomesoplenty","crag"),
                        com.witchersbestiary.fist.FistRegistry.ROCK_TROLL,Blocks.STONE);
            player.discard();
            c.complete();
        });
    }

    private void natural(TestContext c, BlockPos center, Identifier id, EntityType<?> expected, net.minecraft.block.Block floor) {
        var world=c.getWorld();
        var biome=world.getRegistryManager().get(RegistryKeys.BIOME).getEntry(id).orElseThrow();
        var chunk=world.getChunk(center);
        chunk.populateBiomes((x,y,z,sampler)->biome,world.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
        for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){
            var at=center.add(x,0,z);
            world.setBlockState(at.down(),floor.getDefaultState());
            for(int y=0;y<4;y++)world.setBlockState(at.up(y),Blocks.AIR.getDefaultState());
        }
        c.assertTrue(biome.value().getSpawnSettings().getSpawnEntries(SpawnGroup.CREATURE).getEntries().stream()
                .anyMatch(entry->entry.type==expected && entry.minGroupSize==1 && entry.maxGroupSize==1),
                "Registered singleton in "+id);
        var found=new java.util.ArrayList<net.minecraft.entity.mob.MobEntity>();
        int attempts=0;
        for(;attempts<1024&&found.isEmpty();attempts++){
            SpawnHelper.spawnEntitiesInChunk(SpawnGroup.CREATURE,world,chunk,center,
                    (type,pos,source)->type==expected&&world.getBiome(pos).matchesId(id),
                    (mob,source)->{if(mob.getType()==expected)found.add(mob);});
        }
        c.assertTrue(found.size()==1 && world.getEntity(found.getFirst().getUuid())==found.getFirst(),
                "SpawnHelper created exactly one "+expected+" in "+id);
        c.assertTrue(!SpawnRestriction.canSpawn(expected,world,SpawnReason.NATURAL,center,Random.create(22)),
                "Local cap rejects the next animal");
        System.out.println("22FINAL-A NATURAL "+expected+" in "+id+" after "+attempts+" SpawnHelper invocations");
        found.getFirst().discard();
    }
}
