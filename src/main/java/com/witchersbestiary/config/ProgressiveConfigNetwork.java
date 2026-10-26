package com.witchersbestiary.config;
import com.google.gson.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.*;

/** Server-authoritative editing. The client only receives a display snapshot. */
public final class ProgressiveConfigNetwork {
 public record Edit(String json) implements CustomPayload{
  public static final Id<Edit> ID=new Id<>(Identifier.of("witchersbestiary","progressive_edit"));
  public static final PacketCodec<RegistryByteBuf,Edit> CODEC=PacketCodec.of((p,b)->b.writeString(p.json,2048),b->new Edit(b.readString(2048)));
  public Id<Edit> getId(){return ID;}
 }
 public record Snapshot(String json) implements CustomPayload{
  public static final Id<Snapshot> ID=new Id<>(Identifier.of("witchersbestiary","progressive_snapshot"));
  public static final PacketCodec<RegistryByteBuf,Snapshot> CODEC=PacketCodec.of((p,b)->b.writeString(p.json,4096),b->new Snapshot(b.readString(4096)));
  public Id<Snapshot> getId(){return ID;}
 }
 private static long revision;
 private static final Map<UUID,Integer> lastEdit=new HashMap<>();
 private ProgressiveConfigNetwork(){}
 public static boolean canEdit(ServerPlayerEntity p){var s=p.getServer();return p.hasPermissionLevel(2)||s.isSingleplayer()&&s.isHost(p.getGameProfile());}
 public static long revision(){return revision;}
 /** Called on the server thread, including by the real packet receiver. No disk work before authorization. */
 public static String apply(ServerPlayerEntity p,String json){
  if(!canEdit(p))return "forbidden";
  try{
   // The protocol contains only a root object and nine primitive options, never arrays.
   if(json.length()>2048||json.indexOf('[')>=0||json.chars().filter(ch->ch=='{').count()>2)return "invalid";
   var root=JsonParser.parseString(json).getAsJsonObject();if(root.size()!=2)throw new IllegalArgumentException();
   long expected=root.getAsJsonPrimitive("revision").getAsBigDecimal().longValueExact();if(expected!=revision)return "conflict";
   var options=ProgressiveOptions.parse(root.getAsJsonObject("values"));int now=p.getServer().getTicks();var last=lastEdit.get(p.getUuid());if(last!=null&&now-last<10)return "busy";
   ModConfig.saveProgressive(options);revision++;lastEdit.put(p.getUuid(),now);return "saved";
  }catch(java.io.IOException e){com.witchersbestiary.WitchersBestiary.LOGGER.warn("Could not save progressive difficulty",e);return "save_error";}
   catch(RuntimeException e){return "invalid";}
 }
 public static Snapshot snapshot(ServerPlayerEntity p,String status){var j=new JsonObject();j.addProperty("revision",revision);j.addProperty("editable",canEdit(p));j.addProperty("status",status);j.add("values",ProgressiveOptions.from(ModConfig.progressive()).json());return new Snapshot(j.toString());}
 private static void send(ServerPlayerEntity p,String status){if(ServerPlayNetworking.canSend(p,Snapshot.ID))ServerPlayNetworking.send(p,snapshot(p,status));}
 public static void initialize(){
  PayloadTypeRegistry.playC2S().register(Edit.ID,Edit.CODEC);PayloadTypeRegistry.playS2C().register(Snapshot.ID,Snapshot.CODEC);
  ServerPlayNetworking.registerGlobalReceiver(Edit.ID,(packet,ctx)->ctx.server().execute(()->{
   if(packet.json.isEmpty()){send(ctx.player(),"ready");return;}
   String result=apply(ctx.player(),packet.json);if(result.equals("saved"))for(var p:ctx.server().getPlayerManager().getPlayerList())send(p,p==ctx.player()?"saved":"updated");else send(ctx.player(),result);
  }));
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->send(handler.player,"ready"));
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->lastEdit.remove(handler.player.getUuid()));
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{revision=0;lastEdit.clear();});
 }
}
