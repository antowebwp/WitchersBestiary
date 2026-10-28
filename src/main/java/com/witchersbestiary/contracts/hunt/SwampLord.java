package com.witchersbestiary.contracts.hunt;

import com.witchersbestiary.contracts.*;
import com.witchersbestiary.entity.ModEntities;
import com.witchersbestiary.entity.monster.DrownerEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.*;
import net.minecraft.nbt.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import java.util.*;

public final class SwampLord {
 public static final Identifier ID=Identifier.of("witchersbestiary:swamp_lord");
 private static final Map<MinecraftServer,HuntSearchQueue> QUEUES=new WeakHashMap<>();
 private static final Map<UUID,Long> LAST_CHUNK=new HashMap<>();
 private static final Map<UUID,ServerBossBar> BARS=new HashMap<>();
 public static boolean hasBossBar(UUID target){return BARS.containsKey(target);}
 public static int bossBars(){return BARS.size();}
 public static boolean named(ContractInstance c){return c.definition().id().equals(ID);}
 public static void accepted(ServerPlayerEntity p,ContractInstance c){
  if(!named(c))return;
  c.hunt=new SwampHunt(p.getServerWorld().getRegistryKey().getValue().toString(),p.getBlockPos());
  ContractState.get(p.getServer()).markDirty();enqueue(p,c);p.sendMessage(Text.translatable("hunt.witchersbestiary.started"),false);
 }
 public static void validateAccept(ServerPlayerEntity p,Identifier id){if(id.equals(ID)&&ContractState.get(p.getServer()).ownedBy(p.getUuid()).stream().anyMatch(c->c.hunt!=null&&c.status()==ContractInstance.Status.ACCEPTED))throw new IllegalStateException("One active hunt per player");}
 private static void enqueue(ServerPlayerEntity p,ContractInstance c){
  final int min=Math.max(48,Math.min(999,Integer.getInteger("witchersbestiary.huntMinRadius",150))),max=Math.max(min+1,Math.min(1000,Integer.getInteger("witchersbestiary.huntMaxRadius",500)));
  var h=c.hunt;if(h==null||h.target!=null||c.status()!=ContractInstance.Status.ACCEPTED)return;
  QUEUES.computeIfAbsent(p.getServer(),s->new HuntSearchQueue()).enqueue(new HuntSearchQueue.Search(){int probes;
   public UUID owner(){return p.getUuid();}
   public HuntSearchQueue.Outcome step(){
    if(p.isRemoved()||c.status()!=ContractInstance.Status.ACCEPTED||!p.getServerWorld().getRegistryKey().getValue().toString().equals(h.dimension))return HuntSearchQueue.Outcome.CANCELLED;
    if(++probes>128){h.status=SwampHunt.Status.WAITING;ContractState.get(p.getServer()).markDirty();return HuntSearchQueue.Outcome.WAITING;}
    int n=h.attempts++;double angle=n*2.399963229728653;int radius=min+Math.floorMod(n*73,max-min);int x=h.origin.getX()+(int)(Math.cos(angle)*radius),z=h.origin.getZ()+(int)(Math.sin(angle)*radius);
    var w=p.getServerWorld();var chunk=w.getChunkManager().getWorldChunk(x>>4,z>>4);if(chunk==null)return HuntSearchQueue.Outcome.CONTINUE;
    var at=new BlockPos(x,chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x&15,z&15)+1,z);
    if(!safe(w,at,true)||p.squaredDistanceTo(at.toCenterPos())<48*48)return HuntSearchQueue.Outcome.CONTINUE;
    return spawn(p,c,at)?HuntSearchQueue.Outcome.FOUND:HuntSearchQueue.Outcome.WAITING;
   }
  });
 }
 public static boolean safe(ServerWorld w,BlockPos at,boolean habitat){
  // Require the complete footprint in an already loaded chunk before any world collision lookup.
  for(int dx=-2;dx<=2;dx+=4)for(int dz=-2;dz<=2;dz+=4)if(w.getChunkManager().getWorldChunk((at.getX()+dx)>>4,(at.getZ()+dz)>>4)==null)return false;
  if(w.isOutOfHeightLimit(at.up(3))||!w.getWorldBorder().contains(at)||!w.getFluidState(at).isEmpty()||!w.getBlockState(at.down()).isIn(BlockTags.DIRT))return false;
  if(!w.isSkyVisible(at.up(3))||!w.isSpaceEmpty(new Box(at).expand(.35,0,.35).stretch(0,2,0)))return false;
  if(w.isNearOccupiedPointOfInterest(at))return false;
  if(habitat){var biome=w.getBiome(at);boolean water=biome.isIn(BiomeTags.IS_RIVER)||biome.matchesKey(net.minecraft.world.biome.BiomeKeys.SWAMP)||biome.matchesKey(net.minecraft.world.biome.BiomeKeys.MANGROVE_SWAMP);
   for(int dx=-2;dx<=2&&!water;dx++)for(int dz=-2;dz<=2;dz++)if(w.getFluidState(at.add(dx,-1,dz)).isIn(FluidTags.WATER))water=true;
   if(!water)return false;
  }return true;
 }
 public static boolean spawn(ServerPlayerEntity p,ContractInstance c,BlockPos at){
  if(!named(c)||c.hunt==null||!p.getServerWorld().getRegistryKey().getValue().toString().equals(c.hunt.dimension)||c.hunt.target!=null||c.status()!=ContractInstance.Status.ACCEPTED||!safe(p.getServerWorld(),at,true))return false;
  var e=ModEntities.DROWNER.create(p.getServerWorld());if(e==null)return false;var h=c.hunt;
  e.huntContract=c.id();e.setPersistent();e.setCustomName(Text.translatable("hunt.witchersbestiary.swamp_lord"));e.setCustomNameVisible(true);e.setBaby(false);
  e.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(108);e.setHealth(108);e.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(9);e.getAttributeInstance(EntityAttributes.GENERIC_SCALE).setBaseValue(1.12);com.witchersbestiary.combat.ProgressiveDifficulty.freshSpawn(e);
  e.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);if(!p.getServerWorld().doesNotIntersectEntities(e))return false;h.target=e.getUuid();h.position=at.toImmutable();h.status=SwampHunt.Status.RESERVED;
  ContractState.get(p.getServer()).markDirty();
  if(!p.getServerWorld().spawnEntity(e)){h.target=null;h.position=null;h.status=SwampHunt.Status.WAITING;ContractState.get(p.getServer()).markDirty();return false;}h.status=SwampHunt.Status.ACTIVE;ContractState.get(p.getServer()).markDirty();return true;
 }
 public static boolean death(LivingEntity e){
  if(!(e instanceof DrownerEntity d)||!(e.getWorld() instanceof ServerWorld w))return false;
  if(d.huntParent!=null){var parent=ContractState.get(w.getServer()).find(d.huntParent);if(parent!=null&&parent.hunt!=null&&parent.hunt.target!=null&&w.getEntity(parent.hunt.target) instanceof DrownerEntity boss)for(int i=0;i<2;i++)if(d.getUuid().equals(boss.huntHelpers[i]))boss.huntHelpers[i]=null;return true;}if(d.huntContract==null)return false;
  var store=ContractState.get(w.getServer());var c=store.find(d.huntContract);
  if(c!=null&&c.hunt!=null&&d.getUuid().equals(c.hunt.target)&&!e.isAlive()&&c.status()==ContractInstance.Status.ACCEPTED){
   c.hunt.status=SwampHunt.Status.DEAD;c.credit(d.getUuid(),c.owner(),c.definition().target(),System.currentTimeMillis());store.markDirty();
  }cleanup(d,w);return true;
 }
 private static void cleanup(DrownerEntity d,ServerWorld w){for(UUID id:d.huntHelpers){if(id==null)continue;var e=w.getEntity(id);if(e!=null)e.discard();}var bar=BARS.remove(d.getUuid());if(bar!=null)bar.clearPlayers();}
 public static void tick(DrownerEntity d){
  if(!(d.getWorld() instanceof ServerWorld w))return;
  if(d.huntParent!=null){if(d.age%20==0){var c=ContractState.get(w.getServer()).find(d.huntParent);if(c==null||c.status()!=ContractInstance.Status.ACCEPTED||c.hunt==null||c.hunt.status==SwampHunt.Status.DEAD||c.hunt.status==SwampHunt.Status.CANCELLED)d.discard();}return;}
  if(d.huntContract==null)return;
  if(!d.isAlive()){cleanup(d,w);return;}
  var store=ContractState.get(w.getServer());var c=store.find(d.huntContract);
  if(c==null||c.hunt==null||!d.getUuid().equals(c.hunt.target)||c.status()!=ContractInstance.Status.ACCEPTED){cleanup(d,w);d.discard();return;}
  if(c.hunt.status==SwampHunt.Status.DEAD){cleanup(d,w);d.discard();return;}
  if(d.age%10==0){var bar=BARS.computeIfAbsent(d.getUuid(),id->new ServerBossBar(Text.translatable("hunt.witchersbestiary.swamp_lord"),BossBar.Color.GREEN,BossBar.Style.PROGRESS));bar.setPercent(d.getHealth()/d.getMaxHealth());
   for(var p:w.getServer().getPlayerManager().getPlayerList()){boolean near=!p.isSpectator()&&p.getServerWorld()==w&&p.squaredDistanceTo(d)<32*32;if(near)bar.addPlayer(p);else bar.removePlayer(p);}
  }
  if(d.huntDash>0)d.huntDash--;if(d.huntSummon>0)d.huntSummon--;
  var target=d.getTarget();if(target==null||!target.isAlive())return;
  if(d.huntWindup>0){d.getNavigation().stop();d.setVelocity(0,d.getVelocity().y,0);if(--d.huntWindup==0){var v=target.getPos().subtract(d.getPos()).multiply(1,0,1).normalize().multiply(.8);d.setVelocity(v.x,.2,v.z);d.velocityModified=true;d.huntDash=120;}return;}
  if(d.huntDash==0&&d.squaredDistanceTo(target)>9&&d.squaredDistanceTo(target)<64){d.huntWindup=15;d.getNavigation().stop();w.playSound(null,d.getBlockPos(),SoundEvents.ENTITY_DROWNED_SHOOT,SoundCategory.HOSTILE,1,.65f);w.spawnParticles(ParticleTypes.SPLASH,d.getX(),d.getY()+1,d.getZ(),8,.3,.3,.3,0);}
  if(d.huntSummon==0){
   for(int i=0;i<2;i++)if(d.huntHelpers[i]==null){var at=d.getBlockPos().add(i==0?2:-2,0,0);if(safe(w,at,false)){var helper=ModEntities.DROWNER.create(w);if(helper!=null){helper.huntParent=c.id();helper.setPersistent();helper.setBaby(false);helper.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);helper.setTarget(target);if(w.spawnEntity(helper))d.huntHelpers[i]=helper.getUuid();}}}
   d.huntSummon=300;
  }
 }
 public static void write(DrownerEntity d,NbtCompound n){if(d.huntContract!=null)n.putUuid("WbHunt",d.huntContract);if(d.huntParent!=null)n.putUuid("WbHuntParent",d.huntParent);n.putInt("WbDash",d.huntDash);n.putInt("WbSummon",d.huntSummon);n.putInt("WbWindup",d.huntWindup);for(int i=0;i<2;i++)if(d.huntHelpers[i]!=null)n.putUuid("WbHelper"+i,d.huntHelpers[i]);}
 public static void read(DrownerEntity d,NbtCompound n){d.huntContract=n.containsUuid("WbHunt")?n.getUuid("WbHunt"):null;d.huntParent=n.containsUuid("WbHuntParent")?n.getUuid("WbHuntParent"):null;d.huntDash=n.getInt("WbDash");d.huntSummon=n.getInt("WbSummon");d.huntWindup=n.getInt("WbWindup");for(int i=0;i<2;i++)d.huntHelpers[i]=n.containsUuid("WbHelper"+i)?n.getUuid("WbHelper"+i):null;}
 public static void initialize(){
  ServerLifecycleEvents.SERVER_STARTED.register(server->{var store=ContractState.get(server);if(store.readOnly())return;for(var c:store.all())if(named(c)&&c.hunt!=null&&c.hunt.status==SwampHunt.Status.DEAD&&c.status()==ContractInstance.Status.ACCEPTED){c.credit(c.hunt.target,c.owner(),c.definition().target(),System.currentTimeMillis());store.markDirty();}});
  net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(net.minecraft.server.command.CommandManager.literal("wbcontracts").requires(source->source.hasPermissionLevel(2)).then(net.minecraft.server.command.CommandManager.literal("hunt").executes(context->{var p=context.getSource().getPlayerOrThrow();for(var c:ContractState.get(p.getServer()).ownedBy(p.getUuid()))if(c.hunt!=null)context.getSource().sendFeedback(()->Text.literal(c.id()+" "+c.hunt.status+" target="+c.hunt.target+" position="+c.hunt.position),false);return 1;}))));
  ServerTickEvents.END_SERVER_TICK.register(s->{var q=QUEUES.get(s);if(q!=null&&q.pending()>0)q.tick();if(s.getTicks()%20==0)for(var p:s.getPlayerManager().getPlayerList()){
   long chunk=p.getChunkPos().toLong();var previous=LAST_CHUNK.put(p.getUuid(),chunk);
   if(previous!=null&&previous==chunk)continue;
   for(var c:ContractState.get(s).ownedBy(p.getUuid()))if(c.hunt!=null&&c.hunt.target==null&&c.status()==ContractInstance.Status.ACCEPTED)enqueue(p,c);
  }});
  net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler,s)->{LAST_CHUNK.remove(handler.player.getUuid());var q=QUEUES.get(s);if(q!=null)q.cancel(handler.player.getUuid());BARS.values().forEach(b->b.removePlayer(handler.player));});
  ServerEntityEvents.ENTITY_LOAD.register((e,w)->{if(e instanceof DrownerEntity d&&d.huntContract!=null){var store=ContractState.get(w.getServer());var c=store.find(d.huntContract);if(c!=null&&c.hunt!=null&&d.getUuid().equals(c.hunt.target)&&c.hunt.status==SwampHunt.Status.RESERVED){c.hunt.status=SwampHunt.Status.ACTIVE;store.markDirty();}}});
  ServerEntityEvents.ENTITY_UNLOAD.register((e,w)->{var b=BARS.remove(e.getUuid());if(b!=null)b.clearPlayers();});
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{var q=QUEUES.remove(s);if(q!=null)q.clear();BARS.values().forEach(ServerBossBar::clearPlayers);BARS.clear();LAST_CHUNK.clear();});
 }
}
