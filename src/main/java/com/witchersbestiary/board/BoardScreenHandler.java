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
    private final Map<Integer,String> failures=new HashMap<>();
    private static final List<String> LEGENDS = List.of("witchersbestiary:swamp_lord", "witchersbestiary:grave_devourer");
    private String rotationKey() { return legendary ? village + "/legends" : village; }
    private List<String> eligible(){return legendary ? LEGENDS : Contracts.DEFINITIONS.all().keySet().stream().filter(com.witchersbestiary.contracts.procedural.ProceduralHunts::isDefinition).map(Object::toString).sorted().toList();}
    private VillageOffers.Generation current(ServerPlayerEntity p){return VillageOffers.get(p.getServer()).personal(rotationKey(),p.getUuid(),p.getServer().getOverworld().getTime(),eligible());}
    private ContractInstance instance(ServerPlayerEntity p,int index){
        var state=ContractState.get(p.getServer());
        if(archived.containsKey(index))return state.find(archived.get(index));
        var offer=generation.offers().get(index);
        return state.ownedBy(owner).stream().filter(c->offer.id().equals(c.offer) || legendary && c.definition().id().toString().equals(offer.definition()) && (c.status()==ContractInstance.Status.ACCEPTED || c.status()==ContractInstance.Status.COMPLETED)).findFirst().orElse(null);
    }
    private void reloadOffers(ServerPlayerEntity p) {
        generation=current(p);
        archived.clear();
        failures.clear();
        var list=new ArrayList<>(generation.offers().stream().map(o->Identifier.of(o.definition())).toList());
        if(!legendary) for(var c:ContractState.get(p.getServer()).ownedBy(owner)){
            if(list.size()>=30)break;
            if((c.status()==ContractInstance.Status.ACCEPTED||c.status()==ContractInstance.Status.COMPLETED)&&generation.offers().stream().noneMatch(o->o.id().equals(c.offer))){archived.put(list.size(),c.id());list.add(c.definition().id());}
        }
        offers=List.copyOf(list);
    }

    public BoardScreenHandler(int sync,PlayerInventory inventory,BlockPos root){
        super(NoticeBoards.SCREEN,sync);this.root=root.toImmutable();this.owner=inventory.player.getUuid();this.viewer=inventory.player;
        if(inventory.player instanceof ServerPlayerEntity p){village=VillageOffers.identity(p.getServerWorld(),root);reloadOffers(p);ContractState.get(p.getServer()).compact(VillageOffers.get(p.getServer())::live);}
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
    public boolean cancel(ServerPlayerEntity player,UUID instanceId){
        if(player.currentScreenHandler!=this||!canUse(player))return false;
        var c=ContractState.get(player.getServer()).find(instanceId);
        if(c==null||!c.owner().equals(player.getUuid())||c.status()!=ContractInstance.Status.ACCEPTED)return false;
        boolean visible=false;for(int index=0;index<offers.size();index++){var shown=instance(player,index);if(shown!=null&&shown.id().equals(instanceId)){visible=true;break;}}
        if(!visible)return false;
        Contracts.cancel(player,instanceId);send(player,"cancelled");return true;
    }
    @Override public boolean onButtonClick(PlayerEntity player,int button){
        if(!(player instanceof ServerPlayerEntity server)||server.currentScreenHandler!=this||!canUse(server))return false;
        if(button==65){legendary=!legendary;reloadOffers(server);send(server,"");return true;}
        if(button==64){
            try{VillageOffers.get(server.getServer()).refresh(rotationKey(),owner,server.getServer().getOverworld().getTime(),eligible(),o->ContractState.get(server.getServer()).ownedBy(owner).stream().anyMatch(c->o.id().equals(c.offer)&&c.status()==ContractInstance.Status.REWARDED));reloadOffers(server);send(server,"updated");}
            catch(IllegalStateException e){send(server,"rejected");}
            return true;
        }
        if(button<32&&!current(server).id().equals(generation.id())){reloadOffers(server);send(server,"rejected");return false;}
        int index=button%32;if(button<0||button>=64||index>=offers.size())return false;
        int tick=server.getServer().getTicks();if(tick-lastAction<5)return false;lastAction=tick;
        var id=offers.get(index);String status="updated";
        try {
            if(Contracts.DEFINITIONS.find(id)==null)throw new IllegalStateException("unavailable");
            var existing=instance(server,index);
            if(button<32){if(existing!=null)throw new IllegalStateException("already");
                if(!legendary){
                    if(com.witchersbestiary.contracts.hunt.HuntTerrain.pending(owner)){send(server,"preparing");return true;}
                    var expected=generation.id();var offer=generation.offers().get(index);
                    com.witchersbestiary.contracts.hunt.SwampLord.validateAccept(server,id);
                    com.witchersbestiary.contracts.hunt.HuntTerrain.request(server,root,()->server.currentScreenHandler==this&&canUse(server)&&!legendary&&generation.id().equals(expected)&&current(server).id().equals(expected),()->{
                        try{var accepted=Contracts.assignPrepared(server,id,offer.id().getMostSignificantBits(),root);accepted.village=village;accepted.offer=offer.id();accepted.generation=expected;ContractState.get(server.getServer()).markDirty();return true;}catch(IllegalStateException e){if(e.getMessage()!=null&&e.getMessage().startsWith("No safe loaded hunt area"))return false;throw e;}
                    },success->{if(!success)failures.put(index,"no_safe_route");send(server,success?"updated":"no_safe_route");});
                    send(server,"preparing");return true;
                }
                var accepted=Contracts.assignPrepared(server,id,null,root);
                accepted.village=village;
                accepted.offer=generation.offers().get(index).id();
                accepted.generation=generation.id();
                ContractState.get(server.getServer()).markDirty();
            }
            else {if(existing==null)throw new IllegalStateException("unavailable");Contracts.claim(server,existing.id());}
        }catch(IllegalArgumentException|IllegalStateException e){status=button<32&&instance(server,index)!=null?"already":e.getMessage()!=null&&e.getMessage().startsWith("No safe loaded hunt area")?"no_safe_route":"rejected";if(status.equals("no_safe_route"))failures.put(index,status);}
        send(server,status);return true;
    }
    public JsonObject view(ServerPlayerEntity player,String message){
        var j=new JsonObject();j.addProperty("message",message);j.addProperty("preparing",com.witchersbestiary.contracts.hunt.HuntTerrain.pending(owner));j.addProperty("village",village);j.addProperty("refresh_days",Math.max(0,(generation.created()+VillageOffers.INTERVAL-player.getServer().getOverworld().getTime()+23999)/24000));var array=new JsonArray();
        for(int rowIndex=0;rowIndex<offers.size();rowIndex++){var id=offers.get(rowIndex);var live=Contracts.DEFINITIONS.find(id);if(live==null)continue;
            var current=instance(player,rowIndex);var definition=current==null?live:current.definition();if(!legendary&&current==null&&rowIndex<5){var o=generation.offers().get(rowIndex);var profile=com.witchersbestiary.contracts.procedural.ProceduralProfile.generate(o.id().getMostSignificantBits(),com.witchersbestiary.contracts.procedural.ContractSpecies.species(live.target().getPath()));definition=com.witchersbestiary.contracts.procedural.ProceduralHunts.snapshot(live,profile);}
            if(current==null)definition=com.witchersbestiary.economy.CrownRewards.fresh(definition);
            var row=definition.toJson();
            if(!legendary&&current==null&&rowIndex<5){var o=generation.offers().get(rowIndex);var profile=com.witchersbestiary.contracts.procedural.ProceduralProfile.generate(o.id().getMostSignificantBits(),com.witchersbestiary.contracts.procedural.ContractSpecies.species(live.target().getPath()));row.addProperty("rarity",profile.tier().name());}
            if(current!=null&&current.procedural!=null){row.addProperty("generated_name",current.procedural.nameKey());row.addProperty("generated_epithet",current.procedural.epithetKey());row.addProperty("rarity",current.procedural.tier().name());}else if((id.equals(com.witchersbestiary.contracts.hunt.SwampLord.ID)||id.equals(com.witchersbestiary.contracts.hunt.GraveDevourer.ID)))row.addProperty("rarity","LEGENDARY");
            if(current!=null&&current.hunt!=null){row.addProperty("hunt_status",current.hunt.status.name().toLowerCase(java.util.Locale.ROOT));row.addProperty("hunt_dimension",current.hunt.dimension);var i=current.investigation;var destination=i!=null?(i.stage==com.witchersbestiary.contracts.tracking.ContractInvestigation.Stage.TRACKING&&i.next()!=null?i.next().position:i.lair):current.hunt.position;if(destination!=null)row.addProperty("hunt_area",(Math.floorDiv(destination.getX(),16)*16+8)+", "+(Math.floorDiv(destination.getZ(),16)*16+8));}
            if(current!=null&&current.investigation!=null){var investigation=current.investigation;row.addProperty("investigation_stage",investigation.stage.name().toLowerCase(java.util.Locale.ROOT));row.addProperty("clues",investigation.current);row.addProperty("clue_total",investigation.clues.size());if(investigation.current>0)row.addProperty("last_finding",com.witchersbestiary.contracts.tracking.Investigations.description(current,investigation.clues.get(investigation.current-1)));}
            if(current!=null)row.addProperty("instance",current.id().toString());
            if(current==null&&failures.containsKey(rowIndex))row.addProperty("unavailable_reason",failures.get(rowIndex));
            if(current!=null&&current.status()==ContractInstance.Status.ACCEPTED&&current.hunt!=null&&current.hunt.status==com.witchersbestiary.contracts.hunt.SwampHunt.Status.WAITING)row.addProperty("recovery_unavailable",true);
            row.addProperty("button",rowIndex);row.addProperty("state",current==null?"AVAILABLE":current.status().name());row.addProperty("progress",current==null?0:current.progress());array.add(row);
        }j.add("offers",array);return j;
    }
    public void send(ServerPlayerEntity player,String message){if(ServerPlayNetworking.canSend(player,BoardSnapshot.ID))ServerPlayNetworking.send(player,new BoardSnapshot(syncId,view(player,message).toString()));}
    @Override public void sendContentUpdates(){super.sendContentUpdates();if(viewer instanceof ServerPlayerEntity p&&canUse(p)&&p.getServer().getTicks()-lastSync>=20){lastSync=p.getServer().getTicks();if(!current(p).id().equals(generation.id()))reloadOffers(p);send(p,com.witchersbestiary.contracts.hunt.HuntTerrain.pending(owner)?"preparing":"");}}
}
