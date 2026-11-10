package com.witchersbestiary.client;

import com.witchersbestiary.client.fist.FistClient;
import com.witchersbestiary.equipment.armory.SchoolArmor;
import java.util.Locale;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class SchoolArmorTooltip {
    private static final String KEY="tooltip.witchersbestiary.school_armor.";

    static void initialize(){
        ItemTooltipCallback.EVENT.register((stack, context, type, lines)->{
            if(!(stack.getItem() instanceof SchoolArmor armor))return;
            var school=armor.school();
            var color=switch(school){case BEAR->Formatting.RED;case CAT->Formatting.LIGHT_PURPLE;case GRIFFIN->Formatting.AQUA;};
            var name=Text.translatable(KEY+school.name().toLowerCase(Locale.ROOT)).formatted(color);
            var tier=Text.translatable(KEY+"tier."+armor.tier()).formatted(armor.tier()==2?Formatting.GOLD:Formatting.GRAY);
            lines.add(name.append(Text.literal(" / ").formatted(Formatting.GRAY)).append(tier));
            var player=MinecraftClient.getInstance().player;
            int pieces=player==null?0:SchoolArmor.equippedPieces(player,school);
            boolean suspended=player!=null&&FistClient.status()!=null;
            float bonus=suspended?0:SchoolArmor.fraction(pieces);
            if(player!=null)lines.add(Text.translatable(KEY+"pieces",pieces).formatted(suspended?Formatting.GRAY:color));
            if(suspended)lines.add(Text.translatable(KEY+"paused").formatted(Formatting.GRAY));
            lines.add(Text.translatable(KEY+"partial",percent(school.monsterReduction,bonus)).formatted(bonus>0?Formatting.GREEN:Formatting.GRAY));
            if(school.swordDamage>0)
                lines.add(Text.translatable(KEY+"sword",percent(school.swordDamage,bonus)).formatted(bonus>0?Formatting.GREEN:Formatting.GRAY));
            else lines.add(Text.translatable(KEY+"signs",percent(SchoolArmor.SIGN_RANGE,bonus),
                percent(SchoolArmor.SIGN_DAMAGE,bonus)).formatted(bonus>0?Formatting.GREEN:Formatting.GRAY));
            String id=school.name().toLowerCase(Locale.ROOT);
            lines.add(Text.translatable(KEY+"full",Text.translatable(KEY+id+".effect"))
                .formatted(pieces==4&&!suspended?Formatting.GREEN:Formatting.GRAY));
            if(!Screen.hasShiftDown()){
                lines.add(Text.translatable(KEY+"shift").formatted(Formatting.DARK_GRAY));
                return;
            }
            lines.add(Text.translatable(KEY+"thresholds",Math.round(SchoolArmor.fraction(2)*100),
                Math.round(SchoolArmor.fraction(3)*100),Math.round(SchoolArmor.fraction(4)*100)).formatted(Formatting.GRAY));
            var detail=switch(school){
                case BEAR->Text.translatable(KEY+id+".detail",SchoolArmor.BEAR_HIT,SchoolArmor.BEAR_GUARD,
                    SchoolArmor.BEAR_WINDOW/20,SchoolArmor.BEAR_READY/20);
                case CAT->Text.translatable(KEY+id+".detail",SchoolArmor.CAT_DODGE,SchoolArmor.CAT_REPRISAL,
                    SchoolArmor.CAT_WINDOW/20,SchoolArmor.CAT_READY/20);
                case GRIFFIN->Text.translatable(KEY+id+".detail",SchoolArmor.COMBO_CASTS,SchoolArmor.SIGN_COMBO);
            };
            lines.add(detail.formatted(Formatting.GRAY));
            lines.add(Text.translatable(KEY+"eligible").formatted(Formatting.DARK_GRAY));
            lines.add(Text.translatable(KEY+"tier_details").formatted(Formatting.DARK_GRAY));
        });
    }

    private static String percent(int max,float fraction){
        return String.format(Locale.ROOT,"%.1f",max*fraction).replaceFirst("\\.0$","");
    }
}
