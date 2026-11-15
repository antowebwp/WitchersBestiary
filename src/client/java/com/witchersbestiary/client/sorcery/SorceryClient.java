package com.witchersbestiary.client.sorcery;
import com.witchersbestiary.sorcery.*;import net.minecraft.client.*;import net.minecraft.client.gui.*;import net.minecraft.client.option.KeyBinding;import net.minecraft.client.util.InputUtil;import net.minecraft.client.render.*;import net.minecraft.text.Text;import net.fabricmc.fabric.api.client.networking.v1.*;import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;import net.fabricmc.fabric.api.client.rendering.v1.*;import java.util.*;
public final class SorceryClient {
 public static SorceryPackets.View view;private static long sequence;private static KeyBinding cast;
 public static void send(int action){var c=MinecraftClient.getInstance();if(view!=null&&c.world!=null)ClientPlayNetworking.send(new SorceryPackets.Intent(view.session(),++sequence,action));}
 public static void initialize(){cast=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.witchersbestiary.sorcery",InputUtil.Type.KEYSYM,org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,"key.categories.gameplay"));ClientPlayNetworking.registerGlobalReceiver(SorceryPackets.View.ID,(p,c)->c.client().execute(()->view=p));ClientPlayNetworking.registerGlobalReceiver(SorceryPackets.Effect.ID,(p,c)->{
  var world=c.client().world;
  if(world!=null)com.witchersbestiary.client.vfx.VfxManager.ice(p.entity(),p.ticks(),world.getTime());
 });ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{view=null;sequence=0;});ClientTickEvents.END_CLIENT_TICK.register(c->{while(cast.wasPressed())if(c.player!=null&&c.currentScreen==null&&c.isWindowFocused())send(1);});
  HudRenderCallback.EVENT.register((draw,tick)->{var c=MinecraftClient.getInstance();if(c.player==null||view==null||!view.learned()||c.currentScreen!=null||c.options.hudHidden||com.witchersbestiary.client.fist.FistClient.status()!=null)return;int y=c.getWindow().getScaledHeight()-69;draw.fill(6,y-3,190,y+24,0xa0182639);draw.drawTextWithShadow(c.textRenderer,Text.translatable("spell.witchersbestiary.icy_grasp"),12,y,0xff90dfff);draw.drawTextWithShadow(c.textRenderer,Text.translatable("sorcery.witchersbestiary.cost"),12,y+12,0xffbdd3e5);draw.drawTextWithShadow(c.textRenderer,String.format(java.util.Locale.ROOT,"%.1fs",view.cooldown()/20f),157,y+12,0xffeeeeff);});
 }
}
