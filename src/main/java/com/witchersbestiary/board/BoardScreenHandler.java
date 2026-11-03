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
    private String village;
    private boolean legendary;
    private VillageOffers.Generation generation;
    private final Map<Integer,UUID> archived=new HashMap<>();
    private List<String> eligible(){return Contracts.DEFINITIONS.all().keySet().stream().filter(com.witchersbestiary.contracts.procedural.ProceduralHunts::isDefinition).map(Object::toString).sorted().toList();}
    private VillageOffers.Generation current(ServerPlayerEntity p){return VillageOffers.get(p.getServer()).personal(village,p.getUuid(),p.getServer().getOverworld().getTime(),eligible());}
    private ContractInstance instance(ServerPlayerEntity p,int index){if(legendary)return existing(p,offers.get(index));if(archived.containsKey(index))return ContractState.get(p.getServer()).find(archived.get(index));var offer=generation.offers().get(index).id();return ContractState.get(p.getServer()).ownedBy(p.getUuid()).stream().filter(c->offer.equals(c.offer)).findFirst().orElse(null);}

    public BoardScreenHandler(int sync,PlayerInventory inventory,BlockPos root){
        super(NoticeBoards.SCREEN,sync);this.root=root.toImmutable();this.owner=inventory.player.getUuid();this.viewer=inventory.player;
        if(inventory.player instanceof ServerPlayerEntity p){village=VillageOffers.identity(p.getServerWorld(),root);generation=current(p);ContractState.get(p.getServer()).compact(VillageOffers.get(p.getServer())::live);var list=new ArrayList<>(generation.offers().stream().map(o->Identifier.of(o.definition())).toList());for(var c:ContractState.get(p.getServer()).ownedBy(owner)){if(list.size()>=30)break;if((c.status()==ContractInstance.Status.ACCEPTED||c.status()==ContractInstance.Status.COMPLETED)&&generation.offers().stream().noneMatch(o->o.id().equals(c.offer))){archived.put(list.size(),c.id());list.add(c.definition().id());}}offers=List.copyOf(list);}
    }
    public BlockPos boardPos(){return root;}
    @Override public boolean canUse(PlayerEntity player){
        if(!owner.equals(player.getUuid())||player.squaredDistanceTo(root.toCenterPos().add(0,1,0))>64)return false;
        var state=player.getWorld().getBlockState(root);
        return state.isOf(NoticeBoards.BLOCK)&&NoticeBoardBlock.complete(player.getWorld(),root,state.get(NoticeBoardBlock.FACING));
    }
    @Override public ItemStack quickMove(PlayerEntity p,int slot){return ItemStack.EMPTY;}
    public static ContractInstance existing(ServerPlayerEntity player,Identifier definition){
        return ContractState.get(player.getServer()).ownedBy(player.getUuid()).stream().filter(c->c.definition().id().equals(definition)).filter(c->!com.witchersbestiary.contracts.hunt.SwampLord.named(c)||c.status()!=ContractInstance.Status.CANCELLED&&c.status()!=ContractInstance.Status.EXPIRED).filter(c->!com.witchersbestiary.contracts.procedural.ProceduralHunts.isDefinition(definition)||c.status()==ContractInstance.Status.ACCEPTED||c.status()==ContractInstance.Status.COMPLETED).findFirst().orElse(null);
    }
    @Override public boolean onButtonClick(PlayerEntity player,int button){
        if(!(player instanceof ServerPlayerEntity server)||server.currentScreenHandler!=this||!canUse(server))return false;
        if(button==65){legendary=!legendary;offers=legendary?List.of(com.witchersbestiary.contracts.hunt.SwampLord.ID,com.witchersbestiary.contracts.hunt.GraveDevourer.ID):generation.offers().stream().map(o->Identifier.of(o.definition())).toList();archived.clear();send(server,"");return true;}
        if(button==64){try{VillageOffers.get(server.getServer()).refresh(village,owner,server.getServer().getOverworld().getTime(),eligible(),o->ContractState.get(server.getServer()).ownedBy(owner).stream().anyMatch(c->o.id().equals(c.offer)&&c.status()==ContractInstance.Status.REWARDED));server.closeHandledScreen();}catch(IllegalStateException e){}send(server,"");return true;}
        if(!legendary&&button<32&&!current(server).id().equals(generation.id())){send(server,"rejected");return false;}
        int index=button%32;if(button<0||button>=64||index>=offers.size())return false;
        int tick=server.getServer().getTicks();if(tick-lastAction<5)return false;lastAction=tick;
        var id=offers.get(index);String status="updated";
        try {
            if(Contracts.DEFINITIONS.find(id)==null)throw new IllegalStateException("unavailable");
            var existing=instance(server,index);
            if(button<32){if(existing!=null)throw new IllegalStateException("already");var accepted=legendary?Contracts.assign(server,id):Contracts.assign(server,id,generation.offers().get(index).id().getMostSignificantBits());if(!legendary){accepted.village=village;accepted.offer=generation.offers().get(index).id();accepted.generation=generation.id();}ContractState.get(server.getServer()).markDirty();}
            else {if(existing==null)throw new IllegalStateException("unavailable");Contracts.claim(server,existing.id());}
        }catch(IllegalArgumentException|IllegalStateException e){status=button<32&&instance(server,index)!=null?"already":"rejected";}
        send(server,status);return true;
    }
    public JsonObject view(ServerPlayerEntity player,String message){
        var j=new JsonObject();j.addProperty("message",message);j.addProperty("village",village);j.addProperty("refresh_days",Math.max(0,(generation.created()+VillageOffers.INTERVAL-player.getServer().getOverworld().getTime()+23999)/24000));var array=new JsonArray();
        for(int rowIndex=0;rowIndex<offers.size();rowIndex++){var id=offers.get(rowIndex);var live=Contracts.DEFINITIONS.find(id);if(live==null)continue;
            var current=instance(player,rowIndex);var definition=current==null?live:current.definition();if(!legendary&&current==null&&rowIndex<5){var o=generation.offers().get(rowIndex);var profile=com.witchersbestiary.contracts.procedural.ProceduralProfile.generate(o.id().getMostSignificantBits(),com.witchersbestiary.contracts.procedural.ContractSpecies.species(live.target().getPath()));definition=com.witchersbestiary.contracts.procedural.ProceduralHunts.snapshot(live,profile);}
            if(current==null)definition=com.witchersbestiary.economy.CrownRewards.fresh(definition);
            var row=definition.toJson();
            if(!legendary&&current==null&&rowIndex<5){var o=generation.offers().get(rowIndex);var profile=com.witchersbestiary.contracts.procedural.ProceduralProfile.generate(o.id().getMostSignificantBits(),com.witchersbestiary.contracts.procedural.ContractSpecies.species(live.target().getPath()));row.addProperty("rarity",profile.tier().name());}
            if(current!=null&&current.procedural!=null){row.addProperty("generated_name",current.procedural.nameKey());row.addProperty("generated_epithet",current.procedural.epithetKey());row.addProperty("rarity",current.procedural.tier().name());}else if((id.equals(com.witchersbestiary.contracts.hunt.SwampLord.ID)||id.equals(com.witchersbestiary.contracts.hunt.GraveDevourer.ID)))row.addProperty("rarity","LEGENDARY");
            if(current!=null&&current.hunt!=null){row.addProperty("hunt_status",current.hunt.status.name().toLowerCase(java.util.Locale.ROOT));row.addProperty("hunt_area",(Math.floorDiv((current.hunt.position==null?current.hunt.origin:current.hunt.position).getX(),128)*128)+", "+(Math.floorDiv((current.hunt.position==null?current.hunt.origin:current.hunt.position).getZ(),128)*128));}
            if(current!=null&&current.investigation!=null){var investigation=current.investigation;row.addProperty("investigation_stage",investigation.stage.name().toLowerCase(java.util.Locale.ROOT));row.addProperty("clues",investigation.current);row.addProperty("clue_total",investigation.clues.size());if(investigation.current>0)row.addProperty("last_finding",com.witchersbestiary.contracts.tracking.Investigations.description(current,investigation.clues.get(investigation.current-1)));}
            row.addProperty("button",rowIndex);row.addProperty("state",current==null?"AVAILABLE":current.status().name());row.addProperty("progress",current==null?0:current.progress());array.add(row);
        }j.add("offers",array);return j;
    }
    public void send(ServerPlayerEntity player,String message){if(ServerPlayNetworking.canSend(player,BoardSnapshot.ID))ServerPlayNetworking.send(player,new BoardSnapshot(syncId,view(player,message).toString()));}
    @Override public void sendContentUpdates(){super.sendContentUpdates();if(viewer instanceof ServerPlayerEntity p&&canUse(p)&&p.getServer().getTicks()-lastSync>=20){lastSync=p.getServer().getTicks();send(p,"");}}
}
