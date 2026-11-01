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
  h.onButtonClick(p,0);var state=com.witchersbestiary.contracts.ContractState.get(w.getServer());var accepted=state.ownedBy(p.getUuid()).getFirst();
  c.assertTrue(accepted.offer!=null&&accepted.village!=null&&accepted.generation!=null,"Persisted source identity");
  c.assertTrue(peer.view(q,"").getAsJsonArray("offers").get(0).getAsJsonObject().get("state").getAsString().equals("AVAILABLE"),"Peer remains available");
  h.onButtonClick(p,0);c.assertTrue(state.ownedBy(p.getUuid()).size()==1,"Repeated accept is idempotent");
  var copy=com.witchersbestiary.contracts.ContractInstance.fromNbt(accepted.toNbt());c.assertTrue(accepted.offer.equals(copy.offer)&&accepted.id().equals(copy.id()),"Instance source roundtrip");
  p.currentScreenHandler=p.playerScreenHandler;q.currentScreenHandler=q.playerScreenHandler;p.discard();q.discard();c.complete();
 }
}
