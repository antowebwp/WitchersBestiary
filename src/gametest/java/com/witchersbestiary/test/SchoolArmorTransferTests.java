package com.witchersbestiary.test;

import com.witchersbestiary.equipment.armory.SchoolArmor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

public final class SchoolArmorTransferTests implements FabricGameTest {
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
    public void transferMergesAndConsumesOnce(TestContext c){
        var player=c.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        player.getAbilities().creativeMode=false;
        player.getInventory().clear();
        player.experienceLevel=30;
        var handler=new AnvilScreenHandler(1,player.getInventory());
        var registry=c.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var protection=registry.entryOf(Enchantments.PROTECTION);
        var fire=registry.entryOf(Enchantments.FIRE_PROTECTION);
        var feather=registry.entryOf(Enchantments.FEATHER_FALLING);
        var mending=registry.entryOf(Enchantments.MENDING);
        var aqua=registry.entryOf(Enchantments.AQUA_AFFINITY);

        var griffin=Registries.ITEM.get(Identifier.of("witchersbestiary","griffin_chestplate")).getDefaultStack();
        griffin.setDamage(37);
        griffin.set(DataComponentTypes.REPAIR_COST,127);
        griffin.addEnchantment(protection,2);
        var donor=Items.DIAMOND_CHESTPLATE.getDefaultStack();
        donor.addEnchantment(protection,2);
        donor.addEnchantment(fire,4);
        donor.addEnchantment(feather,4);
        handler.getSlot(0).setStack(griffin);
        handler.getSlot(1).setStack(donor);
        var output=handler.getSlot(2).getStack();
        var enchants=EnchantmentHelper.getEnchantments(output);
        c.assertTrue(output.isOf(griffin.getItem())&&output.getDamage()==37&&enchants.getLevel(protection)==3
            &&enchants.getLevel(fire)==0&&enchants.getLevel(feather)==0,"Compatible levels merge without changing the original armor");
        int cost=handler.getLevelCost();
        c.assertTrue(cost>=5&&cost<=20,"High prior work does not block transfer");
        handler.quickMove(player,2);
        c.assertTrue(player.experienceLevel==30-cost&&handler.getSlot(0).getStack().isEmpty()
            &&handler.getSlot(1).getStack().isEmpty()&&handler.getSlot(2).getStack().isEmpty()
            &&player.getInventory().count(griffin.getItem())==1,"Shift-click consumes both inputs and charges once");
        handler.quickMove(player,2);
        c.assertTrue(player.getInventory().count(griffin.getItem())==1&&player.experienceLevel==30-cost,"No second output");

        var cat=Registries.ITEM.get(Identifier.of("witchersbestiary","cat_boots")).getDefaultStack();
        var boots=Items.NETHERITE_BOOTS.getDefaultStack();
        boots.addEnchantment(feather,4);
        boots.addEnchantment(mending,1);
        handler.getSlot(0).setStack(cat);handler.getSlot(1).setStack(boots);
        var catResult=handler.getSlot(2).getStack();
        c.assertTrue(catResult.isOf(cat.getItem())&&EnchantmentHelper.getEnchantments(catResult).getLevel(feather)==4
            &&EnchantmentHelper.getEnchantments(catResult).getLevel(mending)==1,"Netherite boot transfer supports slot and durability enchantments");
        var bear=Registries.ITEM.get(Identifier.of("witchersbestiary","bear_mastercrafted_helmet")).getDefaultStack();
        handler.getSlot(0).setStack(bear);
        c.assertTrue(handler.getSlot(2).getStack().isEmpty()&&handler.getLevelCost()==0,"Wrong donor slot rejected");
        var helmet=Items.NETHERITE_HELMET.getDefaultStack();
        helmet.addEnchantment(aqua,1);
        handler.getSlot(1).setStack(helmet);
        var bearResult=handler.getSlot(2).getStack();
        c.assertTrue(bearResult.isOf(bear.getItem())&&((SchoolArmor)bearResult.getItem()).school()==SchoolArmor.School.BEAR
            &&EnchantmentHelper.getEnchantments(bearResult).getLevel(aqua)==1,"Mastercrafted Bear identity and helmet enchantment preserved");
        player.discard();c.complete();
    }
}
