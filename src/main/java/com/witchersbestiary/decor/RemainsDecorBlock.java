package com.witchersbestiary.decor;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
/** Small static decorations: support-aware, ordinary block loot, no ticking entities. */
public final class RemainsDecorBlock extends Block {
 private final boolean wall;
 public RemainsDecorBlock(Settings settings,boolean wall){super(settings);this.wall=wall;setDefaultState(getStateManager().getDefaultState().with(Properties.HORIZONTAL_FACING,Direction.NORTH));}
 @Override protected MapCodec<? extends Block> getCodec(){return createCodec(s->new RemainsDecorBlock(s,wall));}
 @Override protected void appendProperties(StateManager.Builder<Block,BlockState> b){b.add(Properties.HORIZONTAL_FACING);}
 @Override public BlockState getPlacementState(ItemPlacementContext c){return wall?(c.getSide().getAxis().isHorizontal()?getDefaultState().with(Properties.HORIZONTAL_FACING,c.getSide()):null):(c.getSide()==Direction.UP?getDefaultState().with(Properties.HORIZONTAL_FACING,c.getHorizontalPlayerFacing().getOpposite()):null);}
 @Override protected boolean canPlaceAt(BlockState s,WorldView w,BlockPos p){Direction support=wall?s.get(Properties.HORIZONTAL_FACING).getOpposite():Direction.DOWN;BlockPos q=p.offset(support);return w.getBlockState(q).isSideSolidFullSquare(w,q,support.getOpposite());}
 @Override protected BlockState getStateForNeighborUpdate(BlockState s,Direction side,BlockState neighbor,WorldAccess w,BlockPos p,BlockPos q){return !s.canPlaceAt(w,p)?Blocks.AIR.getDefaultState():s;}
 @Override protected VoxelShape getOutlineShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){if(!wall)return Block.createCuboidShape(3,0,3,13,15,13);return switch(s.get(Properties.HORIZONTAL_FACING)){case NORTH->Block.createCuboidShape(2,1,12,14,15,16);case SOUTH->Block.createCuboidShape(2,1,0,14,15,4);case EAST->Block.createCuboidShape(0,1,2,4,15,14);default->Block.createCuboidShape(12,1,2,16,15,14);};}
}
