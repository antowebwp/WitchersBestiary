package com.witchersbestiary.world.graveyard;

import com.witchersbestiary.entity.ModEntities;
import net.minecraft.block.*;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.*;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import java.util.*;

/** A small deterministic blueprint; no world reads outside the current chunk box. */
public final class GraveyardPiece extends StructurePiece {
 private final int kind,turn,y; private final long seed; private boolean boss;
 private int contentVersion=1,ghostMask,generatedChunks;
 public record Cell(int x,int y,int z,BlockState state){}
 public record Site(String purpose,int x,int y,int z){}
 public GraveyardPiece(int k,int x,int y,int z,long seed,int turn){super(GraveyardStructure.PIECE,0,new BlockBox(x,y-6,z,x+GraveyardStructure.size(k)-1,y+10,z+GraveyardStructure.size(k)-1));kind=k;this.y=y;this.seed=seed;this.turn=turn;}
 public GraveyardPiece(NbtCompound n){super(GraveyardStructure.PIECE,n);kind=n.getInt("Kind");y=n.getInt("SurfaceY");seed=n.getLong("LayoutSeed");turn=n.getInt("Turn");boss=n.getBoolean("BossCreated");contentVersion=n.getInt("ContentVersion");ghostMask=n.getInt("GhostMask");generatedChunks=n.getInt("GeneratedChunks");}
 @Override protected void writeNbt(StructureContext c,NbtCompound n){n.putInt("Kind",kind);n.putInt("SurfaceY",y);n.putLong("LayoutSeed",seed);n.putInt("Turn",turn);n.putBoolean("BossCreated",boss);n.putInt("ContentVersion",contentVersion);n.putInt("GhostMask",ghostMask);n.putInt("GeneratedChunks",generatedChunks);}
 public static GraveyardPiece terrainProbe(int k,int x,int y,int z,long seed,int turn){var p=new GraveyardPiece(k,x,y,z,seed,turn);p.contentVersion=0;return p;}
 public boolean haunted(){return contentVersion==1;}
 public int kind(){return kind;}
 public long layoutSeed(){return seed;}
 public int surfaceY(){return y;}
 public int ghostMask(){return ghostMask;}
 public String placementKey(){return boundingBox.getMinX()+":"+y+":"+boundingBox.getMinZ()+":"+kind;}
 public boolean consumeGhost(int slot){int bit=1<<slot;if((ghostMask&bit)!=0)return false;ghostMask|=bit;return true;}
 private int n(){return GraveyardStructure.size(kind);}
 public BlockPos position(int x,int dy,int z){int s=n()-1;return switch(turn){case 1->new BlockPos(boundingBox.getMinX()+s-z,y+dy,boundingBox.getMinZ()+x);case 2->new BlockPos(boundingBox.getMinX()+s-x,y+dy,boundingBox.getMinZ()+s-z);case 3->new BlockPos(boundingBox.getMinX()+z,y+dy,boundingBox.getMinZ()+s-x);default->new BlockPos(boundingBox.getMinX()+x,y+dy,boundingBox.getMinZ()+z);};}
 public List<Site> sites(){int m=n()/2;return List.of(new Site("guardian",m,0,m),new Site("future_reward",m-3,0,n()-7),new Site("future_spirit",4,0,m),new Site("altar",m,1,n()-6));}
 private static BlockState state(Block b){return b.getDefaultState();}
 public List<Cell> blueprint(){
  var map=new LinkedHashMap<String,Cell>();Random r=Random.create(seed);int n=n(),m=n/2;
  java.util.function.Consumer<Cell> put=c->map.put(c.x+":"+c.y+":"+c.z,c);
  class Draw {
   void b(int x,int y,int z,Block b){put.accept(new Cell(x,y,z,state(b)));}
   void box(int x,int y,int z,int xx,int yy,int zz,Block b){for(int a=x;a<=xx;a++)for(int v=y;v<=yy;v++)for(int c=z;c<=zz;c++)b(a,v,c,b);}
   Block stone(){return switch(r.nextInt(5)){case 0,1->Blocks.MOSSY_STONE_BRICKS;case 2->Blocks.CRACKED_STONE_BRICKS;default->Blocks.STONE_BRICKS;};}
   void pillar(int x,int z,int h){for(int v=0;v<h;v++)b(x,v,z,stone());b(x,h,z,Blocks.STONE_BRICK_SLAB);}
  }var d=new Draw();
  // Irregular low perimeter with open gate, capped piers, and broken rails.
  for(int t=1;t<n-1;t++)for(int side=0;side<4;side++){
   int x=side<2?t:(side==2?1:n-2),z=side<2?(side==0?1:n-2):t;
   if(z==1&&Math.abs(x-m)<=2)continue;
   if(t%5==1)d.pillar(x,z,2+r.nextInt(kind==2?3:2));
   else if(r.nextInt(kind==1?4:7)!=0){d.b(x,0,z,d.stone());d.b(x,1,z,kind==0?Blocks.OAK_FENCE:Blocks.COBBLESTONE_WALL);}
  }
  // Connected paths and an open central court. No rectangular terrain pad.
  for(int x=2;x<n-2;x++)for(int z=1;z<n-2;z++)if(Math.abs(x-m)<=1||Math.abs(z-m)<=1){
   d.b(x,-1,z,r.nextInt(5)==0?Blocks.COARSE_DIRT:r.nextBoolean()?Blocks.GRAVEL:Blocks.MOSSY_COBBLESTONE);
   d.box(x,0,z,x,2,z,Blocks.AIR);
  }
  for(int x=4;x<n-4;x+=4)for(int z=5;z<n-8;z+=5){
   if(Math.abs(x-m)<3||Math.abs(z-m)<3||r.nextInt(7)==0)continue;
   d.box(x,-1,z,x+1,-1,z+2,r.nextBoolean()?Blocks.COARSE_DIRT:Blocks.MOSS_BLOCK);
   d.box(x,0,z,x+1,2,z+2,Blocks.AIR);
   int type=r.nextInt(4);d.b(x,0,z,d.stone());
   if(type==0){d.b(x,1,z,Blocks.COBBLESTONE_WALL);d.b(x,2,z,Blocks.STONE_BRICK_SLAB);d.b(x+1,1,z,Blocks.STONE_BRICK_SLAB);}
   else if(type==1){d.b(x,1,z,Blocks.CHISELED_STONE_BRICKS);d.b(x,2,z,Blocks.STONE_BRICK_SLAB);}
   else if(type==2)d.b(x+1,0,z+1,Blocks.STONE_BRICK_STAIRS);
   else d.b(x,1,z,Blocks.STONE_BRICK_SLAB);
   if(r.nextBoolean())d.b(x+1,0,z+2,Blocks.FERN);
   if(r.nextInt(6)==0)d.b(x+1,0,z+1,Blocks.COBWEB);
  }
  // Architectural focus: ruined village chapel, forest altar, monumental necropolis.
  int rear=n-6;
  if(kind==0){
   d.box(m-4,-1,rear-2,m+4,-1,rear+2,Blocks.STONE_BRICKS);
   d.box(m-3,0,rear-2,m+3,4,rear+2,Blocks.AIR);
   for(int x=m-4;x<=m+4;x++){int h=2+r.nextInt(4);d.box(x,0,rear+2,x,h,rear+2,d.stone());}
   for(int z=rear-2;z<=rear+1;z++){d.pillar(m-4,z,2+r.nextInt(3));d.pillar(m+4,z,1+r.nextInt(3));}
   d.box(m-3,5,rear,m+1,5,rear,Blocks.DARK_OAK_SLAB);
   d.b(m-3,3,rear+1,Blocks.COBWEB);
  }
  d.box(m-1,0,rear,m+1,0,rear,Blocks.STONE_BRICKS);d.b(m,1,rear,Blocks.CHISELED_STONE_BRICKS);d.b(m,2,rear,Blocks.STONE_BRICK_SLAB);
  if(kind==1){
   for(int z=m-4;z<=m+4;z++)d.b(3,0,z,z%2==0?Blocks.MOSSY_COBBLESTONE:Blocks.MOSSY_STONE_BRICKS);
   d.pillar(n-6,rear,4);d.b(n-5,1,rear,Blocks.COBWEB);
   for(int x=5;x<n-4;x+=5){d.b(x,0,n-4,Blocks.OAK_LEAVES);d.b(x,0,n-5,Blocks.MOSS_BLOCK);}
  }
  if(kind==2){
   for(int x:new int[]{m-4,m+4}){d.pillar(x,2,6);d.pillar(x,m,5);}
   d.box(m-4,6,2,m+4,6,2,Blocks.STONE_BRICKS);d.box(m-2,7,2,m+2,7,2,Blocks.STONE_BRICK_SLAB);
   for(int x:new int[]{5,n-7})for(int z:new int[]{m-5,m+4}){d.box(x,0,z,x+1,0,z+3,Blocks.STONE_BRICKS);d.box(x,1,z,x+1,1,z+3,Blocks.SMOOTH_STONE_SLAB);}
   // Sealed stone shell, dry chamber, open stair descending from the main path.
   d.box(m-4,-6,rear-3,m+4,-1,rear+3,Blocks.STONE_BRICKS);
   d.box(m-3,-5,rear-2,m+3,-2,rear+2,Blocks.AIR);
   for(int step=0;step<5;step++){
    int z=rear-7+step,yy=-1-step;
    d.box(m-1,yy-1,z,m+1,yy-1,z,Blocks.STONE_BRICKS);
    for(int x=m-1;x<=m+1;x++)put.accept(new Cell(x,yy,z,Blocks.STONE_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING,Direction.NORTH)));
    d.box(m-1,yy+1,z,m+1,2,z,Blocks.AIR);
    d.box(m-2,-6,z,m-2,-1,z,Blocks.STONE_BRICKS);d.box(m+2,-6,z,m+2,-1,z,Blocks.STONE_BRICKS);
   }
   d.box(m-1,-5,rear-2,m+1,-2,rear-2,Blocks.AIR);
   d.b(m-3,-4,rear+1,Blocks.CHISELED_STONE_BRICKS);
  }
  // Guaranteed 3x3 solid guardian court and full two-block clearance.
  d.box(m-1,-1,m-1,m+1,-1,m+1,Blocks.MOSSY_STONE_BRICKS);d.box(m-1,0,m-1,m+1,3,m+1,Blocks.AIR);
    // Low vegetation patches occupy gaps, leaving the linked paths and graves readable.
  for(int x=2;x<n-2;x++)for(int z=3;z<n-3;z++){
   if(map.containsKey(x+":0:"+z)||Math.abs(x-m)<3||Math.abs(z-m)<2)continue;
   if(r.nextInt(kind==1?3:5)==0){d.b(x,-1,z,r.nextBoolean()?Blocks.MOSS_BLOCK:Blocks.ROOTED_DIRT);d.b(x,0,z,r.nextInt(9)==0?Blocks.POPPY:Blocks.FERN);}
   else if(kind==1&&r.nextInt(14)==0){d.b(x,-1,z,Blocks.ROOTED_DIRT);d.b(x,0,z,Blocks.OAK_LEAVES);}
  }
  for(int x:new int[]{m-2,m+2}){d.b(x,-1,m-2,Blocks.MOSSY_COBBLESTONE);d.b(x,0,m-2,Blocks.COBBLESTONE_WALL);d.b(x,1,m-2,Blocks.LANTERN);}
  if(haunted())GraveyardContent.decorate(this,map);
  // Every above-ground masonry column has its own shallow footing.
  for(Cell c:new ArrayList<>(map.values()))if(c.y==0&&!c.state.isAir()&&c.state.isSolid()) {
   String key=c.x+":-1:"+c.z;if(!map.containsKey(key))d.b(c.x,-1,c.z,Blocks.MOSSY_COBBLESTONE);
  }
    // Explicit rail connections are deterministic across chunk boundaries.
  for(Cell c:new ArrayList<>(map.values())){
   BlockState s=c.state;
   if(s.getBlock() instanceof LeavesBlock)s=s.with(LeavesBlock.PERSISTENT,true);
   if(s.getBlock() instanceof FenceBlock||s.getBlock() instanceof WallBlock){
    for(Direction dir:Direction.Type.HORIZONTAL){var adjacent=map.get((c.x+dir.getOffsetX())+":"+c.y+":"+(c.z+dir.getOffsetZ()));boolean linked=adjacent!=null&&!adjacent.state.isAir();
     if(s.getBlock() instanceof FenceBlock)s=s.with(switch(dir){case NORTH->FenceBlock.NORTH;case SOUTH->FenceBlock.SOUTH;case EAST->FenceBlock.EAST;default->FenceBlock.WEST;},linked);
     else s=s.with(switch(dir){case NORTH->WallBlock.NORTH_SHAPE;case SOUTH->WallBlock.SOUTH_SHAPE;case EAST->WallBlock.EAST_SHAPE;default->WallBlock.WEST_SHAPE;},linked?net.minecraft.block.enums.WallShape.LOW:net.minecraft.block.enums.WallShape.NONE);
    }
   }
   map.put(c.x+":"+c.y+":"+c.z,new Cell(c.x,c.y,c.z,s));
  }
  return List.copyOf(map.values());
 }
 @Override public void generate(StructureWorldAccess world,StructureAccessor accessor,ChunkGenerator generator,Random random,BlockBox box,ChunkPos chunk,BlockPos pivot){
  int chunkBit=1<<((chunk.x-(boundingBox.getMinX()>>4))*3+chunk.z-(boundingBox.getMinZ()>>4));
  if(haunted()&&(generatedChunks&chunkBit)!=0)return;
  BlockRotation rotation=BlockRotation.values()[turn];
  var cells=blueprint();var planned=new HashSet<BlockPos>();
  for(Cell cell:cells)planned.add(position(cell.x,cell.y,cell.z));
  for(Cell c:cells){
   BlockPos p=position(c.x,c.y,c.z);if(!box.contains(p))continue;
   // Only small local foundations, no massive pad or downward unbounded fill.
   if(c.y==-1&&!c.state.isAir())for(int depth=1;depth<=3;depth++){
    BlockPos q=p.down(depth);if(!box.contains(q)||planned.contains(q))continue;
    var old=world.getBlockState(q);if(!old.isSolidBlock(world,q))world.setBlockState(q,state(Blocks.DIRT),2);
   }
   // Preserve mature forest trunks except the small focal construction / walking court.
   if(kind==1&&(!haunted()||!GraveyardContent.reserved(this,c.x,c.z))&&world.getBlockState(p).isIn(net.minecraft.registry.tag.BlockTags.LOGS)&& (Math.abs(c.x-n()/2)>1||Math.abs(c.z-n()/2)>1))continue;
   world.setBlockState(p,c.state.rotate(rotation),2);
  }
  if(haunted())for(var reward:GraveyardContent.rewards(this)){
   var chest=position(reward.x(),reward.y(),reward.z());
   if(box.contains(chest)&&world.getBlockEntity(chest) instanceof net.minecraft.block.entity.LootableContainerBlockEntity container)
    container.setLootTable(GraveyardContent.loot(kind),seed^chest.asLong());
  }
  BlockPos spawn=position(n()/2,0,n()/2);
  if(!boss&&box.contains(spawn)){
   var hag=ModEntities.GRAVE_HAG.create(world.toServerWorld());
   if(hag!=null){hag.refreshPositionAndAngles(spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5,turn*90,0);hag.initialize(world,world.getLocalDifficulty(spawn),SpawnReason.STRUCTURE,null);hag.setPersistent();world.spawnEntityAndPassengers(hag);boss=true;}
  }
  if(haunted())generatedChunks|=chunkBit;
 }
}





