package com.witchersbestiary.client.board;

import com.google.gson.*;
import com.witchersbestiary.board.BoardScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import java.util.*;

public final class NoticeBoardScreen extends HandledScreen<BoardScreenHandler> {
 private static final Identifier TEXTURE=Identifier.of("witchersbestiary:textures/gui/notice_board.png");
 private final List<ButtonWidget> rows=new ArrayList<>();private ButtonWidget action,abandon;private UUID confirming;private Text confirmingName;private int selected,page,leftWidth;
 public NoticeBoardScreen(BoardScreenHandler h,PlayerInventory inventory,Text title){super(h,inventory,title);}
 private JsonArray offers(){return handler.snapshot.has("offers")?handler.snapshot.getAsJsonArray("offers"):new JsonArray();}
 private Text name(JsonObject row){return row.has("generated_name")?Text.translatable("hunt.witchersbestiary.proc.name_format",Text.translatable(row.get("generated_name").getAsString()),Text.translatable(row.get("generated_epithet").getAsString())):Text.translatable(row.get("name").getAsString());}
 private String status(JsonObject row){if(row.has("unavailable_reason"))return "unavailable";String state=row.get("state").getAsString();return state.equals("ACCEPTED")&&row.has("investigation_stage")&&!row.get("investigation_stage").getAsString().equals("searching")?"investigating":state.toLowerCase(Locale.ROOT);}
 private Text key(String suffix){return Text.translatable("gui.witchersbestiary.board."+suffix);}
 @Override protected void init(){
  backgroundWidth=Math.min(384,width-12);backgroundHeight=Math.min(240,height-12);super.init();leftWidth=backgroundWidth/3;rows.clear();action=null;abandon=null;
  if(confirming!=null){addDrawableChild(new JournalButton(x+leftWidth+15,y+backgroundHeight-40,(backgroundWidth-leftWidth-42)/2,24,key("cancel_confirm"),button->{net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.witchersbestiary.board.BoardCancel(handler.syncId,confirming));confirming=null;clearAndInit();}));addDrawableChild(new JournalButton(x+leftWidth+25+(backgroundWidth-leftWidth-42)/2,y+backgroundHeight-40,(backgroundWidth-leftWidth-42)/2,24,key("cancel_back"),button->{confirming=null;clearAndInit();}));return;}
  for(int i=0;i<3;i++){final int slot=i;rows.add(addDrawableChild(new JournalButton(x+9,y+34+i*50,leftWidth-18,37,Text.empty(),b->{selected=page*3+slot;refresh();})));}
  addDrawableChild(new JournalButton(x+9,y+backgroundHeight-35,24,22,Text.literal("<"),b->{if(page>0){page--;selected=page*3;refresh();}}));
  addDrawableChild(new JournalButton(x+leftWidth-33,y+backgroundHeight-35,24,22,Text.literal(">"),b->{if((page+1)*3<offers().size()){page++;selected=page*3;refresh();}}));
  action=addDrawableChild(new JournalButton(x+leftWidth+15,y+backgroundHeight-34,backgroundWidth-leftWidth-30,23,key("accept"),b->{
   if(client.interactionManager==null||selected>=offers().size())return;var row=offers().get(selected).getAsJsonObject();int button=row.get("button").getAsInt();if(row.get("state").getAsString().equals("COMPLETED"))button+=32;client.interactionManager.clickButton(handler.syncId,button);
  }));
  abandon=addDrawableChild(new JournalButton(x+leftWidth+15,y+backgroundHeight-57,backgroundWidth-leftWidth-30,20,key("abandon"),button->{if(selected>=offers().size())return;var row=offers().get(selected).getAsJsonObject();if(!row.has("instance")||!row.get("state").getAsString().equals("ACCEPTED"))return;confirming=UUID.fromString(row.get("instance").getAsString());confirmingName=name(row);clearAndInit();}));
  addDrawableChild(new JournalButton(x+9,y+backgroundHeight-60,leftWidth-18,20,key("refresh"),b->{if(client.interactionManager!=null)client.interactionManager.clickButton(handler.syncId,64);}));
  addDrawableChild(new JournalButton(x+36,y+backgroundHeight-35,leftWidth-72,22,key("legendary"),b->{if(client.interactionManager!=null){page=0;selected=0;client.interactionManager.clickButton(handler.syncId,65);}}));refresh();
 }
 public void refresh(){
  if(confirming!=null)return;var data=offers();if(selected>=data.size())selected=0;
  for(int i=0;i<rows.size();i++){int index=page*3+i;var button=rows.get(i);button.visible=index<data.size();if(button.visible)button.setMessage(name(data.get(index).getAsJsonObject()));}
  if(abandon!=null){abandon.visible=selected<data.size()&&data.get(selected).getAsJsonObject().get("state").getAsString().equals("ACCEPTED");}
  if(action!=null){action.active=false;if(selected<data.size()){String state=data.get(selected).getAsJsonObject().get("state").getAsString();action.active=!(handler.snapshot.has("preparing")&&handler.snapshot.get("preparing").getAsBoolean())&&state.equals("AVAILABLE")&&!data.get(selected).getAsJsonObject().has("unavailable_reason")||state.equals("COMPLETED");action.setMessage(key(data.get(selected).getAsJsonObject().has("unavailable_reason")?"unavailable":state.equals("AVAILABLE")?"accept":state.equals("COMPLETED")?"claim":state.toLowerCase(Locale.ROOT)));}}
 }
 private void wrapped(DrawContext c,Text text,int xx,int yy,int maxWidth,int maxLines,int color){var lines=textRenderer.wrapLines(text,maxWidth);for(int i=0;i<Math.min(maxLines,lines.size());i++)c.drawText(textRenderer,lines.get(i),xx,yy+i*11,color,false);}
 @Override protected void drawBackground(DrawContext c,float delta,int mouseX,int mouseY){c.drawTexture(TEXTURE,x,y,backgroundWidth,backgroundHeight,0f,0f,384,240,384,240);}
 @Override protected void drawForeground(DrawContext c,int mouseX,int mouseY){
  if(confirming!=null){wrapped(c,key("cancel_title"),12,20,leftWidth-24,5,0xfff0d49d);wrapped(c,confirmingName,leftWidth+15,20,backgroundWidth-leftWidth-30,4,0xff372617);wrapped(c,key("cancel_warning"),leftWidth+15,85,backgroundWidth-leftWidth-30,7,0xff7c2920);return;}
  c.drawText(textRenderer,key("title"),10,13,0xfff0d49d,false);if(handler.snapshot.has("refresh_days"))c.drawText(textRenderer,Text.translatable("gui.witchersbestiary.board.rotation",handler.snapshot.get("refresh_days").getAsInt()),10,25,0xffedd5aa,false);c.drawText(textRenderer,Text.literal((page+1)+" / "+Math.max(1,(offers().size()+2)/3)),leftWidth/2-12,backgroundHeight-26,0xffedd5aa,false);
  for(int i=0;i<3;i++){int n=page*3+i;if(n<offers().size())c.drawText(textRenderer,key(status(offers().get(n).getAsJsonObject())),11,73+i*50,0xffefcfa0,false);}
  if(selected>=offers().size()){wrapped(c,key("loading"),leftWidth+15,35,backgroundWidth-leftWidth-30,4,0xff382817);return;}
  var row=offers().get(selected).getAsJsonObject();int xx=leftWidth+14,ww=backgroundWidth-leftWidth-28;
  wrapped(c,name(row),xx,15,ww-24,2,0xff372617);
  var target=Identifier.of(row.get("target").getAsString());var iconId=Identifier.of(target.getNamespace(),target.getPath()+"_head");
  if(Registries.ITEM.containsId(iconId))c.drawItem(new ItemStack(Registries.ITEM.get(iconId)),xx,43);
  wrapped(c,Text.translatable("entity."+target.getNamespace()+"."+target.getPath()),xx+22,45,ww-22,2,0xff46321d);
  wrapped(c,Text.translatable(row.has("last_finding")?row.get("last_finding").getAsString():row.get("description").getAsString()),xx,70,ww,3,0xff49351e);
  if(row.has("rarity")){String rarity=row.get("rarity").getAsString();int color=switch(rarity){case "RARE"->0xff244b80;case "EPIC"->0xff702e83;case "LEGENDARY"->0xff8a5019;default->0xff49351e;};c.drawText(textRenderer,Text.translatable("hunt.witchersbestiary.proc.rarity."+rarity.toLowerCase(Locale.ROOT)),xx,108,color,false);}else c.drawText(textRenderer,key("difficulty").copy().append(": "+row.get("difficulty").getAsInt()+" / 5"),xx,108,0xff6c2c23,false);
  c.drawText(textRenderer,key("progress").copy().append(": "+row.get("progress").getAsInt()+" / "+row.get("required").getAsInt()),xx,122,0xff372617,false);
  var reward=row.getAsJsonObject("reward");var rewardText=key("reward").copy().append(": "+reward.get("experience").getAsInt()+" XP");
  for(var e:reward.getAsJsonArray("items")){var item=e.getAsJsonObject();var id=Identifier.of(item.get("item").getAsString());rewardText.append(", "+item.get("count").getAsInt()+" × ").append(Registries.ITEM.get(id).getName());}
  wrapped(c,rewardText,xx,139,ww,2,0xff49351e);
  if(row.has("recovery_unavailable"))wrapped(c,key("recovery_unavailable"),xx,162,ww,2,0xff7c2920);
  else if(row.has("hunt_dimension")&&client.world!=null&&!row.get("hunt_dimension").getAsString().equals(client.world.getRegistryKey().getValue().toString()))wrapped(c,Text.translatable("tracking.witchersbestiary.target_dimension",row.get("hunt_dimension").getAsString()),xx,162,ww,2,0xff49351e);
  else if(row.has("investigation_stage"))wrapped(c,Text.translatable("tracking.witchersbestiary.stage."+row.get("investigation_stage").getAsString()).append(row.get("clue_total").getAsInt()>0?" "+row.get("clues").getAsInt()+"/"+row.get("clue_total").getAsInt():"").append(row.has("hunt_area")?Text.literal(" · ").append(Text.translatable("hunt.witchersbestiary.area",row.get("hunt_area").getAsString())):Text.empty()),xx,162,ww,2,0xff49351e);
  else if(row.has("hunt_status"))wrapped(c,Text.translatable("hunt.witchersbestiary.status."+row.get("hunt_status").getAsString()).append(row.has("hunt_area")?Text.literal(" · ").append(Text.translatable("hunt.witchersbestiary.area",row.get("hunt_area").getAsString())):Text.empty()),xx,162,ww,2,0xff49351e);
  if(row.has("unavailable_reason"))wrapped(c,key("no_safe_route"),xx,174,ww,3,0xff7c2920);
  if(!row.has("unavailable_reason")&&!row.has("recovery_unavailable")&&(abandon==null||!abandon.visible)&&handler.snapshot.has("message")&&!handler.snapshot.get("message").getAsString().isEmpty()&&!handler.snapshot.get("message").getAsString().equals("updated"))wrapped(c,key(handler.snapshot.get("message").getAsString()),xx,backgroundHeight-60,ww,2,0xff7c2920);
 }
 @Override public void close(){if(confirming!=null){confirming=null;clearAndInit();}else super.close();}
 @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){super.render(c,mouseX,mouseY,delta);drawMouseoverTooltip(c,mouseX,mouseY);}
 private final class JournalButton extends ButtonWidget {
  JournalButton(int x,int y,int w,int h,Text text,PressAction press){super(x,y,w,h,text,press,DEFAULT_NARRATION_SUPPLIER);}
  @Override protected void renderWidget(DrawContext c,int mx,int my,float delta){int border=isHovered()||isFocused()?0xffd1a35b:0xff887249;c.fill(getX(),getY(),getX()+width,getY()+height,border);c.fill(getX()+1,getY()+1,getX()+width-1,getY()+height-1,active?0xff493120:0xff62503b);var text=textRenderer.trimToWidth(getMessage().getString(),width-10);c.drawCenteredTextWithShadow(textRenderer,text,getX()+width/2,getY()+(height-8)/2,active?0xfff2dcac:0xffc7b993);}
 }
}
