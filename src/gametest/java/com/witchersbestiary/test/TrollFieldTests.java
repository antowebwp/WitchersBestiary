package com.witchersbestiary.test;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.fist.FistRegistry;
import com.witchersbestiary.unicorn.Unicorns;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public final class TrollFieldTests implements FabricGameTest {
    private BlockPos arena(TestContext c) {
        var center = c.getAbsolutePos(new BlockPos(8, 12, 8));
        for (int x = -7; x <= 32; x++) for (int z = -9; z <= 9; z++) {
            var pos = center.add(x, 0, z);
            c.getWorld().setBlockState(pos.down(), Blocks.STONE.getDefaultState());
            for (int y = 0; y < 4; y++) c.getWorld().setBlockState(pos.up(y), Blocks.AIR.getDefaultState());
        }
        return center;
    }

    private void place(Entity entity, BlockPos pos) {
        entity.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
    }

    @GameTest(templateName="witchersbestiary:village_board_arena", tickLimit=950)
    public void combatAttacksAndFriendlyFire(TestContext c) {
        var world=c.getWorld();
        var at=arena(c);
        world.setTimeOfDay(18000);
        var owner=c.createMockCreativeServerPlayerInWorld();
        place(owner,at.west(2));
        var troll=FistRegistry.ROCK_TROLL.create(world);
        var tag=new NbtCompound();
        tag.putUuid("TrollOwner",owner.getUuid());
        troll.readCustomDataFromNbt(tag);
        place(troll,at);
        world.spawnEntity(troll);
        var hostile=EntityType.ZOMBIE.create(world);
        hostile.setAiDisabled(true);
        hostile.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(1000);
        hostile.setHealth(1000);
        hostile.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);
        place(hostile,at.east(7));
        world.spawnEntity(hostile);
        var unicorn=Unicorns.TYPE.create(world);
        unicorn.bondWithPlayer(owner);
        unicorn.setAiDisabled(true);
        place(unicorn,at.south(2));
        world.spawnEntity(unicorn);
        var nekker=ModEntities.NEKKER.create(world);
        var petTag=new NbtCompound();
        petTag.putUuid("NekkerOwner",owner.getUuid());
        nekker.readCustomDataFromNbt(petTag);
        nekker.setAiDisabled(true);
        place(nekker,at.north(2));
        world.spawnEntity(nekker);
        var ally=EntityType.VILLAGER.create(world);
        ally.setAiDisabled(true);
        place(ally,at.west(2).south());
        world.spawnEntity(ally);
        float[] health={hostile.getHealth()};
        boolean[] hits=new boolean[3];
        c.runAtEveryTick(()->{
            owner.setAttacker(hostile);
            if(hostile.getHealth()<health[0]){
                switch(troll.pose()){
                    case "throw" -> hits[0]=true;
                    case "wild_quick","wild_heavy" -> hits[1]=true;
                    case "stomp" -> hits[2]=true;
                }
                health[0]=hostile.getHealth();
            }
            if(hits[0]){
                place(hostile,troll.getBlockPos().east(2));
                hostile.setVelocity(0,0,0);
                hostile.setOnGround(true);
            }
            if(hits[0]&&hits[1]&&hits[2]){
                c.assertTrue(unicorn.getHealth()==unicorn.getMaxHealth()&&nekker.getHealth()==nekker.getMaxHealth()
                    &&ally.getHealth()==ally.getMaxHealth()&&owner.getHealth()==owner.getMaxHealth(),"Allies take no damage");
                troll.discard();hostile.discard();unicorn.discard();nekker.discard();ally.discard();owner.discard();c.complete();
            }
        });
        c.runAtTick(900,()->c.assertTrue(false,"Confirmed attacks: "+java.util.Arrays.toString(hits)));
    }

    @GameTest(templateName="witchersbestiary:village_board_arena", tickLimit=260)
    public void pathAndCatchUpWithOwnerOnlyControl(TestContext c) {
        var world=c.getWorld();
        var at=arena(c);
        var owner=c.createMockCreativeServerPlayerInWorld();
        var stranger=c.createMockCreativeServerPlayerInWorld();
        place(owner,at.west(2));
        place(stranger,at.south(2));
        var troll=FistRegistry.ROCK_TROLL.create(world);
        var tag=new NbtCompound();
        tag.putUuid("TrollOwner",owner.getUuid());
        troll.readCustomDataFromNbt(tag);
        place(troll,at);
        world.spawnEntity(troll);
        owner.getInventory().clear();stranger.getInventory().clear();
        troll.interact(owner,Hand.MAIN_HAND);
        troll.interact(stranger,Hand.MAIN_HAND);
        var saved=new NbtCompound();
        troll.writeCustomDataToNbt(saved);
        c.assertTrue(saved.getBoolean("TrollWaiting"),"Second player cannot alter the owner mode");
        troll.interact(owner,Hand.MAIN_HAND);
        for(int z=-2;z<=2;z++)for(int y=0;y<4;y++)world.setBlockState(at.add(4,y,z),Blocks.STONE.getDefaultState());
        place(owner,at.east(11));
        c.runAtTick(170,()->{
            c.assertTrue(troll.getX()>at.getX()+4&&troll.squaredDistanceTo(owner)<49,"Troll navigates around the wall");
            for(int z=-4;z<=4;z++)world.setBlockState(at.add(26,0,z),Blocks.LAVA.getDefaultState());
            for(int y=0;y<4;y++)world.setBlockState(at.add(29,y,3),Blocks.STONE.getDefaultState());
            place(owner,at.east(28));
        });
        c.runAtTick(220,()->{
            c.assertTrue(troll.squaredDistanceTo(owner)<81&&world.isSpaceEmpty(troll,troll.getBoundingBox())
                &&!world.containsFluid(troll.getBoundingBox()),"Distant catch-up lands in clear, dry space");
            troll.discard();owner.discard();stranger.discard();c.complete();
        });
    }
}
