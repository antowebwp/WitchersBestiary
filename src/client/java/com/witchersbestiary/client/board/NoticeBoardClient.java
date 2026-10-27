package com.witchersbestiary.client.board;
import com.google.gson.JsonParser;
import com.witchersbestiary.board.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
public final class NoticeBoardClient {
 public static void initialize(){
  HandledScreens.register(NoticeBoards.SCREEN,NoticeBoardScreen::new);
  ClientPlayNetworking.registerGlobalReceiver(BoardSnapshot.ID,(packet,ctx)->ctx.client().execute(()->{
   if(ctx.player().currentScreenHandler instanceof BoardScreenHandler h&&h.syncId==packet.syncId()){
    h.snapshot=JsonParser.parseString(packet.json()).getAsJsonObject();
    if(ctx.client().currentScreen instanceof NoticeBoardScreen screen)screen.refresh();
   }
  }));
 }
}
