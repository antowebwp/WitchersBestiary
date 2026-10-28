package com.witchersbestiary.test;
import com.witchersbestiary.board.*;
import com.witchersbestiary.contracts.*;
import com.witchersbestiary.contracts.hunt.*;
import com.witchersbestiary.entity.monster.GhoulEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.block.Blocks;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.gen.*;
import net.minecraft.world.level.LevelInfo;
import java.nio.file.*;
import java.util.*;
/** One bounded client: real board packets, loaded-terrain search, combat and claim. */
public final class GraveDevourerReviewClient implements ClientModInitializer {
 private static final Path OUT=Path.of("../stage20c2-review");
 private int phase,ticks,button;private volatile boolean ready,finished;private volatile Throwable failure;private BlockPos root;private volatile UUID target,contract;private final Map<String,Object> results=new LinkedHashMap<>();
 public void onInitializeClient(){try{Files.createDirectories(OUT);}catch(Exception e){throw new RuntimeException(e);}ClientTickEvents.END_CLIENT_TICK.register(this::tick);var watchdog=new Thread(()->{try{Thread.sleep(240000);if(!finished){Files.writeString(OUT.resolve("timeout.txt"),"240s native review limit");Runtime.getRuntime().halt(2);}}catch(Exception ignored){}},"grave-review-limit");watchdog.setDaemon(true);watchdog.start();}
 private void capture(MinecraftClient c,String name)throws Exception{try(var image=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())){image.writeTo(OUT.resolve(name+".png"));}}
 private void server(MinecraftClient c,Runnable action){ready=false;c.getServer().execute(()->{try{action.run();ready=true;}catch(Throwable t){failure=t;}});}
 private void open(MinecraftClient c){server(c,()->{var w=c.getServer().getOverworld();var p=c.getServer().getPlayerManager().getPlayerList().getFirst();p.changeGameMode(GameMode.CREATIVE);p.teleport(w,.5,root.getY(),-2,0,0);w.getBlockState(root).onUse(w,p,new BlockHitResult(root.toCenterPos(),Direction.NORTH,root,false));});}
 private void select(MinecraftClient c)throws Exception{var h=(BoardScreenHandler)c.player.currentScreenHandler;var offers=h.snapshot.getAsJsonArray("offers");if(offers==null)return;for(int i=0;i<offers.size();i++)if(offers.get(i).getAsJsonObject().get("id").getAsString().equals(GraveDevourer.ID.toString())){button=offers.get(i).getAsJsonObject().get("button").getAsInt();var screen=c.currentScreen;for(String key:new String[]{"selected","page"}){var field=screen.getClass().getDeclaredField(key);field.setAccessible(true);field.setInt(screen,key.equals("selected")?i:i/3);}((com.witchersbestiary.client.board.NoticeBoardScreen)screen).refresh();}}
 private void tick(MinecraftClient c){try{
  if(failure!=null)throw new IllegalStateException(failure);
  if(phase==0){if(c.getOverlay()!=null)return;c.options.pauseOnLostFocus=false;c.options.getViewDistance().setValue(5);c.options.getSimulationDistance().setValue(5);c.options.getGuiScale().setValue(2);c.options.getFov().setValue(70);phase=1;c.createIntegratedServerLoader().createAndStart("GraveDevourer-"+System.currentTimeMillis(),new LevelInfo("Grave Devourer Review",GameMode.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),DataConfiguration.SAFE_MODE),new GeneratorOptions(202001L,false,false),r->r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder(),null);return;}
  if(phase==1&&c.player!=null){c.setScreen(null);if(++ticks<80)return;ticks=0;phase=2;server(c,()->{var w=c.getServer().getOverworld();int y=w.getTopY(Heightmap.Type.WORLD_SURFACE,0,0);root=new BlockPos(0,y,0);w.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,c.getServer());w.setTimeOfDay(6000);if(!NoticeBoardBlock.place(w,root,Direction.NORTH))throw new IllegalStateException("Board placement");var forest=w.getRegistryManager().get(RegistryKeys.BIOME).getEntry(net.minecraft.world.biome.BiomeKeys.FOREST).orElseThrow();for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){var chunk=w.getChunkManager().getWorldChunk(x,z);if(chunk!=null)chunk.populateBiomes((a,b,d,sampler)->forest,w.getChunkManager().getNoiseConfig().getMultiNoiseSampler());}});return;}
  if(phase==2&&ready){phase=3;open(c);return;}
  if(phase==3&&ready&&c.currentScreen instanceof com.witchersbestiary.client.board.NoticeBoardScreen){select(c);if(++ticks==20)capture(c,"available");if(ticks==25)c.interactionManager.clickButton(c.player.currentScreenHandler.syncId,button);if(ticks==65){capture(c,"active");phase=4;ticks=0;c.player.closeHandledScreen();server(c,()->{var p=c.getServer().getPlayerManager().getPlayerList().getFirst();var instance=BoardScreenHandler.existing(p,GraveDevourer.ID);if(instance==null||instance.hunt.target==null)throw new IllegalStateException("Loaded-bank queued search failed: "+(instance==null?"missing":instance.hunt.status));contract=instance.id();target=instance.hunt.target;var boss=(GhoulEntity)p.getServerWorld().getEntity(target);if(boss==null)throw new IllegalStateException("Target missing");p.changeGameMode(GameMode.SURVIVAL);p.setInvulnerable(false);p.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setNoGravity(true);p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.NETHERITE_SWORD));p.teleport(p.getServerWorld(),boss.getX(),boss.getY(),boss.getZ()-5,0,0);boss.setTarget(p);boss.huntLeap=0;results.put("queued_loaded_forest_spawn",true);results.put("target",target.toString());});}return;}
  if(phase==4&&ready){ticks++;if(ticks==20)capture(c,"boss-and-bar");if(ticks==35){phase=5;ticks=0;server(c,()->{var p=c.getServer().getPlayerManager().getPlayerList().getFirst();var boss=(GhoulEntity)p.getServerWorld().getEntity(target);results.put("leap_cooldown",boss.huntLeap);results.put("boss_bars",SwampLord.bossBars());p.teleport(p.getServerWorld(),boss.getX(),boss.getY(),boss.getZ()-2,0,0);});}return;}
  if(phase==5&&ready){ticks++;if(ticks%20==0)c.getServer().execute(()->{var e=c.getServer().getOverworld().getEntity(target);if(e instanceof GhoulEntity g){results.put("rage_used",g.huntRageUsed);results.put("healed",g.huntHealed);}});net.minecraft.entity.Entity boss=null;for(var e:c.world.getEntities())if(e.getUuid().equals(target))boss=e;
   if(boss!=null&&boss.isAlive()&&ticks%25==0){c.interactionManager.attackEntity(c.player,boss);c.player.swingHand(Hand.MAIN_HAND);}
   if(ticks==70)capture(c,"combat");if(ticks>600)throw new IllegalStateException("Bounded combat did not finish");
   if(ticks>30&&(boss==null||!boss.isAlive())){phase=6;ticks=0;server(c,()->{var p=c.getServer().getPlayerManager().getPlayerList().getFirst();var instance=ContractState.get(c.getServer()).find(contract);if(instance.status()!=ContractInstance.Status.COMPLETED)throw new IllegalStateException("Death completion missing");if(!Boolean.TRUE.equals(results.get("rage_used"))||!(results.get("healed") instanceof Number healed)||healed.floatValue()<=0||healed.floatValue()>12)throw new IllegalStateException("Missing rage/feed evidence "+results);results.put("real_attack_completion",true);results.put("bars_after_death",SwampLord.bossBars());});}return;}
  if(phase==6&&ready){phase=7;open(c);return;}
  if(phase==7&&ready&&c.currentScreen instanceof com.witchersbestiary.client.board.NoticeBoardScreen){select(c);if(++ticks==20)capture(c,"completed");if(ticks==25)c.interactionManager.clickButton(c.player.currentScreenHandler.syncId,button+32);if(ticks==50){capture(c,"rewarded");server(c,()->{var p=c.getServer().getPlayerManager().getPlayerList().getFirst();if(ContractState.get(c.getServer()).find(contract).status()!=ContractInstance.Status.REWARDED||p.getInventory().count(Items.EMERALD)!=14)throw new IllegalStateException("Reward claim failed");results.put("real_packet_claim",true);});}if(ticks==65){Files.writeString(OUT.resolve("results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results));finished=true;c.scheduleStop();}return;}
 }catch(Throwable t){try{Files.writeString(OUT.resolve("failure.txt"),t+"\n"+Arrays.toString(t.getStackTrace()));}catch(Exception ignored){}finished=true;c.scheduleStop();}}
}
