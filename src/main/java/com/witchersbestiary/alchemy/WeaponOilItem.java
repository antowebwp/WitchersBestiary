package com.witchersbestiary.alchemy;

import com.witchersbestiary.combat.MonsterCategory;
import com.witchersbestiary.registry.ModDataComponents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.item.tooltip.TooltipType;
import java.util.List;

/** Inventory oil. Legacy constructor signatures remain, but every oil uses 15%/40 charges. */
public final class WeaponOilItem extends Item {
    private final MonsterCategory category;
    public MonsterCategory category(){return category;}
    public float bonus(){return InventoryOils.BONUS;}
    public int duration(){return 0;}
    public WeaponOilItem(Settings settings,String id,MonsterCategory category,float bonus,int duration){super(settings);this.category=category;}
    public WeaponOilItem(Settings settings,String id,MonsterCategory category){this(settings,id,category,InventoryOils.BONUS,0);}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand){return TypedActionResult.pass(player.getStackInHand(hand));}
    public static boolean supportsMeleeDamage(ItemStack weapon){
        AttributeModifiersComponent modifiers=weapon.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        return modifiers!=null&&modifiers.modifiers().stream().anyMatch(entry->entry.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE));
    }
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,List<Text> tooltip,TooltipType type){
        tooltip.add(Text.translatable("tooltip.witchersbestiary.auto_oil_category",Text.translatable("category.witchersbestiary."+category.name().toLowerCase(java.util.Locale.ROOT))).formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("tooltip.witchersbestiary.auto_oil_bonus").formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.witchersbestiary.auto_oil_charges",InventoryOils.charges(stack),InventoryOils.CAPACITY).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.witchersbestiary.auto_oil_use").formatted(Formatting.DARK_GRAY));
    }
}
