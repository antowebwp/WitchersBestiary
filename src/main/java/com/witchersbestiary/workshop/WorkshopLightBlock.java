package com.witchersbestiary.workshop;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
public final class WorkshopLightBlock extends WorkshopDecorBlock {
 public WorkshopLightBlock(WorkshopKind k,Settings s){super(k,s);setDefaultState(getDefaultState().with(Properties.LIT,true).with(Properties.HANGING,false));}
 @Override protected MapCodec<? extends Block> getCodec(){return createCodec(s->new WorkshopLightBlock(kind,s));}
 @Override protected void appendProperties(StateManager.Builder<Block,BlockState> b){super.appendProperties(b);b.add(Properties.LIT,Properties.HANGING);}
 @Override public BlockState getPlacementState(ItemPlacementContext c){var state=super.getPlacementState(c);if(state==null)return null;if(kind==WorkshopKind.GHOST_LANTERN){if(c.getSide()!=Direction.DOWN&&c.getSide()!=Direction.UP)return null;state=state.with(Properties.HANGING,c.getSide()==Direction.DOWN);}return state;}
 /** Model-space wick centres, including the half-pixel wick height. */
 public double[][] flameOffsets(){return kind==WorkshopKind.RITUAL_CANDLES?new double[][]{{4.8,9.1,5.8},{9.8,12.1,4.8},{6.8,7.1,10.8},{10.8,10.1,9.8}}:kind==WorkshopKind.SKULL_CANDLES?new double[][]{{5.8,12.2,7.8},{11.8,8.5,10.8},{3.8,5.5,10.8}}:new double[][]{{8,4.5,8}};}
 public java.util.List<Vec3d> flamePositions(BlockState state,BlockPos pos){
  var out=new java.util.ArrayList<Vec3d>();
  // Model north is unrotated; blockstate y rotates clockwise viewed from above.
  int rotation=switch(state.get(Properties.HORIZONTAL_FACING)){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
  for(var wick:flameOffsets()){double x=wick[0]-8,z=wick[2]-8;for(int i=0;i<rotation;i++){double old=x;x=-z;z=old;}out.add(new Vec3d(pos.getX()+(x+8)/16,pos.getY()+wick[1]/16,pos.getZ()+(z+8)/16));}return out;
 }
 @Override public void randomDisplayTick(BlockState state,World world,BlockPos pos,net.minecraft.util.math.random.Random random){
  if(!state.get(Properties.LIT))return;
  if(kind==WorkshopKind.GHOST_LANTERN&&random.nextInt(3)!=0)return;
  for(var point:flamePositions(state,pos))world.addParticle(kind==WorkshopKind.GHOST_LANTERN?net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME:net.minecraft.particle.ParticleTypes.SMALL_FLAME,point.x,point.y,point.z,0,0,0);
 }
 @Override protected Direction support(BlockState s){return kind==WorkshopKind.GHOST_LANTERN&&s.get(Properties.HANGING)?Direction.UP:Direction.DOWN;}
 @Override protected ActionResult onUse(BlockState s,World w,BlockPos p,PlayerEntity player,BlockHitResult hit){if(!w.isClient)w.setBlockState(p,s.cycle(Properties.LIT));return ActionResult.success(w.isClient);}
}
