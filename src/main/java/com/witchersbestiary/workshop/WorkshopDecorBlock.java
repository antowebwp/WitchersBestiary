package com.witchersbestiary.workshop;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
public class WorkshopDecorBlock extends Block {
 public final WorkshopKind kind;
 public WorkshopDecorBlock(WorkshopKind kind,Settings settings){super(settings);this.kind=kind;setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING,Direction.NORTH));}
 @Override protected MapCodec<? extends Block> getCodec(){return createCodec(s->new WorkshopDecorBlock(kind,s));}
 @Override protected void appendProperties(StateManager.Builder<Block,BlockState> b){b.add(Properties.HORIZONTAL_FACING);}
 @Override public BlockState getPlacementState(ItemPlacementContext c){
  boolean wall=kind.mount==WorkshopKind.Mount.WALL;
  if(wall&&!c.getSide().getAxis().isHorizontal()||kind.mount==WorkshopKind.Mount.FLOOR&&c.getSide()!=Direction.UP||kind.mount==WorkshopKind.Mount.CEILING&&c.getSide()!=Direction.DOWN)return null;
  return getDefaultState().with(Properties.HORIZONTAL_FACING,wall?c.getSide():c.getHorizontalPlayerFacing().getOpposite());
 }
 protected Direction support(BlockState s){return kind.mount==WorkshopKind.Mount.WALL?s.get(Properties.HORIZONTAL_FACING).getOpposite():kind.mount==WorkshopKind.Mount.CEILING?Direction.UP:Direction.DOWN;}
 @Override protected boolean canPlaceAt(BlockState s,WorldView w,BlockPos p){var d=support(s);return w.getBlockState(p.offset(d)).isSideSolidFullSquare(w,p.offset(d),d.getOpposite());}
 @Override protected BlockState getStateForNeighborUpdate(BlockState s,Direction d,BlockState next,WorldAccess w,BlockPos p,BlockPos q){return s.canPlaceAt(w,p)?s:Blocks.AIR.getDefaultState();}
 @Override protected BlockState rotate(BlockState s,BlockRotation r){return s.with(Properties.HORIZONTAL_FACING,r.rotate(s.get(Properties.HORIZONTAL_FACING)));}
 @Override protected BlockState mirror(BlockState s,BlockMirror m){return s.rotate(m.getRotation(s.get(Properties.HORIZONTAL_FACING)));}
 @Override protected VoxelShape getOutlineShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){
  if(kind.mount==WorkshopKind.Mount.WALL)return switch(s.get(Properties.HORIZONTAL_FACING)){case NORTH->createCuboidShape(0,1,10,16,16,16);case SOUTH->createCuboidShape(0,1,0,16,16,6);case EAST->createCuboidShape(0,1,0,6,16,16);default->createCuboidShape(10,1,0,16,16,16);};
  return createCuboidShape(1,0,1,15,kind==WorkshopKind.HUNTING_TABLE||kind==WorkshopKind.ALCHEMY_WORKBENCH?13:16,15);
 }
}
