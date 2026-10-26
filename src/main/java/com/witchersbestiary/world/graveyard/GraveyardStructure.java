package com.witchersbestiary.world.graveyard;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.*;
import net.minecraft.structure.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.*;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Vanilla structure lifecycle: starts, references, chunk clipping and piece NBT. */
public final class GraveyardStructure extends Structure {
 public static final MapCodec<GraveyardStructure> CODEC=RecordCodecBuilder.mapCodec(i->i.group(configCodecBuilder(i),com.mojang.serialization.Codec.intRange(0,2).fieldOf("kind").forGetter(s->s.kind)).apply(i,GraveyardStructure::new));
 public static final StructureType<GraveyardStructure> TYPE=()->CODEC;
 public static final StructurePieceType PIECE=(context,nbt)->new GraveyardPiece(nbt);
 private final int kind;
 public GraveyardStructure(Config config,int kind){super(config);this.kind=kind;}
 public static void initialize(){Registry.register(Registries.STRUCTURE_TYPE,Identifier.of("witchersbestiary","graveyard"),TYPE);Registry.register(Registries.STRUCTURE_PIECE,Identifier.of("witchersbestiary","graveyard"),PIECE);}
 @Override public StructureType<?> getType(){return TYPE;}
 public static int size(int kind){return new int[]{23,27,39}[kind];}
 public static boolean acceptable(int min,int max,int sea){return min>sea+2&&max-min<=2;}
 // Three blocks of fill below the foundation plus the foundation itself bridge
 // up to four blocks of natural relief. Only the 39x39 necropolis needs this;
 // requiring <=2 there made otherwise safe sites practically unfindable.
 private int maxRise(){return kind==2?4:2;}
 private boolean acceptableTerrain(int min,int max,int sea){return min>sea+2&&max-min<=maxRise();}

 /** Per-attempt memoization only. Never holds a world, chunk or cross-world cache. */
 private static final class Terrain {
  private final Context context;
  private final Map<Long,Integer> surfaces=new HashMap<>(),floors=new HashMap<>();
  private final Map<Long,VerticalBlockSample> columns=new HashMap<>();
  Terrain(Context context){this.context=context;}
  int height(int x,int z,Heightmap.Type type){
   var cache=type==Heightmap.Type.WORLD_SURFACE_WG?surfaces:floors;
   return cache.computeIfAbsent(ChunkPos.toLong(x,z),key->context.chunkGenerator().getHeight(x,z,type,context.world(),context.noiseConfig()));
  }
  VerticalBlockSample column(int x,int z){return columns.computeIfAbsent(ChunkPos.toLong(x,z),key->context.chunkGenerator().getColumnSample(x,z,context.world(),context.noiseConfig()));}
  void refine(int x,int z){
   long key=ChunkPos.toLong(x,z);
   if(surfaces.containsKey(key)&&floors.containsKey(key))return;
   if(!(context.chunkGenerator() instanceof NoiseChunkGenerator)){height(x,z,Heightmap.Type.WORLD_SURFACE_WG);height(x,z,Heightmap.Type.OCEAN_FLOOR_WG);return;}
   // NoiseChunkGenerator uses the same sampleHeightmap for getHeight and
   // getColumnSample. Sample ONCE, derive both exact native height predicates,
   // then reuse the same column for the crypt. No approximate terrain tests.
   var blocks=column(x,z);int surface=context.world().getBottomY(),floor=surface;boolean surfaceFound=false;
   for(int y=context.world().getTopY()-1;y>=context.world().getBottomY();y--){
    var block=blocks.getState(y);
    if(!surfaceFound&&Heightmap.Type.WORLD_SURFACE_WG.getBlockPredicate().test(block)){surface=y+1;surfaceFound=true;}
    if(Heightmap.Type.OCEAN_FLOOR_WG.getBlockPredicate().test(block)){floor=y+1;break;}
   }
   surfaces.put(key,surface);floors.put(key,floor);
  }
 }
 private boolean biomePossible(Context c,int x,int z,int minY,int maxY){
  for(int by=minY>>2;by<=maxY>>2;by++){
   var biome=c.biomeSource().getBiome(x>>2,by,z>>2,c.noiseConfig().getMultiNoiseSampler());
   if(c.biomePredicate().test(biome))return true;
  }
  return false;
 }
 @Override protected Optional<StructurePosition> getStructurePosition(Context c){
  int n=size(kind),x=c.chunkPos().getStartX(),z=c.chunkPos().getStartZ(),sea=c.chunkGenerator().getSeaLevel();
  int centerX=x+n/2,centerZ=z+n/2;
  // Conservative vertical biome gate BEFORE ANY terrain query. Enumerate all
  // possible accepted surface quart levels, not just sea level (mountain/cave
  // biome transitions must not reject a site the vanilla final check accepts).
  if(!biomePossible(c,centerX,centerZ,Math.max(sea+3,c.world().getBottomY()+6),c.world().getTopY()))return Optional.empty();
  var terrain=new Terrain(c);int centerY=terrain.height(centerX,centerZ,Heightmap.Type.WORLD_SURFACE_WG);
  if(centerY<=sea+2||centerY-6<c.world().getBottomY())return Optional.empty();
  // Final height can only be centerY..centerY+maxRise(). Vanilla still checks the exact Y.
  if(!biomePossible(c,centerX,centerZ,centerY,centerY+maxRise()))return Optional.empty();
  int min=centerY,max=centerY;
  // Same 5x5 safety probes, but reject immediately instead of paying for all 50
  // noise samples even when the first pair already proves the site unsuitable.
  for(int a=0;a<5;a++)for(int b=0;b<5;b++){
   int px=x+a*(n-1)/4,pz=z+b*(n-1)/4;
   int h=terrain.height(px,pz,Heightmap.Type.WORLD_SURFACE_WG);
   min=Math.min(min,h);max=Math.max(max,h);
   if(min-6<c.world().getBottomY()||!acceptableTerrain(min,max,sea))return Optional.empty();
   if(h!=terrain.height(px,pz,Heightmap.Type.OCEAN_FLOOR_WG))return Optional.empty();
  }
  long seed=c.random().nextLong();int rotation=c.random().nextInt(4);
  var probe=GraveyardPiece.terrainProbe(kind,x,max,z,seed,rotation);
  var columns=new java.util.HashSet<Long>();
  for(var cell:probe.blueprint())if(cell.y()==-1&&!cell.state().isAir()){
   var p=probe.position(cell.x(),0,cell.z());if(!columns.add(ChunkPos.toLong(p.getX(),p.getZ())))continue;
   terrain.refine(p.getX(),p.getZ());
   int h=terrain.height(p.getX(),p.getZ(),Heightmap.Type.WORLD_SURFACE_WG);
   min=Math.min(min,h);max=Math.max(max,h);
   if(!acceptableTerrain(min,max,sea))return Optional.empty();
   if(h!=terrain.height(p.getX(),p.getZ(),Heightmap.Type.OCEAN_FLOOR_WG))return Optional.empty();
  }
  int y=max;
  // Keep the exact dry/solid crypt envelope check; never defer rejection to placement.
  if(kind==2)for(int a=n/2-4;a<=n/2+4;a++)for(int b=n-16;b<=n-3;b++){
   var pos=probe.position(a,0,b);
   var column=terrain.column(pos.getX(),pos.getZ());
   for(int dy=-6;dy<=-2;dy++){var block=column.getState(y+dy);if(!block.getFluidState().isEmpty()||block.isAir())return Optional.empty();}
  }
  return Optional.of(new StructurePosition(new BlockPos(centerX,y,centerZ),collector->collector.addPiece(new GraveyardPiece(kind,x,y,z,seed,rotation))));
 }
}
