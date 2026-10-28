package com.witchersbestiary.test;
import com.witchersbestiary.board.*;
import com.witchersbestiary.contracts.*;
import com.witchersbestiary.contracts.hunt.*;
import com.witchersbestiary.entity.monster.DrownerEntity;
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
public final class SwampLordReviewClient implements ClientModInitializer {
 private static final Path OUT=Path.of("../stage20c1-review");
 private int phase,ticks,button;private volatile boolean ready,finished;private volatile Throwable failure;private BlockPos root;private volatile UUID target,contract;private final Map<String,Object> results=new LinkedHashMap<>();
 public void onInitializeClient(){try{Files.createDirectories(OUT);}catch(Exception e){throw new RuntimeException(e);}ClientTickEvents.END_CLIENT_TICK.register(this::tick);var watchdog=new Thread(()->{try{Thread.sleep(240000);if(!finished){Files.writeString(OUT.resolve("timeout.txt"),"240s native review limit");Runtime.getRuntime().halt(2);}}catch(Exception ignored){}},"swamp-review-limit");watchdog.setDaemon(true);watchdog.start();}
 private void capture(MinecraftClient c,String name)throws Exception{try(var image=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())){image.writeTo(OUT.resolve(name+".png"));}}
 private void server(MinecraftClient c,Runnable action){ready=false;c.getServer().execute(()->{try{action.run();ready=true;}catch(Throwable t){failure=t;}});}
 private void open(MinecraftClient c){server(c,()->{var w=c.getServer().getOverworld();var p=c.getServer().getPlayerManager().getPlayerList().getFirst();p.changeGameMode(GameMode.CREATIVE);p.teleport(w,.5,root.getY(),-2,0,0);w.getBlockState(root).onUse(w,p,new BlockHitResult(root.toCenterPos(),Direction.NORTH,root,false));});}
 private void select(MinecraftClient c)throws Exception{var h=(BoardScreenHandler)c.player.currentScreenHandler;var offers=h.snapshot.getAsJsonArray("offers");if(offers==null)return;for(int i=0;i<offers.size();i++)if(offers.get(i).getAsJsonObject().get("id").getAsString().equals(SwampLord.ID.toString())){button=offers.get(i).getAsJsonObject().get("button").getAsInt();var screen=c.currentScreen;for(String key:new String[]{"selected","page"}){var field=screen.getClass().getDeclaredField(key);field.setAccessible(true);field.setInt(screen,key.equals("selected")?i:i/3);}((com.witchersbestiary.client.board.NoticeBoardScreen)screen).refresh();}}
 private void tick(MinecraftClient c){try{
  if(failure!=null)throw new IllegalStateException(failure);
  if(phase==0){if(c.getOverlay()!=null)return;c.options.pauseOnLostFocus=false;c.options.getViewDistance().setValue(5);c.options.getSimulationDistance().setValue(5);c.options.getGuiScale().setValue(2);c.options.getFov().setValue(70);phase=1;c.createIntegratedServerLoader().createAndStart("SwampLord-"+System.currentTimeMillis(),new LevelInfo("Swamp Lord Review",GameMode.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),DataConfiguration.SAFE_MODE),new GeneratorOptions(202001L,false,false),r->r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder(),null);return;}
  if(phase==1&&c.player!=null){c.setScreen(null);if(++ticks<80)return;ticks=0;phase=2;server(c,()->{var w=c.getServer().getOverworld();int y=w.getTopY(Heightmap.Type.WORLD_SURFACE,0,0);root=new BlockPos(0,y,0);w.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,c.getServer());w.setTimeOfDay(6000);if(!NoticeBoardBlock.place(w,root,Direction.NORTH))throw new IllegalStateException("Board placement");for(int x=40;x<=60;x++)for(int z=-8;z<=8;z++)if(z%4==0&&w.getChunkManager().getWorldChunk(x>>4,z>>4)!=null)w.setBlockState(new BlockPos(x,y-1,z),Blocks.WATER.getDefaultState());});return;}
  if(phase==2&&ready){phase=3;open(c);return;}
  if(phase==3&&ready&&c.currentScreen instanceof com.witchersbestiary.client.board.NoticeBoardScreen){select(c);if(++ticks==20)capture(c,"available");if(ticks==25)c.interactionManager.clickButton(c.player.currentScreenHandler.syncId,button);if(ticks==65){capture(c,"active");phase=4;ticks=0;c.player.closeHandledScreen();server(c,()->{var p=c.getServer().getPlayerManager().getPlayerList().getFirst();var instance=BoardScreenHandler.existing(p,SwampLord.ID);if(instance==null||instance.hunt.target==null)throw new IllegalStateException("Loaded-bank queued search failed: "+(instance==null?"missing":instance.hunt.status));contract=instance.id();target=instance.hunt.target;var boss=(DrownerEntity)p.getServerWorld().getEntity(target);if(boss==null)throw new IllegalStateException("Target missing");p.changeGameMode(GameMode.SURVIVAL);p.setInvulnerable(true);p.setNoGravity(true);p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.NETHERITE_SWORD));p.teleport(p.getServerWorld(),boss.getX(),boss.getY(),boss.getZ()-5,0,0);boss.setTarget(p);boss.huntDash=0;boss.huntSummon=0;results.put("queued_loaded_bank_spawn",true);results.put("target",target.toString());});}return;}
  if(phase==4&&ready){ticks++;if(ticks==20)capture(c,"boss-and-bar");if(ticks==35){phase=5;ticks=0;server(c,()->{var p=c.getServer().getPlayerManager().getPlayerList().getFirst();var boss=(DrownerEntity)p.getServerWorld().getEntity(target);results.put("helpers",Arrays.stream(boss.huntHelpers).filter(Objects::nonNull).count());results.put("boss_bars",SwampLord.bossBars());p.teleport(p.getServerWorld(),boss.getX(),boss.getY(),boss.getZ()-2,0,0);});}return;}
  if(phase==5&&ready){ticks++;net.minecraft.entity.Entity boss=null;for(var e:c.world.getEntities())if(e.getUuid().equals(target))boss=e;
   if(boss!=null&&boss.isAlive()&&ticks%25==0){c.interactionManager.attackEntity(c.player,boss);c.player.swingHand(Hand.MAIN_HAND);}
   if(ticks==70)capture(c,"combat");if(ticks>600)throw new IllegalStateException("Bounded combat did not finish");
   if(ticks>30&&(boss==null||!boss.isAlive())){phase=6;ticks=0;server(c,()->{var p=c.getServer().getPlayerManager().getPlayerList().getFirst();var instance=ContractState.get(c.getServer()).find(contract);if(instance.status()!=ContractInstance.Status.COMPLETED)throw new IllegalStateException("Death completion missing");results.put("real_attack_completion",true);results.put("bars_after_death",SwampLord.bossBars());});}return;}
  if(phase==6&&ready){phase=7;open(c);return;}
  if(phase==7&&ready&&c.currentScreen instanceof com.witchersbestiary.client.board.NoticeBoardScreen){select(c);if(++ticks==20)capture(c,"completed");if(ticks==25)c.interactionManager.clickButton(c.player.currentScreenHandler.syncId,button+32);if(ticks==50){capture(c,"rewarded");server(c,()->{var p=c.getServer().getPlayerManager().getPlayerList().getFirst();if(ContractState.get(c.getServer()).find(contract).status()!=ContractInstance.Status.REWARDED||p.getInventory().count(Items.EMERALD)!=12)throw new IllegalStateException("Reward claim failed");results.put("real_packet_claim",true);});}if(ticks==65){Files.writeString(OUT.resolve("results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results));finished=true;c.scheduleStop();}return;}
 }catch(Throwable t){try{Files.writeString(OUT.resolve("failure.txt"),t+"\n"+Arrays.toString(t.getStackTrace()));}catch(Exception ignored){}finished=true;c.scheduleStop();}}
}
