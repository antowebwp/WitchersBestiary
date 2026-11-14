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
 private List<RecipeEntry<CraftingRecipe>> recipes=List.of();private int selected=-1,page;private int category;private String query="";private boolean searchDirty;private final Map<String,Integer> alternatives=new HashMap<>();private ButtonWidget brew;
 private Text text(String key){return Text.translatable("alchemy.witchersbestiary."+key);}
 public AlchemyScreen(AlchemyScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title);backgroundWidth=320;backgroundHeight=238;}
 private int categoryOf(ItemStack stack){return stack.getItem() instanceof com.witchersbestiary.alchemy.WitcherBombItem?4:stack.getItem() instanceof com.witchersbestiary.alchemy.MutagenItem?3:stack.getItem() instanceof WeaponOilItem?1:AlchemyRecipes.consumable(stack)?0:2;}
 private List<Integer> filtered(){var list=new ArrayList<Integer>();for(int i=0;i<recipes.size();i++)if(categoryOf(recipes.get(i).value().getResult(client.world.getRegistryManager()))==category&&recipes.get(i).value().getResult(client.world.getRegistryManager()).getName().getString().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))list.add(i);return list;}
 private ButtonWidget button(Text label,int xx,int yy,int w,int h,Runnable run){return addDrawableChild(ButtonWidget.builder(label,b->run.run()).dimensions(x+xx,y+yy,w,h).build());}
 @Override protected void init(){super.init();if(client.world==null)return;recipes=AlchemyRecipes.catalog(client.world);
  var search=new net.minecraft.client.gui.widget.TextFieldWidget(textRenderer,x+6,y+39,88,14,text("search"));
  search.setText(query);search.setPlaceholder(text("search"));search.setMaxLength(40);
  search.setChangedListener(value->{query=value;page=0;searchDirty=true;});
  addDrawableChild(search);
  var visible=filtered();page=Math.clamp(page,0,Math.max(0,(visible.size()-1)/5));if(!visible.contains(selected))selected=visible.isEmpty()?-1:visible.getFirst();
  button(text(new String[]{"elixirs","oils","components","mutagens","bombs"}[category]).copy().append(" >"),6,18,88,18,()->{category=(category+1)%5;page=0;selected=-1;clearAndInit();});
  for(int row=0;row<5&&page*5+row<visible.size();row++){int index=visible.get(page*5+row);var result=recipes.get(index).value().getResult(client.world.getRegistryManager());button(Text.literal((index==selected?"> ":"")+textRenderer.trimToWidth(result.getName().getString(),72)),6,55+row*12,88,12,()->{selected=index;clearAndInit();}).setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(result.getName()));}
  button(Text.literal("<"),6,116,26,18,()->{page=Math.max(0,page-1);clearAndInit();});button(Text.literal(">"),68,116,26,18,()->{page=Math.max(0,Math.min((visible.size()-1)/5,page+1));clearAndInit();});
  if(category==3)for(int i=0;i<3;i++){int slot=i;button(Text.literal("-"+(i+1)),100+i*32,119,30,18,()->client.interactionManager.clickButton(handler.syncId,10+slot)).setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(text("remove_mutagen")));}
  brew=button(text("brew"),224,100,90,18,()->{if(selected>=0&&client.interactionManager!=null)client.interactionManager.clickButton(handler.syncId,100+selected);});
 }
 @Override protected void handledScreenTick(){super.handledScreenTick();if(searchDirty){searchDirty=false;clearAndInit();for(var child:children())if(child instanceof net.minecraft.client.gui.widget.TextFieldWidget search){setFocused(search);search.setFocused(true);}}if(brew==null||client.world==null||client.player==null)return;var table=new net.minecraft.inventory.SimpleInventory(4);for(int i=0;i<4;i++)table.setStack(i,handler.slots.get(i).getStack().copy());brew.active=selected>=0&&AlchemyRecipes.plan(client.world,table,client.player,recipes.get(selected))!=null;brew.setTooltip(brew.active?null:net.minecraft.client.gui.tooltip.Tooltip.of(text("unavailable")));}
 private ItemStack result(){return selected<0?ItemStack.EMPTY:recipes.get(selected).value().getResult(client.world.getRegistryManager());}
 private String key(Ingredient ingredient){return Arrays.stream(ingredient.getMatchingStacks()).map(s->net.minecraft.registry.Registries.ITEM.getId(s.getItem()).toString()).sorted().collect(java.util.stream.Collectors.joining(","));}
 private List<Ingredient> displayIngredients(){var unique=new LinkedHashMap<String,Ingredient>();for(var ingredient:recipes.get(selected).value().getIngredients())if(!ingredient.isEmpty())unique.putIfAbsent(key(ingredient),ingredient);return new ArrayList<>(unique.values());}
 private int needed(Ingredient ingredient){return (int)recipes.get(selected).value().getIngredients().stream().filter(g->key(g).equals(key(ingredient))).count();}
 private int have(Ingredient ingredient){int have=0;for(int slot=0;slot<handler.slots.size();slot++)if(slot!=3&&ingredient.test(handler.slots.get(slot).getStack()))have+=handler.slots.get(slot).getStack().getCount();return have;}
 @Override protected void drawBackground(DrawContext c,float delta,int mx,int my){c.fill(x,y,x+320,y+238,0xff31271f);c.fill(x+2,y+2,x+318,y+236,0xff4a3b2c);c.fill(x+4,y+4,x+96,y+148,0xff201d19);c.fill(x+98,y+18,x+220,y+116,0xff292b29);for(int xx:new int[]{2,316})for(int yy:new int[]{2,234})c.fill(x+xx,y+yy,x+xx+2,y+yy+2,0xffb29a6e);
  for(var slot:handler.slots){int xx=x+slot.x,yy=y+slot.y;c.fill(xx-1,yy-1,xx+17,yy+17,0xffaa926b);c.fill(xx,yy,xx+16,yy+16,0xff24211d);}
  if(selected>=0){var output=result();c.drawItem(output,x+224,y+26);c.drawItemInSlot(textRenderer,output,x+224,y+26);c.drawText(textRenderer,textRenderer.trimToWidth(output.getName().getString(),90),x+224,y+45,0xffead5ad,false);int lineY=y+58;for(var line:textRenderer.wrapLines(AlchemyRecipes.description(output),89)){if(lineY>=y+94)break;c.drawText(textRenderer,line,x+224,lineY,0xffb8cd8a,false);lineY+=9;}var ingredients=displayIngredients();for(int i=0;i<ingredients.size();i++){var ingredient=ingredients.get(i);var matching=ingredient.getMatchingStacks();if(matching.length==0)continue;int xx=x+102+(i%4)*28,yy=y+50+(i/4)*24;c.drawItem(matching[alternatives.getOrDefault(key(ingredient),0)%matching.length],xx,yy);int needed=needed(ingredient),have=have(ingredient);c.drawText(textRenderer,Text.literal(Math.min(99,have)+"/"+needed),xx,yy+16,have>=needed?0xffb8cd8a:0xffeea18c,false);}}
 }
 @Override protected void drawForeground(DrawContext c,int mx,int my){c.drawText(textRenderer,title,6,6,0xffead5ad,false);c.drawText(textRenderer,text("inventory"),72,146,0xffd4bb91,false);c.drawText(textRenderer,text("stored"),100,126,0xffd4bb91,false);}
 @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
  if(selected>=0&&button==0&&mouseX>=x+102&&mouseY>=y+50&&mouseX<x+214&&mouseY<y+116){
   int index=(int)((mouseY-y-50)/24)*4+(int)((mouseX-x-102)/28);
   var ingredients=displayIngredients();
   if(index<ingredients.size()){
    var ingredient=ingredients.get(index);
    if(ingredient.getMatchingStacks().length>1){alternatives.merge(key(ingredient),1,Integer::sum);return true;}
   }
  }
  return super.mouseClicked(mouseX,mouseY,button);
 }
 @Override public void render(DrawContext c,int mx,int my,float delta){super.render(c,mx,my,delta);drawMouseoverTooltip(c,mx,my);if(selected<0)return;if(mx>=x+224&&mx<x+314&&my>=y+24&&my<y+98)c.drawItemTooltip(textRenderer,result(),mx,my);var ingredients=displayIngredients();for(int i=0;i<ingredients.size();i++){int xx=x+102+i%4*28,yy=y+50+i/4*24;var ingredient=ingredients.get(i);var stacks=ingredient.getMatchingStacks();if(stacks.length>0&&mx>=xx&&mx<xx+28&&my>=yy&&my<yy+24){int need=needed(ingredient);var lines=new ArrayList<Text>();for(var option:stacks)lines.add(option.getName());lines.add(Text.translatable("alchemy.witchersbestiary.missing",Math.max(0,need-have(ingredient))));lines.add(text("ingredient_help"));lines.add(text("ingredient_counts"));c.drawTooltip(textRenderer,lines,mx,my);}}}
}
