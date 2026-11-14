package com.witchersbestiary.test;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.fist.FistRegistry;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import net.minecraft.util.math.BlockPos;

public final class CompanionGuardTests implements FabricGameTest {
    @GameTest(templateName="witchersbestiary:village_board_arena",tickLimit=335)
    public void bothPetsGuardAndRetreat(TestContext c) {
        var world=c.getWorld();
        var center=c.getAbsolutePos(new BlockPos(10,10,10));
        for(int x=-5;x<=21;x++)for(int z=-5;z<=15;z++){
            var at=center.add(x,0,z);
            world.setBlockState(at.down(),Blocks.STONE.getDefaultState());
            for(int y=0;y<4;y++)world.setBlockState(at.up(y),Blocks.AIR.getDefaultState());
        }
        var owner=c.createMockCreativeServerPlayerInWorld();
        owner.refreshPositionAndAngles(center.getX()+15.5,center.getY(),center.getZ()+.5,0,0);
        var nekker=ModEntities.NEKKER.create(world);
        var troll=FistRegistry.ROCK_TROLL.create(world);
        var pets=java.util.List.of(nekker,troll);
        var posts=java.util.List.of(center,center.south(9));
        var prefixes=java.util.List.of("Nekker","Troll");
        var enemies=new java.util.ArrayList<net.minecraft.entity.mob.ZombieEntity>();
        for(int i=0;i<2;i++){
            var pet=pets.get(i);var post=posts.get(i);var key=prefixes.get(i);
            var n=new NbtCompound();
            n.putUuid(key+"Owner",owner.getUuid());n.putInt(key+"Mode",2);
            n.putDouble(key+"PostX",post.getX()+.5);n.putDouble(key+"PostY",post.getY());n.putDouble(key+"PostZ",post.getZ()+.5);
            pet.readCustomDataFromNbt(n);
            pet.refreshPositionAndAngles(post.getX()+.5,post.getY(),post.getZ()+.5,-90,0);
            world.spawnEntity(pet);
            var enemy=EntityType.ZOMBIE.create(world);
            enemy.setAiDisabled(true);enemy.equipStack(net.minecraft.entity.EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
            enemy.refreshPositionAndAngles(post.getX()+2.1,post.getY(),post.getZ()+.5,0,0);
            world.spawnEntity(enemy);
            enemies.add(enemy);
        }
        nekker.storage().setStack(8,new ItemStack(Items.EMERALD,7));
        c.runAtTick(65,()->{
            for(int i=0;i<2;i++){
                var pet=pets.get(i);var post=posts.get(i);var enemy=enemies.get(i);
                c.assertTrue(enemy.getHealth()<enemy.getMaxHealth(),"Guard damaged its hostile intruder: "+prefixes.get(i)+" pose/target "+pet.getTarget()+" at "+pet.getPos());
                enemy.refreshPositionAndAngles(post.getX()+12.5,post.getY(),post.getZ()+.5,0,0);
            }
        });
        c.runAtTick(100,()->{
            for(int i=0;i<2;i++){
                var pet=pets.get(i);var post=posts.get(i);
                c.assertTrue(pet.getTarget()==null&&pet.squaredDistanceTo(post.toCenterPos())<4,"Boundary clears target and returns to post");
                pet.setHealth(pet.getMaxHealth()*.2f);
            }
        });
        c.runAtTick(135,()->{
            for(int i=0;i<2;i++){
                var pet=pets.get(i);var key=prefixes.get(i);
                var saved=new NbtCompound();pet.writeCustomDataToNbt(saved);
                c.assertTrue(saved.getBoolean(key+"Retreating")&&pet.getTarget()==null&&pet.getX()>posts.get(i).getX()+2,
                    "Critical health retreats toward owner");
                pet.setHealth(pet.getMaxHealth()*.5f);
            }
        });
        c.runAtTick(145,()->{
            for(int i=0;i<2;i++){
                var pet=pets.get(i);var key=prefixes.get(i);
                var saved=new NbtCompound();pet.writeCustomDataToNbt(saved);pet.readCustomDataFromNbt(saved);
                c.assertTrue(!saved.getBoolean(key+"Retreating")&&saved.getInt(key+"Mode")==2
                    &&saved.getInt(key+"GuardRadius")==10&&saved.getDouble(key+"PostX")==posts.get(i).getX()+.5,
                    "Recovery and saved guard post");
            }
            c.assertTrue(nekker.storage().getStack(8).getCount()==7,"Nekker inventory survives reload");
            enemies.forEach(net.minecraft.entity.Entity::discard);
            for(int i=0;i<2;i++){
                var pool=posts.get(i).east(4);
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
                    var at=pool.add(x,0,z);
                    world.setBlockState(at.down(3),Blocks.STONE.getDefaultState());
                    world.setBlockState(at.down(2),Blocks.WATER.getDefaultState());
                    world.setBlockState(at.down(),Blocks.WATER.getDefaultState());
                }
                pets.get(i).refreshPositionAndAngles(pool.getX()+.5,pool.getY()-1.5,pool.getZ()+.5,0,0);
            }
        });
        c.runAtTick(290,()->{
            c.assertTrue(pets.stream().allMatch(pet->pet.isAlive()&&!pet.isTouchingWater()),
                "Both pets escape a two-block-deep pool");
            var feeder = c.createMockPlayer(net.minecraft.world.GameMode.SURVIVAL);
            for (int i=0;i<2;i++) {
                var pet=pets.get(i);
                var ownership = new NbtCompound();
                pet.writeCustomDataToNbt(ownership);
                ownership.putUuid(prefixes.get(i)+"Owner", feeder.getUuid());
                pet.readCustomDataFromNbt(ownership);
                pet.setHealth(pet.getMaxHealth()*.10f);
                pet.refreshPositionAndAngles(center.getX()+10.5,center.getY(),center.getZ()+.5,0,0);
                feeder.refreshPositionAndAngles(pet.getX()+1,pet.getY(),pet.getZ(),0,0);
                feeder.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,ItemStack.EMPTY);
                pet.interact(feeder,net.minecraft.util.Hand.MAIN_HAND); // Guard -> Follow
                pet.interact(feeder,net.minecraft.util.Hand.MAIN_HAND); // Follow -> Wait
                var saved=new NbtCompound();
                pet.writeCustomDataToNbt(saved);
                c.assertTrue(saved.getInt(prefixes.get(i)+"Mode")==1 && pet.getNavigation().isIdle(),"Wait stops a critically wounded companion");
                float health=pet.getHealth();
                feeder.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,new ItemStack(i==0?Items.BEEF:Items.STONE,2));
                pet.interact(feeder,net.minecraft.util.Hand.MAIN_HAND);
                c.assertTrue(Math.abs(pet.getHealth()-health-6)<.01,"Owner food restores 6 HP");
                pet.setHealth(pet.getMaxHealth()*.6f);
                pet.writeCustomDataToNbt(saved);
                c.assertTrue(saved.getInt(prefixes.get(i)+"Mode")==1,"Healing preserves Wait");
                feeder.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,ItemStack.EMPTY);
                pet.interact(feeder,net.minecraft.util.Hand.MAIN_HAND);
                pet.writeCustomDataToNbt(saved);
                c.assertTrue(saved.getInt(prefixes.get(i)+"Mode")==2,"Owner can command Guard after feeding");
                pet.interact(feeder,net.minecraft.util.Hand.MAIN_HAND);
                pet.writeCustomDataToNbt(saved);
                c.assertTrue(saved.getInt(prefixes.get(i)+"Mode")==0,"Owner can command Follow after feeding");
            }
            pets.forEach(net.minecraft.entity.Entity::discard);owner.discard();c.complete();
        });
    }
}
