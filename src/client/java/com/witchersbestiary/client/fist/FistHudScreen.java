package com.witchersbestiary.client.fist;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
public final class FistHudScreen extends Screen {
 private ButtonWidget positionButton;private final Screen parent; private boolean dragging;private double dx,dy;
 public FistHudScreen(Screen parent){super(Text.translatable("fist.hud.title"));this.parent=parent;}
 protected void init(){positionButton=addDrawableChild(ButtonWidget.builder(position(),b->{FistHudSettings.corner=(FistHudSettings.corner+1)%5;b.setMessage(position());}).dimensions(width/2-100,height-96,200,20).build());addDrawableChild(ButtonWidget.builder(Text.translatable("fist.hud.scale"),b->{FistHudSettings.scale=FistHudSettings.scale>=1.49?.5:FistHudSettings.scale+.1;}).dimensions(width/2-100,height-73,98,20).build());addDrawableChild(ButtonWidget.builder(Text.translatable("fist.hud.reset"),b->{FistHudSettings.corner=0;FistHudSettings.scale=1;FistHudSettings.x=.8;FistHudSettings.y=.15;clearAndInit();}).dimensions(width/2+2,height-73,98,20).build());addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"),b->close()).dimensions(width/2-100,height-50,200,20).build());}
 private Text position(){return Text.translatable("fist.hud.position."+FistHudSettings.corner);}
 public void render(DrawContext c,int mx,int my,float delta){super.render(c,mx,my,delta);var b=FistHudSettings.bounds(width,height);c.fill(b[0],b[1],b[0]+b[2],b[1]+b[3],0xd0201814);c.drawTextWithShadow(textRenderer,Text.translatable("fist.hud.preview"),b[0]+4,b[1]+4,0xffe6ca9a);c.drawCenteredTextWithShadow(textRenderer,Text.translatable("fist.hud.scale_value",(int)Math.round(FistHudSettings.scale*100)),width/2,height-112,0xffffff);}
 public boolean mouseClicked(double x,double y,int button){if(super.mouseClicked(x,y,button))return true;var b=FistHudSettings.bounds(width,height);if(button==0&&x>=b[0]&&x<b[0]+b[2]&&y>=b[1]&&y<b[1]+b[3]){dragging=true;dx=x-b[0];dy=y-b[1];FistHudSettings.corner=4;positionButton.setMessage(position());return true;}return false;}
 public boolean mouseDragged(double x,double y,int button,double vx,double vy){if(dragging&&button==0){var b=FistHudSettings.bounds(width,height);FistHudSettings.x=Math.clamp((x-dx)/Math.max(1,width-b[2]),0,1);FistHudSettings.y=Math.clamp((y-dy)/Math.max(1,height-48-b[3]),0,1);return true;}return super.mouseDragged(x,y,button,vx,vy);}
 public boolean mouseReleased(double x,double y,int button){dragging=false;return super.mouseReleased(x,y,button);}
 public boolean shouldPause(){return false;}
 public void close(){FistHudSettings.save();client.setScreen(parent);}
 public void removed(){FistHudSettings.save();}
}
