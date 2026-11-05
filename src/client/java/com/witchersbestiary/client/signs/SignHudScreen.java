package com.witchersbestiary.client.signs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
public final class SignHudScreen extends Screen {
 private ButtonWidget positionButton;private final Screen parent; private boolean dragging;private double dx,dy;
 public SignHudScreen(){super(Text.translatable("fist.hud.title"));this.parent=null;}
 protected void init(){positionButton=addDrawableChild(ButtonWidget.builder(position(),b->{SignHudSettings.corner=(SignHudSettings.corner+1)%5;b.setMessage(position());}).dimensions(width/2-100,height-96,200,20).build());addDrawableChild(ButtonWidget.builder(Text.translatable("fist.hud.scale"),b->{SignHudSettings.scale=SignHudSettings.scale>=1.49?.5:SignHudSettings.scale+.1;}).dimensions(width/2-100,height-73,98,20).build());addDrawableChild(ButtonWidget.builder(Text.translatable("fist.hud.reset"),b->{SignHudSettings.corner=0;SignHudSettings.scale=1;SignHudSettings.x=.8;SignHudSettings.y=.15;clearAndInit();}).dimensions(width/2+2,height-73,98,20).build());addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"),b->close()).dimensions(width/2-100,height-50,200,20).build());}
 private Text position(){return Text.translatable("fist.hud.position."+SignHudSettings.corner);}
 public void render(DrawContext c,int mx,int my,float delta){super.render(c,mx,my,delta);SignsClient.drawHud(c,client);var b=SignHudSettings.bounds(width,height);c.drawCenteredTextWithShadow(textRenderer,Text.translatable("fist.hud.scale_value",(int)Math.round(SignHudSettings.scale*100)),width/2,height-112,0xffffff);}
 public boolean mouseClicked(double x,double y,int button){if(super.mouseClicked(x,y,button))return true;var b=SignHudSettings.bounds(width,height);if(button==0&&x>=b[0]&&x<b[0]+b[2]&&y>=b[1]&&y<b[1]+b[3]){dragging=true;dx=x-b[0];dy=y-b[1];SignHudSettings.corner=4;positionButton.setMessage(position());return true;}return false;}
 public boolean mouseDragged(double x,double y,int button,double vx,double vy){if(dragging&&button==0){var b=SignHudSettings.bounds(width,height);SignHudSettings.x=Math.clamp((x-dx)/Math.max(1,width-b[2]),0,1);SignHudSettings.y=Math.clamp((y-dy)/Math.max(1,height-48-b[3]),0,1);return true;}return super.mouseDragged(x,y,button,vx,vy);}
 public boolean mouseReleased(double x,double y,int button){dragging=false;return super.mouseReleased(x,y,button);}
 public boolean shouldPause(){return false;}
 public void close(){SignHudSettings.save();client.setScreen(parent);}
 public void removed(){SignHudSettings.save();}
}
