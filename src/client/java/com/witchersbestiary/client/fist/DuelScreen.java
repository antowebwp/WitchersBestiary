package com.witchersbestiary.client.fist;
import com.witchersbestiary.fist.*;
import net.minecraft.client.gui.*;import net.minecraft.client.gui.screen.Screen;import net.minecraft.client.gui.widget.ButtonWidget;import net.minecraft.text.Text;
public final class DuelScreen extends Screen {
 private boolean surrender;
 public DuelScreen(){super(Text.translatable("duel.witchersbestiary.title"));}
 private Text text(String key){return Text.translatable("duel.witchersbestiary."+key);}
 public void update(){clearAndInit();}
 private void send(int action){var v=DuelClient.view();if(v!=null)net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new DuelPackets.Control(v.session(),action));}
 @Override protected void init(){var v=DuelClient.view();if(v==null)return;int x=width/2-140,y=height/2;
  if(v.stage().equals("WAITING")){addDrawableChild(ButtonWidget.builder(text("cancel"),b->send(1)).dimensions(x,y+40,280,20).build());}
  else if(v.stage().equals("CONFIRMING")){var accept=addDrawableChild(ButtonWidget.builder(text(v.acceptedSelf()?"confirmed":"accept"),b->send(0)).dimensions(x,y+40,135,20).build());accept.active=!v.acceptedSelf();addDrawableChild(ButtonWidget.builder(text("decline"),b->send(1)).dimensions(x+145,y+40,135,20).build());}
  else if(v.stage().equals("ACTIVE")||v.stage().equals("COUNTDOWN")){addDrawableChild(ButtonWidget.builder(text(surrender?"surrender_confirm":"surrender"),b->{if(surrender)send(2);else{surrender=true;clearAndInit();}}).dimensions(x,y+40,135,20).build());addDrawableChild(ButtonWidget.builder(text("back"),b->close()).dimensions(x+145,y+40,135,20).build());}
  else addDrawableChild(ButtonWidget.builder(text("back"),b->close()).dimensions(x,y+40,280,20).build());
 }
 @Override public boolean shouldPause(){return false;}
 @Override public void render(DrawContext draw,int mouseX,int mouseY,float delta){draw.fill(width/2-152,height/2-83,width/2+152,height/2+79,0xf025201c);draw.fill(width/2-150,height/2-81,width/2+150,height/2-78,0xffa57d4c);super.render(draw,mouseX,mouseY,delta);draw.drawCenteredTextWithShadow(textRenderer,title,width/2,height/2-66,0xffe6ca9a);var v=DuelClient.view();if(v==null)return;Text line=v.stage().equals("WAITING")?Text.translatable("duel.witchersbestiary.waiting",v.seconds()):v.stage().equals("FINISHED")?DuelClient.status():Text.translatable("duel.witchersbestiary.opponent",v.opponent());draw.drawCenteredTextWithShadow(textRenderer,line,width/2,height/2-43,0xfff0d49d);Text warning=surrender?text("surrender_warning"):v.stage().equals("CONFIRMING")?text(v.acceptedOther()?"other_confirmed":"consent"):text("rules");var lines=textRenderer.wrapLines(warning,280);for(int i=0;i<Math.min(4,lines.size());i++)draw.drawTextWithShadow(textRenderer,lines.get(i),width/2-textRenderer.getWidth(lines.get(i))/2,height/2-18+i*11,0xffd2b99b);}
}
