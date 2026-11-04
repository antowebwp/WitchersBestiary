package com.witchersbestiary.test;

import com.witchersbestiary.contracts.*;
import com.witchersbestiary.contracts.hunt.*;
import com.witchersbestiary.contracts.procedural.*;
import com.witchersbestiary.contracts.tracking.*;
import com.witchersbestiary.economy.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;

public final class ContractHotfixTests implements FabricGameTest {
 private ContractInstance waiting(ServerPlayerEntity p){
  var profile=ProceduralProfile.generate(215L,"katakan");
  var definition=ProceduralHunts.snapshot(Contracts.DEFINITIONS.find(ProceduralHunts.id("katakan")),profile);
  var hunt=new ContractInstance(UUID.randomUUID(),p.getUuid(),CrownRewards.fresh(definition),System.currentTimeMillis());
  hunt.procedural=profile;hunt.transition(ContractInstance.Status.ACCEPTED);
  hunt.hunt=new SwampHunt(p.getServerWorld().getRegistryKey().getValue().toString(),p.getBlockPos());hunt.hunt.status=SwampHunt.Status.WAITING;
  hunt.investigation=new ContractInvestigation(hunt.id(),p.getUuid(),hunt.hunt.dimension);return hunt;
 }
 @GameTest(templateName="witchersbestiary:village_board_arena",batchId="contract-hotfix",tickLimit=40)
 public void savedEmptyKatakanCanBeAbandonedByNonOperatorOnlyForSelf(TestContext c){
  var p=c.createMockCreativeServerPlayerInWorld();var other=c.createMockCreativeServerPlayerInWorld();
  p.getInventory().clear();p.getInventory().setStack(0,new ItemStack(Currency.WALLET));p.getInventory().getStack(0).set(WalletComponents.BALANCE,40L);
  var store=ContractState.get(p.getServer());var stuck=ContractInstance.fromNbt(waiting(p).toNbt());var foreign=waiting(other);var history=waiting(p);
  history.credit(UUID.randomUUID(),p.getUuid(),history.definition().target(),System.currentTimeMillis());history.transition(ContractInstance.Status.REWARDED);
  store.add(stuck);store.add(foreign);store.add(history);var otherBefore=foreign.toNbt();var historyBefore=history.toNbt();int xp=p.totalExperience;
  c.assertTrue(stuck.investigation.clues.isEmpty()&&stuck.investigation.lair==null&&stuck.hunt.position==null,"Reproduce saved SEARCHING0/0, no route or coordinates");
  Investigations.accepted(p,stuck);c.assertTrue(Investigations.view(p).get("total").getAsInt()==0,"Legacy pending investigation remains readable");
  p.getServer().getCommandManager().executeWithPrefix(p.getCommandSource().withLevel(0),"wbcontract abandon");
  c.assertTrue(stuck.status()==ContractInstance.Status.CANCELLED&&stuck.hunt.status==SwampHunt.Status.CANCELLED&&stuck.investigation.stage==ContractInvestigation.Stage.CANCELLED,"Non-operator command terminates only own active hunt");
  c.assertTrue(!Investigations.view(p).has("contract"),"Cancelled HUD invalidated immediately");
  c.assertTrue(foreign.toNbt().equals(otherBefore)&&history.toNbt().equals(historyBefore),"Foreign player and own terminal history unchanged");
  c.assertTrue(WalletTransactions.balance(p.getInventory().getStack(0))==40&&p.totalExperience==xp&&stuck.progress()==0,"No crowns XP or kill credit");
  boolean denied=false;try{Contracts.claim(p,stuck.id());}catch(IllegalStateException expected){denied=true;}c.assertTrue(denied,"Cancelled contract cannot pay");
  p.getServer().getCommandManager().executeWithPrefix(p.getCommandSource().withLevel(0),"wbcontract abandon");
  c.assertTrue(foreign.toNbt().equals(otherBefore),"Repeated command cannot touch another player");
  var reloaded=ContractState.read(store.writeNbt(new NbtCompound(),c.getWorld().getRegistryManager()),c.getWorld().getRegistryManager());
  c.assertTrue(reloaded.find(stuck.id()).status()==ContractInstance.Status.CANCELLED&&reloaded.find(foreign.id()).toNbt().equals(otherBefore),"Cancel and other ownership survive save/reload");
  p.discard();other.discard();c.complete();
 }
 @GameTest(templateName="witchersbestiary:village_board_arena",batchId="contract-hotfix",tickLimit=40)
 public void unsafeBoardAcceptanceDoesNotCreateAnActiveContractOrLoadTerrain(TestContext c){
  var p=c.createMockCreativeServerPlayerInWorld();p.refreshPositionAndAngles(20000000,100,20000000,0,0);
  var w=c.getWorld();c.assertTrue(w.getChunkManager().getWorldChunk(20000000>>4,20000000>>4)==null,"Region starts unloaded");
  var store=ContractState.get(p.getServer());int before=store.ownedBy(p.getUuid()).size();boolean rejected=false;
  try{Contracts.assignPrepared(p,ProceduralHunts.id("katakan"),215L);}catch(IllegalStateException expected){rejected=true;}
  c.assertTrue(rejected&&store.ownedBy(p.getUuid()).size()==before,"No feasible route means no committed acceptance or receipt");
  c.assertTrue(w.getChunkManager().getWorldChunk(20000000>>4,20000000>>4)==null,"Preflight cannot force chunks");
  c.assertTrue(!Investigations.view(p).has("contract"),"Rejected provisional route cannot leak into HUD selection");
  p.discard();c.complete();
 }
 @GameTest(templateName="witchersbestiary:village_board_arena",batchId="contract-hotfix",tickLimit=60)
 public void loadedForestFallbackCommitsTwoCluesAndSavedLairBeforeAcceptance(TestContext c){
  var w=c.getWorld();for(int x=0;x<48;x++)for(int z=0;z<48;z++){c.setBlockState(new BlockPos(x,1,z),Blocks.GRASS_BLOCK);for(int y=2;y<15;y++)c.setBlockState(new BlockPos(x,y,z),Blocks.AIR);}
  for(int x=12;x<42;x++)for(int z=18;z<30;z++)c.setBlockState(new BlockPos(x,10,z),Blocks.OAK_LEAVES);
  var at=c.getAbsolutePos(new BlockPos(30,2,24));var forest=w.getRegistryManager().get(RegistryKeys.BIOME).getEntry(net.minecraft.world.biome.BiomeKeys.FOREST).orElseThrow();
  for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var chunk=w.getChunkManager().getWorldChunk((at.getX()>>4)+x,(at.getZ()>>4)+z);if(chunk!=null)chunk.populateBiomes((a,b,d,s)->forest,w.getChunkManager().getNoiseConfig().getMultiNoiseSampler());}
  var p=c.createMockCreativeServerPlayerInWorld();var origin=c.getAbsolutePos(new BlockPos(-18,2,24));p.refreshPositionAndAngles(origin.getX(),origin.getY(),origin.getZ(),0,0);
  var difficulty=w.getDifficulty();boolean peacefulDenied=false;int ownedBefore=ContractState.get(p.getServer()).ownedBy(p.getUuid()).size();p.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,false);try{Contracts.assignPrepared(p,ProceduralHunts.id("katakan"),215L);}catch(IllegalStateException expected){peacefulDenied=true;}finally{p.getServer().setDifficulty(difficulty,false);}c.assertTrue(peacefulDenied&&ContractState.get(p.getServer()).ownedBy(p.getUuid()).size()==ownedBefore,"Peaceful cannot consume an offer for an impossible hostile target");
  var hunt=Contracts.assignPrepared(p,ProceduralHunts.id("katakan"),215L);var i=hunt.investigation;
  c.assertTrue(i.stage==ContractInvestigation.Stage.TRACKING&&i.clues.size()==2&&i.lair!=null&&hunt.hunt.position!=null&&hunt.hunt.target==null,"Accepted hunt already has complete route, not0/0");
  c.assertTrue(w.getBlockState(i.lair.down()).isIn(net.minecraft.registry.tag.BlockTags.DIRT)&&i.clues.stream().allMatch(clue->w.getBlockState(clue.position.down()).isIn(net.minecraft.registry.tag.BlockTags.DIRT)),"Lair and clues stay on ground beneath forest canopy");
  c.assertTrue(Investigations.view(p).has("area")&&Investigations.view(p).get("total").getAsInt()==2,"Immediate HUD area and0/2");
  double gap=i.clues.getFirst().position.toCenterPos().subtract(i.clues.getLast().position.toCenterPos()).horizontalLength();c.assertTrue(gap>=40&&gap<=60&&i.routeVersion==3,"New route spacing40..60");
  var before=Investigations.view(p);var area=before.getAsJsonObject("area");c.assertTrue(Math.abs(area.get("x").getAsInt()-i.next().position.getX())<=8&&Math.abs(area.get("z").getAsInt()-i.next().position.getZ())<=8,"Approximate coordinates stay in correct cell including negative coordinates");
  var stored=ContractState.get(p.getServer());var disk=ContractState.read(stored.writeNbt(new NbtCompound(),w.getRegistryManager()),w.getRegistryManager());c.assertTrue(hunt.id().equals(disk.tracked(p.getUuid())),"Tracked instance survives save/reload");
  Investigations.disconnected(p.getUuid());p.refreshPositionAndAngles(at.getX()+400,at.getY(),at.getZ()+400,0,0);c.assertTrue(Investigations.view(p).getAsJsonObject("area").equals(area),"Rejoin cache reset and distance do not remove area");
  p.setServerWorld(p.getServer().getWorld(net.minecraft.world.World.NETHER));var different=Investigations.view(p);c.assertTrue(different.get("hint").getAsString().equals("dimension")&&!different.has("area")&&different.get("target_dimension").getAsString().equals(i.dimension),"Cross-dimension HUD retains contract but suppresses false coordinates");p.setServerWorld(w);
  var saved=ContractInstance.fromNbt(hunt.toNbt());c.assertTrue(saved.procedural.equals(hunt.procedural)&&saved.investigation.lair.equals(i.lair)&&saved.investigation.next().id.equals(i.next().id),"Route and immutable roll survive existing schema");
  for(var clue:i.clues){p.refreshPositionAndAngles(clue.position.getX()+.5,clue.position.getY(),clue.position.getZ()+.5,0,0);c.assertTrue(Investigations.inspect(p,clue.id),"Both generated clues can be studied");c.assertTrue(Investigations.view(p).get("objective").getAsString().equals(i.current==1?"second":"lair_found"),"HUD immediately advances sequential objective");}
  p.refreshPositionAndAngles(i.lair.getX()+.5,i.lair.getY(),i.lair.getZ()-2.2,0,0);c.assertTrue(Investigations.trySpawn(p,hunt),"Prepared Katakan can actually spawn after investigation");
  var mob=(net.minecraft.entity.mob.MobEntity)w.getEntity(hunt.hunt.target);Contracts.abandonActive(p);ProceduralHunts.tick(mob);c.assertTrue(mob.isRemoved()&&hunt.progress()==0,"Abandon also cleans bound live target without credit");
  p.discard();c.complete();
 }
 @GameTest(templateName="witchersbestiary:village_board_arena",batchId="contract-hotfix-gui",tickLimit=40)
 public void boardCancellationUsesInstanceOwnershipAndPreservesVillageOffers(TestContext c){
  var w=c.getWorld();var root=c.getAbsolutePos(new BlockPos(12,2,12));for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)w.setBlockState(root.add(x,-1,z),Blocks.GRASS_BLOCK.getDefaultState());
  c.assertTrue(com.witchersbestiary.board.NoticeBoardBlock.place(w,root,net.minecraft.util.math.Direction.NORTH),"Board placement");
  var p=c.createMockCreativeServerPlayerInWorld();var q=c.createMockCreativeServerPlayerInWorld();p.refreshPositionAndAngles(root.getX(),root.getY(),root.getZ()-2,0,0);q.refreshPositionAndAngles(root.getX(),root.getY(),root.getZ()-2,0,0);
  var store=ContractState.get(p.getServer());var own=waiting(p);var peer=waiting(q);store.add(own);store.add(peer);var peerBefore=peer.toNbt();
  var h=new com.witchersbestiary.board.BoardScreenHandler(711,p.getInventory(),root);var other=new com.witchersbestiary.board.BoardScreenHandler(712,q.getInventory(),root);p.currentScreenHandler=h;q.currentScreenHandler=other;
  var peerOffers=other.view(q,"").getAsJsonArray("offers").deepCopy();
  c.assertTrue(!h.cancel(q,own.id())&&!h.cancel(p,peer.id()),"Foreign source and foreign instance rejected");
  c.assertTrue(h.cancel(p,own.id())&&!h.cancel(p,own.id()),"Selected own archived broken instance cancelled once");
  c.assertTrue(peer.toNbt().equals(peerBefore)&&other.view(q,"").getAsJsonArray("offers").equals(peerOffers),"Peer contract and board generation unchanged");
  own=ContractInstance.fromNbt(own.toNbt());c.assertTrue(own.status()==ContractInstance.Status.CANCELLED,"Cancellation persisted");
  var completed=waiting(p);completed.credit(UUID.randomUUID(),p.getUuid(),completed.definition().target(),System.currentTimeMillis());store.add(completed);
  var reopened=new com.witchersbestiary.board.BoardScreenHandler(713,p.getInventory(),root);p.currentScreenHandler=reopened;c.assertTrue(!reopened.cancel(p,completed.id())&&completed.status()==ContractInstance.Status.COMPLETED,"Unclaimed completed contract cannot be abandoned");
  p.currentScreenHandler=p.playerScreenHandler;q.currentScreenHandler=q.playerScreenHandler;p.discard();q.discard();c.complete();
 }
 @GameTest(templateName="witchersbestiary:village_board_arena",batchId="contract-hotfix-recovery",tickLimit=40)
 public void savedLairCanRecoverWithoutRerollOrRespawningExistingTarget(TestContext c){
  var w=c.getWorld();for(int x=0;x<48;x++)for(int z=0;z<48;z++){c.setBlockState(new BlockPos(x,1,z),Blocks.GRASS_BLOCK);for(int y=2;y<15;y++)c.setBlockState(new BlockPos(x,y,z),Blocks.AIR);}
  var at=c.getAbsolutePos(new BlockPos(30,2,24));var forest=w.getRegistryManager().get(RegistryKeys.BIOME).getEntry(net.minecraft.world.biome.BiomeKeys.FOREST).orElseThrow();for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var chunk=w.getChunkManager().getWorldChunk((at.getX()>>4)+x,(at.getZ()>>4)+z);if(chunk!=null)chunk.populateBiomes((a,b,d,s)->forest,w.getChunkManager().getNoiseConfig().getMultiNoiseSampler());}
  var p=c.createMockCreativeServerPlayerInWorld();p.refreshPositionAndAngles(at.getX()-18,at.getY(),at.getZ(),0,0);var broken=waiting(p);broken.hunt.position=at;broken=ContractInstance.fromNbt(broken.toNbt());var roll=broken.procedural;
  c.assertTrue(SwampLord.recoverSavedRoute(p,broken)&&broken.investigation.clues.size()==2&&broken.procedural.equals(roll)&&broken.progress()==0,"Saved coordinates recover missing clues without reward or profile reroll");
  broken.hunt.target=UUID.randomUUID();broken.investigation.stage=ContractInvestigation.Stage.SEARCHING;c.assertTrue(!SwampLord.recoverSavedRoute(p,broken),"Reserved target UUID forbids reconstruction or duplicate spawn");p.discard();c.complete();
 }

}
