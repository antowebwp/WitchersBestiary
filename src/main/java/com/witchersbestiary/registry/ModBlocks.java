package com.witchersbestiary.registry;

import com.witchersbestiary.WitchersBestiary;
import com.witchersbestiary.block.LeshenHeadBlock;
import com.witchersbestiary.block.TrophyHeadBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    public static final Block LESHEN_HEAD = Registry.register(Registries.BLOCK,
            Identifier.of(WitchersBestiary.MOD_ID, "leshen_head"), new LeshenHeadBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BROWN).strength(1.5f).nonOpaque().noCollision()
                    .sounds(BlockSoundGroup.WOOD).luminance(state -> 8)));
    public static final Block DROWNER_HEAD = Registry.register(Registries.BLOCK,
            Identifier.of(WitchersBestiary.MOD_ID, "drowner_head"), new TrophyHeadBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.DARK_AQUA).strength(1.2f).nonOpaque().noCollision()
                    .sounds(BlockSoundGroup.BONE).luminance(state -> 8)));
    public static final Block GHOUL_HEAD = Registry.register(Registries.BLOCK,
            Identifier.of(WitchersBestiary.MOD_ID, "ghoul_head"), new TrophyHeadBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BROWN).strength(1.2f).nonOpaque().noCollision()
                    .sounds(BlockSoundGroup.BONE).luminance(state -> 0)));
    public static final Block FOGLET_HEAD = Registry.register(Registries.BLOCK,
            Identifier.of(WitchersBestiary.MOD_ID, "foglet_head"), new TrophyHeadBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.GRAY).strength(1.3f).nonOpaque().noCollision()
                    .sounds(BlockSoundGroup.BONE).luminance(state -> 2)));
    private ModBlocks() { }
    public static final Block NOONWRAITH_HEAD = Registry.register(Registries.BLOCK,
            Identifier.of(WitchersBestiary.MOD_ID, "noonwraith_head"), new TrophyHeadBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.PALE_YELLOW).strength(1.3f).nonOpaque().noCollision()
                    .sounds(BlockSoundGroup.BONE).luminance(state -> 3)));
    public static final Block WEREWOLF_HEAD=Registry.register(Registries.BLOCK,Identifier.of(WitchersBestiary.MOD_ID,"werewolf_head"),
            new TrophyHeadBlock(AbstractBlock.Settings.create().mapColor(MapColor.BROWN).strength(1.5f).nonOpaque().noCollision().sounds(BlockSoundGroup.BONE)));
    public static final Block KATAKAN_HEAD=Registry.register(Registries.BLOCK,Identifier.of(WitchersBestiary.MOD_ID,"katakan_head"),new TrophyHeadBlock(AbstractBlock.Settings.create().mapColor(MapColor.GRAY).strength(1.5f).nonOpaque().noCollision().sounds(BlockSoundGroup.BONE)));
    public static final Block WRAITH_HEAD=Registry.register(Registries.BLOCK,Identifier.of(WitchersBestiary.MOD_ID,"wraith_head"),new TrophyHeadBlock(AbstractBlock.Settings.create().mapColor(MapColor.GRAY).strength(1.2f).nonOpaque().noCollision().sounds(BlockSoundGroup.BONE)));
    public static final Block BONECRUSHER_HEAD=Registry.register(Registries.BLOCK,Identifier.of(WitchersBestiary.MOD_ID,"bonecrusher_head"),new TrophyHeadBlock(AbstractBlock.Settings.create().mapColor(MapColor.BROWN).strength(1.5f).nonOpaque().noCollision().sounds(BlockSoundGroup.BONE)));
    public static final Block NEKKER_HEAD=Registry.register(Registries.BLOCK,Identifier.of(WitchersBestiary.MOD_ID,"nekker_head"),new TrophyHeadBlock(AbstractBlock.Settings.create().mapColor(MapColor.BROWN).strength(1.2f).nonOpaque().noCollision().sounds(BlockSoundGroup.BONE)));
    public static final Block ENDREGA_WARRIOR_HEAD=Registry.register(Registries.BLOCK,Identifier.of(WitchersBestiary.MOD_ID,"endrega_warrior_head"),new TrophyHeadBlock(AbstractBlock.Settings.create().mapColor(MapColor.BROWN).strength(1.5f).nonOpaque().noCollision().sounds(BlockSoundGroup.BONE)));
    public static final Block FIEND_HEAD=Registry.register(Registries.BLOCK,Identifier.of(WitchersBestiary.MOD_ID,"fiend_head"),new TrophyHeadBlock(AbstractBlock.Settings.create().mapColor(MapColor.BROWN).strength(1.5f).nonOpaque().noCollision().sounds(BlockSoundGroup.BONE)));
    public static final Block GRIFFIN_HEAD=Registry.register(Registries.BLOCK,Identifier.of(WitchersBestiary.MOD_ID,"griffin_head"),new TrophyHeadBlock(AbstractBlock.Settings.create().mapColor(MapColor.BROWN).strength(1.5f).nonOpaque().noCollision().sounds(BlockSoundGroup.BONE)));
    public static void initialize() { }
    public static final Block INFERNAL_BASILISK_HEAD=Registry.register(Registries.BLOCK,Identifier.of(WitchersBestiary.MOD_ID,"infernal_basilisk_head"),new TrophyHeadBlock(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(1.5f).nonOpaque().noCollision().sounds(BlockSoundGroup.BONE)));
    public static final Block WATER_HAG_HEAD=Registry.register(Registries.BLOCK,Identifier.of(WitchersBestiary.MOD_ID,"water_hag_head"),new TrophyHeadBlock(AbstractBlock.Settings.create().mapColor(MapColor.DARK_AQUA).strength(1.5f).nonOpaque().noCollision().sounds(BlockSoundGroup.BONE)));
    public static final Block GRAVE_HAG_HEAD=Registry.register(Registries.BLOCK,Identifier.of(WitchersBestiary.MOD_ID,"grave_hag_head"),new TrophyHeadBlock(AbstractBlock.Settings.create().mapColor(MapColor.GRAY).strength(1.5f).nonOpaque().noCollision().sounds(BlockSoundGroup.BONE)));
}
