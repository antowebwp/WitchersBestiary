package com.witchersbestiary.test;
import com.google.gson.*;import com.witchersbestiary.fist.*;import com.witchersbestiary.economy.*;
import net.fabricmc.api.ModInitializer;import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.*;import net.minecraft.server.network.ServerPlayerEntity;import net.minecraft.util.math.*;import net.minecraft.item.*;import net.minecraft.nbt.*;import net.minecraft.block.Blocks;import net.minecraft.world.GameMode;import java.nio.file.*;import java.util.*;
/** Native GUI/input and lifecycle campaign; no virtual-health/winner/reward mutation. */
public final class DuelNetworkServer implements ModInitializer {
 private Path dir;private final String[] names={"WBOne","WBTwo"};private final long[] sent=new long[2];private long nonce=System.currentTimeMillis();private int phase,round=1,ticks;private final BlockPos stone=new BlockPos(0,129,0);private DuelSessions.Session duel;private UUID old;private final String[] inventories=new String[2];private final int[] xp=new int[2];private final Map<String,Object> results=new LinkedHashMap<>();
 private void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
 private void write(String name,Object value)throws Exception{var target=dir.resolve(name);var temp=target.resolveSibling(name+".tmp");Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(value));Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
 private JsonObject read(String name)throws Exception{return JsonParser.parseString(Files.readString(dir.resolve(name))).getAsJsonObject();}
 private void command(int i,String action,Object...args)throws Exception{var j=new JsonObject();j.addProperty("nonce",++nonce);sent[i]=nonce;j.addProperty("action",action);for(int a=0;a<args.length;a+=2)j.addProperty(args[a].toString(),args[a+1].toString());write(names[i]+".command.json",j);}
 private void use(int i)throws Exception{command(i,"duelUse","x",stone.getX(),"y",stone.getY(),"z",stone.getZ());}
 private boolean ack(int i)throws Exception{return Files.exists(dir.resolve(names[i]+".reply.json"))&&read(names[i]+".reply.json").get("nonce").getAsLong()==sent[i];}
 private String inventory(ServerPlayerEntity p){return p.getInventory().writeNbt(new NbtList()).toString();}
 private void stable(List<ServerPlayerEntity> players){for(int i=0;i<2;i++){var p=players.get(i);check(p.isAlive()&&p.getHealth()==2,"Real health must stay2, no ordinary death");check(inventory(p).equals(inventories[i])&&p.totalExperience==xp[i]&&WalletTransactions.balance(p.getInventory().getStack(8))==47,"Inventory, XP and crowns unchanged");check(FistRewards.get(p.getServer()).pending(p.getUuid())==0,"No PvP pending rewards");}}
 private void near(List<ServerPlayerEntity> players){for(int i=0;i<2;i++){var p=players.get(i);p.closeHandledScreen();p.teleport(p.getServerWorld(),stone.getX()+.5,stone.getY(),stone.getZ()+(i==0?-2.5:2.5),i==0?0:180,0);}}
 private void setup(MinecraftServer s,List<ServerPlayerEntity> players){var w=s.getOverworld();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)w.getChunk(x,z);for(int x=-17;x<=17;x++)for(int z=-17;z<=17;z++){w.setBlockState(new BlockPos(x,128,z),Blocks.STONE.getDefaultState(),2);for(int y=129;y<136;y++){var at=new BlockPos(x,y,z);if(!w.getBlockState(at).isAir())w.setBlockState(at,Blocks.AIR.getDefaultState(),2);}}w.setTimeOfDay(6000);w.getGameRules().get(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE).set(false,s);w.getGameRules().get(net.minecraft.world.GameRules.DO_MOB_SPAWNING).set(false,s);
  for(int i=0;i<2;i++){var p=players.get(i);p.changeGameMode(GameMode.SURVIVAL);p.setInvulnerable(false);p.getInventory().clear();p.getInventory().selectedSlot=0;p.getInventory().setStack(8,new ItemStack(com.witchersbestiary.economy.Currency.WALLET));p.getInventory().getStack(8).set(WalletComponents.BALANCE,47L);p.getInventory().setStack(7,new ItemStack(Items.DIAMOND,3));p.getInventory().setStack(6,new ItemStack(Items.BREAD,4));p.setHealth(2);p.getHungerManager().setFoodLevel(10);p.getHungerManager().setSaturationLevel(0);}players.get(0).getInventory().setStack(0,new ItemStack(DuelStone.BLOCK));near(players);
 }
 private void finish(MinecraftServer s)throws Exception{results.put("complete",true);write("complete.json",results);for(int i=0;i<2;i++)command(i,"stop");phase=999;}
 public void onInitialize(){if(!Boolean.getBoolean("witchersbestiary.duelNetwork"))return;dir=Path.of(System.getProperty("witchersbestiary.stage20eOutput"));phase=Files.exists(dir.resolve("duel-resume.json"))?200:0;
 ServerTickEvents.END_SERVER_TICK.register(s->{try{if(phase==999)return;if(++ticks>15000)throw new IllegalStateException("Bounded PvP timeout phase="+phase+" round="+round);
  if(phase==34){if(!ack(0)||!ack(1))return;for(int i=0;i<2;i++)command(i,"wait");s.getPlayerManager().saveAllPlayerData();s.save(false,true,true);phase=999;s.stop(false);return;}
  if(phase==32&&round==4&&ack(1)&&duel.result==DuelSessions.Result.DISCONNECT){results.put("real_disconnect_cleanup",true);command(1,"wait");phase=33;}
  var a=s.getPlayerManager().getPlayer(names[0]);var b=s.getPlayerManager().getPlayer(names[1]);if(a==null||b==null)return;var players=List.of(a,b);
  switch(phase){
   case 0->{setup(s,players);command(0,"duelPlace","x",0,"y",129,"z",0);command(1,"wait");phase=1;}
   case 1->{if(!ack(0)||!s.getOverworld().getBlockState(stone).isOf(DuelStone.BLOCK))return;check(a.getMainHandStack().isEmpty(),"Survival block item consumed by real placement");for(int i=0;i<2;i++){inventories[i]=inventory(players.get(i));xp[i]=players.get(i).totalExperience;}results.put("real_survival_stone_placement",true);use(0);phase=2;}
   case 2->{if(!ack(0))return;if(!s.isPvpEnabled()){use(1);phase=90;return;}check(DuelSessions.get(a)!=null&&DuelSessions.get(a).stage==DuelSessions.Stage.WAITING,"A creates waiting challenge");use(1);phase=3;}
   case 3->{if(!ack(1))return;duel=DuelSessions.get(a);check(duel!=null&&duel.stage==DuelSessions.Stage.CONFIRMING,"B receives consent GUI");command(1,"duelClick","button","decline");phase=4;}
   case 4->{if(!ack(1)||DuelSessions.get(a)!=null)return;check(duel.result==DuelSessions.Result.DECLINED,"B declines without battle");results.put("real_gui_decline",true);near(players);use(0);phase=20;}
   case 20->{if(!ack(0))return;check(DuelSessions.get(a)!=null&&DuelSessions.get(a).stage==DuelSessions.Stage.WAITING,"Repeat challenge starts after cleanup");use(1);phase=21;}
   case 21->{if(!ack(1))return;duel=DuelSessions.get(a);check(duel!=null&&duel.stage==DuelSessions.Stage.CONFIRMING&&!duel.acceptedA&&!duel.acceptedB,"Fresh session requires two fresh consents");command(1,"duelClick","button","accept");phase=22;}
   case 22->{if(!ack(1))return;check(duel.stage==DuelSessions.Stage.CONFIRMING&&duel.acceptedB&&!duel.acceptedA,"B cannot confirm for A");command(0,"duelClick","button","accept");phase=23;}
   case 23->{if(!ack(0))return;check(duel.acceptedA&&duel.acceptedB&&(duel.stage==DuelSessions.Stage.COUNTDOWN||duel.stage==DuelSessions.Stage.ACTIVE),"Two consents trigger one server countdown");if(duel.stage!=DuelSessions.Stage.ACTIVE)return;
    if(round==1){command(0,"duelFight");command(1,"duelFight");}
    else if(round==2){command(0,"wait");command(1,"duelSurrender");}
    else if(round==3){command(0,"duelBoundary","x",0,"z",0);command(1,"wait");}
    else if(round==4){command(0,"wait");command(1,"disconnect");}
    else if(round==5){command(0,"wait");command(1,"wait");s.getOverworld().breakBlock(stone,true);}
    else if(round==6){old=duel.id;write("duel-resume.json",Map.of("session",old,"aInventory",inventories[0],"bInventory",inventories[1],"aXp",xp[0],"bXp",xp[1]));write("duel-results.json",results);write("resume.json",Map.of("duel",true));for(int i=0;i<2;i++)command(i,"disconnect");phase=34;return;}
    phase=32;
   }
   case 32->{if(!ack(0)||!ack(1)||duel.stage!=DuelSessions.Stage.FINISHED&&duel.stage!=DuelSessions.Stage.CLEANUP)return;
    if(round==1){check(duel.result==DuelSessions.Result.KNOCKOUT&&duel.winner!=null,"Real100HP virtual knockout");var first=read(names[0]+".reply.json");var second=read(names[1]+".reply.json");check(first.get("betterCombat").getAsBoolean()&&second.get("betterCombat").getAsBoolean(),"Better Combat present on BOTH real clients");check(first.get("guiScale").getAsDouble()!=second.get("guiScale").getAsDouble(),"Different actual GUI scales on two native windows");results.put("different_gui_scales",List.of(first.get("guiScale"),second.get("guiScale")));results.put("better_combat_loaded_two_clients",true);results.put("real_knockout_health2_inventory_xp_crowns_unchanged",true);results.put("one_sees_other_poses",first.get("duelOtherPoses"));results.put("two_sees_other_poses",second.get("duelOtherPoses"));}
    else if(round==2){check(duel.result==DuelSessions.Result.SURRENDER&&a.getUuid().equals(duel.winner),"Real confirmed surrender awards opponent");results.put("real_surrender_and_rematch_without_restart",true);}
    else if(round==3){check(duel.result==DuelSessions.Result.BOUNDARY&&b.getUuid().equals(duel.winner),"Real movement beyond arena forfeits after grace");results.put("real_boundary_grace",true);}
    else if(round==5){check(duel.result==DuelSessions.Result.STONE_REMOVED,"Destroyed stone closes session");results.put("stone_destruction_cleanup",true);s.getOverworld().setBlockState(stone,DuelStone.BLOCK.getDefaultState());for(var item:s.getOverworld().getEntitiesByClass(net.minecraft.entity.ItemEntity.class,new Box(stone).expand(2),e->e.getStack().isOf(DuelStone.BLOCK.asItem())))item.discard();}
    stable(players);phase=33;
   }
   case 33->{if(DuelSessions.get(a)!=null||DuelSessions.get(b)!=null)return;stable(players);round++;near(players);use(0);phase=20;}
   case 90->{if(!ack(1))return;check(!s.isPvpEnabled()&&DuelSessions.get(a)==null&&DuelSessions.get(b)==null,"pvp=false safely refuses both without enabling global PvP");stable(players);results.put("pvp_false_safe_refusal_two_real_clients",true);finish(s);}
   case 200->{var r=read("duel-resume.json");old=UUID.fromString(r.get("session").getAsString());inventories[0]=r.get("aInventory").getAsString();inventories[1]=r.get("bInventory").getAsString();xp[0]=r.get("aXp").getAsInt();xp[1]=r.get("bXp").getAsInt();for(var e:read("duel-results.json").entrySet())results.put(e.getKey(),e.getValue());s.getOverworld().getChunk(0,0);check(s.getOverworld().getBlockState(stone).isOf(DuelStone.BLOCK)&&DuelSessions.get(a)==null&&DuelSessions.get(b)==null,"Stone survives restart, active sessions do not");stable(players);near(players);command(0,"duelReplay","session",old);command(1,"duelReplay","session",old);phase=201;}
   case 201->{if(!ack(0)||!ack(1))return;check(DuelSessions.get(a)==null&&DuelSessions.get(b)==null,"Old session packets after restart inert");use(0);phase=202;}
   case 202->{if(!ack(0))return;var fresh=DuelSessions.get(a);check(fresh!=null&&!fresh.id.equals(old)&&fresh.stage==DuelSessions.Stage.WAITING,"New challenge works after restart");command(0,"duelReplay","session",old);phase=203;}
   case 203->{if(!ack(0))return;check(DuelSessions.get(a).stage==DuelSessions.Stage.WAITING,"Old UUID cannot confirm or surrender new session");results.put("restart_stone_inventory_cleanup_and_stale_uuid",true);command(0,"duelClick","button","cancel");phase=204;}
   case 204->{if(!ack(0)||DuelSessions.get(a)!=null)return;stable(players);finish(s);}
  }
  if(ticks%100==0)write("heartbeat.json",Map.of("phase",phase,"round",round,"ticks",ticks));
 }catch(Exception e){try{write("server-failure.json",Map.of("phase",phase,"round",round,"error",e.toString()));}catch(Exception ignored){}phase=999;s.stop(false);}});
 }
}
