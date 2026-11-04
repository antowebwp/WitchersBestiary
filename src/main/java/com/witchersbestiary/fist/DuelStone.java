package com.witchersbestiary.fist;
import net.minecraft.block.*;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.item.*;import net.minecraft.registry.*;import net.minecraft.util.*;import net.minecraft.util.hit.BlockHitResult;import net.minecraft.util.math.BlockPos;import net.minecraft.util.shape.*;import net.minecraft.world.*;
public final class DuelStone extends Block {
 public static final Identifier ID=Identifier.of("witchersbestiary:duel_stone");
 private static final VoxelShape SHAPE=VoxelShapes.union(createCuboidShape(0,0,0,16,3,16),createCuboidShape(1,3,1,15,6,15),createCuboidShape(3,6,3,13,10,13),createCuboidShape(.5,10,.5,15.5,14.2,15.5));
 public static final DuelStone BLOCK=Registry.register(Registries.BLOCK,ID,new DuelStone());

 public DuelStone(){super(AbstractBlock.Settings.create().strength(3.5f,8).requiresTool().sounds(net.minecraft.sound.BlockSoundGroup.STONE).luminance(s->2));}
 @Override protected com.mojang.serialization.MapCodec<? extends Block>getCodec(){return createCodec(settings->new DuelStone());}
 @Override protected VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return SHAPE;}
 @Override protected VoxelShape getCollisionShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return SHAPE;}
 @Override protected ActionResult onUse(BlockState state,World world,BlockPos pos,PlayerEntity player,BlockHitResult hit){if(player instanceof net.minecraft.server.network.ServerPlayerEntity p)DuelSessions.interact(p,pos);return ActionResult.SUCCESS;}
 public static void initialize(){var item=Registry.register(Registries.ITEM,ID,new BlockItem(BLOCK,new Item.Settings()));net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(e->e.add(item));DuelPackets.initialize();DuelSessions.initialize();}
}
