package com.witchersbestiary.workshop;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
public final class GravestoneBlock extends WorkshopDecorBlock {
 public static final IntProperty VARIANT=IntProperty.of("variant",0,2);
 public GravestoneBlock(Settings s){super(WorkshopKind.ANCIENT_GRAVESTONE,s);setDefaultState(getDefaultState().with(VARIANT,0));}
 @Override protected MapCodec<? extends Block> getCodec(){return createCodec(GravestoneBlock::new);}
 @Override protected void appendProperties(StateManager.Builder<Block,BlockState> b){super.appendProperties(b);b.add(VARIANT);}
 @Override public BlockState getPlacementState(ItemPlacementContext c){var s=super.getPlacementState(c);return s==null?null:s.with(VARIANT,Math.floorMod(c.getBlockPos().hashCode(),3));}
}
