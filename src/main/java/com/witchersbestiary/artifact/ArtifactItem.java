package com.witchersbestiary.artifact;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;
public final class ArtifactItem extends Item {
 private final String id;
 public ArtifactItem(String id){super(new Settings().maxCount(1).rarity(net.minecraft.util.Rarity.RARE));this.id=id;}
 @Override public void appendTooltip(ItemStack s,TooltipContext c,List<Text> lines,TooltipType t){lines.add(Text.translatable("tooltip.witchersbestiary."+id).formatted(Formatting.GREEN));lines.add(Text.translatable("tooltip.witchersbestiary.artifact_slot."+(id.endsWith("ring")?"ring":"aglet")).formatted(Formatting.GRAY));if(!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets"))lines.add(Text.translatable("tooltip.witchersbestiary.requires_trinkets").formatted(Formatting.DARK_GRAY));}
}
