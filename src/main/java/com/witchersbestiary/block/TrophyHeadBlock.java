package com.witchersbestiary.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** Общая статичная основа напольных и настенных трофейных голов. */
public class TrophyHeadBlock extends WallMountedBlock {
    public static final MapCodec<TrophyHeadBlock> CODEC = createCodec(TrophyHeadBlock::new);
    private static final VoxelShape FLOOR = Block.createCuboidShape(2, 0, 2, 14, 16, 14);
    private static final VoxelShape NORTH = Block.createCuboidShape(2, 0, 6, 14, 16, 16);
    private static final VoxelShape SOUTH = Block.createCuboidShape(2, 0, 0, 14, 16, 10);
    private static final VoxelShape EAST = Block.createCuboidShape(0, 0, 2, 10, 16, 14);
    private static final VoxelShape WEST = Block.createCuboidShape(6, 0, 2, 16, 16, 14);

    public TrophyHeadBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(FACE, BlockFace.FLOOR));
    }
    @Override protected MapCodec<? extends WallMountedBlock> getCodec() { return CODEC; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(FACING, FACE); }
    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        Direction side=context.getSide();
        if (side==Direction.DOWN) return null;
        BlockState state=getDefaultState().with(FACE,side==Direction.UP?BlockFace.FLOOR:BlockFace.WALL)
                .with(FACING,side==Direction.UP?context.getHorizontalPlayerFacing().getOpposite():side);
        return state.canPlaceAt(context.getWorld(),context.getBlockPos())?state:null;
    }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if(state.get(FACE)!=BlockFace.WALL) return FLOOR;
        return switch(state.get(FACING)){case NORTH->NORTH;case SOUTH->SOUTH;case EAST->EAST;case WEST->WEST;default->FLOOR;};
    }
    @Override protected BlockState rotate(BlockState state, BlockRotation rotation) { return state.with(FACING,rotation.rotate(state.get(FACING))); }
    @Override protected BlockState mirror(BlockState state, BlockMirror mirror) { return state.rotate(mirror.getRotation(state.get(FACING))); }
}
