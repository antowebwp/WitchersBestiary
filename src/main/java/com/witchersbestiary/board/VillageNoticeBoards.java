package com.witchersbestiary.board;

import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.*;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.Heightmap;

/** One candidate chunk per vanilla village, bounded in-chunk checks, no saved-world scan/tickets. */
public final class VillageNoticeBoards {
    private VillageNoticeBoards(){}
    public static boolean generate(StructureWorldAccess world,StructureStart start,ChunkPos chunk) {
        var id=world.getRegistryManager().get(RegistryKeys.STRUCTURE).getId(start.getStructure());
        if(id==null||!id.getNamespace().equals("minecraft")||!id.getPath().startsWith("village_"))return false;
        var bounds=start.getBoundingBox();
        var selected=new ChunkPos(new BlockPos((bounds.getMinX()+bounds.getMaxX())/2,0,bounds.getMinZ()+5));
        if(!selected.equals(chunk))return false;
        return attempt(world,chunk);
    }
    public static boolean attempt(StructureWorldAccess world,ChunkPos chunk) {
        // A revisit cannot produce a second board; all candidates stay in this single chunk.
        for(int x=chunk.getStartX();x<=chunk.getEndX();x++)for(int z=chunk.getStartZ();z<=chunk.getEndZ();z++){
            int y=world.getTopY((world instanceof net.minecraft.world.ChunkRegion?Heightmap.Type.WORLD_SURFACE_WG:Heightmap.Type.WORLD_SURFACE),x,z);
            if(world.getBlockState(new BlockPos(x,y-1,z)).isOf(NoticeBoards.BLOCK))return false;
        }
        int[][] offsets={{8,8},{4,4},{12,12},{4,12},{12,4},{8,4},{8,12},{4,8},{12,8}};
        for(var offset:offsets){int x=chunk.getStartX()+offset[0],z=chunk.getStartZ()+offset[1];int y=world.getTopY((world instanceof net.minecraft.world.ChunkRegion?Heightmap.Type.WORLD_SURFACE_WG:Heightmap.Type.WORLD_SURFACE),x,z);
            if(world.getBlockState(new BlockPos(x,y-1,z)).isOf(Blocks.SNOW))y--;
            var root=new BlockPos(x,y,z);
            for(var facing:Direction.Type.HORIZONTAL){boolean terrain=true;
                for(int i=0;i<3;i++){var support=NoticeBoardBlock.cell(root,facing,i).down();var s=world.getBlockState(support);
                    if(!(s.isIn(BlockTags.DIRT)||s.isIn(BlockTags.SAND)||s.isOf(Blocks.STONE)||s.isOf(Blocks.GRAVEL)||s.isOf(Blocks.SNOW_BLOCK))||s.isOf(Blocks.DIRT_PATH))terrain=false;
                }
                if(terrain&&NoticeBoardBlock.place(world,root,facing))return true;
            }
        }return false;
    }
}
