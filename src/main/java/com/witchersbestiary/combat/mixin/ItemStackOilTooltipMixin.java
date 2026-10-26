package com.witchersbestiary.combat.mixin;

import com.witchersbestiary.registry.ModDataComponents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackOilTooltipMixin {
    @Inject(method="getTooltip",at=@At("RETURN"))
    private void witchersbestiary$appendOil(Item.TooltipContext context,PlayerEntity player,TooltipType type,CallbackInfoReturnable<List<Text>> cir){
        ItemStack stack=(ItemStack)(Object)this;
        var category=stack.get(ModDataComponents.OIL_CATEGORY);int hits=stack.getOrDefault(ModDataComponents.OIL_HITS,0);
        if(category==null||hits<=0)return;
        List<Text> tooltip=new ArrayList<>(cir.getReturnValue());
        tooltip.add(Text.translatable("tooltip.witchersbestiary.legacy_oil",hits).formatted(Formatting.AQUA));
        cir.setReturnValue(tooltip);
    }
}
