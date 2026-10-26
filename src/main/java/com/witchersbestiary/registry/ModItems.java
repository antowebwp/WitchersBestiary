package com.witchersbestiary.registry;

import com.witchersbestiary.WitchersBestiary;
import com.witchersbestiary.entity.ModEntities;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Rarity;
import com.witchersbestiary.alchemy.RelictElixirItem;
import com.witchersbestiary.alchemy.HunterElixirItem;
import com.witchersbestiary.alchemy.WeaponOilItem;
import com.witchersbestiary.combat.MonsterCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item LESHEN_SPAWN_EGG = Registry.register(Registries.ITEM,
            Identifier.of(WitchersBestiary.MOD_ID, "leshen_spawn_egg"),
            new SpawnEggItem(ModEntities.LESHEN, 0x50412a, 0xcdc2a3, new Item.Settings()));
    public static final Item DROWNER_SPAWN_EGG = Registry.register(Registries.ITEM,
            Identifier.of(WitchersBestiary.MOD_ID,"drowner_spawn_egg"),new SpawnEggItem(ModEntities.DROWNER,0x315e5b,0x9eb5a0,new Item.Settings()));
    public static final Item GRAVE_HAG_SPAWN_EGG=register("grave_hag_spawn_egg",new SpawnEggItem(ModEntities.GRAVE_HAG,0x68605a,0x9c8775,new Item.Settings()));
    public static final Item GRAVE_HAG_TONGUE=register("grave_hag_tongue",new Item(new Item.Settings()));
    public static final Item GRAVEYARD_ESSENCE=register("graveyard_essence",new Item(new Item.Settings().rarity(Rarity.UNCOMMON)));
    public static final Item NECROPHAGE_REMAINS=register("necrophage_remains",new Item(new Item.Settings()));
    public static final Item GRAVEBOUND_STEEL_FRAGMENT=register("gravebound_steel_fragment",new Item(new Item.Settings().rarity(Rarity.RARE)));
    public static final Item STEEL_HUNTER_SWORD=register("steel_hunter_sword",new com.witchersbestiary.equipment.armory.WitcherSwordItem(com.witchersbestiary.equipment.armory.WitcherSwordItem.Kind.STEEL,7,1.7,950));
    public static final Item SILVER_WITCHER_SWORD=register("silver_witcher_sword",new com.witchersbestiary.equipment.armory.WitcherSwordItem(com.witchersbestiary.equipment.armory.WitcherSwordItem.Kind.SILVER,5,1.6,850));
    public static final Item GRAVE_BLADE=register("grave_blade",new com.witchersbestiary.equipment.armory.WitcherSwordItem(com.witchersbestiary.equipment.armory.WitcherSwordItem.Kind.GRAVE,7,1.5,1250));
    private ModItems() { }
    public static final Item LESHEN_HEAD = register("leshen_head", new BlockItem(ModBlocks.LESHEN_HEAD, new Item.Settings().rarity(Rarity.RARE)));
    public static final Item FOREST_ESSENCE = register("forest_essence", new Item(new Item.Settings()));
    public static final Item ANCIENT_LESHEN_HEART = register("ancient_leshen_heart", new Item(new Item.Settings().rarity(Rarity.RARE)));
    public static final Item RELICT_ELIXIR = register("relict_elixir", new RelictElixirItem(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)));
    public static final Item DROWNER_ESSENCE=register("drowner_essence",new Item(new Item.Settings()));
    public static final Item DROWNER_CLAW=register("drowner_claw",new Item(new Item.Settings()));
    public static final Item DROWNER_HEAD=register("drowner_head",new BlockItem(ModBlocks.DROWNER_HEAD,new Item.Settings().rarity(Rarity.UNCOMMON)));
    public static final Item NECROPHAGE_ELIXIR=register("necrophage_elixir",new HunterElixirItem(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON),ModEffects.NECROPHAGE_HUNTER));
    public static final Item RELICT_OIL=register("relict_oil",new WeaponOilItem(new Item.Settings().maxCount(16),"relict_oil",MonsterCategory.RELICT));
    public static final Item NECROPHAGE_OIL=register("necrophage_oil",new WeaponOilItem(new Item.Settings().maxCount(16),"necrophage_oil",MonsterCategory.NECROPHAGE));
    public static final Item GHOUL_SPAWN_EGG=register("ghoul_spawn_egg",new SpawnEggItem(ModEntities.GHOUL,0x554638,0xb8a879,new Item.Settings()));
    public static final Item GHOUL_ESSENCE=register("ghoul_essence",new Item(new Item.Settings()));
    public static final Item GHOUL_CLAW=register("ghoul_claw",new Item(new Item.Settings()));
    public static final Item GHOUL_HEAD=register("ghoul_head",new BlockItem(ModBlocks.GHOUL_HEAD,new Item.Settings().rarity(Rarity.UNCOMMON)));
    public static final Item FOGLET_SPAWN_EGG=register("foglet_spawn_egg",new SpawnEggItem(ModEntities.FOGLET,0x515954,0xc5aa79,new Item.Settings()));
    public static final Item FOGLET_ESSENCE=register("foglet_essence",new Item(new Item.Settings()));
    public static final Item FOGLET_CLAW=register("foglet_claw",new Item(new Item.Settings()));
    public static final Item FOGLET_HEAD=register("foglet_head",new BlockItem(ModBlocks.FOGLET_HEAD,new Item.Settings().rarity(Rarity.RARE)));
    public static final Item HUNTER_MEDALLION=register("hunter_medallion",new com.witchersbestiary.accessory.HunterAccessoryItem(new Item.Settings().rarity(Rarity.RARE),"medallion"));
    public static final Item WEREWOLF_SPAWN_EGG=register("werewolf_spawn_egg",new SpawnEggItem(ModEntities.WEREWOLF,0x282927,0xa78559,new Item.Settings()));
    public static final Item WEREWOLF_ESSENCE=register("werewolf_essence",new Item(new Item.Settings()));
    public static final Item WEREWOLF_CLAW=register("werewolf_claw",new Item(new Item.Settings()));
    public static final Item WEREWOLF_HEAD=register("werewolf_head",new BlockItem(ModBlocks.WEREWOLF_HEAD,new Item.Settings().rarity(Rarity.RARE)));
    public static final Item CURSED_ELIXIR=register("cursed_elixir",new HunterElixirItem(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON),ModEffects.CURSED_HUNTER));
    public static final Item CURSED_OIL=register("cursed_oil",new WeaponOilItem(new Item.Settings().maxCount(16),"cursed_oil",MonsterCategory.CURSED));
    public static final Item VAMPIRE_FANG=register("vampire_fang",new com.witchersbestiary.accessory.HunterAccessoryItem(new Item.Settings().rarity(Rarity.RARE),"fang"));
    public static final Item VAMPIRE_ELIXIR=register("vampire_elixir",new HunterElixirItem(new Item.Settings().maxCount(16),ModEffects.VAMPIRE_HUNTER));
    public static final Item VAMPIRE_OIL=register("vampire_oil",new WeaponOilItem(new Item.Settings().maxCount(16),"vampire_oil",MonsterCategory.VAMPIRE));
    public static final Item KATAKAN_SPAWN_EGG=register("katakan_spawn_egg",new SpawnEggItem(ModEntities.KATAKAN,0x6b777e,0xcabc98,new Item.Settings()));
    public static final Item KATAKAN_ESSENCE=register("katakan_essence",new Item(new Item.Settings()));
    public static final Item KATAKAN_CLAW=register("katakan_claw",new Item(new Item.Settings()));
    public static final Item KATAKAN_HEAD=register("katakan_head",new BlockItem(ModBlocks.KATAKAN_HEAD,new Item.Settings().rarity(Rarity.RARE)));
    public static final Item ENDREGA_WARRIOR_SPAWN_EGG=register("endrega_warrior_spawn_egg",new SpawnEggItem(ModEntities.ENDREGA_WARRIOR,0x3d3926,0x9c4930,new Item.Settings()));
    public static final Item ENDREGA_ESSENCE=register("endrega_essence",new Item(new Item.Settings()));
    public static final Item CHITIN_PLATE=register("chitin_plate",new Item(new Item.Settings()));
    public static final Item VENOM_GLAND=register("venom_gland",new Item(new Item.Settings()));
    public static final Item ENDREGA_WARRIOR_HEAD=register("endrega_warrior_head",new BlockItem(ModBlocks.ENDREGA_WARRIOR_HEAD,new Item.Settings().rarity(Rarity.RARE)));
    public static final Item INSECTOID_OIL=register("insectoid_oil",new WeaponOilItem(new Item.Settings().maxCount(16),"insectoid_oil",MonsterCategory.INSECTOID));
    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(WitchersBestiary.MOD_ID, name), item);
    }
    public static final Item INFERNAL_BASILISK_SPAWN_EGG=register("infernal_basilisk_spawn_egg",new SpawnEggItem(ModEntities.INFERNAL_BASILISK,0x24252a,0xff6c12,new Item.Settings()));
    public static final Item BASILISK_SCALE=register("basilisk_scale",new Item(new Item.Settings().fireproof()));
    public static final Item VOLCANIC_GLAND=register("volcanic_gland",new Item(new Item.Settings().fireproof()));
    public static final Item MOLTEN_HEART=register("molten_heart",new Item(new Item.Settings().fireproof().rarity(Rarity.RARE)));
    public static final Item INFERNAL_BASILISK_HEAD=register("infernal_basilisk_head",new BlockItem(ModBlocks.INFERNAL_BASILISK_HEAD,new Item.Settings().fireproof().rarity(Rarity.RARE)));
    public static final Item NOONWRAITH_SPAWN_EGG = register("noonwraith_spawn_egg", new SpawnEggItem(ModEntities.NOONWRAITH,
            0xddd6b5, 0x625b49, new Item.Settings()));
    public static final Item NOONWRAITH_ESSENCE = register("noonwraith_essence", new Item(new Item.Settings()));
    public static final Item NOONWRAITH_REMAINS = register("noonwraith_remains", new Item(new Item.Settings()));
    public static final Item NOONWRAITH_HEAD = register("noonwraith_head", new BlockItem(ModBlocks.NOONWRAITH_HEAD,
            new Item.Settings().rarity(Rarity.RARE)));
    public static final Item SPECTER_ELIXIR = register("specter_elixir", new HunterElixirItem(new Item.Settings().maxCount(16), ModEffects.SPECTER_HUNTER));
    public static final Item SPECTER_OIL = register("specter_oil", new WeaponOilItem(new Item.Settings().maxCount(16), "specter_oil", MonsterCategory.SPECTER));
    public static final Item BESTIARY = register("bestiary", new com.witchersbestiary.bestiary.BestiaryItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));
    public static final Item WRAITH_SPAWN_EGG=register("wraith_spawn_egg",new SpawnEggItem(ModEntities.WRAITH,0x343735,0xb4b3a0,new Item.Settings()));
    public static final Item WRAITH_ESSENCE=register("wraith_essence",new Item(new Item.Settings()));
    public static final Item WRAITH_REMAINS=register("wraith_remains",new Item(new Item.Settings()));
    public static final Item WRAITH_HEAD=register("wraith_head",new BlockItem(ModBlocks.WRAITH_HEAD,new Item.Settings().rarity(Rarity.RARE)));
    public static final Item BONECRUSHER_SPAWN_EGG=register("bonecrusher_spawn_egg",new SpawnEggItem(ModEntities.BONECRUSHER,0x75645a,0xc4ad7f,new Item.Settings()));
    public static final Item OGROID_ESSENCE=register("ogroid_essence",new Item(new Item.Settings()));
    public static final Item OGROID_FANG=register("ogroid_fang",new Item(new Item.Settings().rarity(Rarity.UNCOMMON)));
    public static final Item BONECRUSHER_HEAD=register("bonecrusher_head",new BlockItem(ModBlocks.BONECRUSHER_HEAD,new Item.Settings().rarity(Rarity.RARE)));
    public static final Item BONECRUSHER_CLUB=register("bonecrusher_club",new com.witchersbestiary.equipment.OgroidHammerItem());
    public static final Item OGROID_OIL=register("ogroid_oil",new WeaponOilItem(new Item.Settings().maxCount(16),"ogroid_oil",MonsterCategory.OGROID,.25f,3600));
    public static final Item NEKKER_SPAWN_EGG=register("nekker_spawn_egg",new SpawnEggItem(ModEntities.NEKKER,0x806456,0xcdbd95,new Item.Settings()));
    public static final Item NEKKER_CLAW=register("nekker_claw",new Item(new Item.Settings()));
    public static final Item NEKKER_HEAD=register("nekker_head",new BlockItem(ModBlocks.NEKKER_HEAD,new Item.Settings().rarity(Rarity.RARE)));
    public static final Item FIEND_SPAWN_EGG=register("fiend_spawn_egg",new SpawnEggItem(ModEntities.FIEND,0x352820,0xffa72a,new Item.Settings()));
    public static final Item FIEND_HORN=register("fiend_horn",new Item(new Item.Settings().rarity(Rarity.UNCOMMON)));
    public static final Item FIEND_EYE=register("fiend_eye",new Item(new Item.Settings().rarity(Rarity.RARE)));
    public static final Item FIEND_HEAD=register("fiend_head",new BlockItem(ModBlocks.FIEND_HEAD,new Item.Settings().rarity(Rarity.RARE)));
    public static final Item CLEAR_MIND_ELIXIR=register("clear_mind_elixir",new HunterElixirItem(new Item.Settings().maxCount(16).rarity(Rarity.RARE),ModEffects.CLEAR_MIND));
    public static final Item GRIFFIN_SPAWN_EGG=register("griffin_spawn_egg",new SpawnEggItem(ModEntities.GRIFFIN,0x322d29,0xa34d2c,new Item.Settings()));
    public static final Item HYBRID_ESSENCE=register("hybrid_essence",new Item(new Item.Settings()));
    public static final Item GRIFFIN_FEATHER=register("griffin_feather",new Item(new Item.Settings()));
    public static final Item GRIFFIN_CLAW=register("griffin_claw",new Item(new Item.Settings().rarity(Rarity.UNCOMMON)));
    public static final Item GRIFFIN_HEAD=register("griffin_head",new BlockItem(ModBlocks.GRIFFIN_HEAD,new Item.Settings().rarity(Rarity.RARE)));
    public static final Item HYBRID_OIL=register("hybrid_oil",new WeaponOilItem(new Item.Settings().maxCount(16),"hybrid_oil",MonsterCategory.HYBRID));
    public static final Item WATER_HAG_SPAWN_EGG=register("water_hag_spawn_egg",new SpawnEggItem(ModEntities.WATER_HAG,0x58604a,0xaaa078,new Item.Settings()));
    public static final Item SWAMP_GLAND=register("swamp_gland",new Item(new Item.Settings()));
    public static final Item MUDDY_CLAW=register("muddy_claw",new Item(new Item.Settings()));
    public static final Item MUD_CLOD=register("mud_clod",new Item(new Item.Settings()));
    public static final Item WATER_HAG_HEAD=register("water_hag_head",new BlockItem(ModBlocks.WATER_HAG_HEAD,new Item.Settings().rarity(Rarity.RARE)));
    public static final Item GRAVE_HAG_HEAD=register("grave_hag_head",new BlockItem(ModBlocks.GRAVE_HAG_HEAD,new Item.Settings().rarity(Rarity.RARE)));
    public static void initialize() {
        ModItemGroups.initialize();
    }
}
