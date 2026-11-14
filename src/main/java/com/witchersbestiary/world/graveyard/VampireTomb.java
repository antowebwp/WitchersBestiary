package com.witchersbestiary.world.graveyard;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.StairsBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.*;
import net.minecraft.structure.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.*;
import java.util.Optional;

public final class VampireTomb extends Structure {
    public static final Identifier ID=Identifier.of("witchersbestiary","ancient_vampire_tomb");
    public static final MapCodec<VampireTomb> CODEC=createCodec(VampireTomb::new);
    private static final StructureType<VampireTomb> TYPE=()->CODEC;
    private static final StructurePieceType PIECE=(context,nbt)->new Chamber(nbt);
    public static final int DEPTH=20;

    public VampireTomb(Config config){super(config);}
    public static void initialize(){
        Registry.register(Registries.STRUCTURE_TYPE,ID,TYPE);
        Registry.register(Registries.STRUCTURE_PIECE,ID,PIECE);
    }
    @Override public StructureType<?> getType(){return TYPE;}
    @Override protected Optional<StructurePosition> getStructurePosition(Context c){
        int x=c.chunkPos().getStartX()+8,z=c.chunkPos().getStartZ()+8;
        int y=c.chunkGenerator().getHeight(x,z,Heightmap.Type.WORLD_SURFACE_WG,c.world(),c.noiseConfig());
        if(y<=c.chunkGenerator().getSeaLevel()+2||y-DEPTH<=c.world().getBottomY()+2)return Optional.empty();
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
            var column=c.chunkGenerator().getColumnSample(x+dx,z+dz,c.world(),c.noiseConfig());
            if(!column.getState(y-1).isSolidBlock(EmptyBlockView.INSTANCE,BlockPos.ORIGIN)
                    || !column.getState(y).getFluidState().isEmpty())return Optional.empty();
        }
        var entry=new BlockPos(x,y,z);
        return Optional.of(new StructurePosition(entry, collector->collector.addPiece(new Chamber(entry))));
    }

    public static final class Chamber extends StructurePiece {
        public Chamber(BlockPos entry){
            super(PIECE,0,new BlockBox(entry.getX()-14,entry.getY()-DEPTH-1,entry.getZ()-2,
                    entry.getX()+14,entry.getY()+4,entry.getZ()+44));
        }
        public Chamber(NbtCompound nbt){super(PIECE,nbt);}
        public BlockPos entry(){return new BlockPos(boundingBox.getMinX()+14,boundingBox.getMaxY()-4,boundingBox.getMinZ()+2);}
        public BlockPos arena(){return entry().add(0,-DEPTH,32);}
        @Override protected void writeNbt(StructureContext context,NbtCompound nbt){}
        @Override public void generate(StructureWorldAccess world,StructureAccessor accessor,ChunkGenerator generator,
                                      Random random,BlockBox chunkBox,ChunkPos chunk,BlockPos pivot){
            var entry=entry();
            for(int z=-2;z<=44;z++)for(int x=-14;x<=14;x++){
                if (!chunkBox.contains(entry.add(x,0,z))) continue;
                for(int y=-DEPTH-1;y<=4;y++){
                    var state=cell(x,y,z);
                    if(state!=null)world.setBlockState(entry.add(x,y,z),state,2);
                }
            }
        }
        private net.minecraft.block.BlockState cell(int x,int y,int z){
            int ax=Math.abs(x), height=y+DEPTH;
            if(z>=21&&ax<=14&&height<=13){
                int corner=Math.max(0,ax-10)+Math.max(0,Math.abs(z-32)-8);
                if(corner>6)return null;
                int roof=13-Math.max(0,ax-9);
                if(height>roof)return null;
                boolean entrance=ax<=2&&z<=22&&height>=0&&height<=4;
                if(height==-1||height==roof||!entrance&&(ax==14||z==21||z==44||corner==6))
                    return Math.floorMod(x*31+y*13+z*17,9)==0?Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState():Blocks.DEEPSLATE_BRICKS.getDefaultState();
                if(ax==11&&(z==25||z==32||z==39))
                    return height==2?Blocks.SHROOMLIGHT.getDefaultState():Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
                if(ax>=10&&ax<=12&&(z==28||z==36)&&height==0)return Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.getDefaultState();
                if(z>=41&&ax>=7&&ax<=9&&height==0)return Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
                if(z==42&&ax==8&&height==1)return Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState();
                if(z==43&&ax<=2&&height==2)return Blocks.REDSTONE_LAMP.getDefaultState().with(net.minecraft.block.RedstoneLampBlock.LIT,true);
                return Blocks.AIR.getDefaultState();
            }
            if(z>=0&&z<=20&&ax<=3){
                int tread=-Math.min(z,DEPTH);
                if(y<tread-1||y>tread+4)return null;
                if(ax==3||y==tread-1||y==tread+4&&z>=4)return Blocks.DEEPSLATE_BRICKS.getDefaultState();
                if(y==tread&&z<20)return Blocks.DEEPSLATE_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING,Direction.NORTH).with(StairsBlock.HALF,BlockHalf.BOTTOM);
                return Blocks.AIR.getDefaultState();
            }
            if(z<0&&ax<=3&&y>=0&&y<=3){
                if(ax==3||y==3)return Blocks.CHISELED_DEEPSLATE.getDefaultState();
                return Blocks.AIR.getDefaultState();
            }
            return null;
        }
    }
}
