package com.witchersbestiary.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.WallMountedBlock;

/** Совместимое имя блока трофея лешего; размещение общее для всех трофеев. */
public final class LeshenHeadBlock extends TrophyHeadBlock {
    public static final MapCodec<LeshenHeadBlock> CODEC = createCodec(LeshenHeadBlock::new);
    public LeshenHeadBlock(Settings settings) { super(settings); }
    @Override protected MapCodec<? extends WallMountedBlock> getCodec() { return CODEC; }
}
