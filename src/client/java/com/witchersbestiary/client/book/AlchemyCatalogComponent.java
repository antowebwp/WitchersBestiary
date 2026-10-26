package com.witchersbestiary.client.book;

import com.witchersbestiary.alchemy.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.registry.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import vazkii.patchouli.api.*;
import java.util.*;
import java.util.function.UnaryOperator;

/** Live RecipeManager catalog rendered inside the existing Patchouli spread. */
public final class AlchemyCatalogComponent implements ICustomComponent {
    public boolean list=true;
    private int x,y,pageNum;
    private static int revision,seenRevision=-1,offset,category,detail;
    private static Object manager,world;
    private static String language="",query="";
    private static Identifier selected;
    private static List<RecipeEntry<CraftingRecipe>> recipes=List.of();
    public static void invalidate(){revision++;}
    @Override public void build(int x,int y,int pageNum){this.x=x;this.y=y;this.pageNum=pageNum;}
    @Override public void onVariablesAvailable(UnaryOperator<IVariable> lookup,RegistryWrapper.WrapperLookup registries){}
    @Override public void onDisplayed(IComponentRenderContext context){
        if(!list)return;
        var c=MinecraftClient.getInstance();var search=new TextFieldWidget(c.textRenderer,x,y+37,110,14,Text.translatable("book.witchersbestiary.search"));search.setMaxLength(48);search.setText(query);search.setPlaceholder(Text.translatable("book.witchersbestiary.search"));search.setChangedListener(value->{query=value;offset=0;});context.addWidget(search,pageNum);
    }
    private static int kind(ItemStack stack){return stack.getItem() instanceof WeaponOilItem?1:stack.getItem() instanceof HunterElixirItem||stack.getItem() instanceof RelictElixirItem?2:3;}
    private static List<RecipeEntry<CraftingRecipe>> current(){
        var c=MinecraftClient.getInstance();if(c.world==null||c.getNetworkHandler()==null)return List.of();
        var rm=c.getNetworkHandler().getRecipeManager();String lang=c.getLanguageManager().getLanguage();
        if(manager!=rm||world!=c.world||seenRevision!=revision||!language.equals(lang)){
            manager=rm;world=c.world;seenRevision=revision;language=lang;
            recipes=rm.listAllOfType(RecipeType.CRAFTING).stream().filter(e->{ItemStack out=e.value().getResult(c.world.getRegistryManager());return !out.isEmpty()&&(e.id().getNamespace().equals("witchersbestiary")||Registries.ITEM.getId(out.getItem()).getNamespace().equals("witchersbestiary"));}).sorted(Comparator.comparing(e->e.value().getResult(c.world.getRegistryManager()).getName().getString())).toList();
        }
        var filtered=recipes.stream().filter(e->{var out=e.value().getResult(c.world.getRegistryManager());return (category==0||kind(out)==category)&&(query.isBlank()||out.getName().getString().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))||e.id().toString().contains(query.toLowerCase(Locale.ROOT)));}).toList();
        offset=Math.clamp(offset,0,Math.max(0,(filtered.size()-1)/4));
        if(filtered.stream().noneMatch(e->e.id().equals(selected)))selected=filtered.isEmpty()?null:filtered.getFirst().id();
        return filtered;
    }
    private void line(DrawContext draw,IComponentRenderContext context,Text text,int xx,int yy,int width){var c=MinecraftClient.getInstance();draw.drawText(c.textRenderer,net.minecraft.text.OrderedText.styledForwardsVisitedString(c.textRenderer.trimToWidth(text.getString(),width),context.getFont()),xx,yy,context.getTextColor(),false);}
    @Override public void render(DrawContext draw,IComponentRenderContext ctx,float partial,int mx,int my){
        var c=MinecraftClient.getInstance();var all=current();
        if(list){
            line(draw,ctx,Text.translatable("book.witchersbestiary.catalog"),x,y,114);
            line(draw,ctx,Text.translatable("book.witchersbestiary.filter."+category),x,y+34,114);line(draw,ctx,Text.literal("<    >"),x+77,y+34,36);
            for(int i=0;i<4;i++){
                int index=offset*4+i;if(index>=all.size())break;var e=all.get(index);int yy=y+50+i*20;
                if(e.id().equals(selected))draw.fill(x-1,yy-2,x+114,yy+19,0x22644d2e);
                var stack=e.value().getResult(c.world.getRegistryManager());ctx.renderItemStack(draw,x,yy,mx,my,stack);line(draw,ctx,stack.getName(),x+20,yy+4,92);
                if(ctx.isAreaHovered(mx,my,x+20,yy,93,18))ctx.setHoverTooltipComponents(List.of(stack.getName(),Text.literal(e.id().toString())));
            }
            line(draw,ctx,Text.literal("<   "+(offset+1)+" / "+Math.max(1,(all.size()+3)/4)+"   >"),x+15,y+136,100);
            if(all.isEmpty())line(draw,ctx,Text.translatable("book.witchersbestiary.no_recipes"),x,y+60,114);
            return;
        }
        var recipe=all.stream().filter(e->e.id().equals(selected)).findFirst().orElse(null);if(recipe==null)return;
        ItemStack result=recipe.value().getResult(c.world.getRegistryManager());line(draw,ctx,result.getName(),x,y,114);
        line(draw,ctx,Text.translatable("book.witchersbestiary."+(detail==0?"crafting":"effects")),x,y+15,114);line(draw,ctx,Text.literal("<>"),x+98,y+15,16);
        if(detail==0){
            var ingredients=recipe.value().getIngredients();boolean shaped=recipe.value() instanceof ShapedRecipe;
            int width=shaped?((ShapedRecipe)recipe.value()).getWidth():ingredients.size()<=4?2:3;
            int height=shaped?((ShapedRecipe)recipe.value()).getHeight():(ingredients.size()+width-1)/width;
            int grid=width<=2&&height<=2?2:3;
            for(int row=0;row<grid;row++)for(int col=0;col<grid;col++){
                int xx=x+col*19,yy=y+36+row*19;draw.fill(xx-1,yy-1,xx+17,yy+17,0xffa28c68);draw.fill(xx,yy,xx+16,yy+16,0xffd0bd96);
                int index=row*width+col;if(row<height&&col<width&&index<ingredients.size())ctx.renderIngredient(draw,xx,yy,mx,my,ingredients.get(index));
            }
            line(draw,ctx,Text.literal("→"),x+64,y+55,12);ctx.renderItemStack(draw,x+86,y+52,mx,my,result);
            int yy=y+103;var label=Text.translatable("book.witchersbestiary."+(shaped?"shaped":"shapeless"));
            for(var text:c.textRenderer.wrapLines(label.copy().setStyle(ctx.getFont()),112)){draw.drawText(c.textRenderer,text,x,yy,ctx.getTextColor(),false);yy+=10;}
            line(draw,ctx,Text.literal(recipe.id().getPath()),x,y+132,114);
        }else{
            ctx.renderItemStack(draw,x+47,y+31,mx,my,result);
            var wrapped=properties(result,ctx);int pages=Math.max(1,(wrapped.size()+7)/8);detail=Math.clamp(detail,1,pages);int yy=y+55;
            for(int i=(detail-1)*8;i<Math.min(wrapped.size(),detail*8);i++){draw.drawText(c.textRenderer,wrapped.get(i),x,yy,ctx.getTextColor(),false);yy+=10;}
            line(draw,ctx,Text.literal(detail+" / "+pages+"  >"),x+40,y+136,72);
        }
    }
    private List<net.minecraft.text.OrderedText> properties(ItemStack result,IComponentRenderContext ctx){
        var c=MinecraftClient.getInstance();var lines=new ArrayList<Text>();int kind=kind(result);
        lines.add(Text.translatable("book.witchersbestiary.description."+kind));
        result.getItem().appendTooltip(result,net.minecraft.item.Item.TooltipContext.create(c.world),lines,net.minecraft.item.tooltip.TooltipType.BASIC);
        var out=new ArrayList<net.minecraft.text.OrderedText>();for(Text line:lines)out.addAll(c.textRenderer.wrapLines(line.copy().setStyle(ctx.getFont()),112));return out;
    }
    @Override public boolean mouseClicked(IComponentRenderContext ctx,double mx,double my,int button){
        if(button!=0)return false;var all=current();
        if(list){
            if(mx>=x&&mx<x+114&&my>=y+32&&my<y+45){category=(category+(mx<x+90?3:1))%4;offset=0;return true;}
            if(mx>=x&&mx<x+114&&my>=y+48&&my<y+130){int i=(int)(my-y-48)/20+offset*4;if(i<all.size()){selected=all.get(i).id();detail=0;}return true;}
            if(mx>=x&&mx<x+114&&my>=y+134&&my<y+146){offset=Math.clamp(offset+(mx<x+57?-1:1),0,Math.max(0,(all.size()-1)/4));return true;}
        }else if(mx>=x&&mx<x+114&&my>=y+13&&my<y+28){var c=MinecraftClient.getInstance();var recipe=all.stream().filter(e->e.id().equals(selected)).findFirst().orElse(null);int pages=recipe==null?1:Math.max(1,(properties(recipe.value().getResult(c.world.getRegistryManager()),ctx).size()+7)/8);detail=(detail+1)%(pages+1);return true;}
        return false;
    }
}
