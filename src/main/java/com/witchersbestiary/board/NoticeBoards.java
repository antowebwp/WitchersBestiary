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
    public static void initialize(){net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(net.minecraft.server.command.CommandManager.literal("wbboard").requires(source->source.hasPermissionLevel(2)).then(net.minecraft.server.command.CommandManager.literal("repair").executes(ctx->{var p=ctx.getSource().getPlayerOrThrow();var w=p.getServerWorld();var center=p.getChunkPos();for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){var chunk=w.getChunkManager().getWorldChunk(center.x+dx,center.z+dz);if(chunk==null)continue;for(int x=0;x<16;x++)for(int z=0;z<16;z++)if(w.getBlockState(new BlockPos((center.x+dx)*16+x,chunk.sampleHeightmap(net.minecraft.world.Heightmap.Type.WORLD_SURFACE,x,z), (center.z+dz)*16+z)).isOf(BLOCK)){ctx.getSource().sendFeedback(()->net.minecraft.text.Text.literal("Existing notice board nearby; no placement."),false);return 0;}}boolean placed=VillageNoticeBoards.attempt(w,center);ctx.getSource().sendFeedback(()->net.minecraft.text.Text.literal(placed?"Notice board placed safely in this chunk.":"No safe site in this chunk; terrain unchanged."),false);return placed?1:0;}))));PayloadTypeRegistry.playS2C().register(BoardSnapshot.ID,BoardSnapshot.CODEC);ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries->entries.add(ITEM));}
}
