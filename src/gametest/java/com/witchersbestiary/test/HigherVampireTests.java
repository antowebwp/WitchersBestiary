package com.witchersbestiary.test;

import com.witchersbestiary.entity.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

public final class HigherVampireTests implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 110)
    public void transformsInPlaceWithClearance(TestContext context) {
        var world = context.getWorld();
        var pos = context.getAbsolutePos(new BlockPos(3, 2, 3));
        world.setBlockState(pos.down(), Blocks.STONE.getDefaultState());
        var vampire = ModEntities.HIGHER_VAMPIRE.create(world);
        vampire.refreshPositionAndAngles(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        vampire.setAiDisabled(true);
        vampire.setHealth(52);
        world.spawnEntity(vampire);
        var uuid = vampire.getUuid();

        world.setBlockState(pos.up(3), Blocks.STONE.getDefaultState());
        context.assertTrue(!vampire.beginTransformation(), "Ceiling must prevent transformation");
        world.setBlockState(pos.up(3), Blocks.AIR.getDefaultState());
        context.assertTrue(vampire.beginTransformation(), "Clear space permits transformation");
        context.assertTrue(!vampire.beginTransformation(), "Transformation cannot restart");

        context.runAtTick(85, () -> {
            context.assertTrue(vampire.isHybrid(), "Hybrid form active");
            context.assertTrue(world.getEntity(uuid) == vampire, "Original entity and UUID retained");
            context.assertTrue(vampire.getHealth() == 52, "Health retained");
            context.assertTrue(vampire.getHeight() == 3.2f && vampire.getWidth() == 1.32f, "Hybrid hitbox active");
            context.assertTrue(Math.abs(vampire.getY() - pos.getY()) < .01, "Feet stay on the floor");
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 245)
    public void advancesOnceAndPersistsDefeat(TestContext context) {
        var world = context.getWorld();
        var pos = context.getAbsolutePos(new BlockPos(4, 2, 4));
        world.setBlockState(pos.down(), Blocks.STONE.getDefaultState());
        var vampire = ModEntities.HIGHER_VAMPIRE.create(world);
        vampire.refreshPositionAndAngles(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        world.spawnEntity(vampire);
        context.assertTrue(vampire.getMaxHealth() == 600, "Solo base health");
        vampire.setHealth(390);

        context.runAtTick(6, () -> context.assertTrue(vampire.isTransforming(), "65% triggers transition"));
        context.runAtTick(85, () -> {
            context.assertTrue(vampire.isHybrid() && !vampire.isFinalPhase(), "True combat form at 65%");
            context.assertTrue(vampire.getHealth() == 390, "No phase heal");
            vampire.setHealth(149);
        });
        context.runAtTick(90, () -> {
            context.assertTrue(vampire.isFinalPhase(), "Final phase below 25%");
            vampire.setHealth(170);
            var target = net.minecraft.entity.EntityType.IRON_GOLEM.create(world);
            target.refreshPositionAndAngles(pos.getX()+.5, pos.getY(), pos.getZ()+6.5, 180, 0);
            vampire.setAiDisabled(true);
            vampire.setTarget(target);
            vampire.suppressRegeneration(75);
        });
        context.runAtTick(145, () -> context.assertTrue(vampire.getHealth() == 170, "Suppression postpones healing"));
        context.runAtTick(170, () -> {
            context.assertTrue(vampire.getHealth() == 173, "Regeneration restores three health per pulse");
            vampire.setTarget(null);
            context.assertTrue(vampire.isFinalPhase(), "Healing does not revert the phase");
            vampire.damage(world.getDamageSources().genericKill(), 1000);
            context.assertTrue(vampire.isDefeated() && vampire.getHealth() == 1, "Defeat retains the entity");
            context.assertTrue(!vampire.damage(world.getDamageSources().mobAttack(vampire), 5), "Defeated entity is inert");
            var saved = vampire.writeNbt(new net.minecraft.nbt.NbtCompound());
            var loaded = ModEntities.HIGHER_VAMPIRE.create(world);
            loaded.readNbt(saved);
            context.assertTrue(loaded.isDefeated() && loaded.isHybrid(), "Defeat and form persist");
            context.assertTrue(loaded.getUuid().equals(vampire.getUuid()), "UUID persists");
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 180)
    public void closeAttacksHaveWindupAndRelease(TestContext context) {
        var world = context.getWorld();
        var pos = context.getAbsolutePos(new BlockPos(3, 2, 3));
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            world.setBlockState(pos.add(x, -1, z), Blocks.STONE.getDefaultState());
        }
        var target = net.minecraft.entity.EntityType.IRON_GOLEM.create(world);
        target.setAiDisabled(true);
        target.refreshPositionAndAngles(pos.getX()+.5, pos.getY(), pos.getZ()+3.05, 180, 0);
        world.spawnEntity(target);
        float startingHealth = target.getHealth();
        var vampire = ModEntities.HIGHER_VAMPIRE.create(world);
        vampire.refreshPositionAndAngles(pos.getX()+.5, pos.getY(), pos.getZ()+.5, 0, 0);
        world.spawnEntity(vampire);
        vampire.setTarget(target);
        boolean[] armed = {false}, damaged = {false};
        context.runAtEveryTick(() -> {
            String clip = vampire.activeCombatClip();
            if (!armed[0] && clip.startsWith("claw")) {
                context.assertTrue(target.getHealth() == startingHealth, "Claws do not hit on their first frame");
                armed[0] = true;
            }
            if (armed[0] && target.getHealth() < startingHealth) damaged[0] = true;
            if (damaged[0] && clip.isEmpty()) {
                target.discard();
                context.complete();
            }
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 110)
    public void bleedingExpiresAndDefeatDoesNotDropSword(TestContext context) {
        var world = context.getWorld();
        var pos = context.getAbsolutePos(new BlockPos(3, 2, 3));
        world.setBlockState(pos.down(), Blocks.STONE.getDefaultState());
        var target = net.minecraft.entity.EntityType.IRON_GOLEM.create(world);
        target.refreshPositionAndAngles(pos.getX()+.5, pos.getY(), pos.getZ()+.5, 0, 0);
        target.setAiDisabled(true);
        world.spawnEntity(target);
        var effect = com.witchersbestiary.registry.ModEffects.BLEEDING;
        com.witchersbestiary.alchemy.BleedingEffect.inflict(target, 2);
        com.witchersbestiary.alchemy.BleedingEffect.inflict(target, 1);
        context.assertTrue(target.getStatusEffect(effect).getAmplifier()==1, "Weaker hit cannot downgrade Bleeding");
        context.runAtTick(85, () -> {
            context.assertTrue(target.getHealth()<target.getMaxHealth()&&!target.hasStatusEffect(effect), "Bleeding damages and expires");
            target.discard();
            var boss = ModEntities.HIGHER_VAMPIRE.create(world);
            boss.refreshPositionAndAngles(pos.getX()+.5, pos.getY(), pos.getZ()+.5, 0, 0);
            var nbt = new net.minecraft.nbt.NbtCompound();
            nbt.putBoolean("HybridForm",true);
            nbt.putBoolean("FinalPhase",true);
            boss.readCustomDataFromNbt(nbt);
            world.spawnEntity(boss);
            var player = context.createMockPlayer(net.minecraft.world.GameMode.SURVIVAL);
            var source = world.getDamageSources().playerAttack(player);
            boss.damage(source, 1000);
            boss.damage(source, 1000);
            var drops = world.getEntitiesByClass(net.minecraft.entity.ItemEntity.class, boss.getBoundingBox().expand(3),
                    item -> item.getStack().isOf(com.witchersbestiary.registry.ModItems.BLOOD_LEGACY));
            context.assertTrue(boss.canBeSealed()&&drops.isEmpty(), "Defeat opens sealing without dropping the reward");
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void sealsAndReplaysWithoutDuplicateRewards(TestContext c) {
        var world = c.getWorld();
        var pos = c.getAbsolutePos(new BlockPos(3, 2, 3));
        for(int x=-2;x<=2;x++) for(int z=-2;z<=8;z++)
            world.setBlockState(pos.add(x,-1,z),Blocks.STONE.getDefaultState());
        var owner=new net.minecraft.server.network.ServerPlayerEntity(world.getServer(),world,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"VampireFixture"),
                net.minecraft.network.packet.c2s.common.SyncedClientOptions.createDefault());
        owner.networkHandler=new net.minecraft.server.network.ServerPlayNetworkHandler(world.getServer(),
                new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND),owner,
                net.minecraft.server.network.ConnectedClientData.createDefault(owner.getGameProfile(),false));
        owner.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        var hunt=com.witchersbestiary.contracts.Contracts.assign(owner,com.witchersbestiary.contracts.hunt.VampireHunt.ID);
        var item=com.witchersbestiary.registry.ModItems.CRIMSON_PHYLACTERY;
        var sword=com.witchersbestiary.registry.ModItems.BLOOD_LEGACY;
        var stack=owner.getInventory().main.stream().filter(i->i.isOf(item)).findFirst().orElseThrow();
        owner.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,stack);
        var original=ModEntities.HIGHER_VAMPIRE.create(world);
        original.refreshPositionAndAngles(owner.getX(),owner.getY(),owner.getZ()+3,180,0);
        world.spawnEntity(original);
        c.assertTrue(com.witchersbestiary.contracts.hunt.VampireHunt.bind(owner,original),"Contract binds one boss");
        defeat(original,world);
        var hand=net.minecraft.util.Hand.MAIN_HAND;
        item.use(world,owner,hand);
        item.usageTick(world,owner,stack,51);
        owner.stopUsingItem();
        c.assertTrue(original.canBeSealed()&&owner.getInventory().count(sword)==0,"Interrupted use has no reward");
        c.assertTrue(item.use(world,owner,hand).getResult().isAccepted(),"Aimed use begins sealing");
        item.usageTick(world,owner,stack,1);
        c.assertTrue(original.isRemoved()&&owner.getInventory().count(sword)==1,"First sealing rewards once: "+original.isRemoved()+"/"+hunt.status()+"/"+owner.getInventory().count(sword));
        stack=owner.getMainHandStack();
        var duplicate=stack.copy();
        for(int x=-1;x<=1;x++) for(int z=4;z<=6;z++) for(int y=0;y<4;y++)
            world.setBlockState(pos.add(x,y,z),Blocks.AIR.getDefaultState());
        owner.setYaw(0);
        owner.setPitch(0);
        item.use(world,owner,hand);
        c.assertTrue(hunt.vampire!=null,"Sealed relic summons a replay: "+hunt.status()+"/"+stack);
        var id=hunt.vampire;
        owner.setStackInHand(net.minecraft.util.Hand.OFF_HAND,duplicate);
        item.use(world,owner,net.minecraft.util.Hand.OFF_HAND);
        c.assertTrue(id.equals(hunt.vampire),"Second copy cannot summon a concurrent boss");
        var replay=(com.witchersbestiary.entity.monster.HigherVampireEntity)world.getEntity(id);
        owner.refreshPositionAndAngles(replay.getX(),replay.getY(),replay.getZ()-3,0,0);
        defeat(replay,world);
        c.assertTrue(item.use(world,owner,hand).getResult().isAccepted(),"Aimed use begins sealing");
        item.usageTick(world,owner,stack,1);
        c.assertTrue(replay.isRemoved()&&hunt.vampire==null&&owner.getInventory().count(sword)==1,"Replay seals without rewards");

        c.complete();
    }

    private void defeat(com.witchersbestiary.entity.monster.HigherVampireEntity boss,net.minecraft.server.world.ServerWorld world) {
        var nbt=boss.writeNbt(new net.minecraft.nbt.NbtCompound());
        nbt.putBoolean("HybridForm",true);
        nbt.putBoolean("FinalPhase",true);
        boss.readNbt(nbt);
        boss.damage(world.getDamageSources().genericKill(),1000);
    }
}
