package com.witchersbestiary.decor;
import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import java.util.List;
public final class ModRemains {
 private ModRemains(){}
 public static final Block TOOTH_NECKLACE=block("tooth_necklace",true);
 public static final Block MONSTER_ORGAN_JAR=block("monster_organ_jar",false);
 public static final Block MONSTER_WALL_TROPHY=block("monster_wall_trophy",true);
 private static Block block(String id,boolean wall){var b=Registry.register(Registries.BLOCK,Identifier.of("witchersbestiary",id),new RemainsDecorBlock(AbstractBlock.Settings.create().strength(.6f).nonOpaque().noCollision().sounds(wall?BlockSoundGroup.WOOD:BlockSoundGroup.GLASS),wall));Registry.register(Registries.ITEM,Identifier.of("witchersbestiary",id),new BlockItem(b,new Item.Settings()));return b;}
 public static final Item BONE_DUST=Registry.register(Registries.ITEM,Identifier.of("witchersbestiary","bone_dust"),new Item(new Item.Settings()){
 @Override public void appendTooltip(ItemStack stack,Item.TooltipContext context,java.util.List<net.minecraft.text.Text> tooltip,net.minecraft.item.tooltip.TooltipType type){tooltip.add(net.minecraft.text.Text.translatable("tooltip.witchersbestiary.bone_dust").formatted(net.minecraft.util.Formatting.GRAY));}
 });
 public static final List<Item> ALL=List.of(TOOTH_NECKLACE.asItem(),MONSTER_ORGAN_JAR.asItem(),BONE_DUST,MONSTER_WALL_TROPHY.asItem());
 public static void initialize(){}
}
