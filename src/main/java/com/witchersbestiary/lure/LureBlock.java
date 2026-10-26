package com.witchersbestiary.lure;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
import net.minecraft.server.world.ServerWorld;
public final class LureBlock extends BlockWithEntity {
 public static final DirectionProperty FACING=Properties.HORIZONTAL_FACING;
 public final LureKind kind;
 public LureBlock(LureKind kind,Settings settings){super(settings);this.kind=kind;setDefaultState(getStateManager().getDefaultState().with(FACING,Direction.NORTH));}
 @Override protected MapCodec<? extends BlockWithEntity> getCodec(){return createCodec(s->new LureBlock(kind,s));}
 @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(FACING);}
 @Override public BlockState getPlacementState(ItemPlacementContext c){return c.getSide()==Direction.UP?getDefaultState().with(FACING,c.getHorizontalPlayerFacing().getOpposite()):null;}
 @Override protected BlockRenderType getRenderType(BlockState s){return BlockRenderType.MODEL;}
 @Override protected net.minecraft.util.shape.VoxelShape getOutlineShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return Block.createCuboidShape(1,0,1,15,kind==LureKind.FIEND?18:30,15);}
 @Override protected boolean canPlaceAt(BlockState s,WorldView w,BlockPos p){return w.getBlockState(p.down()).isSideSolidFullSquare(w,p.down(),Direction.UP)&&w.getBlockState(p.up()).isAir();}
 @Override protected BlockState getStateForNeighborUpdate(BlockState s,Direction d,BlockState other,WorldAccess w,BlockPos p,BlockPos q){return !s.canPlaceAt(w,p)?Blocks.AIR.getDefaultState():s;}
 @Override public BlockEntity createBlockEntity(BlockPos p,BlockState s){return new LureBlockEntity(p,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World w,BlockState s,BlockEntityType<T> type){return w.isClient?null:validateTicker(type,ModLures.BLOCK_ENTITY,LureBlockEntity::tick);}
 @Override protected void onStateReplaced(BlockState s,World w,BlockPos p,BlockState next,boolean moved){if(!s.isOf(next.getBlock())&&w instanceof ServerWorld server&&w.getBlockEntity(p) instanceof LureBlockEntity be)be.refund(server,false);super.onStateReplaced(s,w,p,next,moved);}
}
