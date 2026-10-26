package com.witchersbestiary.world.graveyard;

import com.witchersbestiary.entity.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.util.math.*;
import net.minecraft.world.Difficulty;
import java.util.HashSet;

/** Player-local encounters. No structure jobs, global index, chunk tickets or world scans. */
public final class GraveyardHaunting {
    private GraveyardHaunting() {}
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTicks()%40!=0)return;
            for(var player:server.getPlayerManager().getPlayerList())if(!player.isSpectator()&&player.isAlive())visit(player);
        });
    }
    public static void visit(ServerPlayerEntity player) {
        var world=player.getServerWorld();var cp=player.getChunkPos();
        // Non-creating lookup: missing chunks/start chunks wait for normal loading.
        var current=world.getChunkManager().getWorldChunk(cp.x,cp.z);
        if(current==null)return;
        var seen=new HashSet<Long>();int examined=0;
        for(var reference:current.getStructureReferences().entrySet()) {
            if(!(reference.getKey() instanceof GraveyardStructure))continue;
            for(long packed:reference.getValue()) {
                if(++examined>16)return;
                if(!seen.add(packed))continue;
                var startCp=new ChunkPos(packed);
                var startChunk=world.getChunkManager().getWorldChunk(startCp.x,startCp.z);
                if(startChunk==null)continue;
                var start=startChunk.getStructureStarts().get(reference.getKey());
                if(start==null||!start.hasChildren())continue;
                for(var part:start.getChildren())if(part instanceof GraveyardPiece p&&p.haunted()&&p.getBoundingBox().contains(player.getBlockPos())) {
                    int before=p.ghostMask();awaken(world,p,player.getY());
                    if(before!=p.ghostMask())startChunk.setNeedsSaving(true);
                }
            }
        }
    }
    public static int awaken(ServerWorld world,GraveyardPiece piece,double visitorY) {
        if(!piece.haunted()||world.getDifficulty()==Difficulty.PEACEFUL)return 0;
        var points=GraveyardContent.ghosts(piece);int spawned=0;
        for(int slot=0;slot<points.size();slot++) {
            if((piece.ghostMask()&(1<<slot))!=0)continue;
            var local=points.get(slot);
            boolean night=com.witchersbestiary.world.NaturalSpawns.specterWindow(ModEntities.WRAITH,world.getTimeOfDay());
            if(!night&&(local.y()>=0||visitorY>piece.surfaceY()-2))continue;
            var pos=piece.position(local.x(),local.y(),local.z());
            var box=new Box(pos.getX()+.1,pos.getY(),pos.getZ()+.1,pos.getX()+.9,pos.getY()+2.7,pos.getZ()+.9);
            boolean loaded=true;
            for(int x=MathHelper.floor(box.minX)>>4;x<=MathHelper.floor(box.maxX)>>4;x++)for(int z=MathHelper.floor(box.minZ)>>4;z<=MathHelper.floor(box.maxZ)>>4;z++)
                if(world.getChunkManager().getWorldChunk(x,z)==null)loaded=false;
            if(!loaded)continue;
            // Consume before the one attempt, including an obstructed/tampered slot.
            // Never infer a refund from a missing/unloaded/dead entity.
            if(!piece.consumeGhost(slot))continue;
            if(!world.getWorldBorder().contains(box)||!world.isSpaceEmpty(box)||world.containsFluid(box))continue;
            var ghost=ModEntities.WRAITH.create(world);
            if(ghost==null)continue;
            ghost.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
            ghost.initialize(world,world.getLocalDifficulty(pos),SpawnReason.STRUCTURE,null);
            ghost.setPersistent();ghost.addCommandTag("wb_graveyard:"+piece.placementKey());ghost.addCommandTag("wb_grave_slot:"+slot);
            if(world.spawnEntity(ghost)) {
                spawned++;
                world.spawnParticles(ParticleTypes.SOUL,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,3,.2,.3,.2,.01);
            }
        }
        if(spawned>0){var pos=piece.position(GraveyardStructure.size(piece.kind())/2,0,GraveyardStructure.size(piece.kind())/2);world.playSound(null,pos,SoundEvents.ENTITY_PHANTOM_AMBIENT,SoundCategory.HOSTILE,.35f,.65f);}
        return spawned;
    }
}
