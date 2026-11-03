package com.witchersbestiary.test;
import com.google.gson.*;
import com.witchersbestiary.board.*;
import com.witchersbestiary.client.tracking.WitcherSensesClient;
import com.witchersbestiary.contracts.tracking.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.*;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.text.Text;
import java.nio.file.*;
import java.util.*;
/** Separate real clients; local test files coordinate actions, but all mutations use normal C2S packets. */
public final class Stage20ENetworkClient implements ClientModInitializer {
 private final String role=System.getProperty("witchersbestiary.stage20eClient");private final Path dir=Path.of(System.getProperty("witchersbestiary.stage20eOutput","../stage20e-network"));private long nonce=-1;private int ticks,step,button,sync,lastConnect=-1000;private JsonObject command=new JsonObject();private boolean completed,seenTarget,hidden;
 private void reply(MinecraftClient c,String status)throws Exception{var j=new JsonObject();j.addProperty("nonce",nonce);j.addProperty("status",status);j.addProperty("senses",WitcherSensesClient.enabled());j.addProperty("screen",c.currentScreen==null?"none":c.currentScreen.getClass().getSimpleName());if(c.world!=null)j.addProperty("dimension",c.world.getRegistryKey().getValue().toString());j.add("snapshot",WitcherSensesClient.snapshot());var path=dir.resolve(role+".reply.json");var temp=path.resolveSibling(path.getFileName()+".tmp");Files.writeString(temp,j.toString());for(int retry=0;;retry++){try{Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);break;}catch(java.nio.file.AccessDeniedException locked){if(retry>=99)throw locked;java.util.concurrent.locks.LockSupport.parkNanos(1000000);}}completed=true;}
 private void capture(MinecraftClient c,String name)throws Exception{try(var image=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())){image.writeTo(dir.resolve(role+"-"+name+".png"));}}
 private boolean choose(MinecraftClient c)throws Exception{if(!(c.currentScreen instanceof com.witchersbestiary.client.board.NoticeBoardScreen screen))return false;var h=(BoardScreenHandler)c.player.currentScreenHandler;var offers=h.snapshot.getAsJsonArray("offers");if(offers==null)return false;for(int n=0;n<offers.size();n++){var row=offers.get(n).getAsJsonObject();if(!row.get("id").getAsString().equals(command.get("definition").getAsString()))continue;button=row.get("button").getAsInt();sync=h.syncId;for(String name:new String[]{"selected","page"}){var field=screen.getClass().getDeclaredField(name);field.setAccessible(true);field.setInt(screen,name.equals("selected")?n:n/3);}screen.refresh();return true;}return false;}
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);var watchdog=new Thread(()->{try{Thread.sleep(900000);if(!Files.exists(dir.resolve("complete.json"))){Files.writeString(dir.resolve(role+".timeout.txt"),"900s dedicated review cap");Runtime.getRuntime().halt(2);}}catch(Exception ignored){}},"network-client-limit");watchdog.setDaemon(true);watchdog.start();}
 private void tick(MinecraftClient c){try{ticks++;if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(c.getWindow().getHandle());hidden=true;}if(c.getOverlay()!=null)return;c.options.pauseOnLostFocus=false;c.options.getMaxFps().setValue(30);c.options.getEnableVsync().setValue(false);c.options.getViewDistance().setValue(Boolean.getBoolean("witchersbestiary.stage20gVillage")?8:4);c.options.getSimulationDistance().setValue(5);c.options.getGuiScale().setValue(2);var path=dir.resolve(role+".command.json");if(Files.exists(path)){var j=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(j.get("nonce").getAsLong()!=nonce){nonce=j.get("nonce").getAsLong();command=j;step=0;completed=false;seenTarget=false;}}
  String action=command.has("action")?command.get("action").getAsString():"wait";
  if(action.equals("stop")){capture(c,"final");reply(c,"stopped");c.scheduleStop();return;}
  if(action.equals("disconnect")){if(!completed&&c.world!=null){reply(c,"disconnected");c.world.disconnect();c.disconnect();c.setScreen(new TitleScreen());}return;}
  if(c.world==null||c.player==null){if(ticks-lastConnect>100&&!(c.currentScreen instanceof ConnectScreen)){lastConnect=ticks;ConnectScreen.connect(new TitleScreen(),c,ServerAddress.parse("127.0.0.1:25583"),new ServerInfo("Local Stage20E","127.0.0.1:25583",ServerInfo.ServerType.OTHER),false,null);}return;}
  if(completed)return;step++;
  if(action.equals("walletProof")){if(c.player.currentScreenHandler instanceof com.witchersbestiary.economy.WalletHandler h&&h.balance==54&&step>20){capture(c,"wallet-restart");reply(c,"wallet-pass");}return;}
  if(action.equals("walletFlow")){if(c.player.currentScreenHandler instanceof com.witchersbestiary.economy.WalletHandler h&&c.currentScreen instanceof com.witchersbestiary.client.economy.WalletScreen){if(step==30)ClientPlayNetworking.send(new com.witchersbestiary.economy.WalletPackets.Action(h.syncId,h.nonce,true,64));if(step==60){var a=new com.witchersbestiary.economy.WalletPackets.Action(h.syncId,h.nonce,false,10);ClientPlayNetworking.send(a);ClientPlayNetworking.send(a);}if(step>90&&h.balance==54){capture(c,"wallet-gui");reply(c,"wallet-pass");}}return;}
  if(action.equals("boardProof")){if(c.currentScreen instanceof com.witchersbestiary.client.board.NoticeBoardScreen&&step>20){capture(c,"village-gui");reply(c,"board-screen");}return;}
  if(action.equals("wait")){if(step>5)reply(c,"waiting");return;}
  if(action.equals("respawn")){if(c.player.getHealth()<=0&&step%20==1)c.player.requestRespawn();if(c.player.getHealth()>0&&step>25){c.setScreen(null);reply(c,"respawned");}return;}
  if(action.equals("dimension")){if(c.world.getRegistryKey().getValue().toString().equals(command.get("dimension").getAsString())&&step>20){capture(c,"dimension");reply(c,"dimension");}return;}
  if(action.equals("toggleBurst")){if(step==5)for(int n=0;n<command.get("presses").getAsInt();n++)KeyBinding.onKeyPressed(InputUtil.fromKeyCode(86,0));if(step>25)reply(c,"burst");return;}
  if(action.equals("forged")){if(step==5)ClientPlayNetworking.send(new TrackingPackets.Inspect(UUID.fromString(command.get("clue").getAsString())));if(step>20)reply(c,"forged-sent");return;}
  if(action.equals("accept")||action.equals("claim")||action.equals("replayClaim")){if(!choose(c))return;if(step>12){if(action.equals("accept"))c.interactionManager.clickButton(sync,button);else c.interactionManager.clickButton(sync,button+32);capture(c,action);reply(c,"button-sent");}return;}
  if(action.equals("staleClaim")){if(step==5){c.player.closeHandledScreen();c.getNetworkHandler().sendPacket(new ButtonClickC2SPacket(sync,button+32));}if(step>20)reply(c,"stale-sent");return;}
  if(action.equals("study")){if(c.currentScreen!=null)c.setScreen(null);if(!WitcherSensesClient.enabled()&&step%20==1)KeyBinding.onKeyPressed(InputUtil.fromKeyCode(86,0));var s=WitcherSensesClient.snapshot();if(s.has("progress")&&s.get("progress").getAsInt()>=command.get("progress").getAsInt()){capture(c,"study-"+command.get("progress").getAsInt());reply(c,"studied");return;}if(!s.has("point")||!s.getAsJsonObject("point").has("id"))return;var p=s.getAsJsonObject("point");var destination=new Vec3d(p.get("x").getAsDouble()+.5,p.get("y").getAsDouble()+.06,p.get("z").getAsDouble()+.5);var delta=destination.subtract(c.player.getEyePos());c.player.setYaw((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));c.player.setPitch((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.horizontalLengthSquared()))));if(step>15&&step%10==0)c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);return;}
  if(action.equals("fight")){if(c.currentScreen!=null)c.setScreen(null);UUID target=UUID.fromString(command.get("target").getAsString());net.minecraft.entity.Entity boss=null;for(var e:c.world.getEntities())if(e.getUuid().equals(target))boss=e;if(boss!=null)seenTarget=true;if(boss!=null&&boss.isAlive()&&step%25==0){c.interactionManager.attackEntity(c.player,boss);c.player.swingHand(Hand.MAIN_HAND);}if(step==25)capture(c,"boss");if(step>40&&seenTarget&&(boss==null||!boss.isAlive()))reply(c,"target-gone");return;}
 }catch(Throwable t){try{Files.writeString(dir.resolve(role+".failure.txt"),t+"\n"+Arrays.toString(t.getStackTrace()));}catch(Exception ignored){}c.scheduleStop();}}
}
