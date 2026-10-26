package com.witchersbestiary.client.config;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
public final class BestiarySettingsScreen extends Screen{
 private final Screen parent;
 public BestiarySettingsScreen(Screen parent){super(Text.translatable("config.witchersbestiary.title"));this.parent=parent;}
 @Override protected void init(){addDrawableChild(ButtonWidget.builder(Text.translatable("config.witchersbestiary.progressive"),b->client.setScreen(new ProgressiveConfigScreen(this))).dimensions(width/2-140,height/2-10,280,20).build());addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"),b->close()).dimensions(width/2-75,height-30,150,20).build());}
 @Override public void render(DrawContext c,int x,int y,float delta){super.render(c,x,y,delta);c.drawCenteredTextWithShadow(textRenderer,title,width/2,20,0xffffff);}
 @Override public boolean shouldPause(){return false;}
 @Override public void close(){client.setScreen(parent);}
}
