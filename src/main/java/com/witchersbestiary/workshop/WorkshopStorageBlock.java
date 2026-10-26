package com.witchersbestiary.workshop;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public final class WorkshopStorageBlock extends WorkshopDecorBlock implements BlockEntityProvider {
 public WorkshopStorageBlock(WorkshopKind k,Settings s){super(k,s);}
 @Override protected MapCodec<? extends Block> getCodec(){return createCodec(s->new WorkshopStorageBlock(kind,s));}
 @Override public BlockEntity createBlockEntity(BlockPos p,BlockState s){return new WorkshopInventory(p,s);}
 @Override protected ActionResult onUse(BlockState s,World w,BlockPos p,PlayerEntity player,BlockHitResult hit){if(!w.isClient&&w.getBlockEntity(p) instanceof WorkshopInventory be){if(kind.rack())be.interact(player,Hand.MAIN_HAND);else player.openHandledScreen(be);}return ActionResult.success(w.isClient);}
 @Override protected ItemActionResult onUseWithItem(ItemStack stack,BlockState s,World w,BlockPos p,PlayerEntity player,Hand hand,BlockHitResult hit){if(kind.rack()){if(!w.isClient&&w.getBlockEntity(p) instanceof WorkshopInventory be)be.interact(player,hand);return ItemActionResult.success(w.isClient);}return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
 @Override protected void onStateReplaced(BlockState s,World w,BlockPos p,BlockState next,boolean moved){if(!s.isOf(next.getBlock())&&w.getBlockEntity(p) instanceof WorkshopInventory be){if(!w.isClient){ItemScatterer.spawn(w,p,be);be.clear();w.updateComparators(p,this);}}super.onStateReplaced(s,w,p,next,moved);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world,BlockState state,BlockEntityType<T> type){return world.isClient&&kind==WorkshopKind.WITCHER_CHEST&&type==ModWorkshop.INVENTORY?(w,p,s,be)->((WorkshopInventory)be).tickLid():null;}
 @Override protected boolean onSyncedBlockEvent(BlockState state,World world,BlockPos pos,int type,int data){super.onSyncedBlockEvent(state,world,pos,type,data);return world.getBlockEntity(pos) instanceof WorkshopInventory be&&be.onSyncedBlockEvent(type,data);}
 @Override protected void scheduledTick(BlockState state,net.minecraft.server.world.ServerWorld world,BlockPos pos,net.minecraft.util.math.random.Random random){if(kind==WorkshopKind.WITCHER_CHEST&&world.getBlockEntity(pos) instanceof WorkshopInventory be)be.updateViewers();}
 @Override protected boolean hasComparatorOutput(BlockState s){return true;}
 @Override protected int getComparatorOutput(BlockState s,World w,BlockPos p){return w.getBlockEntity(p) instanceof WorkshopInventory be?net.minecraft.screen.ScreenHandler.calculateComparatorOutput((net.minecraft.inventory.Inventory)be):0;}
}
