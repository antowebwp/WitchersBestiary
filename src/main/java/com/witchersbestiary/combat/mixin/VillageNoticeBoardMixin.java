package com.witchersbestiary.combat.mixin;
import com.witchersbestiary.board.VillageNoticeBoards;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(StructureStart.class)
public abstract class VillageNoticeBoardMixin {
 @Inject(method="place",at=@At("TAIL"))
 private void witchersbestiary$noticeBoard(StructureWorldAccess world,StructureAccessor accessor,ChunkGenerator generator,Random random,BlockBox box,ChunkPos chunk,CallbackInfo ci){VillageNoticeBoards.generate(world,(StructureStart)(Object)this,chunk);}
}
