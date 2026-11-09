package com.witchersbestiary.test;

import com.witchersbestiary.signs.*;
import com.witchersbestiary.unicorn.Unicorns;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.BiomeKeys;

public final class Stage22BWorldTests implements FabricGameTest {
    private static final String ARENA="witchersbestiary:village_board_arena";
    private final SignTests fixtures=new SignTests();

    @GameTest(templateName=ARENA,tickLimit=65)
    public void yrdenSlowSmallCirclesAndPulses(TestContext c) {
        var casters=new java.util.ArrayList<ServerPlayerEntity>();var foes=new java.util.ArrayList<net.minecraft.entity.mob.ZombieEntity>();
        for(int form=1;form<=3;form++){
            var p=new SignFormsTests().equipped(c,SignType.YRDEN,form);p.setPosition(p.getX()+form*8,p.getY(),p.getZ());
            var at=p.getBlockPos();
            for(int dx=-5;dx<=5;dx++)for(int dz=-4;dz<=4;dz++){
                c.getWorld().setBlockState(at.add(dx,-1,dz),Blocks.STONE.getDefaultState());
                c.getWorld().setBlockState(at.add(dx,0,dz),Blocks.AIR.getDefaultState());
            }
            var foe=fixtures.target(c,p);foe.setPosition(p.getX()+(form==2?3.7:.8),p.getY(),p.getZ());
            c.assertTrue(SignManager.cast(p,1,1),"Yrden alternative placed");
            casters.add(p);foes.add(foe);
        }
        c.runAtTick(21,()->{
            c.assertTrue(foes.get(0).getStatusEffect(StatusEffects.SLOWNESS).getAmplifier()>=2,"Binding trap stronger slow");
            c.assertTrue(foes.get(1).getHealth()<20,"Side circle hits beyond center radius");
            c.assertTrue(foes.get(2).getHealth()<=18,"Pulse increases damage");
            casters.forEach(ServerPlayerEntity::discard);foes.forEach(net.minecraft.entity.Entity::discard);c.complete();
        });
    }
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,tickLimit=40)
    public void naturalSpawnEntriesClearanceAndLocalCap(TestContext c){
        var world=c.getWorld();var biomes=world.getRegistryManager().get(RegistryKeys.BIOME);
        for(var biome:java.util.List.of(BiomeKeys.PLAINS,BiomeKeys.MEADOW,BiomeKeys.FLOWER_FOREST)){
            var pool=biomes.get(biome).getSpawnSettings().getSpawnEntries(SpawnGroup.CREATURE);
            c.assertTrue(pool.getEntries().stream().anyMatch(entry->entry.type==Unicorns.TYPE),"Natural spawn entry in "+biome.getValue());
        }
        var at=c.getAbsolutePos(new BlockPos(1,12,1));
        world.setBlockState(at.down(),Blocks.GRASS_BLOCK.getDefaultState());
        world.setBlockState(at.east(2),Blocks.SEA_LANTERN.getDefaultState());
        c.runAtTick(12,()->{
            var random=Random.create(2202);int allowed=0;
            for(int i=0;i<64;i++)if(SpawnRestriction.canSpawn(Unicorns.TYPE,world,SpawnReason.NATURAL,at,random))allowed++;
            c.assertTrue(allowed>0&&allowed<64,"Safe natural location is eligible with rare random gate");
            world.setBlockState(at.up(),Blocks.STONE.getDefaultState());
            for(int i=0;i<16;i++)c.assertTrue(!SpawnRestriction.canSpawn(Unicorns.TYPE,world,SpawnReason.NATURAL,at,random),"Obstruction prevents natural spawn");
            world.setBlockState(at.up(),Blocks.AIR.getDefaultState());
            var wild=Unicorns.TYPE.create(world);wild.refreshPositionAndAngles(at.getX()+5.5,at.getY(),at.getZ()+.5,0,0);world.spawnEntity(wild);
            for(int i=0;i<16;i++)c.assertTrue(!SpawnRestriction.canSpawn(Unicorns.TYPE,world,SpawnReason.NATURAL,at,random),"Nearby wild unicorn caps another spawn");
            wild.discard();c.complete();
        });
    }
}
