package com.witchersbestiary.client.signs;

import com.witchersbestiary.signs.*;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class SignMasteryScreen extends Screen {
    private final Screen parent;
    private SignType sign;
    private int tab;
    private SignPackets.Mastery displayed;

    public SignMasteryScreen(Screen parent, SignType sign) {
        super(Text.translatable("sign.mastery.open"));
        this.parent=parent;
        this.sign=sign;
    }

    @Override protected void init() {
        displayed=SignsClient.mastery;
        int w=Math.min(330,width-12), x=(width-w)/2, y=top();
        for(var branch:SignType.values()){
            var label=Text.translatable("sign.witchersbestiary."+branch.name().toLowerCase(Locale.ROOT));
            button(label,x+branch.ordinal()*(w/5),y,w/5-2,()->{sign=branch;clearAndInit();}).active=branch!=sign;
        }
        String[] tabs={"upgrades","forms","loadout"};
        for(int i=0;i<3;i++){
            int selected=i;
            button(Text.translatable("sign.mastery."+tabs[i]),x+i*(w/3),y+23,w/3-2,()->{tab=selected;clearAndInit();}).active=i!=tab;
        }
        if(displayed!=null)populate(x,y+49,w);
        button(Text.translatable("gui.done"),x,Math.min(height-21,y+135),w,this::close);
    }

    private void populate(int x,int y,int width) {
        int branch=sign.ordinal();
        boolean learned=SignsClient.view!=null&&(SignsClient.view.learned()&(1<<branch))!=0;
        if(tab==0) {
            for(var node:SignPlayerState.Upgrade.values()){
                int i=node.ordinal(),level=displayed.level(branch,i),cost=2+level*2;
                String key="sign.mastery.node."+node.name().toLowerCase(Locale.ROOT);
                var label=Text.translatable("sign.mastery.upgrade_row",Text.translatable(key),level,level==3?"–":cost);
                var widget=button(label,x+(i%2)*(width/2),y+(i/2)*22,width/2-2,()->SignsClient.send(3,branch*16+i));
                widget.active=learned&&level<3&&displayed.points()>=cost;
                widget.setTooltip(Tooltip.of(Text.translatable(key+".tip")));
            }
        } else if(tab==1) {
            int total=0;for(int i=0;i<6;i++)total+=displayed.level(branch,i);
            for(int form=0;form<4;form++){
                int choice=form;
                boolean unlocked=(displayed.unlocked(branch)&(1<<form))!=0;
                var label=Text.translatable(unlocked?"sign.mastery.known":"sign.mastery.unlock",formName(sign,form),form+2,form*2);
                var widget=button(label,x,y+form*21,width,()->SignsClient.send(4,branch*16+choice));
                widget.active=learned&&!unlocked&&total>=form*2&&displayed.points()>=form+2;
                widget.setTooltip(Tooltip.of(Text.translatable(formKey(sign,form)+".tip")));
            }
        } else {
            for(int slot=0;slot<2;slot++){
                int index=slot, form=displayed.form(branch,slot);
                var widget=button(Text.translatable("sign.mastery.slot."+slot,formName(sign,form)),x,y+slot*23,width,()->{
                    int next=form;
                    do {next=(next+1)%4;}while((displayed.unlocked(branch)&(1<<next))==0);
                    SignsClient.send(5,branch*16+index*4+next);
                });
                widget.active=learned&&displayed.unlocked(branch)!=1;
                widget.setTooltip(Tooltip.of(Text.translatable(formKey(sign,form)+".tip")));
            }
        }
    }

    private ButtonWidget button(Text label,int x,int y,int w,Runnable press){
        return addDrawableChild(ButtonWidget.builder(label,b->press.run()).dimensions(x,y,w,20).build());
    }
    private int top(){return Math.max(25,(height-190)/2);}
    private static String formKey(SignType sign,int form){return "sign.mastery.form."+sign.name().toLowerCase(Locale.ROOT)+"."+form;}
    public static Text formName(SignType sign,int form){return Text.translatable(formKey(sign,form));}
    @Override public void tick(){if(displayed!=SignsClient.mastery)clearAndInit();}
    @Override public void render(DrawContext draw,int x,int y,float delta){
        super.render(draw,x,y,delta);
        draw.drawCenteredTextWithShadow(textRenderer,title,width/2,top()-23,0xffe4cc98);
        if(displayed!=null)draw.drawCenteredTextWithShadow(textRenderer,Text.translatable("sign.mastery.points",displayed.points()),width/2,top()-12,0xffffffff);
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void close(){client.setScreen(parent);}
}
