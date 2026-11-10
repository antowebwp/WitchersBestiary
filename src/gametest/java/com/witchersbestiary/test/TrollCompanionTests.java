package com.witchersbestiary.test;

import com.witchersbestiary.economy.Currency;
import com.witchersbestiary.fist.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public final class TrollCompanionTests implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void duelUnlockIsPersonalAndRestores(TestContext c) {
        var world = c.getWorld();
        var at = c.getAbsolutePos(new BlockPos(1, 2, 1));
        var owner = c.createMockCreativeServerPlayerInWorld();
        var other = c.createMockCreativeServerPlayerInWorld();
        var troll = FistRegistry.ROCK_TROLL.create(world);
        troll.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        owner.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+1.9,0,0);
        other.refreshPositionAndAngles(at.getX()+1.5,at.getY(),at.getZ()+.5,0,0);
        world.spawnEntity(troll);
        owner.setSneaking(true);
        troll.interact(owner,Hand.MAIN_HAND);
        c.assertTrue(troll.canDuel(),"No friendship before victory");
        FistFights.invite(other,troll);
        c.assertTrue(FistFights.confirm(other,troll),"Wild troll accepts ordinary challenge");
        FistFights.cancel(other);
        FistFights.invite(owner,troll);
        c.assertTrue(FistFights.confirm(owner,troll),"Owner challenge starts");
        var f=FistFights.active(owner).fight();
        for(int i=0;i<60;i++)f.tick();
        f.opponent.health=1;
        f.act(true,FistFightSession.Action.QUICK);
        for(int i=0;i<4;i++)f.tick();
        f.hit(true,true,true);
        FistFights.tick(world.getServer());
        c.assertTrue(troll.canDuel()&&troll.isAlive()&&owner.getInventory().count(Currency.CROWN)==90,"Victory preserves the troll and reward without taming");
        owner.getInventory().clear();
        other.setSneaking(true);
        troll.interact(other,Hand.MAIN_HAND);
        c.assertTrue(troll.canDuel(),"Cancelled challenger cannot claim winner's troll");

        var saved=new NbtCompound();
        troll.writeCustomDataToNbt(saved);
        var expired=saved.copy();
        expired.getCompound("TrollVictories").putLong(owner.getUuidAsString(),world.getTime());
        troll.readCustomDataFromNbt(expired);
        troll.interact(owner,Hand.MAIN_HAND);
        c.assertTrue(troll.canDuel(),"Expired right cannot tame");
        troll.readCustomDataFromNbt(saved);
        troll.interact(owner,Hand.MAIN_HAND);
        c.assertTrue(owner.getUuid().equals(troll.getOwnerUuid())&&!troll.canDuel(),"Winner can accept saved right");
        troll.interact(other,Hand.MAIN_HAND);
        c.assertTrue(owner.getUuid().equals(troll.getOwnerUuid()),"Ownership cannot transfer");
        owner.setSneaking(false);
        troll.interact(owner,Hand.MAIN_HAND);
        var pet=new NbtCompound();
        troll.writeCustomDataToNbt(pet);
        c.assertTrue(pet.getBoolean("TrollWaiting"),"Owner can command waiting");
        troll.readCustomDataFromNbt(pet);
        c.assertTrue(owner.getUuid().equals(troll.getOwnerUuid())&&!troll.canDuel(),"Owner survives NBT reload");
        troll.interact(owner,Hand.MAIN_HAND);
        troll.interact(owner,Hand.MAIN_HAND);
        var zombie=EntityType.ZOMBIE.create(world);
        zombie.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+2.5,0,0);
        owner.setAttacker(zombie);
        troll.tick();
        c.assertTrue(troll.getTarget()==zombie,"Following troll targets hostile attacker");
        c.assertTrue(!com.witchersbestiary.signs.SignManager.enemy(other,troll),"Signs recognize tamed troll as a pet");
        troll.discard();owner.discard();other.discard();zombie.discard();c.complete();
    }
}
