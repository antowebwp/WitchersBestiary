package com.witchersbestiary.test;

import com.mojang.authlib.GameProfile;
import com.witchersbestiary.entity.monster.WraithEntity;
import com.witchersbestiary.equipment.armory.LegendaryAbilities;
import com.witchersbestiary.registry.ModItems;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

public final class LegendaryAbilityTests implements FabricGameTest {
    @GameTest(templateName = "witchersbestiary:village_board_arena")
    public void bloodlustRequiresConfirmedPrimaryHitsAndRespectsMaxHealth(TestContext context) {
        var player = player(context, "BloodTest");
        player.setHealth(5);
        var stack = ModItems.BLOOD_LEGACY.getDefaultStack();
        player.setStackInHand(Hand.MAIN_HAND, stack);
        var target = EntityType.ZOMBIE.create(context.getWorld());
        target.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        context.assertTrue(!LegendaryAbilities.activate(player), "Empty charge cannot activate");

        for (int i = 0; i < LegendaryAbilities.FULL_CHARGE; i++) {
            var source = player.getDamageSources().playerAttack(player);
            target.timeUntilRegen = 0;
            stack.getItem().getBonusAttackDamage(target, 14, source);
            context.assertTrue(target.damage(source, 14), "Prepared melee hit deals damage");
        }
        context.assertTrue(player.getHealth() == 5, "Charging does not heal");
        context.assertTrue(LegendaryAbilities.activate(player) && !LegendaryAbilities.activate(player),
                "Full charge activates exactly once");
        for (int i = 0; i < 6; i++) {
            var source = player.getDamageSources().playerAttack(player);
            target.timeUntilRegen = 0;
            stack.getItem().getBonusAttackDamage(target, 14, source);
            target.damage(source, 14);
        }
        context.assertTrue(player.getHealth() == player.getMaxHealth(), "Healing stops at maximum health");
        target.discard();
        player.discard();
        context.complete();
    }

    @GameTest(templateName = "witchersbestiary:village_board_arena")
    public void summonUsesOwnedTemporaryWraithAndProtectsPlayers(TestContext context) {
        var player = player(context, "GraveTest");
        player.setStackInHand(Hand.OFF_HAND, ModItems.GRAVE_BLADE.getDefaultStack());
        context.assertTrue(!LegendaryAbilities.activate(player), "Offhand cannot activate");
        player.setStackInHand(Hand.MAIN_HAND, ModItems.GRAVE_BLADE.getDefaultStack());
        context.assertTrue(LegendaryAbilities.activate(player) && !LegendaryAbilities.activate(player),
                "Summon succeeds once");
        var world = context.getWorld();
        var ghost = world.getEntitiesByClass(WraithEntity.class, player.getBoundingBox().expand(4),
                entity -> entity.getOwner() == player).getFirst();
        context.assertTrue(ghost.isSummoned() && !ghost.shouldSave() && ghost.getHealth() == 30
                && ghost.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) == 7, "Temporary allied Wraith profile");
        var cow = EntityType.COW.create(world);
        var zombie = EntityType.ZOMBIE.create(world);
        context.assertTrue(!ghost.permitsAttack(player) && !ghost.permitsAttack(cow) && ghost.permitsAttack(zombie),
                "Owner and peaceful creatures are protected, hostiles are targetable");
        context.assertTrue(!player.damage(world.getDamageSources().mobAttack(ghost), 7), "Damage guard protects owner");
        ghost.bindSummon(player, world.getTime());
        ghost.tick();
        context.assertTrue(ghost.isRemoved() && !LegendaryAbilities.activate(player), "Expiry does not reset the cooldown");
        zombie.discard();
        cow.discard();
        player.discard();
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void legacyGrimoireIdDecodesWithoutRecipeOrSpell(TestContext context) {
        var world = context.getWorld();
        var id = Identifier.of("witchersbestiary", "grimoire");
        var stack = new ItemStack(Registries.ITEM.get(id));
        var decoded = ItemStack.fromNbt(world.getRegistryManager(), stack.encode(world.getRegistryManager())).orElseThrow();
        context.assertTrue(decoded.getItem().getClass() == Item.class && decoded.isOf(stack.getItem()),
                "Old item round-trips as an inert compatibility item");
        context.assertTrue(world.getRecipeManager().get(id).isEmpty(), "Grimoire recipe is gone");
        context.complete();
    }

    static ServerPlayerEntity player(TestContext context, String name) {
        var world = context.getWorld();
        var server = world.getServer();
        var data = ConnectedClientData.createDefault(new GameProfile(UUID.randomUUID(), name), false);
        var player = new ServerPlayerEntity(server, world, data.gameProfile(), data.syncedOptions());
        var connection = new ClientConnection(NetworkSide.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerManager().onPlayerConnect(connection, player, data);
        player.changeGameMode(GameMode.SURVIVAL);
        for (int i = 0; i < 61; i++) player.tick();
        player.setNoGravity(true);
        var at = context.getAbsolutePos(new BlockPos(6, 2, 6));
        player.refreshPositionAndAngles(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
        return player;
    }
}
