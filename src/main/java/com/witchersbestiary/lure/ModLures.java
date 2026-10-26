package com.witchersbestiary.lure;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.*;
public final class ModLures {
 public static final LureBlock LESHEN_BLOCK=block(LureKind.LESHEN),FIEND_BLOCK=block(LureKind.FIEND),GRIFFIN_BLOCK=block(LureKind.GRIFFIN);
 public static final Item LESHEN_LURE=item(LESHEN_BLOCK),FIEND_LURE=item(FIEND_BLOCK),GRIFFIN_LURE=item(GRIFFIN_BLOCK);
 public static final BlockEntityType<LureBlockEntity> BLOCK_ENTITY=Registry.register(Registries.BLOCK_ENTITY_TYPE,Identifier.of("witchersbestiary","hunting_lure"),FabricBlockEntityTypeBuilder.create(LureBlockEntity::new,LESHEN_BLOCK,FIEND_BLOCK,GRIFFIN_BLOCK).build());
 private static LureBlock block(LureKind kind){return Registry.register(Registries.BLOCK,Identifier.of("witchersbestiary",kind.id),new LureBlock(kind,AbstractBlock.Settings.create().mapColor(MapColor.BROWN).strength(1.5f).nonOpaque().noCollision().sounds(BlockSoundGroup.WOOD).luminance(s->kind==LureKind.GRIFFIN?2:5)));}
 private static Item item(LureBlock block){return Registry.register(Registries.ITEM,Identifier.of("witchersbestiary",block.kind.id),new LureItem(block,new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)));}
 public static Item item(LureKind kind){return switch(kind){case LESHEN->LESHEN_LURE;case FIEND->FIEND_LURE;case GRIFFIN->GRIFFIN_LURE;};}
 public static void initialize(){ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{if(entity instanceof PathAwareEntity mob&&java.util.Arrays.stream(LureKind.values()).anyMatch(k->k.type()==mob.getType())){if(LureState.get(world).isHuntBoss(mob.getUuid()))mob.setPersistent();((LureGoalAccess)mob).witchersbestiary$addLureGoal(new LureApproachGoal(mob));}});}
}
