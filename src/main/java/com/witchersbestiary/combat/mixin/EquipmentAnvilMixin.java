package com.witchersbestiary.combat.mixin;

import com.witchersbestiary.equipment.armory.SchoolArmor;
import com.witchersbestiary.equipment.armory.WitcherSwordItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public abstract class EquipmentAnvilMixin extends ForgingScreenHandler {
    @Shadow @Final private Property levelCost;
    @Shadow private int repairItemUsage;

    protected EquipmentAnvilMixin(ScreenHandlerType<?> type, int syncId, PlayerInventory inventory, ScreenHandlerContext context) {
        super(type,syncId,inventory,context);
    }

    @Unique
    private void transferSwordEnchantments(ItemStack target, ItemStack donor) {
        var merged = new ItemEnchantmentsComponent.Builder(EnchantmentHelper.getEnchantments(target));
        var offered = EnchantmentHelper.getEnchantments(donor);
        int weight = 0;
        for (var enchantment : offered.getEnchantments().stream().sorted(java.util.Comparator.comparing(
                entry -> entry.getKey().map(key -> key.getValue().toString()).orElse(""))).toList()) {
            int level = Math.min(enchantment.value().getMaxLevel(), offered.getLevel(enchantment));
            if (level <= merged.getLevel(enchantment) || !enchantment.value().isAcceptableItem(target)
                    || merged.getEnchantments().stream().anyMatch(existing ->
                    !existing.equals(enchantment) && !Enchantment.canBeCombined(existing, enchantment))) continue;
            merged.set(enchantment, level);
            weight += level;
        }
        var result = ItemStack.EMPTY;
        if (weight > 0) {
            result = target.copy();
            result.set(DataComponentTypes.ENCHANTMENTS, merged.build());
        }
        repairItemUsage = 0;
        levelCost.set(weight == 0 ? 0 : Math.min(20, 4 + weight));
        output.setStack(0, result);
        sendContentUpdates();
    }

    @Inject(method="updateResult",at=@At("HEAD"),cancellable=true)
    private void transferEquipmentEnchantments(CallbackInfo ci) {
        ItemStack target=input.getStack(0), donor=input.getStack(1);
        if (target.getItem() instanceof WitcherSwordItem
                && (donor.isOf(Items.DIAMOND_SWORD) || donor.isOf(Items.NETHERITE_SWORD))) {
            transferSwordEnchantments(target, donor);
            ci.cancel();
            return;
        }
        if(!(target.getItem() instanceof SchoolArmor school)||!(donor.getItem() instanceof ArmorItem armor)
            ||!(armor.getMaterial()==ArmorMaterials.DIAMOND||armor.getMaterial()==ArmorMaterials.NETHERITE))return;

        var result=target.copy();
        var merged=new ItemEnchantmentsComponent.Builder(EnchantmentHelper.getEnchantments(target));
        var offered=EnchantmentHelper.getEnchantments(donor);
        int weight=0;
        if(armor.getType()==school.getType()){
            // Process stable registry order when a command-created donor contains conflicting enchantments.
            for(var entry:offered.getEnchantments().stream().sorted(java.util.Comparator.comparing(
                    e->e.getKey().map(key->key.getValue().toString()).orElse(""))).toList()){
                if(!entry.value().isAcceptableItem(target)||merged.getEnchantments().stream().anyMatch(
                        existing->!existing.equals(entry)&&!Enchantment.canBeCombined(existing,entry)))continue;
                int previous=merged.getLevel(entry), donated=offered.getLevel(entry);
                int combined=Math.min(entry.value().getMaxLevel(),previous==donated?previous+1:Math.max(previous,donated));
                if(combined<=previous)continue;
                merged.set(entry,combined);
                weight+=combined;
            }
        }
        if(weight>0)result.set(DataComponentTypes.ENCHANTMENTS,merged.build());
        // A zero material usage invokes vanilla's single-item donor consumption path.
        repairItemUsage=0;
        levelCost.set(weight==0?0:Math.min(20,4+weight));
        output.setStack(0,weight==0?ItemStack.EMPTY:result);
        sendContentUpdates();
        ci.cancel();
    }
}
