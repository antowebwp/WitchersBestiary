package com.witchersbestiary.board;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;


/** Nine synchronized cells, a single item/drop and a single visual model at the center. */
public final class NoticeBoardBlock extends Block {
    public static final IntProperty PART = IntProperty.of("part",0,8);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(()->false);
    public NoticeBoardBlock(Settings settings) { super(settings); setDefaultState(getDefaultState().with(PART,1).with(FACING,Direction.NORTH)); }
    @Override protected MapCodec<? extends Block> getCodec() { return createCodec(NoticeBoardBlock::new); }
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> b) { b.add(PART,FACING); }
    public static BlockPos cell(BlockPos root,Direction facing,int part) { return root.offset(facing.rotateYClockwise(),part%3-1).up(part/3); }
    public static BlockPos root(BlockPos pos,BlockState state) { return pos.offset(state.get(FACING).rotateYClockwise(),1-state.get(PART)%3).down(state.get(PART)/3); }
    public static boolean space(WorldView world,BlockPos root,Direction facing) {
        for(int i=0;i<9;i++) { var at=cell(root,facing,i); if(world.isOutOfHeightLimit(at)||(!world.getBlockState(at).isReplaceable()||!world.getFluidState(at).isEmpty()))return false; }
        for(int i=0;i<3;i++) {var at=cell(root,facing,i).down();var ground=world.getBlockState(at);if(!ground.isSideSolidFullSquare(world,at,Direction.UP)||!ground.getFluidState().isEmpty())return false;}
        return true;
    }
    public static boolean complete(WorldView world,BlockPos root,Direction facing) {
        for(int i=0;i<9;i++){var s=world.getBlockState(cell(root,facing,i));if(!s.isOf(NoticeBoards.BLOCK)||s.get(PART)!=i||s.get(FACING)!=facing)return false;}return true;
    }
    public static boolean place(WorldAccess world,BlockPos root,Direction facing) {
        if(!space(world,root,facing))return false;
        for(int i=0;i<9;i++)world.setBlockState(cell(root,facing,i),NoticeBoards.BLOCK.getDefaultState().with(PART,i).with(FACING,facing),Block.NOTIFY_LISTENERS);
        return true;
    }
    @Override public BlockState getPlacementState(ItemPlacementContext c) {var facing=c.getHorizontalPlayerFacing().getOpposite();return space(c.getWorld(),c.getBlockPos(),facing)?getDefaultState().with(FACING,facing):null;}
    @Override public void onPlaced(World world,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        if(!world.isClient&&state.get(PART)==1) for(int i=0;i<9;i++)if(i!=1)world.setBlockState(cell(pos,state.get(FACING),i),state.with(PART,i),Block.NOTIFY_LISTENERS);
    }
    @Override protected void onStateReplaced(BlockState state,World world,BlockPos pos,BlockState next,boolean moved) {
        if(!world.isClient&&!next.isOf(this)&&!REMOVING.get()) {
            REMOVING.set(true);try {
                var root=root(pos,state);var facing=state.get(FACING);
                for(int i=0;i<9;i++){var at=cell(root,facing,i);if(!at.equals(pos)&&world.getBlockState(at).isOf(this))world.setBlockState(at,Blocks.AIR.getDefaultState(),Block.NOTIFY_ALL);}
            }finally{REMOVING.set(false);}
        }
        super.onStateReplaced(state,world,pos,next,moved);
    }
    @Override protected BlockState getStateForNeighborUpdate(BlockState s,Direction dir,BlockState neighbor,WorldAccess world,BlockPos pos,BlockPos neighborPos) {
        if(!world.isClient()&&dir==Direction.DOWN&&s.get(PART)<3&&!neighbor.isSideSolidFullSquare(world,neighborPos,Direction.UP))world.scheduleBlockTick(pos,this,1);
        return s;
    }
    @Override protected void scheduledTick(BlockState state,net.minecraft.server.world.ServerWorld world,BlockPos pos,net.minecraft.util.math.random.Random random) {
        var below=pos.down();if(state.get(PART)<3&&!world.getBlockState(below).isSideSolidFullSquare(world,below,Direction.UP))world.breakBlock(pos,true);
    }
    @Override protected ActionResult onUse(BlockState state,World world,BlockPos pos,PlayerEntity player,BlockHitResult hit) {
        if(!world.isClient&&player instanceof ServerPlayerEntity server) {
            var root=root(pos,state);if(server.squaredDistanceTo(root.toCenterPos().add(0,1,0))<=64&&complete(world,root,state.get(FACING))) {
                server.openHandledScreen(new net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<BlockPos>() {
                    public BlockPos getScreenOpeningData(ServerPlayerEntity p){return root;}
                    public Text getDisplayName(){return Text.translatable("block.witchersbestiary.notice_board");}
                    public ScreenHandler createMenu(int sync,net.minecraft.entity.player.PlayerInventory inventory,PlayerEntity p){return new BoardScreenHandler(sync,inventory,root);}
                });
                if(server.currentScreenHandler instanceof BoardScreenHandler handler)handler.send(server,"");
            }
        }
        return ActionResult.SUCCESS;
    }
    @Override protected BlockState rotate(BlockState s,BlockRotation r){return s.with(FACING,r.rotate(s.get(FACING)));}
    @Override protected BlockState mirror(BlockState s,BlockMirror m){return s.rotate(m.getRotation(s.get(FACING)));}
    @Override protected VoxelShape getOutlineShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return NoticeBoardShapes.get(s.get(PART),s.get(FACING));}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return getOutlineShape(s,w,p,c);}
}
