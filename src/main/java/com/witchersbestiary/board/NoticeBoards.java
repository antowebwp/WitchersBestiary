package com.witchersbestiary.board;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class NoticeBoards {
    public static final Identifier ID=Identifier.of("witchersbestiary:notice_board");
    public static final NoticeBoardBlock BLOCK=Registry.register(Registries.BLOCK,ID,new NoticeBoardBlock(AbstractBlock.Settings.create().strength(2.5f).nonOpaque().sounds(BlockSoundGroup.WOOD).luminance(s->s.get(NoticeBoardBlock.PART)==3?10:0)));
    public static final Item ITEM=Registry.register(Registries.ITEM,ID,new BlockItem(BLOCK,new Item.Settings()));
    public static final ScreenHandlerType<BoardScreenHandler> SCREEN=Registry.register(Registries.SCREEN_HANDLER,ID,new ExtendedScreenHandlerType<>(BoardScreenHandler::new,BlockPos.PACKET_CODEC));
    public static void initialize(){PayloadTypeRegistry.playS2C().register(BoardSnapshot.ID,BoardSnapshot.CODEC);ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries->entries.add(ITEM));}
}
