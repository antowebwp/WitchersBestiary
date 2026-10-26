package com.witchersbestiary.alchemy;

import com.witchersbestiary.combat.*;
import com.witchersbestiary.registry.ModEffects;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import java.util.*;

/** Rebuilt once per world load from actual CategorizedMonster instances, never per tooltip frame. */
public final class AlchemyTooltips {
    private static volatile Map<MonsterCategory,List<Text>> TARGETS=Map.of();
    private AlchemyTooltips(){}
    public static void rebuild(World world){
        var collected=new EnumMap<MonsterCategory,List<Text>>(MonsterCategory.class);
        for(var type:Registries.ENTITY_TYPE){
            if(!Registries.ENTITY_TYPE.getId(type).getNamespace().equals("witchersbestiary"))continue;
            var entity=type.create(world);
            if(entity instanceof CategorizedMonster monster && !(entity instanceof com.witchersbestiary.entity.monster.LeshenWolfEntity))
                collected.computeIfAbsent(monster.getMonsterCategory(),k->new ArrayList<>()).add(type.getName());
        }
        collected.replaceAll((category,names)->List.copyOf(names));TARGETS=Map.copyOf(collected);
    }
    public static MonsterCategory category(RegistryEntry<StatusEffect> effect){
        if(effect.equals(ModEffects.RELICT_HUNTER))return MonsterCategory.RELICT;
        if(effect.equals(ModEffects.NECROPHAGE_HUNTER))return MonsterCategory.NECROPHAGE;
        if(effect.equals(ModEffects.SPECTER_HUNTER))return MonsterCategory.SPECTER;
        if(effect.equals(ModEffects.CURSED_HUNTER))return MonsterCategory.CURSED;
        if(effect.equals(ModEffects.VAMPIRE_HUNTER))return MonsterCategory.VAMPIRE;
        throw new IllegalArgumentException("Uncategorized hunter effect "+effect);
    }
    public static void append(List<Text> lines,MonsterCategory category,boolean oil){
        lines.add(Text.translatable("tooltip.witchersbestiary."+(oil?"oil_bonus":"elixir_bonus"),Text.translatable("category.witchersbestiary."+category.name().toLowerCase(Locale.ROOT))).formatted(Formatting.GREEN));
        lines.add(Text.translatable("tooltip.witchersbestiary.targets").formatted(Formatting.GOLD));
        for(Text name:TARGETS.getOrDefault(category,List.of()))lines.add(Text.literal(" • ").append(name).formatted(Formatting.GRAY));
        lines.add(Text.translatable("tooltip.witchersbestiary."+(oil?"oil_hits":"duration")).formatted(Formatting.GRAY));
        lines.add(Text.translatable("tooltip.witchersbestiary."+(oil?"oil_use":"category_only")).formatted(Formatting.DARK_GRAY));
    }
}
