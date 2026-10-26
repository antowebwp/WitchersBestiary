package com.witchersbestiary.ward;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
import net.minecraft.text.Text;
public final class SpiritWardBlock extends Block {
 public static final SpiritWardBlock BLOCK=Registry.register(Registries.BLOCK,Identifier.of("witchersbestiary","spirit_ward"),new SpiritWardBlock(Settings.create().strength(2.5f).nonOpaque().luminance(s->7).sounds(BlockSoundGroup.STONE)));
 public static final Item ITEM=Registry.register(Registries.ITEM,Identifier.of("witchersbestiary","spirit_ward"),new BlockItem(BLOCK,new Item.Settings()){
  @Override public void appendTooltip(ItemStack stack,Item.TooltipContext c,java.util.List<Text> lines,net.minecraft.item.tooltip.TooltipType type){lines.add(Text.translatable("tooltip.witchersbestiary.spirit_ward").formatted(net.minecraft.util.Formatting.GRAY));}
 });
 public SpiritWardBlock(Settings s){super(s);}
 @Override protected MapCodec<? extends Block> getCodec(){return createCodec(SpiritWardBlock::new);}
 @Override protected VoxelShape getOutlineShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return net.minecraft.util.shape.VoxelShapes.union(Block.createCuboidShape(-1.6,0,-1.6,17.6,4.2,17.6),Block.createCuboidShape(3.5,4.2,3.5,12.5,13.7,12.5),Block.createCuboidShape(1.5,11,1.5,14.5,24,14.5),Block.createCuboidShape(6.7,24,6.7,9.3,25.6,9.3));}
 @Override public BlockState getPlacementState(ItemPlacementContext c){return c.getSide()==Direction.UP&&c.getWorld().getBlockState(c.getBlockPos().up()).isAir()?super.getPlacementState(c):null;}
 @Override protected boolean canPlaceAt(BlockState s,WorldView w,BlockPos p){return w.getBlockState(p.down()).isSideSolidFullSquare(w,p.down(),Direction.UP);}
 @Override protected BlockState getStateForNeighborUpdate(BlockState s,Direction d,BlockState n,WorldAccess w,BlockPos p,BlockPos q){return !s.canPlaceAt(w,p)?Blocks.AIR.getDefaultState():s;}
 @Override protected void onBlockAdded(BlockState s,World w,BlockPos p,BlockState old,boolean notify){super.onBlockAdded(s,w,p,old,notify);if(w instanceof ServerWorld server&&!old.isOf(this))SpiritWardState.get(server).add(p);}
 @Override protected void onStateReplaced(BlockState s,World w,BlockPos p,BlockState next,boolean moved){if(w instanceof ServerWorld server&&!next.isOf(this))SpiritWardState.get(server).remove(p);super.onStateReplaced(s,w,p,next,moved);}
 @Override public void onPlaced(World w,BlockPos p,BlockState s,net.minecraft.entity.LivingEntity who,ItemStack stack){super.onPlaced(w,p,s,who,stack);w.playSound(null,p,SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,SoundCategory.BLOCKS,.5f,1.2f);}
 @Override public void randomDisplayTick(BlockState s,World w,BlockPos p,net.minecraft.util.math.random.Random random){if(random.nextInt(12)==0)w.addParticle(ParticleTypes.SOUL,p.getX()+.5,p.getY()+1.25,p.getZ()+.5,0,.008,0);}
 public static boolean blocks(ServerWorld w,net.minecraft.entity.EntityType<?> type,net.minecraft.entity.SpawnReason reason,BlockPos p){return reason==net.minecraft.entity.SpawnReason.NATURAL&&(type==com.witchersbestiary.entity.ModEntities.NOONWRAITH||type==com.witchersbestiary.entity.ModEntities.WRAITH)&&SpiritWardState.get(w).protects(p);}
 public static void initialize(){}
}
