package com.witchersbestiary.client.signs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
public final class SignHudScreen extends Screen {
 private ButtonWidget positionButton;private final Screen parent; private boolean dragging;private double dx,dy;
 public SignHudScreen(){super(Text.translatable("sign.hud.title"));this.parent=null;}
 protected void init(){
  int controlWidth=Math.min(200,width-12), left=(width-controlWidth)/2;
  positionButton=addDrawableChild(ButtonWidget.builder(position(),b->{SignHudSettings.corner=(SignHudSettings.corner+1)%5;b.setMessage(position());}).dimensions(left,height-96,controlWidth,20).build());
  int half=(controlWidth-4)/2;
  addDrawableChild(ButtonWidget.builder(Text.translatable("sign.hud.scale_down"),b->adjustScale(-1)).dimensions(left,height-73,half,20).build());
  addDrawableChild(ButtonWidget.builder(Text.translatable("sign.hud.scale_up"),b->adjustScale(1)).dimensions(left+half+4,height-73,half,20).build());
  addDrawableChild(ButtonWidget.builder(Text.translatable("sign.hud.reset"),b->{SignHudSettings.reset();SignHudSettings.save();clearAndInit();}).dimensions(left,height-50,half,20).build());
  addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"),b->close()).dimensions(left+half+4,height-50,half,20).build());
 }
 private void adjustScale(int direction){
  int steps=(int)Math.round(SignHudSettings.scale*20);
  SignHudSettings.scale=Math.clamp(steps+direction,10,40)/20.0;
 }
 private Text position(){return Text.translatable("sign.hud.position."+SignHudSettings.corner);}
 @Override public void renderBackground(DrawContext c,int mx,int my,float delta){
  super.renderBackground(c,mx,my,delta);
  SignsClient.drawHud(c,client);
 }
 public void render(DrawContext c,int mx,int my,float delta){super.render(c,mx,my,delta);c.drawCenteredTextWithShadow(textRenderer,Text.translatable("sign.hud.scale_value",(int)Math.round(SignHudSettings.scale*100)),width/2,height-112,0xffffff);}
 public boolean mouseClicked(double x,double y,int button){if(super.mouseClicked(x,y,button))return true;var b=SignHudSettings.bounds(width,height);if(button==0&&x>=b[0]&&x<b[0]+b[2]&&y>=b[1]&&y<b[1]+b[3]){dragging=true;dx=x-b[0];dy=y-b[1];SignHudSettings.x=b[0]/(double)Math.max(1,width-b[2]);SignHudSettings.y=b[1]/(double)Math.max(1,height-48-b[3]);SignHudSettings.corner=4;positionButton.setMessage(position());return true;}return false;}
 public boolean mouseDragged(double x,double y,int button,double vx,double vy){if(dragging&&button==0){var b=SignHudSettings.bounds(width,height);SignHudSettings.x=Math.clamp((x-dx)/Math.max(1,width-b[2]),0,1);SignHudSettings.y=Math.clamp((y-dy)/Math.max(1,height-48-b[3]),0,1);return true;}return super.mouseDragged(x,y,button,vx,vy);}
 public boolean mouseReleased(double x,double y,int button){dragging=false;return super.mouseReleased(x,y,button);}
 public boolean shouldPause(){return false;}
 public void close(){SignHudSettings.save();client.setScreen(parent);}
 public void removed(){SignHudSettings.save();}
}
