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
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
    public void witcherSwordTransferAndVanillaBooks(TestContext c) {
        var player=c.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        player.getAbilities().creativeMode=false;
        player.experienceLevel=30;
        var registry=c.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var sharpness=registry.entryOf(Enchantments.SHARPNESS);
        var smite=registry.entryOf(Enchantments.SMITE);
        var unbreaking=registry.entryOf(Enchantments.UNBREAKING);
        var mending=registry.entryOf(Enchantments.MENDING);
        var feather=registry.entryOf(Enchantments.FEATHER_FALLING);
        var handler=new AnvilScreenHandler(1,player.getInventory());
        for (String id : java.util.List.of("blood_legacy", "grave_blade", "steel_hunter_sword", "silver_witcher_sword")) {
            var sword=Registries.ITEM.get(Identifier.of("witchersbestiary",id)).getDefaultStack();
            sword.setDamage(19);
            sword.set(DataComponentTypes.CUSTOM_NAME,net.minecraft.text.Text.literal("Fixture"));
            var diamond=Items.DIAMOND_SWORD.getDefaultStack();
            diamond.addEnchantment(sharpness,3);
            diamond.addEnchantment(feather,4);
            handler.getSlot(0).setStack(sword);
            handler.getSlot(1).setStack(diamond);
            var result=handler.getSlot(2).getStack().copy();
            var enchantments=EnchantmentHelper.getEnchantments(result);
            c.assertTrue(result.isOf(sword.getItem()) && result.getDamage()==19
                    && result.get(DataComponentTypes.CUSTOM_NAME).equals(sword.get(DataComponentTypes.CUSTOM_NAME))
                    && enchantments.getLevel(sharpness)==3 && enchantments.getLevel(feather)==0,
                    "Diamond transfer preserves sword identity and rejects incompatible enchantments");
            var netherite=Items.NETHERITE_SWORD.getDefaultStack();
            netherite.addEnchantment(unbreaking,3);
            netherite.addEnchantment(mending,1);
            netherite.addEnchantment(smite,4);
            netherite.addEnchantment(sharpness,3);
            handler.getSlot(0).setStack(result);
            handler.getSlot(1).setStack(netherite);
            result=handler.getSlot(2).getStack().copy();
            enchantments=EnchantmentHelper.getEnchantments(result);
            c.assertTrue(enchantments.getLevel(sharpness)==3 && enchantments.getLevel(unbreaking)==3
                    && enchantments.getLevel(mending)==1 && enchantments.getLevel(smite)==0,
                    "Netherite transfer keeps exact levels and rejects conflicts");
            var restored=net.minecraft.item.ItemStack.fromNbt(c.getWorld().getRegistryManager(),
                    (net.minecraft.nbt.NbtCompound)result.encode(c.getWorld().getRegistryManager())).orElseThrow();
            c.assertTrue(net.minecraft.item.ItemStack.areItemsAndComponentsEqual(result,restored),
                    "Result survives serialization");
            if (id.equals("blood_legacy")) {
                int cost=handler.getLevelCost();
                handler.quickMove(player,2);
                handler.quickMove(player,2);
                c.assertTrue(player.experienceLevel==30-cost && handler.getSlot(1).getStack().isEmpty()
                        && player.getInventory().count(sword.getItem())==1, "Donor consumed once");
            }
            var book=net.minecraft.item.EnchantedBookItem.forEnchantment(
                    new net.minecraft.enchantment.EnchantmentLevelEntry(mending,1));
            handler.getSlot(0).setStack(sword);
            handler.getSlot(1).setStack(book);
            c.assertTrue(EnchantmentHelper.getEnchantments(handler.getSlot(2).getStack()).getLevel(mending)==1,
                    "Vanilla anvil still accepts enchanted books");
        }
        player.discard();
        c.complete();
    }
}
