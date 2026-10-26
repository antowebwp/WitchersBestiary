package com.witchersbestiary.accessory;

import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;

public final class HunterAccessoryItem extends Item {
    private final String tooltip;
    public HunterAccessoryItem(Settings settings,String tooltip){super(settings.maxCount(1));this.tooltip=tooltip;}
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,List<Text> lines,TooltipType type){
        lines.add(Text.translatable("tooltip.witchersbestiary.necklace").formatted(Formatting.GRAY));
        lines.add(Text.translatable("tooltip.witchersbestiary."+tooltip).formatted(Formatting.GREEN));
        lines.add(Text.translatable("tooltip.witchersbestiary."+tooltip+"_limits").formatted(Formatting.DARK_GRAY));
    }
}
