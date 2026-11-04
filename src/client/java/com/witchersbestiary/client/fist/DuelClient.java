package com.witchersbestiary.client.fist;
import com.witchersbestiary.fist.*;
import net.minecraft.client.*;import net.minecraft.client.option.KeyBinding;import net.minecraft.client.util.InputUtil;import net.minecraft.text.Text;
public final class DuelClient {
 private static DuelPackets.View view;private static KeyBinding menu;
 public static DuelPackets.View view(){return view;}
 public static void input(FistFightSession.Action action){if(view!=null)net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new DuelPackets.Input(view.session(),action.ordinal()));}
 public static Text menuHint(){return Text.translatable("duel.witchersbestiary.menu_hint",menu.getBoundKeyLocalizedText());}
 public static Text status(){if(view==null)return Text.empty();if(view.stage().equals("FINISHED"))return view.winner().isEmpty()?Text.translatable("duel.witchersbestiary.ended"):Text.translatable("duel.witchersbestiary.winner",view.winner());return Text.empty();}
 public static void initialize(){menu=net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(new KeyBinding("key.witchersbestiary.duel_menu",InputUtil.Type.KEYSYM,org.lwjgl.glfw.GLFW.GLFW_KEY_O,"category.witchersbestiary.fist"));
  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(DuelPackets.View.ID,(packet,ctx)->ctx.client().execute(()->{var c=ctx.client();var old=view;view=packet.present()?packet:null;if(view==null){if(c.currentScreen instanceof DuelScreen)c.setScreen(null);return;}boolean pending=packet.stage().equals("WAITING")||packet.stage().equals("CONFIRMING");if(pending&&(old==null||!old.session().equals(packet.session())||!old.stage().equals(packet.stage())))c.setScreen(new DuelScreen());else if(c.currentScreen instanceof DuelScreen&&(packet.stage().equals("FINISHED")||!pending&&old!=null&&(old.stage().equals("WAITING")||old.stage().equals("CONFIRMING"))))c.setScreen(null);else if(c.currentScreen instanceof DuelScreen screen)screen.update();}));
  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(c->{while(menu.wasPressed())if(view!=null&&c.currentScreen==null)c.setScreen(new DuelScreen());});
  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h,c)->view=null);net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((c,w)->view=null);
 }
}
