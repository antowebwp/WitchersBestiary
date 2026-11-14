package com.witchersbestiary.merchant;
import net.minecraft.server.*;import net.minecraft.server.world.ServerWorld;import net.minecraft.util.math.*;import net.minecraft.world.*;import net.minecraft.block.*;import net.minecraft.registry.tag.BlockTags;import net.minecraft.entity.EntityType;
import com.witchersbestiary.board.VisitedVillageBoards;
/** Bounded loaded-chunk discovery. No locate calls, chunk generation, tickets or world edits. */
public final class MerchantSpawns {
 private static int cursor;
 public static boolean safe(ServerWorld w,BlockPos pos){if(w.getChunkManager().getWorldChunk(pos.getX()>>4,pos.getZ()>>4)==null||w.isOutOfHeightLimit(pos.up(2))||!w.getWorldBorder().contains(new Box(pos).expand(.4,0,.4)))return false;
  for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var at=pos.add(x,0,z);if(w.getChunkManager().getWorldChunk(at.getX()>>4,at.getZ()>>4)==null)return false;var floor=w.getBlockState(at.down());if(!(floor.isIn(BlockTags.DIRT)||floor.isIn(BlockTags.SAND)||floor.isOf(Blocks.STONE)||floor.isOf(Blocks.GRAVEL)||floor.isOf(Blocks.SNOW_BLOCK))||floor.isOf(Blocks.DIRT_PATH)||!floor.isSideSolidFullSquare(w,at.down(),Direction.UP))return false;if(!w.getBlockState(at).isAir()||!w.getBlockState(at.up()).isAir()||!w.getFluidState(at).isEmpty())return false;}
  var box=new Box(pos).expand(2,0,2).stretch(0,2,0);return w.isSpaceEmpty(box)&&w.getOtherEntities(null,box).isEmpty();
 }
 private static boolean safeVillage(ServerWorld world, BlockPos pos) {
  if (!world.getWorldBorder().contains(pos) || world.isOutOfHeightLimit(pos.up(2))) return false;
  var floor = world.getBlockState(pos.down());
  if (!floor.isSideSolidFullSquare(world, pos.down(), Direction.UP)
      || !floor.getFluidState().isEmpty() || floor.isOf(Blocks.MAGMA_BLOCK)
      || floor.isOf(Blocks.CAMPFIRE) || floor.isOf(Blocks.SOUL_CAMPFIRE)) return false;
  for (int y = 0; y < 3; y++) {
   var state = world.getBlockState(pos.up(y));
   if (!state.isAir()) return false;
  }
  var box = new Box(pos.getX()+.15, pos.getY(), pos.getZ()+.15,
          pos.getX()+.85, pos.getY()+2, pos.getZ()+.85);
  return world.isSpaceEmpty(box) && world.getOtherEntities(null, box).isEmpty();
 }

 public static boolean village(ServerWorld w,String key,BlockPos center){var ledger=MerchantLedger.get(w.getServer());if(ledger.readOnly())return false;var claim=ledger.villages.computeIfAbsent(key,k->new MerchantLedger.Claim());if(claim.entity!=null)return false;if(claim.searchVersion<3){claim.attempts=0;claim.next=w.getTime();claim.searchVersion=3;ledger.markDirty();}if(claim.attempts>=12||w.getTime()<claim.next)return false;
  var existing=w.getEntitiesByType(ArcaneMerchants.TYPE,e->e.mode()==ArcaneMerchant.Mode.VILLAGE&&e.village().equals(key));if(!existing.isEmpty()){claim.entity=existing.getFirst().getUuid();ledger.markDirty();return false;}
  claim.attempts++;claim.next=w.getTime()+1200;ledger.markDirty();for(int i=0;i<9;i++){int angle=(claim.attempts*5+i)*37;int x=center.getX()+(int)(Math.cos(Math.toRadians(angle))*(5+i*2+Math.min(36,(claim.attempts-1)*4))),z=center.getZ()+(int)(Math.sin(Math.toRadians(angle))*(5+i*2+Math.min(36,(claim.attempts-1)*4)));if(w.getChunkManager().getWorldChunk(x>>4,z>>4)==null)continue;var pos=new BlockPos(x,w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z),z);if(!safeVillage(w,pos))continue;var npc=ArcaneMerchants.TYPE.create(w);if(npc==null)return false;npc.refreshPositionAndAngles(x+.5,pos.getY(),z+.5,w.random.nextFloat()*360,0);npc.village(key,pos);if(w.spawnEntity(npc)){claim.entity=npc.getUuid();ledger.markDirty();return true;}}if(claim.attempts==12)com.witchersbestiary.WitchersBestiary.LOGGER.info("Merchant placement exhausted for {}: no safe loaded village surface",key);return false;
 }
 private static void discover(MinecraftServer server) {
  var players = server.getPlayerManager().getPlayerList();
  if (players.isEmpty()) return;
  var player = players.get(Math.floorMod(cursor++, players.size()));
  var world = player.getServerWorld();
  if (world.getRegistryKey() != World.OVERWORLD) return;
  var center = player.getChunkPos();
  var seen = new java.util.HashSet<String>();
  for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
   var chunk = world.getChunkManager().getWorldChunk(center.x + dx, center.z + dz);
   if (chunk == null) continue;
   for (var start : chunk.getStructureStarts().values()) visit(world, start, seen);
   for (var reference : chunk.getStructureReferences().entrySet()) {
    if (!VisitedVillageBoards.supported(world, reference.getKey())) continue;
    for (long packed : reference.getValue()) {
     var origin = new ChunkPos(packed);
     var startChunk = world.getChunkManager().getWorldChunk(origin.x, origin.z);
     if (startChunk != null) visit(world, startChunk.getStructureStart(reference.getKey()), seen);
    }
   }
  }
 }

 private static void visit(ServerWorld world, net.minecraft.structure.StructureStart start,
                           java.util.Set<String> seen) {
  if (start == null || !start.hasChildren() || !VisitedVillageBoards.supported(world, start.getStructure())) return;
  var id = world.getRegistryManager().get(net.minecraft.registry.RegistryKeys.STRUCTURE).getId(start.getStructure());
  String key = world.getRegistryKey().getValue() + "/" + id + "/" + start.getPos().toLong();
  if (!seen.add(key)) return;
  var box = start.getChildren().getFirst().getBoundingBox();
  village(world, key, new BlockPos((box.getMinX()+box.getMaxX())/2, 0, (box.getMinZ()+box.getMaxZ())/2));
 }

 private static void reserveWanderer(ServerWorld world, MerchantLedger ledger, ArcaneMerchant merchant) {
  ledger.wanderer=merchant.getUuid();
  ledger.wanderDeparture=merchant.departureAt();
  ledger.nextWander=ledger.wanderDeparture+24000+world.random.nextInt(24001);
  ledger.markDirty();
 }

 static void depart(ServerWorld world, ArcaneMerchant merchant) {
  var ledger=MerchantLedger.get(world.getServer());
  if(!merchant.getUuid().equals(ledger.wanderer))return;
  long interval=Math.clamp(ledger.nextWander-ledger.wanderDeparture,24000,48000);
  ledger.nextWander=world.getTime()+interval;
  ledger.wanderer=null;
  ledger.wanderDeparture=0;
  ledger.markDirty();
 }

 static void wander(MinecraftServer s){
  var w=s.getOverworld();
  var ledger=MerchantLedger.get(s);
  if(ledger.readOnly()||w.getTime()<ledger.nextWander)return;
  var existing=w.getEntitiesByType(ArcaneMerchants.TYPE,e->e.mode()==ArcaneMerchant.Mode.WANDERING);
  if(!existing.isEmpty()){
   var merchant=existing.getFirst();
   if(!merchant.getUuid().equals(ledger.wanderer))reserveWanderer(w,ledger,merchant);
   return;
  }
  if(ledger.wanderer!=null&&w.getTime()<ledger.wanderDeparture)return;
  var players=w.getPlayers(p->p.isAlive()&&!p.isSpectator());
  if(players.isEmpty())return;
  ledger.nextWander=w.getTime()+1200;
  ledger.markDirty();
  var p=players.get(w.random.nextInt(players.size()));
  for(int i=0;i<12;i++){double angle=w.random.nextDouble()*Math.PI*2;int range=40+w.random.nextInt(25),x=p.getBlockX()+(int)(Math.cos(angle)*range),z=p.getBlockZ()+(int)(Math.sin(angle)*range);if(w.getChunkManager().getWorldChunk(x>>4,z>>4)==null)continue;var pos=new BlockPos(x,w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z),z);if(!safe(w,pos))continue;var npc=ArcaneMerchants.TYPE.create(w);if(npc==null)return;npc.refreshPositionAndAngles(x+.5,pos.getY(),z+.5,w.random.nextFloat()*360,0);npc.arrive();if(w.spawnEntity(npc)){reserveWanderer(w,ledger,npc);return;}}
 }
 public static void initialize(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTicks()%200==0)discover(s);if(s.getTicks()%400==0)wander(s);});}
}
