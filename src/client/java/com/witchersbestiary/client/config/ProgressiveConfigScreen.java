package com.witchersbestiary.client.config;
import com.witchersbestiary.config.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import java.util.*;

public final class ProgressiveConfigScreen extends Screen {
 private final Screen parent;private final List<TextFieldWidget> fields=new ArrayList<>();
 private ButtonWidget toggle,save,defaults;private boolean enabled,editable,dirty,pending,inputInvalid;private long revision;private String status="loading";private int top,row,left;
 public ProgressiveConfigScreen(Screen parent){super(Text.translatable("config.witchersbestiary.progressive"));this.parent=parent;}
 public static String format(double n){return java.math.BigDecimal.valueOf(n).stripTrailingZeros().toPlainString();}
 @Override protected void init(){
  fields.clear();top=48;row=Math.min(24,Math.max(16,(height-105)/9));left=Math.max(10,width/2-170);int fieldX=Math.min(width-100,width/2+65);
  toggle=addDrawableChild(ButtonWidget.builder(Text.empty(),b->{enabled=!enabled;dirty=true;update();}).dimensions(fieldX,top,90,row-2).build());toggle.setTooltip(Tooltip.of(Text.translatable("config.witchersbestiary.enabled.tip")));
  for(int i=0;i<ProgressiveOptions.FIELDS.size();i++){var f=ProgressiveOptions.FIELDS.get(i);var box=new TextFieldWidget(textRenderer,fieldX,top+(i+1)*row,90,row-2,Text.translatable("config.witchersbestiary."+f.key()));box.setMaxLength(20);box.setChangedListener(v->{dirty=true;update();});box.setTooltip(Tooltip.of(Text.translatable("config.witchersbestiary."+f.key()+".tip",format(f.min()*f.scale()),format(f.max()*f.scale()),format(f.fallback()*f.scale()))));fields.add(addDrawableChild(box));}
  int by=height-27;defaults=addDrawableChild(ButtonWidget.builder(Text.translatable("config.witchersbestiary.defaults"),b->{populate(ProgressiveOptions.defaults());dirty=true;update();}).dimensions(width/2-151,by,96,20).build());
  save=addDrawableChild(ButtonWidget.builder(Text.translatable("config.witchersbestiary.save"),b->{try{var options=options();pending=true;status="saving";update();ProgressiveConfigClient.save(options,revision);}catch(RuntimeException e){status="invalid";update();}}).dimensions(width/2-48,by,96,20).build());
  addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"),b->close()).dimensions(width/2+55,by,96,20).build());
  if(ProgressiveConfigClient.connected()){if(ProgressiveConfigClient.view!=null)receive(ProgressiveConfigClient.view);else populate(ProgressiveOptions.from(ModConfig.progressive()));ProgressiveConfigClient.request();}else{editable=false;status="offline";populate(ProgressiveOptions.from(ModConfig.progressive()));}update();
 }
 public ProgressiveOptions options(){var nums=new ArrayList<Double>();for(int i=0;i<fields.size();i++)nums.add(Double.parseDouble(fields.get(i).getText().replace(',','.'))/ProgressiveOptions.FIELDS.get(i).scale());return new ProgressiveOptions(enabled,nums);}
 private void populate(ProgressiveOptions options){enabled=options.enabled();for(int i=0;i<fields.size();i++)fields.get(i).setText(java.math.BigDecimal.valueOf(options.values().get(i)).multiply(java.math.BigDecimal.valueOf(ProgressiveOptions.FIELDS.get(i).scale())).stripTrailingZeros().toPlainString());dirty=false;}
 public void receive(ProgressiveConfigClient.View view){
  editable=view.editable();String result=view.status();
  boolean preserve=dirty&&editable&&(!pending&&(result.equals("ready")||result.equals("updated"))||result.equals("busy")||result.equals("save_error")||result.equals("conflict"));
  if(preserve){status=view.revision()!=revision?"conflict":result;}
  else{revision=view.revision();populate(view.options());status=result.equals("ready")&&!editable?"readonly":result;}
  pending=false;update();
 }
 private void update(){
  if(toggle==null||save==null)return;toggle.setMessage(Text.translatable(enabled?"options.on":"options.off"));boolean valid=true;try{options();}catch(RuntimeException e){valid=false;}
  inputInvalid=!valid;
  for(int i=0;i<fields.size();i++){boolean good=true;try{double n=Double.parseDouble(fields.get(i).getText().replace(',','.'))/ProgressiveOptions.FIELDS.get(i).scale();var f=ProgressiveOptions.FIELDS.get(i);good=Double.isFinite(n)&&n>=f.min()&&n<=f.max()&&(!f.integer()||n==Math.rint(n));}catch(RuntimeException e){good=false;}fields.get(i).setEditableColor(good?0xffffff:0xff6666);}
  toggle.active=defaults.active=editable&&!pending;for(var f:fields)f.setEditable(editable&&!pending);save.active=editable&&!pending&&valid&&dirty;
 }
 @Override public void render(DrawContext c,int x,int y,float delta){super.render(c,x,y,delta);c.drawCenteredTextWithShadow(textRenderer,title,width/2,12,0xffffff);
  String info=Text.translatable("config.witchersbestiary.status."+(inputInvalid?"invalid":status)).getString();c.drawCenteredTextWithShadow(textRenderer,textRenderer.trimToWidth(info,width-20),width/2,29,status.equals("invalid")||status.equals("save_error")?0xff8888:0xbbbbbb);
  c.drawTextWithShadow(textRenderer,Text.translatable("config.witchersbestiary.enabled"),left,top+3,0xffffff);
  for(int i=0;i<fields.size();i++){var key=ProgressiveOptions.FIELDS.get(i).key();String label=Text.translatable("config.witchersbestiary."+key).getString();c.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(label,fields.get(i).getX()-left-8),left,top+(i+1)*row+3,0xffffff);}
  c.drawCenteredTextWithShadow(textRenderer,Text.translatable("config.witchersbestiary.live"),width/2,height-42,0xaaaaaa);
 }
 @Override public boolean shouldPause(){return false;}
 @Override public void close(){client.setScreen(parent);}
}
