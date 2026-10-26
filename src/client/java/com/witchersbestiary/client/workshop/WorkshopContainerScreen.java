package com.witchersbestiary.client.workshop;
import com.witchersbestiary.workshop.WorkshopContainerScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public final class WorkshopContainerScreen extends HandledScreen<WorkshopContainerScreenHandler>{
 public WorkshopContainerScreen(WorkshopContainerScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title);backgroundHeight=166;playerInventoryTitleY=74;}
 @Override protected void drawBackground(DrawContext c,float delta,int mx,int my){c.fill(x,y,x+176,y+166,0xff272a29);c.fill(x+2,y+2,x+174,y+164,0xffccc3ac);for(var slot:handler.slots){int xx=x+slot.x,yy=y+slot.y;c.fill(xx-1,yy-1,xx+17,yy+17,0xff353733);c.fill(xx,yy,xx+16,yy+16,0xff8f8d7f);}}
 @Override public void render(DrawContext c,int mx,int my,float delta){super.render(c,mx,my,delta);drawMouseoverTooltip(c,mx,my);}
}
