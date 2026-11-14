package com.witchersbestiary.test;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import com.witchersbestiary.world.graveyard.VampireTomb;
public final class TombProbe implements ModInitializer {
 public void onInitialize() {
  if (!Boolean.getBoolean("witchersbestiary.stage19Locate")) return;
  ServerLifecycleEvents.SERVER_STARTED.register(server -> {
   try {
    var w=server.getOverworld();
    var pos=w.locateStructure(TagKey.of(RegistryKeys.STRUCTURE,VampireTomb.ID),w.getSpawnPos(),48,false);
    if(pos==null) throw new IllegalStateException("No tomb near "+w.getSpawnPos());
    var piece=w.getChunk(pos.getX()>>4,pos.getZ()>>4).getStructureStarts().values().stream().flatMap(s->s.getChildren().stream()).filter(VampireTomb.Chamber.class::isInstance).map(VampireTomb.Chamber.class::cast).findFirst().orElseThrow();
    var entry=piece.entry();var arena=piece.arena();
    for(int z=0;z<20;z++){ var at=entry.add(0,-z,z); if(!w.getBlockState(at).isOf(net.minecraft.block.Blocks.DEEPSLATE_BRICK_STAIRS)||!w.getBlockState(at.up(2)).isAir())throw new IllegalStateException("Obstructed stair "+at); }
    if(!w.isSpaceEmpty(new net.minecraft.util.math.Box(arena).expand(4,0,4).stretch(0,6,0)))throw new IllegalStateException("Obstructed arena");
    System.out.println("TOMB_PASS seed="+w.getSeed()+" entry="+entry.toShortString()+" arena="+arena.toShortString());
    var owner=new net.minecraft.server.network.ServerPlayerEntity(server,w,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TombFixture"),net.minecraft.network.packet.c2s.common.SyncedClientOptions.createDefault());
    owner.networkHandler=new net.minecraft.server.network.ServerPlayNetworkHandler(server,new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND),owner,net.minecraft.server.network.ConnectedClientData.createDefault(owner.getGameProfile(),false));
    var contract=com.witchersbestiary.contracts.Contracts.assignPrepared(owner,com.witchersbestiary.contracts.hunt.VampireHunt.ID,null,entry.north(90));
    for(var clue:contract.investigation.clues){
      owner.setPosition(clue.position.toCenterPos());
      if(!com.witchersbestiary.contracts.tracking.Investigations.inspect(owner,clue.id))throw new IllegalStateException("Clue rejected");
    }
    owner.setPosition(arena.toCenterPos().add(0,0,-4));
    if(!com.witchersbestiary.contracts.tracking.Investigations.trySpawn(owner,contract))throw new IllegalStateException("Awakening rejected");
    if(com.witchersbestiary.contracts.tracking.Investigations.trySpawn(owner,contract))throw new IllegalStateException("Duplicate");
    System.out.println("TOMB_ROUTE_PASS clues="+contract.investigation.current+" boss="+contract.vampire);
   } finally {server.stop(false);}
  });
 }
}

