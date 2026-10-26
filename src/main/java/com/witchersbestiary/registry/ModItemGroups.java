package com.witchersbestiary.registry;

import com.witchersbestiary.WitchersBestiary;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.List;

/** One ordered, explicit home for every item owned by the mod. */
public final class ModItemGroups {
    public static final RegistryKey<ItemGroup> WITCHERS_BESTIARY_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP, Identifier.of(WitchersBestiary.MOD_ID, "bestiary"));

    private static final List<Item> SPAWN_EGGS = List.of(
            ModItems.GRAVE_HAG_SPAWN_EGG,ModItems.WATER_HAG_SPAWN_EGG, ModItems.LESHEN_SPAWN_EGG, ModItems.DROWNER_SPAWN_EGG, ModItems.GHOUL_SPAWN_EGG, ModItems.FOGLET_SPAWN_EGG, ModItems.NOONWRAITH_SPAWN_EGG,ModItems.WEREWOLF_SPAWN_EGG,ModItems.KATAKAN_SPAWN_EGG,ModItems.WRAITH_SPAWN_EGG,ModItems.BONECRUSHER_SPAWN_EGG,ModItems.NEKKER_SPAWN_EGG,ModItems.ENDREGA_WARRIOR_SPAWN_EGG,ModItems.FIEND_SPAWN_EGG,ModItems.GRIFFIN_SPAWN_EGG,ModItems.INFERNAL_BASILISK_SPAWN_EGG);
    private static final List<Item> TROPHIES = List.of(
            ModItems.WATER_HAG_HEAD, ModItems.GRAVE_HAG_HEAD, ModItems.LESHEN_HEAD, ModItems.DROWNER_HEAD, ModItems.GHOUL_HEAD, ModItems.FOGLET_HEAD, ModItems.NOONWRAITH_HEAD,ModItems.WEREWOLF_HEAD,ModItems.KATAKAN_HEAD,ModItems.WRAITH_HEAD,ModItems.BONECRUSHER_HEAD,ModItems.NEKKER_HEAD,ModItems.ENDREGA_WARRIOR_HEAD,ModItems.FIEND_HEAD,ModItems.GRIFFIN_HEAD,ModItems.INFERNAL_BASILISK_HEAD);
    private static final List<Item> INGREDIENTS = List.of(
            ModItems.GRAVEBOUND_STEEL_FRAGMENT,ModItems.GRAVE_HAG_TONGUE,ModItems.GRAVEYARD_ESSENCE,ModItems.NECROPHAGE_REMAINS,ModItems.SWAMP_GLAND,ModItems.MUDDY_CLAW,ModItems.MUD_CLOD,ModItems.FOREST_ESSENCE, ModItems.ANCIENT_LESHEN_HEART,
            ModItems.DROWNER_ESSENCE, ModItems.DROWNER_CLAW,
            ModItems.GHOUL_ESSENCE, ModItems.GHOUL_CLAW, ModItems.FOGLET_ESSENCE, ModItems.FOGLET_CLAW, ModItems.NOONWRAITH_ESSENCE, ModItems.NOONWRAITH_REMAINS,ModItems.WEREWOLF_ESSENCE,ModItems.WEREWOLF_CLAW,ModItems.KATAKAN_ESSENCE,ModItems.KATAKAN_CLAW,ModItems.WRAITH_ESSENCE,ModItems.WRAITH_REMAINS,ModItems.OGROID_ESSENCE,ModItems.OGROID_FANG,ModItems.NEKKER_CLAW,ModItems.FIEND_HORN,ModItems.ENDREGA_ESSENCE,ModItems.CHITIN_PLATE,ModItems.VENOM_GLAND,ModItems.FIEND_EYE,ModItems.HYBRID_ESSENCE,ModItems.GRIFFIN_FEATHER,ModItems.GRIFFIN_CLAW,ModItems.BASILISK_SCALE,ModItems.VOLCANIC_GLAND,ModItems.MOLTEN_HEART);
    private static final List<Item> ELIXIRS = List.of(ModItems.RELICT_ELIXIR, ModItems.NECROPHAGE_ELIXIR, ModItems.SPECTER_ELIXIR,ModItems.CURSED_ELIXIR,ModItems.VAMPIRE_ELIXIR,ModItems.CLEAR_MIND_ELIXIR);
    private static final List<Item> OILS = List.of(ModItems.RELICT_OIL, ModItems.NECROPHAGE_OIL, ModItems.SPECTER_OIL,ModItems.CURSED_OIL,ModItems.VAMPIRE_OIL,ModItems.INSECTOID_OIL,ModItems.OGROID_OIL,ModItems.HYBRID_OIL);
    /** Add newly registered items here and to their appropriate category above. */
    private static final List<Item> BASE_ITEMS = List.of(ModItems.STEEL_HUNTER_SWORD,ModItems.SILVER_WITCHER_SWORD,ModItems.GRAVE_BLADE,
            ModItems.GRAVE_HAG_SPAWN_EGG,ModItems.WATER_HAG_SPAWN_EGG, ModItems.LESHEN_SPAWN_EGG, ModItems.DROWNER_SPAWN_EGG, ModItems.GHOUL_SPAWN_EGG, ModItems.FOGLET_SPAWN_EGG,
            ModItems.WATER_HAG_HEAD, ModItems.GRAVE_HAG_HEAD, ModItems.LESHEN_HEAD, ModItems.DROWNER_HEAD, ModItems.GHOUL_HEAD, ModItems.FOGLET_HEAD,
            ModItems.GRAVEBOUND_STEEL_FRAGMENT,ModItems.GRAVE_HAG_TONGUE,ModItems.GRAVEYARD_ESSENCE,ModItems.NECROPHAGE_REMAINS,ModItems.SWAMP_GLAND,ModItems.MUDDY_CLAW,ModItems.MUD_CLOD,ModItems.FOREST_ESSENCE, ModItems.ANCIENT_LESHEN_HEART,
            ModItems.DROWNER_ESSENCE, ModItems.DROWNER_CLAW, ModItems.GHOUL_ESSENCE, ModItems.GHOUL_CLAW,
            ModItems.FOGLET_ESSENCE, ModItems.FOGLET_CLAW,
            ModItems.RELICT_ELIXIR, ModItems.NECROPHAGE_ELIXIR, ModItems.RELICT_OIL, ModItems.NECROPHAGE_OIL,
            ModItems.HUNTER_MEDALLION, ModItems.NOONWRAITH_SPAWN_EGG, ModItems.NOONWRAITH_HEAD,
            ModItems.NOONWRAITH_ESSENCE, ModItems.NOONWRAITH_REMAINS, ModItems.SPECTER_ELIXIR, ModItems.SPECTER_OIL, ModItems.BESTIARY,ModItems.WEREWOLF_SPAWN_EGG,ModItems.WEREWOLF_ESSENCE,ModItems.WEREWOLF_CLAW,ModItems.WEREWOLF_HEAD,ModItems.CURSED_ELIXIR,ModItems.CURSED_OIL,ModItems.VAMPIRE_OIL,ModItems.VAMPIRE_FANG,ModItems.KATAKAN_SPAWN_EGG,ModItems.KATAKAN_HEAD,ModItems.KATAKAN_ESSENCE,ModItems.KATAKAN_CLAW,ModItems.VAMPIRE_ELIXIR,ModItems.WRAITH_SPAWN_EGG,ModItems.WRAITH_ESSENCE,ModItems.WRAITH_REMAINS,ModItems.WRAITH_HEAD,ModItems.BONECRUSHER_SPAWN_EGG,ModItems.BONECRUSHER_HEAD,ModItems.OGROID_ESSENCE,ModItems.OGROID_FANG,ModItems.BONECRUSHER_CLUB,ModItems.INSECTOID_OIL,ModItems.OGROID_OIL,ModItems.NEKKER_SPAWN_EGG,ModItems.NEKKER_HEAD,ModItems.NEKKER_CLAW,ModItems.ENDREGA_WARRIOR_SPAWN_EGG,ModItems.FIEND_SPAWN_EGG,ModItems.ENDREGA_WARRIOR_HEAD,ModItems.FIEND_HEAD,ModItems.FIEND_HORN,ModItems.ENDREGA_ESSENCE,ModItems.CHITIN_PLATE,ModItems.VENOM_GLAND,ModItems.FIEND_EYE,ModItems.CLEAR_MIND_ELIXIR,ModItems.GRIFFIN_SPAWN_EGG,ModItems.GRIFFIN_HEAD,ModItems.GRIFFIN_FEATHER,ModItems.GRIFFIN_CLAW,ModItems.HYBRID_ESSENCE,ModItems.HYBRID_OIL,ModItems.INFERNAL_BASILISK_SPAWN_EGG,ModItems.INFERNAL_BASILISK_HEAD,ModItems.BASILISK_SCALE,ModItems.VOLCANIC_GLAND,ModItems.MOLTEN_HEART,com.witchersbestiary.lure.ModLures.LESHEN_LURE,com.witchersbestiary.lure.ModLures.FIEND_LURE,com.witchersbestiary.lure.ModLures.GRIFFIN_LURE,com.witchersbestiary.decor.ModRemains.TOOTH_NECKLACE.asItem(),com.witchersbestiary.decor.ModRemains.MONSTER_ORGAN_JAR.asItem(),com.witchersbestiary.decor.ModRemains.MONSTER_WALL_TROPHY.asItem(),com.witchersbestiary.decor.ModRemains.BONE_DUST,com.witchersbestiary.artifact.ModArtifacts.LESHEN_RING,com.witchersbestiary.artifact.ModArtifacts.WEREWOLF_RING,com.witchersbestiary.artifact.ModArtifacts.DROWNER_CHARM,com.witchersbestiary.artifact.ModArtifacts.GRIFFIN_CHARM,com.witchersbestiary.ward.SpiritWardBlock.ITEM);

    public static final List<Item> ALL_ITEMS=java.util.stream.Stream.concat(BASE_ITEMS.stream(),com.witchersbestiary.workshop.ModWorkshop.BLOCKS.values().stream().map(net.minecraft.block.Block::asItem)).toList();

    private ModItemGroups() { }

    public static void initialize() {
        Registry.register(Registries.ITEM_GROUP, WITCHERS_BESTIARY_KEY,
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(ModItems.HUNTER_MEDALLION))
                        .displayName(Text.translatable("itemGroup.witchersbestiary.bestiary"))
                        .entries((context, entries) -> {
                            com.witchersbestiary.decor.ModRemains.ALL.forEach(entries::add);
                            entries.add(com.witchersbestiary.ward.SpiritWardBlock.ITEM);
                            com.witchersbestiary.artifact.ModArtifacts.ALL.forEach(entries::add);
                            com.witchersbestiary.workshop.ModWorkshop.BLOCKS.values().forEach(b->entries.add(b.asItem()));
                            SPAWN_EGGS.forEach(entries::add);
                            TROPHIES.forEach(entries::add);
                            INGREDIENTS.forEach(entries::add);
                            ELIXIRS.forEach(entries::add);
                            OILS.forEach(entries::add);
                            entries.add(ModItems.HUNTER_MEDALLION);entries.add(ModItems.VAMPIRE_FANG);
                            entries.add(com.witchersbestiary.lure.ModLures.LESHEN_LURE);entries.add(com.witchersbestiary.lure.ModLures.FIEND_LURE);entries.add(com.witchersbestiary.lure.ModLures.GRIFFIN_LURE);
                            entries.add(ModItems.STEEL_HUNTER_SWORD);entries.add(ModItems.SILVER_WITCHER_SWORD);entries.add(ModItems.GRAVE_BLADE);entries.add(ModItems.BONECRUSHER_CLUB);entries.add(ModItems.BESTIARY);
                        }).build());
    }
}
