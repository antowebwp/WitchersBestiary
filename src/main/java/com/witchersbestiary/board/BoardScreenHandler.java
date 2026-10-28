package com.witchersbestiary.board;

import com.google.gson.*;
import com.witchersbestiary.contracts.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/** Vanilla screen-button packets bind every action to an open sync ID and live, nearby board. */
public final class BoardScreenHandler extends ScreenHandler {
    private final BlockPos root;
    private final UUID owner;
    private List<Identifier> offers=List.of();
    public JsonObject snapshot=new JsonObject();
    private int lastAction=-100;
    private final PlayerEntity viewer;
    private int lastSync=-100;
    public BoardScreenHandler(int sync,PlayerInventory inventory,BlockPos root){
        super(NoticeBoards.SCREEN,sync);this.root=root.toImmutable();this.owner=inventory.player.getUuid();this.viewer=inventory.player;
        if(!inventory.player.getWorld().isClient)offers=Contracts.DEFINITIONS.all().keySet().stream().sorted().limit(32).toList();
    }
    public BlockPos boardPos(){return root;}
    @Override public boolean canUse(PlayerEntity player){
        if(!owner.equals(player.getUuid())||player.squaredDistanceTo(root.toCenterPos().add(0,1,0))>64)return false;
        var state=player.getWorld().getBlockState(root);
        return state.isOf(NoticeBoards.BLOCK)&&NoticeBoardBlock.complete(player.getWorld(),root,state.get(NoticeBoardBlock.FACING));
    }
    @Override public ItemStack quickMove(PlayerEntity p,int slot){return ItemStack.EMPTY;}
    public static ContractInstance existing(ServerPlayerEntity player,Identifier definition){
        return ContractState.get(player.getServer()).ownedBy(player.getUuid()).stream().filter(c->c.definition().id().equals(definition)).filter(c->!com.witchersbestiary.contracts.procedural.ProceduralHunts.isDefinition(definition)||c.status()==ContractInstance.Status.ACCEPTED||c.status()==ContractInstance.Status.COMPLETED).findFirst().orElse(null);
    }
    @Override public boolean onButtonClick(PlayerEntity player,int button){
        if(!(player instanceof ServerPlayerEntity server)||server.currentScreenHandler!=this||!canUse(server))return false;
        int index=button%32;if(button<0||button>=64||index>=offers.size())return false;
        int tick=server.getServer().getTicks();if(tick-lastAction<5)return false;lastAction=tick;
        var id=offers.get(index);String status="updated";
        try {
            if(Contracts.DEFINITIONS.find(id)==null)throw new IllegalStateException("unavailable");
            var existing=existing(server,id);
            if(button<32){if(existing!=null)throw new IllegalStateException("already");Contracts.assign(server,id);}
            else {if(existing==null)throw new IllegalStateException("unavailable");Contracts.claim(server,existing.id());}
        }catch(IllegalArgumentException|IllegalStateException e){status=button<32&&existing(server,id)!=null?"already":"rejected";}
        send(server,status);return true;
    }
    public JsonObject view(ServerPlayerEntity player,String message){
        var j=new JsonObject();j.addProperty("message",message);var array=new JsonArray();
        for(var id:offers){var live=Contracts.DEFINITIONS.find(id);if(live==null)continue;
            var current=existing(player,id);var definition=current==null?live:current.definition();var row=definition.toJson();
            if(current!=null&&current.procedural!=null){row.addProperty("generated_name",current.procedural.nameKey());row.addProperty("generated_epithet",current.procedural.epithetKey());row.addProperty("rarity",current.procedural.tier().name());}else if((id.equals(com.witchersbestiary.contracts.hunt.SwampLord.ID)||id.equals(com.witchersbestiary.contracts.hunt.GraveDevourer.ID)))row.addProperty("rarity","LEGENDARY");
            if(current!=null&&current.hunt!=null){row.addProperty("hunt_status",current.hunt.status.name().toLowerCase(java.util.Locale.ROOT));row.addProperty("hunt_area",(Math.floorDiv((current.hunt.position==null?current.hunt.origin:current.hunt.position).getX(),128)*128)+", "+(Math.floorDiv((current.hunt.position==null?current.hunt.origin:current.hunt.position).getZ(),128)*128));}
            if(current!=null&&current.investigation!=null){var investigation=current.investigation;row.addProperty("investigation_stage",investigation.stage.name().toLowerCase(java.util.Locale.ROOT));row.addProperty("clues",investigation.current);row.addProperty("clue_total",investigation.clues.size());if(investigation.current>0)row.addProperty("last_finding",com.witchersbestiary.contracts.tracking.Investigations.description(current,investigation.clues.get(investigation.current-1)));}
            row.addProperty("button",offers.indexOf(id));row.addProperty("state",current==null?"AVAILABLE":current.status().name());row.addProperty("progress",current==null?0:current.progress());array.add(row);
        }j.add("offers",array);return j;
    }
    public void send(ServerPlayerEntity player,String message){if(ServerPlayNetworking.canSend(player,BoardSnapshot.ID))ServerPlayNetworking.send(player,new BoardSnapshot(syncId,view(player,message).toString()));}
    @Override public void sendContentUpdates(){super.sendContentUpdates();if(viewer instanceof ServerPlayerEntity p&&canUse(p)&&p.getServer().getTicks()-lastSync>=20){lastSync=p.getServer().getTicks();send(p,"");}}
}
