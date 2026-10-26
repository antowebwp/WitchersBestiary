package com.witchersbestiary.workshop;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import java.util.*;
public final class ModWorkshop {
 public static final Map<WorkshopKind,Block> BLOCKS=new LinkedHashMap<>();
 public static final TagKey<Item> DISPLAY_WEAPONS=TagKey.of(RegistryKeys.ITEM,id("display_weapons")),RACK_SWORDS=TagKey.of(RegistryKeys.ITEM,id("rack_swords"));
 static {for(var k:WorkshopKind.values()){
  var settings=AbstractBlock.Settings.create().strength(2).nonOpaque().sounds(k==WorkshopKind.GRAVEYARD_FENCE?BlockSoundGroup.METAL:k==WorkshopKind.ANCIENT_GRAVESTONE||k==WorkshopKind.RITUAL_URN?BlockSoundGroup.STONE:BlockSoundGroup.WOOD).luminance(s->k.light()>0&&s.get(Properties.LIT)?k.light():0);
  Block b=k.slots>0?new WorkshopStorageBlock(k,settings):k.light()>0?new WorkshopLightBlock(k,settings):k==WorkshopKind.ANCIENT_GRAVESTONE?new GravestoneBlock(settings):k==WorkshopKind.GRAVEYARD_FENCE?new FenceBlock(settings):new WorkshopDecorBlock(k,settings);
  Registry.register(Registries.BLOCK,id(k.id),b);Registry.register(Registries.ITEM,id(k.id),new BlockItem(b,new Item.Settings()));BLOCKS.put(k,b);
 }}
 public static final BlockEntityType<WorkshopInventory> INVENTORY=Registry.register(Registries.BLOCK_ENTITY_TYPE,id("workshop_inventory"),FabricBlockEntityTypeBuilder.create(WorkshopInventory::new,BLOCKS.values().stream().filter(b->b instanceof WorkshopStorageBlock).toArray(Block[]::new)).build());
 public static final ScreenHandlerType<AlchemyScreenHandler> ALCHEMY_SCREEN=Registry.register(Registries.SCREEN_HANDLER,id("alchemy_workbench"),new ScreenHandlerType<>(AlchemyScreenHandler::new,FeatureFlags.VANILLA_FEATURES));
 public static final ScreenHandlerType<WorkshopContainerScreenHandler> URN_SCREEN=Registry.register(Registries.SCREEN_HANDLER,id("ritual_urn"),new ScreenHandlerType<>((sync,player)->new WorkshopContainerScreenHandler(sync,player,6),FeatureFlags.VANILLA_FEATURES));
 public static final ScreenHandlerType<WorkshopContainerScreenHandler> EQUIPMENT_SCREEN=Registry.register(Registries.SCREEN_HANDLER,id("witcher_equipment_rack"),new ScreenHandlerType<>((sync,player)->new WorkshopContainerScreenHandler(sync,player,8),FeatureFlags.VANILLA_FEATURES));
 public static Identifier id(String path){return Identifier.of("witchersbestiary",path);}
 public static void initialize(){}
}
