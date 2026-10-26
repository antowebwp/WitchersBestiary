package com.witchersbestiary.client.workshop;
import com.witchersbestiary.workshop.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.recipe.*;
import net.minecraft.text.Text;
import java.util.List;
public final class AlchemyScreen extends HandledScreen<AlchemyScreenHandler> {
 private List<RecipeEntry<CraftingRecipe>> recipes=List.of();
 public AlchemyScreen(AlchemyScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title);backgroundHeight=166;}
 @Override protected void init(){super.init();addDrawableChild(ButtonWidget.builder(Text.translatable("gui.witchersbestiary.process"),button->{if(client.interactionManager!=null)client.interactionManager.clickButton(handler.syncId,0);}).dimensions(x+54,y+56,116,20).build());if(client.world!=null)recipes=client.world.getRecipeManager().listAllOfType(RecipeType.CRAFTING).stream().filter(e->e.id().getNamespace().equals("witchersbestiary")&&e.id().getPath().startsWith("workshop/")).toList();}
 @Override protected void drawBackground(DrawContext c,float delta,int mouseX,int mouseY){c.fill(x,y,x+backgroundWidth,y+backgroundHeight,0xff272a29);c.fill(x+2,y+2,x+174,y+164,0xffccc3ac);c.fill(x+7,y+24,x+169,y+53,0xff514e44);for(var slot:handler.slots){int xx=x+slot.x,yy=y+slot.y;c.fill(xx-1,yy-1,xx+17,yy+17,0xff353733);c.fill(xx,yy,xx+16,yy+16,0xff8f8d7f);}if(client.world!=null)for(int i=0;i<Math.min(3,recipes.size());i++)c.drawItem(recipes.get(i).value().getResult(client.world.getRegistryManager()),x+8+i*15,y+58);}
 @Override protected void drawForeground(DrawContext c,int mouseX,int mouseY){c.drawText(textRenderer,title,8,6,0xff292a25,false);c.drawText(textRenderer,Text.translatable("gui.witchersbestiary.inputs"),8,17,0xff343830,false);c.drawText(textRenderer,Text.translatable("gui.witchersbestiary.result"),128,17,0xff343830,false);c.drawText(textRenderer,playerInventoryTitle,8,74,0xff343830,false);}
 @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){super.render(c,mouseX,mouseY,delta);drawMouseoverTooltip(c,mouseX,mouseY);if(mouseY>=y+57&&mouseY<y+75&&client.world!=null)for(int i=0;i<Math.min(3,recipes.size());i++)if(mouseX>=x+8+i*15&&mouseX<x+23+i*15){var lines=new java.util.ArrayList<Text>();lines.add(recipes.get(i).value().getResult(client.world.getRegistryManager()).getName());for(var ingredient:recipes.get(i).value().getIngredients()){var matching=ingredient.getMatchingStacks();if(matching.length>0)lines.add(matching[0].getName());}c.drawTooltip(textRenderer,lines,mouseX,mouseY);}}
}
