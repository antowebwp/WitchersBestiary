package com.witchersbestiary.client.config;
import com.witchersbestiary.config.*;
import com.google.gson.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.command.v2.*;
public final class ProgressiveConfigClient {
 public record View(long revision,boolean editable,String status,ProgressiveOptions options){}
 public static View view;
 private static boolean openRequested;
 public static boolean connected(){return net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler()!=null&&ClientPlayNetworking.canSend(ProgressiveConfigNetwork.Edit.ID);}
 public static void initialize(){
  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(c->{if(openRequested){openRequested=false;c.setScreen(new BestiarySettingsScreen(null));}});
  ClientPlayNetworking.registerGlobalReceiver(ProgressiveConfigNetwork.Snapshot.ID,(packet,ctx)->ctx.client().execute(()->{var j=JsonParser.parseString(packet.json()).getAsJsonObject();view=new View(j.get("revision").getAsLong(),j.get("editable").getAsBoolean(),j.get("status").getAsString(),ProgressiveOptions.parse(j.getAsJsonObject("values")));if(ctx.client().currentScreen instanceof ProgressiveConfigScreen screen)screen.receive(view);}));
  ClientPlayConnectionEvents.DISCONNECT.register((h,c)->view=null);
  ClientCommandRegistrationCallback.EVENT.register((dispatcher,registry)->dispatcher.register(ClientCommandManager.literal("wbconfig").executes(ctx->{openRequested=true;return 1;})));
 }
 public static void request(){if(connected())ClientPlayNetworking.send(new ProgressiveConfigNetwork.Edit(""));}
 public static void save(ProgressiveOptions options,long revision){if(!connected())return;var j=new JsonObject();j.addProperty("revision",revision);j.add("values",options.json());ClientPlayNetworking.send(new ProgressiveConfigNetwork.Edit(j.toString()));}
}
