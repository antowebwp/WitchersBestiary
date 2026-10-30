package com.witchersbestiary.test;
import com.witchersbestiary.board.*;
import com.witchersbestiary.contracts.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.registry.*;
import net.minecraft.test.*;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import java.util.*;

public final class NoticeBoardTests implements FabricGameTest {
 private static final String ARENA="witchersbestiary:notice_board_arena";
 private BlockPos floor(TestContext c){for(int x=0;x<16;x++)for(int z=0;z<16;z++)c.setBlockState(new BlockPos(x,1,z),Blocks.GRASS_BLOCK);return c.getAbsolutePos(new BlockPos(7,2,7));}
 private net.minecraft.server.network.ServerPlayerEntity player(TestContext c,BlockPos root){var p=c.createMockCreativeServerPlayerInWorld();p.teleport(c.getWorld(),root.getX()+.5,root.getY(),root.getZ()-2,0,0);return p;}
 @GameTest(templateName=ARENA,batchId="stage20b",tickLimit=20)
 public void registrationPlacementRotationAndPersistence(TestContext c){var w=c.getWorld();var root=floor(c);c.assertTrue(Registries.BLOCK.get(NoticeBoards.ID)==NoticeBoards.BLOCK&&Registries.ITEM.get(NoticeBoards.ID)==NoticeBoards.ITEM,"Registry");
  for(var direction:Direction.Type.HORIZONTAL){c.assertTrue(NoticeBoardBlock.place(w,root,direction),"Placement each direction");
   for(int part=0;part<9;part++){var pos=NoticeBoardBlock.cell(root,direction,part);var state=w.getBlockState(pos);c.assertTrue(NoticeBoardBlock.root(pos,state).equals(root),"Root lookup all parts");var copy=NbtHelper.toBlockState(w.createCommandRegistryWrapper(RegistryKeys.BLOCK),NbtHelper.fromBlockState(state));c.assertTrue(copy.equals(state),"Facing/part NBT persists");c.assertTrue(!state.getOutlineShape(w,pos).isEmpty(),"Visible outline per cell");}
   var state=w.getBlockState(root);c.assertTrue(state.rotate(BlockRotation.CLOCKWISE_90).get(NoticeBoardBlock.FACING)==direction.rotateYClockwise(),"Rotation mapping");w.breakBlock(root,false);c.assertTrue(!w.getBlockState(root.up()).isOf(NoticeBoards.BLOCK),"Whole cleanup");
  }c.complete();}
 @GameTest(templateName=ARENA,batchId="stage20b",tickLimit=20)
 public void actualBlockItemPlacementAndSupportRemoval(TestContext c){
  var w=c.getWorld();var root=floor(c);var p=player(c,root);p.setStackInHand(Hand.MAIN_HAND,new ItemStack(NoticeBoards.ITEM));
  var hit=new BlockHitResult(root.down().toCenterPos().add(0,.5,0),Direction.UP,root.down(),false);
  var result=NoticeBoards.ITEM.useOnBlock(new ItemUsageContext(p,Hand.MAIN_HAND,hit));
  c.assertTrue(result.isAccepted()&&NoticeBoardBlock.complete(w,root,Direction.NORTH),"Actual BlockItem facing/installation");
  w.breakBlock(NoticeBoardBlock.cell(root,Direction.NORTH,0).down(),false);
  c.runAtTick(3,()->{c.assertTrue(!w.getBlockState(root).isOf(NoticeBoards.BLOCK),"Lost support removes whole board");p.discard();c.complete();});
 }
 @GameTest(templateName=ARENA,batchId="stage20b",tickLimit=20)
 public void blockageAndSingleDrop(TestContext c){var w=c.getWorld();var root=floor(c);w.setBlockState(root.up(2),Blocks.CHEST.getDefaultState());c.assertTrue(!NoticeBoardBlock.place(w,root,Direction.NORTH)&&w.getBlockState(root.up(2)).isOf(Blocks.CHEST),"Important blocks never overwritten");w.setBlockState(root.up(2),Blocks.AIR.getDefaultState());
  c.assertTrue(NoticeBoardBlock.place(w,root,Direction.NORTH),"Place");w.breakBlock(root.up(),true);var drops=w.getEntitiesByClass(ItemEntity.class,new Box(root).expand(4),e->e.getStack().isOf(NoticeBoards.ITEM));c.assertTrue(drops.stream().mapToInt(e->e.getStack().getCount()).sum()==1,"Exactly one board item");for(int i=0;i<9;i++)c.assertTrue(!w.getBlockState(NoticeBoardBlock.cell(root,Direction.NORTH,i)).isOf(NoticeBoards.BLOCK),"No invisible remnants");c.complete();}
 @GameTest(templateName=ARENA,batchId="stage20b",tickLimit=40)
 public void interactionDistanceAcceptanceAndClosedSession(TestContext c){var w=c.getWorld();var root=floor(c);NoticeBoardBlock.place(w,root,Direction.NORTH);var p=player(c,root);var state=w.getBlockState(root);
  state.onUse(w,p,new BlockHitResult(root.toCenterPos(),Direction.NORTH,root,false));c.assertTrue(p.currentScreenHandler instanceof BoardScreenHandler,"Right click opens real screen handler");var h=(BoardScreenHandler)p.currentScreenHandler;
  c.assertTrue(h.canUse(p)&&h.view(p,"").getAsJsonArray("offers").size()==17,"All registered offers available");
  int before=ContractState.get(p.getServer()).ownedBy(p.getUuid()).size();c.assertTrue(h.onButtonClick(p,0),"Accept request");int after=ContractState.get(p.getServer()).ownedBy(p.getUuid()).size();c.assertTrue(after==before+1,"Exactly one accepted instance");h.onButtonClick(p,0);c.assertTrue(ContractState.get(p.getServer()).ownedBy(p.getUuid()).size()==after,"Duplicate rejection");
  var reopened=new BoardScreenHandler(79,p.getInventory(),root);p.currentScreenHandler=reopened;reopened.onButtonClick(p,0);c.assertTrue(ContractState.get(p.getServer()).ownedBy(p.getUuid()).size()==after,"Duplicate acceptance rejected after reopening, independent of action throttle");p.currentScreenHandler=h;
  var second=player(c,root);var foreign=new BoardScreenHandler(99,second.getInventory(),root);c.assertTrue(!h.onButtonClick(second,1),"Other owner denied");
  p.teleport(w,root.getX()+30,root.getY(),root.getZ(),0,0);c.assertTrue(!h.canUse(p)&&!h.onButtonClick(p,1),"Distance denied");p.teleport(w,root.getX(),root.getY(),root.getZ()-2,0,0);p.closeHandledScreen();c.assertTrue(!h.onButtonClick(p,1),"Closed session denied");
  p.currentScreenHandler=foreign;c.assertTrue(!foreign.canUse(p),"Foreign handler owner denied");p.currentScreenHandler=p.playerScreenHandler;second.discard();p.discard();c.complete();}
 @GameTest(templateName=ARENA,batchId="stage20b",tickLimit=20)
 public void boardBreakInvalidatesSessionAndOpeningDoesNotCreateContracts(TestContext c){var w=c.getWorld();var root=floor(c);NoticeBoardBlock.place(w,root,Direction.EAST);var p=player(c,root);var store=ContractState.get(p.getServer());int before=store.ownedBy(p.getUuid()).size();
  for(int n=0;n<5;n++){var h=new BoardScreenHandler(90+n,p.getInventory(),root);p.currentScreenHandler=h;h.view(p,"");c.assertTrue(store.ownedBy(p.getUuid()).size()==before,"Opening never assigns or rewards");}
  var h=(BoardScreenHandler)p.currentScreenHandler;w.breakBlock(root.up(),false);c.assertTrue(!h.canUse(p)&&!h.onButtonClick(p,0),"Destroyed board invalidates acceptance");p.currentScreenHandler=p.playerScreenHandler;p.discard();c.complete();}
 @GameTest(templateName=ARENA,batchId="stage20b",tickLimit=20)
 public void acceptedProgressUses20AStoreAndOwnerIsolation(TestContext c){var w=c.getWorld();var root=floor(c);NoticeBoardBlock.place(w,root,Direction.NORTH);var p=player(c,root);var q=player(c,root);var h=new BoardScreenHandler(80,p.getInventory(),root);p.currentScreenHandler=h;h.onButtonClick(p,0);var id=Identifier.of(h.view(p,"").getAsJsonArray("offers").get(0).getAsJsonObject().get("id").getAsString());var contract=BoardScreenHandler.existing(p,id);
  contract.credit(UUID.randomUUID(),p.getUuid(),contract.definition().target(),System.currentTimeMillis());storeDirty(p);var saved=ContractState.get(p.getServer()).writeNbt(new NbtCompound(),w.getRegistryManager());var restored=ContractState.read(saved,w.getRegistryManager());c.assertTrue(restored.find(contract.id()).progress()==1,"20A progress persists");
  c.assertTrue(BoardScreenHandler.existing(q,id)==null,"Owner isolation");p.currentScreenHandler=p.playerScreenHandler;p.discard();q.discard();c.complete();}
 @GameTest(templateName=ARENA,batchId="stage20b-villages",tickLimit=30)
 public void villageHookSupportsAllFiveVanillaTypes(TestContext c){
  var w=c.getWorld();var root=floor(c);String[] types={"plains","desert","savanna","taiga","snowy"};Block[] surfaces={Blocks.GRASS_BLOCK,Blocks.SAND,Blocks.GRASS_BLOCK,Blocks.DIRT,Blocks.SNOW_BLOCK};
  for(int t=0;t<types.length;t++){
   for(int x=0;x<16;x++)for(int z=0;z<16;z++)c.setBlockState(new BlockPos(x,1,z),surfaces[t]);
   var structure=w.getRegistryManager().get(RegistryKeys.STRUCTURE).get(Identifier.of("minecraft:village_"+types[t]));
   var bounds=new BlockBox(root.getX()-4,root.getY(),root.getZ()-5,root.getX()+4,root.getY()+4,root.getZ()+3);
   var piece=new net.minecraft.structure.StructurePiece(net.minecraft.structure.StructurePieceType.BURIED_TREASURE,0,bounds){
    protected void writeNbt(net.minecraft.structure.StructureContext context,NbtCompound n){}
    public void generate(net.minecraft.world.StructureWorldAccess world,net.minecraft.world.gen.StructureAccessor accessor,net.minecraft.world.gen.chunk.ChunkGenerator generator,net.minecraft.util.math.random.Random random,BlockBox box,ChunkPos cp,BlockPos pivot){}
   };
   var start=new net.minecraft.structure.StructureStart(structure,new ChunkPos(root),0,new net.minecraft.structure.StructurePiecesList(List.of(piece)));
   var actual=piece.getBoundingBox();var chunk=new ChunkPos(new BlockPos((actual.getMinX()+actual.getMaxX())/2,0,(actual.getMinZ()+actual.getMaxZ())/2));
   // Jigsaw terrain adaptation expands the start box: prepare its actual candidate chunk.
   for(int x=chunk.getStartX();x<=chunk.getEndX();x++)for(int z=chunk.getStartZ();z<=chunk.getEndZ();z++){w.setBlockState(new BlockPos(x,root.getY()-1,z),surfaces[t].getDefaultState());for(int y=root.getY();y<Math.max(root.getY()+8,w.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE,x,z)+1);y++)w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());}
   // Invoke the actual mapped vanilla method to exercise the injected tail, not only its helper.
   start.place(w,w.getStructureAccessor(),w.getChunkManager().getChunkGenerator(),w.random,new BlockBox(chunk.getStartX(),w.getBottomY(),chunk.getStartZ(),chunk.getEndX(),w.getTopY()-1,chunk.getEndZ()),chunk);
   var roots=new HashSet<BlockPos>();for(int x=chunk.getStartX();x<=chunk.getEndX();x++)for(int z=chunk.getStartZ();z<=chunk.getEndZ();z++){var at=new BlockPos(x,w.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE,x,z)-1,z);var b=w.getBlockState(at);if(b.isOf(NoticeBoards.BLOCK))roots.add(NoticeBoardBlock.root(at,b));}
   c.assertTrue(roots.size()==1,"Village injection/terrain "+types[t]+": "+roots.size());w.breakBlock(roots.iterator().next(),false);
  }c.complete();
 }
 private void storeDirty(net.minecraft.server.network.ServerPlayerEntity p){ContractState.get(p.getServer()).markDirty();}
 @GameTest(templateName="witchersbestiary:village_board_arena",batchId="stage20b-village-repeat",tickLimit=20)
 public void supportedVillageTerrainAndNoRegeneration(TestContext c){var w=c.getWorld();var root=c.getAbsolutePos(new BlockPos(24,2,24));var chunk=new ChunkPos(root);for(int x=chunk.getStartX();x<=chunk.getEndX();x++)for(int z=chunk.getStartZ();z<=chunk.getEndZ();z++){w.setBlockState(new BlockPos(x,root.getY()-1,z),Blocks.GRASS_BLOCK.getDefaultState());for(int y=root.getY(),ceiling=w.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE,x,z);y<Math.max(root.getY()+5,ceiling);y++)w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());}boolean generated=VillageNoticeBoards.attempt(w,chunk);c.assertTrue(generated,"Bounded candidate placement: base="+root+" height="+w.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE,chunk.getStartX()+8,chunk.getStartZ()+8)+" top="+w.getBlockState(new BlockPos(chunk.getStartX()+8,w.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE,chunk.getStartX()+8,chunk.getStartZ()+8)-1,chunk.getStartZ()+8)));c.assertTrue(!VillageNoticeBoards.attempt(w,chunk),"Existing board prevents repeat");c.complete();}
}
