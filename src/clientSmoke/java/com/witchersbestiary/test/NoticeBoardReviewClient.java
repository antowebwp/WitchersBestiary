package com.witchersbestiary.test;

import com.witchersbestiary.board.*;
import com.witchersbestiary.contracts.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.block.Blocks;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.gen.*;
import net.minecraft.world.level.LevelInfo;
import java.nio.file.*;
import java.util.*;

/** Bounded native rendering/network review, screenshots remain in ignored build/. */
public final class NoticeBoardReviewClient implements ClientModInitializer {
 private static final Path OUT=Path.of("../stage20b-review");
 private volatile Throwable failure;private volatile boolean ready,finished;private int phase,ticks,index;private BlockPos root,village,naturalRoot;private final Map<String,Object> result=new LinkedHashMap<>();
 private final String[] views={"front","back","left","right","above","below","front-45","back-45","village-day","village-night"};
 private final double[][] cameras={{0,0,-5.8},{0,0,5.8},{-5.8,0,0},{5.8,0,0},{0,6,0.01},{0,-3.8,-3.2},{-4.5,2,-4.5},{4.5,2,4.5},{-6,2.8,-6},{-6,2.8,-6}};
 public void onInitializeClient(){try{Files.createDirectories(OUT);}catch(Exception e){throw new RuntimeException(e);}ClientTickEvents.END_CLIENT_TICK.register(this::tick);var watchdog=new Thread(()->{try{Thread.sleep(300000);if(!finished){Files.writeString(OUT.resolve("timeout.txt"),"300 second review limit");Runtime.getRuntime().halt(2);}}catch(Exception ignored){}},"notice-board-review-limit");watchdog.setDaemon(true);watchdog.start();}
 private void capture(MinecraftClient c,String name)throws Exception{try(var image=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())){image.writeTo(OUT.resolve(name+".png"));}}
 private void camera(MinecraftClient c,int i){ready=false;c.setScreen(null);c.getServer().execute(()->{try{var p=c.getServer().getPlayerManager().getPlayerList().getFirst();var w=p.getServerWorld();w.setTimeOfDay(i==9?18000:6000);if(i==5)for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)w.setBlockState(root.add(dx,-1,dz),Blocks.AIR.getDefaultState(),18);
 if(i==6)for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)w.setBlockState(root.add(dx,-1,dz),Blocks.STONE_BRICKS.getDefaultState(),18);
 var base=i>=8&&naturalRoot!=null?naturalRoot:root;var a=cameras[i];double tx=base.getX()+.5,ty=base.getY()+1.4,tz=base.getZ()+.5;double px=tx+a[0],py=ty+a[1]-p.getStandingEyeHeight(),pz=tz+a[2];float yaw=(float)Math.toDegrees(Math.atan2(-(tx-px),tz-pz));float pitch=(float)-Math.toDegrees(Math.atan2(ty-(py+p.getStandingEyeHeight()),Math.hypot(tx-px,tz-pz)));p.teleport(w,px,py,pz,yaw,pitch);p.setNoGravity(true);p.getAbilities().allowFlying=true;p.getAbilities().flying=true;p.sendAbilitiesUpdate();ready=true;}catch(Throwable t){failure=t;}});}
 private void prepare(MinecraftClient c){c.getServer().execute(()->{try{var w=c.getServer().getOverworld();var p=c.getServer().getPlayerManager().getPlayerList().getFirst();w.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,c.getServer());village=w.locateStructure(StructureTags.VILLAGE,new BlockPos(0,70,0),64,false);if(village==null)throw new IllegalStateException("No village within bounded radius");int y=w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,village.getX(),village.getZ());p.teleport(w,village.getX()+.5,y+4,village.getZ()+.5,0,0);result.put("village_located",List.of(village.getX(),village.getZ()));ready=true;}catch(Throwable t){failure=t;}});}
 private void showcase(MinecraftClient c){ready=false;c.getServer().execute(()->{try{var w=c.getServer().getOverworld();
  var start=w.getStructureAccessor().getStructureStarts(new ChunkPos(village),structure->{var id=w.getRegistryManager().get(RegistryKeys.STRUCTURE).getId(structure);return id!=null&&id.getPath().startsWith("village_");}).stream().findFirst().orElse(net.minecraft.structure.StructureStart.DEFAULT);if(start.hasChildren()){
   var box=start.getBoundingBox();var chunk=new ChunkPos(new BlockPos((box.getMinX()+box.getMaxX())/2,0,box.getMinZ()+5));w.getChunk(chunk.x,chunk.z);
   var boards=new HashSet<BlockPos>();for(int x=chunk.getStartX();x<=chunk.getEndX();x++)for(int z=chunk.getStartZ();z<=chunk.getEndZ();z++){int yy=w.getTopY(Heightmap.Type.WORLD_SURFACE,x,z)-1;for(int dy=0;dy<4;dy++){var at=new BlockPos(x,yy-dy,z);var state=w.getBlockState(at);if(state.isOf(NoticeBoards.BLOCK))boards.add(NoticeBoardBlock.root(at,state));}}
   if(!boards.isEmpty())naturalRoot=boards.iterator().next();result.put("natural_board_count_in_candidate_chunk",boards.size());result.put("village_type",w.getRegistryManager().get(RegistryKeys.STRUCTURE).getId(start.getStructure()).toString());
  }
  // A fixed review mount near the natural village keeps all camera positions comparable.
  int x=village.getX()+10,z=village.getZ()-10,y=w.getTopY(Heightmap.Type.WORLD_SURFACE,x,z)+3;root=new BlockPos(x,y,z);
  for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)w.setBlockState(root.add(dx,-1,dz),Blocks.STONE_BRICKS.getDefaultState());
  if(!NoticeBoardBlock.place(w,root,Direction.NORTH))throw new IllegalStateException("Review mount placement failed");result.put("model_has_depth",c.getItemRenderer().getModel(new net.minecraft.item.ItemStack(NoticeBoards.ITEM),c.world,c.player,0).hasDepth());ready=true;
 }catch(Throwable t){failure=t;}});}
 private void tick(MinecraftClient c){try{
  if(failure!=null)throw new IllegalStateException(failure);
  if(phase==0){if(c.getOverlay()!=null)return;c.options.pauseOnLostFocus=false;c.options.getFov().setValue(70);c.options.getGuiScale().setValue(2);c.options.getViewDistance().setValue(5);c.options.getSimulationDistance().setValue(5);c.options.hudHidden=true;c.options.setPerspective(Perspective.FIRST_PERSON);phase=1;c.createIntegratedServerLoader().createAndStart("NoticeBoard-"+System.currentTimeMillis(),new LevelInfo("Notice Board Review",GameMode.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),DataConfiguration.SAFE_MODE),new GeneratorOptions(12345L,true,false),r->r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.DEFAULT).createDimensionsRegistryHolder(),null);return;}
  if(phase==1&&c.player!=null){c.setScreen(null);phase=2;ready=false;prepare(c);return;}
  if(phase==2&&ready){if(++ticks<80)return;ticks=0;phase=3;showcase(c);return;}
  if(phase==3&&ready){phase=4;ticks=0;index=0;camera(c,index);return;}
  if(phase==4&&ready){if(++ticks==35)capture(c,views[index]);if(ticks>=40){ticks=0;if(++index<views.length)camera(c,index);else{phase=5;ready=false;c.getServer().execute(()->{try{var p=c.getServer().getPlayerManager().getPlayerList().getFirst();var w=p.getServerWorld();p.teleport(w,root.getX()+.5,root.getY(),root.getZ()-2,0,0);w.setTimeOfDay(6000);w.getBlockState(root).onUse(w,p,new BlockHitResult(root.toCenterPos(),Direction.NORTH,root,false));ready=true;}catch(Throwable t){failure=t;}});}}return;}
  if(phase==5&&ready){if(c.getOverlay()!=null)return;if(!(c.currentScreen instanceof com.witchersbestiary.client.board.NoticeBoardScreen))return;if(++ticks==30)capture(c,"gui-available");if(ticks==40){var h=(BoardScreenHandler)c.player.currentScreenHandler;c.interactionManager.clickButton(h.syncId,0);}if(ticks==65){capture(c,"gui-accepted");c.getServer().execute(()->{try{var p=c.getServer().getPlayerManager().getPlayerList().getFirst();var all=ContractState.get(c.getServer()).ownedBy(p.getUuid());if(all.size()!=1||all.getFirst().status()!=ContractInstance.Status.ACCEPTED)throw new IllegalStateException("Real client acceptance failed");result.put("real_packet_acceptance",true);result.put("accepted_count",all.size());}catch(Throwable t){failure=t;}});}if(ticks==75){c.getLanguageManager().setLanguage("ru_ru");c.options.language="ru_ru";c.reloadResources();}if(ticks==100)capture(c,"gui-russian");if(ticks==110){result.put("screenshots",views.length+3);Files.writeString(OUT.resolve("results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));finished=true;c.scheduleStop();}return;}
 }catch(Throwable t){try{Files.writeString(OUT.resolve("failure.txt"),t+"\n"+Arrays.toString(t.getStackTrace()));}catch(Exception ignored){}finished=true;c.scheduleStop();}}
}
