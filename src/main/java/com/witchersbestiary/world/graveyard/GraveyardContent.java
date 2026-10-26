package com.witchersbestiary.world.graveyard;

import net.minecraft.block.*;
import net.minecraft.registry.*;
import net.minecraft.loot.LootTable;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import java.util.*;

/** Versioned, bounded additions inside the original footprint and vertical envelope. */
public final class GraveyardContent {
    private GraveyardContent() {}
    public record Point(int x,int y,int z) {}
    public static RegistryKey<LootTable> loot(int kind) {
        return RegistryKey.of(RegistryKeys.LOOT_TABLE,Identifier.of("witchersbestiary","chests/"+new String[]{"village_graveyard","forest_graveyard","ancient_necropolis"}[kind]));
    }
    public static List<Point> rewards(GraveyardPiece p) {
        int n=GraveyardStructure.size(p.kind()),m=n/2,r=n-6;
        return switch(p.kind()) {
            case 0 -> List.of(new Point(m+3,0,r),new Point(m-3,0,r+1));
            case 1 -> List.of(new Point(4,-4,r),new Point(8,-4,r),new Point(m+4,0,r));
            default -> List.of(new Point(m-3,-5,r),new Point(m+3,-5,r),new Point(m,-5,r+2),new Point(m+3,-5,r-4));
        };
    }
    public static List<Point> ghosts(GraveyardPiece p) {
        int n=GraveyardStructure.size(p.kind()),m=n/2,r=n-6;
        var points=new ArrayList<Point>();
        points.add(new Point(5,0,m-4));points.add(new Point(n-6,0,m-4));
        if(p.kind()==0)points.add(new Point(m,0,r-2));
        else if(p.kind()==1){points.add(new Point(6,-4,r-2));points.add(new Point(6,-4,r));points.add(new Point(m,0,r-2));}
        else {points.add(new Point(m-2,-5,r));points.add(new Point(m+2,-5,r));points.add(new Point(m,-5,r));points.add(new Point(m-3,-5,r+2));points.add(new Point(m+3,-5,r+2));}
        int count=new int[]{2,3,5}[p.kind()]+(int)Math.floorMod(p.layoutSeed(),p.kind()==0?2:3);
        return List.copyOf(points.subList(0,count));
    }
    public static boolean reserved(GraveyardPiece p,int x,int z) {
        int n=GraveyardStructure.size(p.kind()),r=n-6;
        if(p.kind()==1&&((x>=3&&x<=9&&z>=r-10&&z<=r+2)||(x>=n/2&&x<=n/2+5&&z>=r-3&&z<=r+1)))return true;
        for(var q:ghosts(p))if(Math.abs(q.x-x)<=1&&Math.abs(q.z-z)<=1)return true;
        for(var q:rewards(p))if(Math.abs(q.x-x)<=1&&Math.abs(q.z-z)<=1)return true;
        return false;
    }
    public static void decorate(GraveyardPiece p,Map<String,GraveyardPiece.Cell> cells) {
        class Draw {
            void state(int x,int y,int z,BlockState state){cells.put(x+":"+y+":"+z,new GraveyardPiece.Cell(x,y,z,state));}
            void block(int x,int y,int z,Block block){state(x,y,z,block.getDefaultState());}
            void box(int x,int y,int z,int xx,int yy,int zz,Block block){for(int a=x;a<=xx;a++)for(int b=y;b<=yy;b++)for(int c=z;c<=zz;c++)block(a,b,c,block);}
            void shell(int x,int floor,int z,int xx,int roof,int zz){box(x,floor,z,xx,roof,zz,Blocks.STONE_BRICKS);box(x+1,floor+1,z+1,xx-1,roof-1,zz-1,Blocks.AIR);}
            void candle(int x,int y,int z){state(x,y,z,Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT,true));}
            void stairs(int x,int startZ,int steps){
                for(int i=0;i<steps;i++){
                    int z=startZ+i,y=-1-i;
                    box(x-2,-6,z,x+2,-1,z,Blocks.STONE_BRICKS);
                    box(x-1,y+1,z,x+1,3,z,Blocks.AIR);
                    for(int dx=-1;dx<=1;dx++)state(x+dx,y,z,Blocks.STONE_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING,Direction.NORTH));
                }
            }
        }
        var d=new Draw();int n=GraveyardStructure.size(p.kind()),m=n/2,r=n-6;
        if(p.kind()==0){
            // Two modest above-ground chapel niches; the second is behind crumbling stone.
            d.shell(m-4,-1,r,m-2,3,r+2);
            d.block(m-3,0,r,Blocks.CRACKED_STONE_BRICKS);d.block(m-3,1,r,Blocks.CRACKED_STONE_BRICKS);
            d.candle(m+2,0,r+2);d.block(m+4,2,r+1,Blocks.COBWEB);
        } else if(p.kind()==1){
            // A sealed burial chamber and open five-step descent, no extra terrain search.
            d.shell(3,-5,r-6,9,-1,r+2);
            d.stairs(6,r-10,4);
            d.box(5,-4,r-6,7,-2,r-6,Blocks.AIR);
            d.block(4,-4,r-3,Blocks.STONE_BRICK_SLAB);d.block(8,-4,r-3,Blocks.STONE_BRICK_SLAB);
            d.candle(4,-4,r-4);d.block(8,-2,r+1,Blocks.COBWEB);
            d.block(8,-2,r-2,Blocks.SOUL_LANTERN);
            // A defined approach to the surface cache, independent of natural terrain/trees.
            d.box(m,-1,r-2,m+4,-1,r-2,Blocks.MOSSY_COBBLESTONE);
            d.box(m,0,r-2,m+4,2,r-2,Blocks.AIR);
            d.box(m+3,-1,r-2,m+3,-1,r,Blocks.MOSSY_COBBLESTONE);
            d.box(m+3,0,r-2,m+3,2,r,Blocks.AIR);
        } else {
            // Original dry crypt envelope: x=m+-4, z=n-16..n-3, y=-6..-1.
            d.shell(m-4,-6,r-9,m+4,-1,r+3);
            d.stairs(m,r-7,5);
            d.box(m-1,-5,r-2,m+1,-2,r+1,Blocks.AIR);
            for(int side:new int[]{-1,1}){
                d.block(m+side*3,-5,r+1,Blocks.STONE_BRICK_SLAB);
                d.block(m+side*3,-4,r+1,Blocks.CHISELED_STONE_BRICKS);
                d.candle(m+side*2,-5,r+2);
                d.block(m+side*3,-2,r,Blocks.SOUL_LANTERN);
            }
            // One breakable partition opens a side burial niche beside the descent.
            d.block(m+3,-5,r-3,Blocks.CRACKED_STONE_BRICKS);
            d.block(m+3,-4,r-3,Blocks.CRACKED_STONE_BRICKS);
            d.block(m+3,-2,r-5,Blocks.COBWEB);
        }
        for(var q:ghosts(p)) {
            // The crypt shell already provides its floor; surface graves need small pads.
            if(q.y==0)d.box(q.x-1,-1,q.z-1,q.x+1,-1,q.z+1,Blocks.MOSSY_STONE_BRICKS);
            d.box(q.x,q.y,q.z,q.x,q.y+2,q.z,Blocks.AIR);
        }
        for(var q:rewards(p)) {
            // Standard vanilla chest lid clearance and deterministic deferred loot.
            d.block(q.x,q.y-1,q.z,Blocks.STONE_BRICKS);
            d.block(q.x,q.y,q.z,Blocks.CHEST);
            d.block(q.x,q.y+1,q.z,Blocks.AIR);
            if(q.y==0&&p.kind()!=0)d.box(q.x,1,q.z,q.x,2,q.z,Blocks.AIR);
        }
    }
}
