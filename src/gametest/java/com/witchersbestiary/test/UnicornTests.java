package com.witchersbestiary.test;

import com.witchersbestiary.unicorn.Unicorns;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

public final class UnicornTests implements FabricGameTest {
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE, tickLimit=30)
    public void ownershipRidingRecoveryAndSave(TestContext c) {
        var world=c.getWorld();
        var at=c.getAbsolutePos(new BlockPos(1,2,1));
        var owner=c.createMockCreativeServerPlayerInWorld();
        var other=c.createMockCreativeServerPlayerInWorld();
        owner.changeGameMode(GameMode.SURVIVAL);
        owner.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.GOLDEN_APPLE));
        var horse=Unicorns.TYPE.create(world);
        horse.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        world.spawnEntity(horse);
        horse.interactMob(owner,Hand.MAIN_HAND);
        c.assertTrue(horse.isTame()&&owner.getUuid().equals(horse.getOwnerUuid())&& (owner.isCreative() || owner.getMainHandStack().isEmpty()),"Peaceful owner bonding respects creative consumption");
        owner.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);
        horse.saddle(new ItemStack(Items.SADDLE),null);
        horse.interactMob(other,Hand.MAIN_HAND);
        c.assertTrue(!other.hasVehicle(),"Foreign rider denied");
        owner.setSneaking(true);horse.interactMob(owner,Hand.MAIN_HAND);owner.setSneaking(false);
        c.assertTrue(horse.waiting(),"Owner can set wait");
        horse.interactMob(owner,Hand.MAIN_HAND);
        c.assertTrue(owner.getVehicle()==horse&&horse.getControllingPassenger()==owner&&horse.canJump(),"Owner rides with vanilla saddle controls and jump");
        horse.damage(world.getDamageSources().genericKill(),1000);
        c.assertTrue(horse.recovering()&&horse.getHealth()==1&&horse.isAlive()&&!owner.hasVehicle(),"Lethal damage starts recovery without death or mount");
        var data=new NbtCompound();horse.writeCustomDataToNbt(data);
        var restored=Unicorns.TYPE.create(world);restored.readCustomDataFromNbt(data);
        c.assertTrue(restored.recovering()&&restored.waiting()&&restored.isSaddled()&&owner.getUuid().equals(restored.getOwnerUuid()),"Owner, saddle, wait and recovery persist");
        for(int i=0;i<600;i++)restored.tick();
        c.assertTrue(!restored.recovering()&&restored.getHealth()==restored.getMaxHealth(),"Recovery ends on the same entity");
        horse.discard();restored.discard();owner.discard();other.discard();c.complete();
    }

    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
    public void combatHealingIsVisibleAndShared(TestContext c) {
        var world=c.getWorld();var at=c.getAbsolutePos(new BlockPos(1,2,1));
        var owner=c.createMockCreativeServerPlayerInWorld();owner.changeGameMode(GameMode.SURVIVAL);
        owner.refreshPositionAndAngles(at.getX()+4.5,at.getY(),at.getZ()+.5,0,0);owner.setHealth(10);
        var horse=Unicorns.TYPE.create(world);var second=Unicorns.TYPE.create(world);
        for(var pet:java.util.List.of(horse,second)){pet.bondWithPlayer(owner);pet.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);world.spawnEntity(pet);}
        c.assertTrue(!horse.healOwner(world),"No out-of-combat healing");
        var hostile=EntityType.ZOMBIE.create(world);owner.setAttacker(hostile);
        var wall=at.east(2);world.setBlockState(wall.up(),Blocks.STONE.getDefaultState());
        c.assertTrue(!horse.healOwner(world),"Wall blocks healing");
        world.setBlockState(wall.up(),Blocks.AIR.getDefaultState());
        c.assertTrue(horse.healOwner(world)&&owner.getHealth()==12.5f&&horse.healing(),"Heal exactly 2.5 HP and begin casting");
        c.assertTrue(!second.healOwner(world),"Two unicorns share one cooldown");
        horse.discard();second.discard();hostile.discard();owner.discard();c.complete();
    }
}
