package com.witchersbestiary.lure;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import java.util.*;

/** Bounded loaded-chunk search, only at placement/expiry; does not force world generation. */
public final class LurePlacement {
 private LurePlacement(){}
 public static boolean loaded(ServerWorld w,Box box){for(int x=MathHelper.floor(box.minX)>>4;x<=MathHelper.floor(box.maxX)>>4;x++)for(int z=MathHelper.floor(box.minZ)>>4;z<=MathHelper.floor(box.maxZ)>>4;z++)if(!w.isChunkLoaded(new BlockPos(x<<4,MathHelper.floor(box.minY),z<<4)))return false;return true;}
 public static boolean support(ServerWorld w,BlockPos p){var b=w.getBlockState(p.down());return b.isSolidBlock(w,p.down())&&!b.isIn(BlockTags.LEAVES)&&!b.isIn(BlockTags.LOGS)&&!b.isOf(Blocks.MAGMA_BLOCK)&&!b.isOf(Blocks.CAMPFIRE)&&!b.isOf(Blocks.CACTUS);}
 public static boolean safe(ServerWorld w,BlockPos p,LureKind kind){
  double width=kind.type().getWidth(),height=kind.type().getHeight();var box=new Box(p.getX()+.5-width/2,p.getY(),p.getZ()+.5-width/2,p.getX()+.5+width/2,p.getY()+height,p.getZ()+.5+width/2);
  if(kind==LureKind.LESHEN)box=box.expand(.9,0,.9).stretch(0,.5,0);
  if(kind==LureKind.FIEND)box=box.expand(.55,0,.55);
  if(kind==LureKind.GRIFFIN)box=box.expand(3.1,0,3.1).stretch(0,5,0);
  if(!w.getWorldBorder().contains(box)||!loaded(w,box)||!w.isSpaceEmpty(box)||w.containsFluid(box)||!support(w,p))return false;
  for(BlockPos q:BlockPos.iterate(p.add(-1,-1,-1),p.add(1,(int)Math.ceil(height),1))){var s=w.getBlockState(q);if(s.isIn(BlockTags.LOGS)||s.isIn(BlockTags.LEAVES)||s.isOf(Blocks.LAVA)||s.isOf(Blocks.FIRE)||s.isOf(Blocks.SOUL_FIRE)||s.isOf(Blocks.POWDER_SNOW))return false;}
  if(kind!=LureKind.LESHEN)for(BlockPos q:BlockPos.iterate(p.add(-1,-1,-1),p.add(1,-1,1)))if(!w.getBlockState(q).isSolidBlock(w,q))return false;
  if(!w.getOtherEntities(null,box.expand(12),e->e instanceof net.minecraft.entity.mob.MobEntity&&e.isAlive()).isEmpty())return false;
  if(!w.getEntitiesByClass(net.minecraft.entity.mob.PathAwareEntity.class,new Box(p).expand(kind==LureKind.FIEND?128:96),m->m.isAlive()&&m.getType()==kind.type()).isEmpty())return false;
  return true;
 }
 public static BlockPos find(ServerWorld w,BlockPos lure,LureKind kind){
  for(int ring:new int[]{40,32,48})for(int i=0;i<24;i++){
   double angle=i*Math.PI/12;int x=lure.getX()+(int)Math.round(Math.cos(angle)*ring),z=lure.getZ()+(int)Math.round(Math.sin(angle)*ring);var column=new BlockPos(x,lure.getY(),z);if(!w.isChunkLoaded(column))continue;
   int y=w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z);var p=new BlockPos(x,y,z);if(Math.abs(y-lure.getY())>8||!safe(w,p,kind))continue;
   boolean clear=true;Vec3d at=Vec3d.ofBottomCenter(p);
   for(var player:w.getPlayers()){if(player.isSpectator())continue;double distance=player.getPos().squaredDistanceTo(at);if(distance<24*24){clear=false;break;}if(distance<64*64&&player.getRotationVec(1).dotProduct(at.add(0,1,0).subtract(player.getEyePos()).normalize())>.45){clear=false;break;}}
   if(clear)return p;
  }return null;
 }
 public static String error(ServerWorld w,BlockPos pos,LureKind kind,PlayerEntity player){
  var habitat=kind.habitatError(w,pos);if(habitat!=null)return habitat;
  if(!support(w,pos)||!w.getBlockState(pos.up()).isAir()||!w.getFluidState(pos).isEmpty())return "ground";
  var reserved=LureState.get(w).reservationError(player.getUuid(),w.getRegistryKey().getValue().toString(),pos,LureState.now(w));if(reserved!=null)return reserved;
  if(!w.getEntitiesByClass(net.minecraft.entity.mob.PathAwareEntity.class,new Box(pos).expand(96),m->m.getType()==kind.type()&&m.isAlive()&&!m.isAiDisabled()).isEmpty())return null;
  return find(w,pos,kind)==null?"space":null;
 }
}
