package com.witchersbestiary.test;
import com.witchersbestiary.board.VillageOffers;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.nbt.NbtCompound;
import java.util.*;
public final class VillageOfferTests implements FabricGameTest {
 @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,batchId="stage20h")
 public void rotationPersistenceAndIsolation(TestContext c){
  var state=new VillageOffers();var pool=List.of("a","b","c","d","e","f");
  var a=state.current("A",0,pool);var b=state.current("B",0,pool);
  c.assertTrue(a.offers().size()==5&&!a.id().equals(b.id()),"Five independent offers");
  c.assertTrue(a.equals(state.current("A",71999,pool)),"Stable before deadline");
  var saved=VillageOffers.read(state.writeNbt(new NbtCompound(),c.getWorld().getRegistryManager()),c.getWorld().getRegistryManager());
  c.assertTrue(a.equals(saved.current("A",100,pool)),"Generation survives NBT");
  c.assertTrue(!a.id().equals(saved.current("A",72000,pool).id()),"Rotate at exactly three days");
  c.assertTrue(b.equals(saved.current("B",10,pool)),"Other village unchanged");c.complete();
 }
 @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,batchId="stage20h")
 public void personalRefreshCooldown(TestContext c){
  var state=new VillageOffers();var pool=List.of("a","b","c","d","e","f");var p=UUID.randomUUID();var other=UUID.randomUUID();
  var base=state.personal("A",p,0,pool);boolean denied=false;
  try{state.refresh("A",p,0,pool,o->true);}catch(IllegalStateException e){denied=true;}
  c.assertTrue(denied,"Immediate refresh denied");
  var personal=state.refresh("A",p,24000,pool,o->true);
  c.assertTrue(!base.id().equals(personal.id())&&base.equals(state.personal("A",other,24000,pool)),"Personal refresh leaves peer unchanged");c.complete();
 }
 @GameTest(templateName="witchersbestiary:notice_board_arena",batchId="stage20h-integration",tickLimit=40)
 public void boardAcceptanceOwnershipAndReplay(TestContext c){
  var w=c.getWorld();var root=c.getAbsolutePos(new net.minecraft.util.math.BlockPos(7,2,7));
  for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)w.setBlockState(root.add(x,-1,z),net.minecraft.block.Blocks.GRASS_BLOCK.getDefaultState());
  c.assertTrue(com.witchersbestiary.board.NoticeBoardBlock.place(w,root,net.minecraft.util.math.Direction.NORTH),"Board placed");
  var p=c.createMockCreativeServerPlayerInWorld();var q=c.createMockCreativeServerPlayerInWorld();
  p.teleport(w,root.getX(),root.getY(),root.getZ()-2,0,0);q.teleport(w,root.getX(),root.getY(),root.getZ()-2,0,0);
  var h=new com.witchersbestiary.board.BoardScreenHandler(701,p.getInventory(),root);var peer=new com.witchersbestiary.board.BoardScreenHandler(702,q.getInventory(),root);p.currentScreenHandler=h;q.currentScreenHandler=peer;
  c.assertTrue(h.view(p,"").getAsJsonArray("offers").equals(peer.view(q,"").getAsJsonArray("offers")),"Shared base snapshot");
  h.onButtonClick(p,65);var special=h.view(p,"").getAsJsonArray("offers");c.assertTrue(special.size()==2&&special.get(0).getAsJsonObject().get("rarity").getAsString().equals("LEGENDARY"),"Separate legendary category");h.onButtonClick(p,65);c.assertTrue(h.view(p,"").getAsJsonArray("offers").size()==5,"Five regular slots retained");
  h.onButtonClick(p,0);var state=com.witchersbestiary.contracts.ContractState.get(w.getServer());var accepted=state.ownedBy(p.getUuid()).getFirst();
  c.assertTrue(accepted.offer!=null&&accepted.village!=null&&accepted.generation!=null,"Persisted source identity");
  c.assertTrue(peer.view(q,"").getAsJsonArray("offers").get(0).getAsJsonObject().get("state").getAsString().equals("AVAILABLE"),"Peer remains available");
  h.onButtonClick(p,0);c.assertTrue(state.ownedBy(p.getUuid()).size()==1,"Repeated accept is idempotent");
  var copy=com.witchersbestiary.contracts.ContractInstance.fromNbt(accepted.toNbt());c.assertTrue(accepted.offer.equals(copy.offer)&&accepted.id().equals(copy.id()),"Instance source roundtrip");
  p.currentScreenHandler=p.playerScreenHandler;q.currentScreenHandler=q.playerScreenHandler;p.discard();q.discard();c.complete();
 }
 @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,batchId="stage20h-final")
 public void hundredCyclesAndBoundedTerminalHistory(TestContext c){
  var offers=new VillageOffers();var pool=List.of("a","b","c","d","e","f");UUID previous=null;
  for(int i=0;i<100;i++){var g=offers.current("A",i*72000L,pool);c.assertTrue(!g.id().equals(previous),"Fresh cycle");previous=g.id();}
  var n=offers.writeNbt(new NbtCompound(),c.getWorld().getRegistryManager());c.assertTrue(n.getList("Villages",10).size()==1,"Only current generation stored");
  var state=new com.witchersbestiary.contracts.ContractState();var owner=UUID.randomUUID();
  var definition=com.witchersbestiary.contracts.Contracts.DEFINITIONS.all().values().stream().filter(d->!com.witchersbestiary.contracts.hunt.SwampLord.named(new com.witchersbestiary.contracts.ContractInstance(UUID.randomUUID(),owner,d,0))).findFirst().orElseThrow();
  UUID old=null;for(int i=0;i<500;i++){var contract=new com.witchersbestiary.contracts.ContractInstance(UUID.randomUUID(),owner,definition,0);if(i==0)old=contract.id();contract.transition(com.witchersbestiary.contracts.ContractInstance.Status.ACCEPTED);contract.transition(com.witchersbestiary.contracts.ContractInstance.Status.CANCELLED);state.add(contract);}
  state.compact(id->false);c.assertTrue(state.all().size()==128&&state.find(old)==null,"Bounded terminal detail; old UUID unavailable for rewards");
  var restored=com.witchersbestiary.contracts.ContractState.read(state.writeNbt(new NbtCompound(),c.getWorld().getRegistryManager()),c.getWorld().getRegistryManager());c.assertTrue(restored.all().size()==128,"Bound survives reload");c.complete();
 }
 @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,batchId="stage20h-final")
 public void unloadedOriginUsesReferenceIdentity(TestContext c){
  var w=c.getWorld();var pos=c.getAbsolutePos(new net.minecraft.util.math.BlockPos(1,1,1));var chunk=w.getChunk(pos);var type=w.getRegistryManager().get(net.minecraft.registry.RegistryKeys.STRUCTURE).get(net.minecraft.util.Identifier.of("minecraft:village_plains"));
  long packed=new net.minecraft.util.math.ChunkPos(1000000,1000000).toLong();chunk.addStructureReference(type,packed);
  String id=VillageOffers.identity(w,pos);c.assertTrue(id.equals(w.getRegistryKey().getValue()+"/minecraft:village_plains/"+packed),"Reference origin usable without loading start");
  c.assertTrue(w.getChunkManager().getWorldChunk(1000000,1000000)==null,"No forced origin load");chunk.getStructureReferences().get(type).remove(packed);c.complete();
 }
}
