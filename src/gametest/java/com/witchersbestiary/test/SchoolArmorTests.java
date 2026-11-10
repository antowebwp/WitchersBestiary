package com.witchersbestiary.test;

import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.equipment.armory.SchoolArmor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;

public final class SchoolArmorTests implements FabricGameTest {
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
    public void recipesAndSetEffects(TestContext c) {
        var world=c.getWorld();
        var player=c.createMockCreativeServerPlayerInWorld();
        var fiend=ModEntities.FIEND.create(world);
        for(var school:SchoolArmor.School.values()){
            String name=school.name().toLowerCase(java.util.Locale.ROOT);
            for(String tier:new String[]{"","enhanced_","mastercrafted_"})
                for(String piece:new String[]{"helmet","chestplate","leggings","boots"}){
                    var id=Identifier.of("witchersbestiary",name+"_"+tier+piece);
                    c.assertTrue(Registries.ITEM.get(id) instanceof SchoolArmor && world.getRecipeManager().get(id).isPresent(),
                        "Armor and survival recipe registered: "+id);
                }
            equip(player,name);
            c.assertTrue(SchoolArmor.pieces(player,school)==4,"Full school recognized");
            player.equipStack(EquipmentSlot.MAINHAND,Items.IRON_SWORD.getDefaultStack());
            float sword=SchoolArmor.damage(fiend,world.getDamageSources().playerAttack(player),10);
            float expected=switch(school){case GRIFFIN->10;case BEAR->10.5f;case CAT->12;};
            c.assertTrue(Math.abs(sword-expected)<.001,"Expected sword modifier");
            if(school==SchoolArmor.School.GRIFFIN){
                c.assertTrue(SchoolArmor.signDamage(player)==1.25f&&SchoolArmor.signRange(player)==1.15f,"Sign power and range");
                for(int i=0;i<3;i++)c.assertTrue(SchoolArmor.signCombo(player)==1,"First three casts charge combo");
                c.assertTrue(SchoolArmor.signCombo(player)==1.2f&&SchoolArmor.signCombo(player)==1,"Combo consumes charge once");
            }
            if(school==SchoolArmor.School.BEAR){
                float incoming=SchoolArmor.damage(player,world.getDamageSources().mobAttack(fiend),10);
                float next=SchoolArmor.damage(player,world.getDamageSources().mobAttack(fiend),10);
                c.assertTrue(Math.abs(incoming-7)<.001&&Math.abs(next-4.55f)<.001,"Bear defense and follow-up absorption");
            }
            player.equipStack(EquipmentSlot.HEAD,Items.IRON_HELMET.getDefaultStack());
            c.assertTrue(SchoolArmor.pieces(player,school)==3&&SchoolArmor.fraction(player,school)==.7f,
                "Equipment swap recomputes partial bonus");
        }
        c.assertTrue(SchoolArmor.damage(player,world.getDamageSources().outOfWorld(),10)==10,"Void cannot be dodged");
        fiend.discard();player.discard();c.complete();
    }

    private void equip(net.minecraft.server.network.ServerPlayerEntity player,String school){
        var slots=new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
        var names=new String[]{"helmet","chestplate","leggings","boots"};
        for(int i=0;i<slots.length;i++)player.equipStack(slots[i],
            Registries.ITEM.get(Identifier.of("witchersbestiary",school+"_"+names[i])).getDefaultStack());
    }
}
