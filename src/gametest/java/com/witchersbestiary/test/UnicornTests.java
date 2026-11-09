package com.witchersbestiary.test;

import com.witchersbestiary.unicorn.Unicorns;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
    public void healingIsVisibleAndShared(TestContext c) {
        var world=c.getWorld();var at=c.getAbsolutePos(new BlockPos(1,2,1));
        var owner=c.createMockCreativeServerPlayerInWorld();owner.changeGameMode(GameMode.SURVIVAL);
        owner.refreshPositionAndAngles(at.getX()+4.5,at.getY(),at.getZ()+.5,0,0);owner.setHealth(owner.getMaxHealth());
        var horse=Unicorns.TYPE.create(world);var second=Unicorns.TYPE.create(world);
        for(var pet:java.util.List.of(horse,second)){pet.bondWithPlayer(owner);pet.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);world.spawnEntity(pet);}
        var other=c.createMockCreativeServerPlayerInWorld();
        other.refreshPositionAndAngles(owner.getX(), owner.getY(), owner.getZ(), 0, 0);
        other.setHealth(10);
        c.assertTrue(!horse.healOwner(world)&&other.getHealth()==10,"Full owner and injured stranger are not healed");
        owner.setHealth(10);
        var wall=at.east(2);world.setBlockState(wall.up(),Blocks.STONE.getDefaultState());
        c.assertTrue(!horse.healOwner(world),"Wall blocks healing");
        world.setBlockState(wall.up(),Blocks.AIR.getDefaultState());
        c.assertTrue(horse.healOwner(world)&&owner.getHealth()==12.5f&&horse.healing(),"Heal exactly 2.5 HP and begin casting");
        c.assertTrue(!second.healOwner(world),"Two unicorns share one cooldown");
        horse.discard();second.discard();other.discard();owner.discard();c.complete();
    }

    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
    public void mountedHealing(TestContext c) {
        var owner = c.createMockCreativeServerPlayerInWorld();
        var horse = Unicorns.TYPE.create(c.getWorld());
        horse.bondWithPlayer(owner);
        horse.saddle(new ItemStack(Items.SADDLE), null);
        owner.startRiding(horse, true);
        owner.setHealth(owner.getMaxHealth() - 1);
        c.assertTrue(horse.healOwner(c.getWorld()) && owner.getHealth() == owner.getMaxHealth()
            && owner.getVehicle() == horse && horse.healing(), "Mounted healing caps health and keeps rider seated");
        horse.discard(); owner.discard(); c.complete();
    }

    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE, tickLimit=40)
    public void ridingSpeedProgressAndSave(TestContext c) {
        var world = c.getWorld();
        var owner = c.createMockCreativeServerPlayerInWorld();
        var horse = Unicorns.TYPE.create(world);
        var second = Unicorns.TYPE.create(world);
        horse.bondWithPlayer(owner);
        second.bondWithPlayer(owner);
        horse.saddle(new ItemStack(Items.SADDLE), null);
        var save = new NbtCompound();
        horse.writeCustomDataToNbt(save);
        save.remove("UnicornSpeedLevel");
        save.remove("UnicornRidingDistance");
        horse.readCustomDataFromNbt(save);
        double base = horse.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        owner.startRiding(horse, true);
        owner.setYaw(0);
        horse.setOnGround(true);
        var step = new Vec3d(0, 0, .15);
        horse.recordRiddenMove(owner, step);
        owner.updateInput(0, 1, false, false);
        horse.recordRiddenMove(owner, Vec3d.ZERO);
        horse.recordRiddenMove(owner, new Vec3d(0, 0, 12));
        horse.recordRiddenMove(owner, step.negate());
        second.recordRiddenMove(owner, step);
        horse.writeCustomDataToNbt(save);
        c.assertTrue(save.getInt("UnicornSpeedLevel") == 1 && save.getDouble("UnicornRidingDistance") == 0,
            "Legacy level one; no idle, teleport or opposite movement credit");
        horse.recordRiddenMove(owner, step);
        horse.recordRiddenMove(owner, step);
        horse.writeCustomDataToNbt(save);
        c.assertTrue(save.getDouble("UnicornRidingDistance") == .15, "One credit per tick");
        var restored = Unicorns.TYPE.create(world);
        restored.readCustomDataFromNbt(save);
        restored.writeCustomDataToNbt(save);
        c.assertTrue(save.getDouble("UnicornRidingDistance") == .15, "Fractional riding XP persists");
        restored.discard();
        second.writeCustomDataToNbt(save);
        c.assertTrue(save.getDouble("UnicornRidingDistance") == 0, "Other unicorn receives no riding XP");

        for (int level = 1; level < 25; level++) {
            final int current = level;
            c.runAtTick(level + 1, () -> {
                horse.writeCustomDataToNbt(save);
                save.putDouble("UnicornRidingDistance", 96 + 32 * (current - 1) - .1);
                horse.readCustomDataFromNbt(save);
                double previous = horse.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                horse.setOnGround(true);
                horse.recordRiddenMove(owner, step);
                horse.writeCustomDataToNbt(save);
                c.assertTrue(save.getInt("UnicornSpeedLevel") == current + 1
                    && horse.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) > previous,
                    "Every level increases movement speed");
            });
        }
        c.runAtTick(27, () -> {
            double max = horse.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            horse.recordRiddenMove(owner, step);
            horse.writeCustomDataToNbt(save);
            c.assertTrue(save.getInt("UnicornSpeedLevel") == 25 && save.getDouble("UnicornRidingDistance") == 0
                && Math.abs(max / base - 1.45) < .001, "Level cap and 45% increase");
            var loaded = Unicorns.TYPE.create(world);
            loaded.readCustomDataFromNbt(save);
            c.assertTrue(Math.abs(loaded.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) - max) < .0001,
                "Level cap restores its movement modifier");
            loaded.discard(); horse.discard(); second.discard(); owner.discard(); c.complete();
        });
    }
}
