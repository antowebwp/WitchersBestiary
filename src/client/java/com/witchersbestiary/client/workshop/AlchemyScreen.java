package com.witchersbestiary.client.workshop;
import com.witchersbestiary.workshop.*;
import com.witchersbestiary.alchemy.WeaponOilItem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.recipe.*;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import java.util.*;
/** RecipeManager browser. All writes go through server ScreenHandler validation. */
public final class AlchemyScreen extends HandledScreen<AlchemyScreenHandler> {
 private List<RecipeEntry<CraftingRecipe>> recipes=List.of();private int selected=-1,page;private int category;private ButtonWidget brew;
 private Text text(String key){return Text.translatable("alchemy.witchersbestiary."+key);}
 public AlchemyScreen(AlchemyScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title);backgroundWidth=300;backgroundHeight=238;}
 private int categoryOf(ItemStack stack){return stack.getItem() instanceof WeaponOilItem?1:AlchemyRecipes.consumable(stack)?0:2;}
 private List<Integer> filtered(){var list=new ArrayList<Integer>();for(int i=0;i<recipes.size();i++)if(categoryOf(recipes.get(i).value().getResult(client.world.getRegistryManager()))==category)list.add(i);return list;}
 private ButtonWidget button(Text label,int xx,int yy,int w,int h,Runnable run){return addDrawableChild(ButtonWidget.builder(label,b->run.run()).dimensions(x+xx,y+yy,w,h).build());}
 @Override protected void init(){super.init();if(client.world==null)return;recipes=AlchemyRecipes.catalog(client.world);var visible=filtered();page=Math.clamp(page,0,Math.max(0,(visible.size()-1)/5));if(!visible.contains(selected))selected=visible.isEmpty()?-1:visible.getFirst();
  button(text(new String[]{"elixirs","oils","components"}[category]).copy().append(" >"),6,18,88,18,()->{category=(category+1)%3;page=0;selected=-1;clearAndInit();});
  for(int row=0;row<5&&page*5+row<visible.size();row++){int index=visible.get(page*5+row);var result=recipes.get(index).value().getResult(client.world.getRegistryManager());button(Text.literal((index==selected?"> ":"")+textRenderer.trimToWidth(result.getName().getString(),72)),6,39+row*14,88,14,()->{selected=index;clearAndInit();}).setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(result.getName()));}
  button(Text.literal("<"),6,116,26,18,()->{page=Math.max(0,page-1);clearAndInit();});button(Text.literal(">"),68,116,26,18,()->{page=Math.max(0,Math.min((visible.size()-1)/5,page+1));clearAndInit();});
  brew=button(text("brew"),100,100,192,18,()->{if(selected>=0&&client.interactionManager!=null)client.interactionManager.clickButton(handler.syncId,100+selected);});
 }
 @Override protected void handledScreenTick(){super.handledScreenTick();if(brew==null||client.world==null||client.player==null)return;var table=new net.minecraft.inventory.SimpleInventory(4);for(int i=0;i<4;i++)table.setStack(i,handler.slots.get(i).getStack().copy());brew.active=selected>=0&&AlchemyRecipes.plan(client.world,table,client.player,recipes.get(selected))!=null;brew.setTooltip(brew.active?null:net.minecraft.client.gui.tooltip.Tooltip.of(text("unavailable")));}
 private ItemStack result(){return selected<0?ItemStack.EMPTY:recipes.get(selected).value().getResult(client.world.getRegistryManager());}
 private String key(Ingredient ingredient){return Arrays.stream(ingredient.getMatchingStacks()).map(s->net.minecraft.registry.Registries.ITEM.getId(s.getItem()).toString()).sorted().collect(java.util.stream.Collectors.joining(","));}
 private List<Ingredient> displayIngredients(){var unique=new LinkedHashMap<String,Ingredient>();for(var ingredient:recipes.get(selected).value().getIngredients())if(!ingredient.isEmpty())unique.putIfAbsent(key(ingredient),ingredient);return new ArrayList<>(unique.values());}
 private int needed(Ingredient ingredient){return (int)recipes.get(selected).value().getIngredients().stream().filter(g->key(g).equals(key(ingredient))).count();}
 private int have(Ingredient ingredient){int have=0;for(int slot=0;slot<handler.slots.size();slot++)if(slot!=3&&ingredient.test(handler.slots.get(slot).getStack()))have+=handler.slots.get(slot).getStack().getCount();return have;}
 @Override protected void drawBackground(DrawContext c,float delta,int mx,int my){c.fill(x,y,x+300,y+238,0xff31271f);c.fill(x+2,y+2,x+298,y+236,0xff4a3b2c);c.fill(x+4,y+4,x+96,y+148,0xff201d19);c.fill(x+98,y+18,x+294,y+98,0xff292b29);for(int xx:new int[]{2,296})for(int yy:new int[]{2,234})c.fill(x+xx,y+yy,x+xx+2,y+yy+2,0xffb29a6e);
  for(var slot:handler.slots){int xx=x+slot.x,yy=y+slot.y;c.fill(xx-1,yy-1,xx+17,yy+17,0xffaa926b);c.fill(xx,yy,xx+16,yy+16,0xff24211d);}
  if(selected>=0){var output=result();c.drawItem(output,x+100,y+19);c.drawItemInSlot(textRenderer,output,x+100,y+19);c.drawText(textRenderer,textRenderer.trimToWidth(output.getName().getString(),172),x+120,y+23,0xffead5ad,false);c.drawText(textRenderer,textRenderer.trimToWidth(AlchemyRecipes.description(output).getString(),188),x+102,y+36,0xffb8cd8a,false);var ingredients=displayIngredients();for(int i=0;i<ingredients.size();i++){var ingredient=ingredients.get(i);var matching=ingredient.getMatchingStacks();if(matching.length==0)continue;int xx=x+102+(i%5)*38,yy=y+50+(i/5)*24;c.drawItem(matching[0],xx,yy);int needed=needed(ingredient),have=have(ingredient);c.drawText(textRenderer,Text.literal(Math.min(99,have)+"/"+needed),xx,yy+16,have>=needed?0xffb8cd8a:0xffeea18c,false);}}
 }
 @Override protected void drawForeground(DrawContext c,int mx,int my){c.drawText(textRenderer,title,6,6,0xffead5ad,false);c.drawText(textRenderer,text("inventory"),72,146,0xffd4bb91,false);c.drawText(textRenderer,text("stored"),100,126,0xffd4bb91,false);}
 @Override public void render(DrawContext c,int mx,int my,float delta){super.render(c,mx,my,delta);drawMouseoverTooltip(c,mx,my);if(selected<0)return;if(mx>=x+100&&mx<x+292&&my>=y+18&&my<y+48)c.drawItemTooltip(textRenderer,result(),mx,my);var ingredients=displayIngredients();for(int i=0;i<ingredients.size();i++){int xx=x+102+i%5*38,yy=y+50+i/5*24;var ingredient=ingredients.get(i);var stacks=ingredient.getMatchingStacks();if(stacks.length>0&&mx>=xx&&mx<xx+35&&my>=yy&&my<yy+24){int need=needed(ingredient);var lines=new ArrayList<Text>();lines.add(stacks[0].getName());lines.add(Text.translatable("alchemy.witchersbestiary.missing",Math.max(0,need-have(ingredient))));lines.add(text("ingredient_help"));lines.add(text("ingredient_counts"));c.drawTooltip(textRenderer,lines,mx,my);}}}
}
