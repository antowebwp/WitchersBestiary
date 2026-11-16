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
    static net.minecraft.server.network.ServerPlayerEntity player(TestContext context) {
        var world = context.getWorld();
        var id = java.util.UUID.randomUUID();
        var profile = new com.mojang.authlib.GameProfile(id, "Uni" + id.toString().substring(0, 8));
        var data = net.minecraft.server.network.ConnectedClientData.createDefault(profile, false);
        var player = new net.minecraft.server.network.ServerPlayerEntity(world.getServer(), world, profile, data.syncedOptions());
        player.networkHandler = new net.minecraft.server.network.ServerPlayNetworkHandler(world.getServer(),
                new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND), player, data);
        world.onPlayerConnected(player);
        player.changeGameMode(GameMode.CREATIVE);
        return player;
    }
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
    public void ownerFeedingSpendsOneAndPreservesMount(TestContext c) {
        var world=c.getWorld();
        var at=c.getAbsolutePos(new BlockPos(1,2,1));
        var owner=player(c);
        var stranger=player(c);
        owner.changeGameMode(GameMode.SURVIVAL);
        var horse=Unicorns.TYPE.create(world);
        for(var entity:java.util.List.of(horse,owner,stranger))
            entity.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        horse.bondWithPlayer(owner);
        world.spawnEntity(horse);
        horse.saddle(new ItemStack(Items.SADDLE),null);
        var food=new ItemStack(Items.CARROT,3);
        owner.setStackInHand(Hand.MAIN_HAND,food);
        horse.interactMob(owner,Hand.MAIN_HAND);
        c.assertTrue(food.getCount()==3&&!owner.hasVehicle(),"Full health does not spend food or accidentally mount");
        horse.setHealth(horse.getMaxHealth()-9);
        float before=horse.getHealth();
        stranger.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.CARROT));
        horse.interactMob(stranger,Hand.MAIN_HAND);
        c.assertTrue(horse.getHealth()==before,"Stranger cannot feed");
        horse.interactMob(owner,Hand.MAIN_HAND);
        c.assertTrue(horse.getHealth()==before+4&&food.getCount()==(owner.isCreative()?3:2),"One carrot restores four HP");
        horse.interactMob(owner,Hand.MAIN_HAND);
        c.assertTrue(horse.getHealth()==before+4&&food.getCount()==(owner.isCreative()?3:2),"Cooldown rejects duplicate feeding");
        owner.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);
        horse.interactMob(owner,Hand.MAIN_HAND);
        c.assertTrue(owner.getVehicle()==horse,"Empty hand still mounts");
        owner.stopRiding();horse.discard();owner.discard();stranger.discard();c.complete();
    }

    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE, tickLimit=30)
    public void ownershipRidingRecoveryAndSave(TestContext c) {
        var world=c.getWorld();
        var at=c.getAbsolutePos(new BlockPos(1,2,1));
        var owner=player(c);
        var other=player(c);
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
        horse.equipBodyArmor(new ItemStack(Items.IRON_HORSE_ARMOR));
        var data = horse.writeNbt(new NbtCompound());
        var id = horse.getUuid();
        data.putShort("DeathTime", (short) 12);
        data.putFloat("Health", 0);
        data.putDouble("UnicornRidingDistance", 24.5);
        data.putInt("UnicornSpeedLevel", 7);
        var restored = Unicorns.TYPE.create(world);
        restored.readNbt(data);
        c.assertTrue(restored.getUuid().equals(id) && restored.recovering() && restored.waiting()
                && restored.isAlive() && restored.isSaddled() && restored.getBodyArmor().isOf(Items.IRON_HORSE_ARMOR)
                && owner.getUuid().equals(restored.getOwnerUuid()), "The same UUID, owner, saddle, armor, wait and recovery persist");
        for (int i = 0; i < 600; i++) restored.tick();
        c.assertTrue(!restored.recovering() && restored.getHealth() == restored.getMaxHealth()
                && restored.deathTime == 0, "Recovery clears vanilla death state");
        var finished = restored.writeNbt(new NbtCompound());
        c.assertTrue(finished.getInt("UnicornSpeedLevel") == 7 && finished.getDouble("UnicornRidingDistance") == 24.5,
                "Recovery preserves speed progression");
        c.assertTrue(restored.interactMob(other, Hand.MAIN_HAND) == net.minecraft.util.ActionResult.PASS,
                "Non-owner cannot mount a recovered unicorn");
        horse.discard();restored.discard();owner.discard();other.discard();c.complete();
    }

    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
    public void healingIsVisibleAndShared(TestContext c) {
        var world=c.getWorld();var at=c.getAbsolutePos(new BlockPos(1,2,1));
        var owner=player(c);owner.changeGameMode(GameMode.SURVIVAL);
        owner.refreshPositionAndAngles(at.getX()+4.5,at.getY(),at.getZ()+.5,0,0);owner.setHealth(owner.getMaxHealth());
        var horse=Unicorns.TYPE.create(world);var second=Unicorns.TYPE.create(world);
        for(var pet:java.util.List.of(horse,second)){pet.bondWithPlayer(owner);pet.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);world.spawnEntity(pet);}
        var other=player(c);
        other.refreshPositionAndAngles(owner.getX(), owner.getY(), owner.getZ(), 0, 0);
        other.setHealth(10);
        c.assertTrue(!horse.healOwner(world)&&other.getHealth()==10,"Full owner and injured stranger are not healed");
        owner.setHealth(10);
        var wall=at.east(2);world.setBlockState(wall.up(),Blocks.STONE.getDefaultState());
        c.assertTrue(!horse.healOwner(world),"Wall blocks healing");
        world.setBlockState(wall.up(),Blocks.AIR.getDefaultState());
        c.assertTrue(horse.healOwner(world)&&owner.getHealth()==14f&&horse.healing(),"Heal exactly 4 HP and begin casting");
        c.assertTrue(!second.healOwner(world),"Two unicorns share one cooldown");
        horse.discard();second.discard();other.discard();owner.discard();c.complete();
    }

    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
    public void mountedHealing(TestContext c) {
        var owner = player(c);
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
        var owner = player(c);
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
        owner.startRiding(horse, true);
        owner.setYaw(0);
        horse.setOnGround(true);
        var step = new Vec3d(0, 0, .402);
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
        c.assertTrue(save.getDouble("UnicornRidingDistance") == .402, "One credit per tick");
        var restored = Unicorns.TYPE.create(world);
        restored.readCustomDataFromNbt(save);
        restored.writeCustomDataToNbt(save);
        c.assertTrue(save.getDouble("UnicornRidingDistance") == .402, "Fractional riding XP persists");
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
                && Math.abs(max * 42.1579 - 22) < .001, "Level cap and 22 blocks/s");
            var loaded = Unicorns.TYPE.create(world);
            loaded.readCustomDataFromNbt(save);
            c.assertTrue(Math.abs(loaded.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) - max) < .0001,
                "Level cap restores its movement modifier");
            loaded.discard(); horse.discard(); second.discard(); owner.discard(); c.complete();
        });
    }
    @GameTest(templateName="witchersbestiary:village_board_arena", tickLimit=165)
    public void followWaitGuardAndWhistleRetainSameUnicorn(TestContext c) {
        var world=c.getWorld();
        var at=c.getAbsolutePos(new BlockPos(6,7,6));
        for (int x=-7;x<=21;x++) for (int z=-5;z<=5;z++) {
            world.setBlockState(at.add(x,-1,z),Blocks.GRASS_BLOCK.getDefaultState());
            for (int y=0;y<3;y++) world.setBlockState(at.add(x,y,z),Blocks.AIR.getDefaultState());
        }
        var owner=player(c);
        owner.refreshPositionAndAngles(at.getX()+12.5,at.getY(),at.getZ()+.5,0,0);
        var horse=Unicorns.TYPE.create(world);
        horse.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        horse.bondWithPlayer(owner);
        world.spawnEntity(horse);
        var id=horse.getUuid();
        c.runAtTick(65,()->{
            c.assertTrue(horse.squaredDistanceTo(owner)<95,"FOLLOW closes a twelve-block gap on foot");
            owner.setSneaking(true);
            horse.interactMob(owner,Hand.MAIN_HAND);
            c.assertTrue(horse.waiting(),"FOLLOW toggles to WAIT");
            owner.refreshPositionAndAngles(at.getX()+19.5,at.getY(),at.getZ()+.5,0,0);
            var whistle=new ItemStack(Unicorns.WHISTLE);
            owner.setStackInHand(Hand.MAIN_HAND,whistle);
            horse.interactMob(owner,Hand.MAIN_HAND);
            c.assertTrue(horse.waiting(),"Binding preserves WAIT");
        });
        c.runAtTick(115,()->{
            c.assertTrue(horse.waiting() && horse.squaredDistanceTo(owner)>36,
                "WAIT does not follow or teleport after the owner walks away");
            owner.setSneaking(false);
            c.assertTrue(Unicorns.WHISTLE.use(world,owner,Hand.MAIN_HAND).getResult().isAccepted(),"Whistle call");
            c.assertTrue(horse.waiting() && horse.squaredDistanceTo(owner)<55 && world.getEntity(id)==horse,
                "Whistle calls the original animal and preserves WAIT");
            owner.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);
            owner.setSneaking(true);
            horse.interactMob(owner,Hand.MAIN_HAND);
            var nbt=new NbtCompound();
            horse.writeCustomDataToNbt(nbt);
            c.assertTrue(nbt.contains("UnicornGuardPoint") && BlockPos.fromLong(nbt.getLong("UnicornGuardPoint")).equals(horse.getBlockPos()),
                "GUARD anchors to the selected point");
            horse.discard();owner.discard();c.complete();
        });
    }
}
