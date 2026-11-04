package com.witchersbestiary.contracts.tracking;
import com.google.gson.*;
import com.witchersbestiary.contracts.*;
import com.witchersbestiary.contracts.hunt.*;
import com.witchersbestiary.contracts.procedural.ProceduralHunts;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import java.util.*;

/** Owner-scoped logical routes and sparse snapshots; no clue entities, world scans or chunk tickets. */
public final class Investigations {
 public static InvestigationSettings settings=InvestigationSettings.DEFAULT;
 private static final class Session {boolean enabled;Boolean pendingToggle;String dimension="",sent="";UUID active;int nextLookup,lastAction=-100,lastToggle=-100,nextSpawn;}
 private static final Map<UUID,Session> SESSIONS=new HashMap<>();
 public static void accepted(ServerPlayerEntity p,ContractInstance c){if(c.investigation==null)c.investigation=new ContractInvestigation(c.id(),c.owner(),c.hunt.dimension);SESSIONS.computeIfAbsent(p.getUuid(),id->new Session()).active=c.id();var store=ContractState.get(p.getServer());if(store.find(c.id())==c)store.track(p.getUuid(),c.id());sync(p);}
 public static int count(ContractInstance c){return c.procedural==null?settings.legendary():switch(c.procedural.tier()){case COMMON->settings.common();case RARE->settings.rare();case EPIC->settings.epic();};}
 public static boolean safeClue(ServerWorld w,BlockPos at){
  for(int dx=-1;dx<=1;dx+=2)for(int dz=-1;dz<=1;dz+=2)if(w.getChunkManager().getWorldChunk((at.getX()+dx)>>4,(at.getZ()+dz)>>4)==null)return false;
  if(w.isOutOfHeightLimit(at.up())||!w.getWorldBorder().contains(at)||!w.getFluidState(at).isEmpty())return false;
  var floor=w.getBlockState(at.down());return (floor.isIn(BlockTags.DIRT)||floor.isIn(BlockTags.SAND)||floor.isIn(BlockTags.BASE_STONE_OVERWORLD))&&w.isSpaceEmpty(new Box(at).expand(.25,0,.25).stretch(0,.35,0));
 }
 public static boolean prepare(ServerPlayerEntity p,ContractInstance c,BlockPos lair){
  if(c.status()!=ContractInstance.Status.ACCEPTED||c.hunt==null||c.hunt.target!=null||!c.hunt.dimension.equals(p.getServerWorld().getRegistryKey().getValue().toString()))return false;
  if(c.investigation!=null&&c.investigation.stage!=ContractInvestigation.Stage.SEARCHING)return true;
  var w=p.getServerWorld();var positions=routePositions(w,c,lair);if(positions.size()!=2)return false;
  var investigation=new ContractInvestigation(c.id(),c.owner(),c.hunt.dimension);investigation.routeVersion=3;investigation.lair=lair.toImmutable();var clues=new ArrayList<ContractClue>(positions.size());var ids=new UUID[positions.size()];for(int i=0;i<ids.length;i++)ids[i]=UUID.randomUUID();String kind=c.definition().target().getPath();
  for(int i=0;i<ids.length;i++){ContractClue.Type type=switch(kind){case "drowner"->i==ids.length-1?ContractClue.Type.REMAINS:ContractClue.Type.FOOTPRINTS;case "ghoul","grave_hag","katakan"->i==ids.length-1?ContractClue.Type.REMAINS:i%2==0?ContractClue.Type.CLAW_MARKS:ContractClue.Type.BLOOD_TRACE;case "leshen","fiend","endrega_warrior","werewolf","griffin"->i%2==0?ContractClue.Type.CLAW_MARKS:ContractClue.Type.BLOOD_TRACE;default->i%2==0?ContractClue.Type.MIST_RESIDUE:ContractClue.Type.FOOTPRINTS;};clues.add(new ContractClue(ids[i],c.id(),c.owner(),type,positions.get(i),c.hunt.dimension,i,i+1<ids.length?ids[i+1]:null));}
  investigation.clues=List.copyOf(clues);investigation.stage=ContractInvestigation.Stage.TRACKING;c.investigation=investigation;c.hunt.position=clues.getFirst().position;c.hunt.status=SwampHunt.Status.TRACKING;ContractState.get(w.getServer()).markDirty();return true;
 }
 /** New v3 routes keep two logical clues 40..60 blocks apart; existing saved routes are untouched. */
 private static BlockPos clueSurface(ServerWorld w,int x,int z){var chunk=w.getChunkManager().getWorldChunk(x>>4,z>>4);if(chunk==null)return null;var at=new BlockPos(x,chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x&15,z&15)+1,z);return safeClue(w,at)?at:null;}
 private static boolean traversable(ServerWorld w,BlockPos first,BlockPos second){if(Math.abs(first.getY()-second.getY())>12)return false;var previous=first;for(int step=1;step<=8;step++){int x=(int)Math.round(MathHelper.lerp(step/8.0,first.getX(),second.getX())),z=(int)Math.round(MathHelper.lerp(step/8.0,first.getZ(),second.getZ()));var at=clueSurface(w,x,z);if(at==null||Math.abs(at.getY()-previous.getY())>4)return false;previous=at;}return true;}
 private static List<BlockPos> routePositions(ServerWorld w,ContractInstance c,BlockPos lair){
  double preferred=Math.atan2(c.hunt.origin.getZ()-lair.getZ(),c.hunt.origin.getX()-lair.getX());int attempts=0;
  for(int near:new int[]{18,6})for(int turn:new int[]{0,1,-1,2,-2,3,-3,4}){
   double angle=preferred+turn*Math.PI/4;var second=clueSurface(w,lair.getX()+(int)Math.round(Math.cos(angle)*near),lair.getZ()+(int)Math.round(Math.sin(angle)*near));
   if(second==null||!traversable(w,lair,second))continue;
   for(int distance:new int[]{50,42,58})for(int branch:new int[]{0,1,-1,2,-2,3,-3,4}){
    if(++attempts>256)return List.of();double bearing=preferred+branch*Math.PI/4;
    var first=clueSurface(w,second.getX()+(int)Math.round(Math.cos(bearing)*distance),second.getZ()+(int)Math.round(Math.sin(bearing)*distance));if(first==null)continue;
    double gap=first.toCenterPos().subtract(second.toCenterPos()).horizontalLength();if(gap>=40&&gap<=60&&traversable(w,first,second))return List.of(first,second);
   }
  }return List.of();
 }
 public static void targetSpawned(ContractInstance c,BlockPos position){if(c.investigation!=null){c.investigation.lair=position.toImmutable();c.investigation.stage=ContractInvestigation.Stage.HUNTING;}}
 public static void afterDeath(LivingEntity e){if(!(e.getWorld() instanceof ServerWorld w))return;var id=ProceduralHunts.binding(e);if(id==null)return;var store=ContractState.get(w.getServer());var c=store.find(id);if(c!=null&&c.status()==ContractInstance.Status.COMPLETED&&c.investigation!=null&&c.investigation.stage!=ContractInvestigation.Stage.COMPLETED){c.investigation.stage=ContractInvestigation.Stage.COMPLETED;store.markDirty();}}
 public static boolean inspect(ServerPlayerEntity p,UUID clue){if(p.getHealth()<=0||p.isSpectator())return false;var store=ContractState.get(p.getServer());for(var c:store.ownedBy(p.getUuid())){
  var i=c.investigation;if(c.status()!=ContractInstance.Status.ACCEPTED||i==null||i.stage!=ContractInvestigation.Stage.TRACKING||!i.owner.equals(p.getUuid())||!i.dimension.equals(p.getServerWorld().getRegistryKey().getValue().toString()))continue;
  var next=i.next();if(next==null||next.studied||!next.id.equals(clue)||p.squaredDistanceTo(next.position.toCenterPos())>4.5*4.5||p.getServerWorld().getChunkManager().getWorldChunk(next.position.getX()>>4,next.position.getZ()>>4)==null)continue;
  next.studied=true;i.current++;if(i.current==i.clues.size())i.stage=ContractInvestigation.Stage.LAIR_FOUND;store.markDirty();sync(p);if(i.stage==ContractInvestigation.Stage.LAIR_FOUND&&i.routeVersion<2)trySpawn(p,c);return true;
 }return false;}
 public static boolean trySpawn(ServerPlayerEntity p,ContractInstance c){var i=c.investigation;if(i==null||i.stage!=ContractInvestigation.Stage.LAIR_FOUND||c.hunt.target!=null||!c.owner().equals(p.getUuid())||!i.dimension.equals(p.getServerWorld().getRegistryKey().getValue().toString())||p.squaredDistanceTo(i.lair.toCenterPos())>(i.routeVersion>=2?3*3:32*32))return false;
  if(SwampLord.spawn(p,c,i.lair))return true;
  // Only a bounded loaded neighbourhood, and only after an explicit discovery/retry.
  for(int radius=3;radius<=12;radius+=3)for(int direction=0;direction<8;direction++){double angle=direction*Math.PI/4;int x=i.lair.getX()+(int)Math.round(Math.cos(angle)*radius),z=i.lair.getZ()+(int)Math.round(Math.sin(angle)*radius);var chunk=p.getServerWorld().getChunkManager().getWorldChunk(x>>4,z>>4);if(chunk==null)continue;var at=new BlockPos(x,chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x&15,z&15)+1,z);if(Math.abs(at.getY()-i.lair.getY())<=3&&SwampLord.spawn(p,c,at))return true;}
  c.hunt.status=SwampHunt.Status.WAITING;ContractState.get(p.getServer()).markDirty();
  return false;
 }
 private static ContractInstance active(ServerPlayerEntity p,Session s){var store=ContractState.get(p.getServer());var tracked=store.tracked(p.getUuid());if(tracked!=null)s.active=tracked;var cached=s.active==null?null:store.find(s.active);if(cached!=null&&cached.owner().equals(p.getUuid())&&cached.investigation!=null&&(cached.status()==ContractInstance.Status.ACCEPTED||cached.status()==ContractInstance.Status.COMPLETED))return cached;if(p.getServer().getTicks()<s.nextLookup)return null;s.nextLookup=p.getServer().getTicks()+100;for(var c:store.ownedBy(p.getUuid()))if(c.status()==ContractInstance.Status.ACCEPTED&&c.investigation!=null){s.active=c.id();return c;}s.active=null;return null;}
 public static void sync(ServerPlayerEntity p){send(p,SESSIONS.computeIfAbsent(p.getUuid(),id->new Session()));}
 public static ContractInstance activeContract(ServerPlayerEntity p){var s=SESSIONS.computeIfAbsent(p.getUuid(),id->new Session());s.nextLookup=0;return active(p,s);}
 public static void refresh(ServerPlayerEntity p){var s=SESSIONS.computeIfAbsent(p.getUuid(),id->new Session());s.active=null;s.nextLookup=0;s.sent="";send(p,s);}
 public static void disconnected(UUID owner){SESSIONS.remove(owner);}
 public static boolean enabled(UUID owner){var s=SESSIONS.get(owner);return s!=null&&s.enabled;}
 public static void toggle(ServerPlayerEntity p,boolean enabled){var s=SESSIONS.computeIfAbsent(p.getUuid(),id->new Session());s.dimension=p.getServerWorld().getRegistryKey().getValue().toString();s.enabled=enabled;s.sent="";send(p,s);}
 public static void requestToggle(ServerPlayerEntity p,boolean value){var s=SESSIONS.computeIfAbsent(p.getUuid(),id->new Session());int tick=p.getServer().getTicks();if(tick-s.lastToggle>=5){s.lastToggle=tick;s.pendingToggle=null;toggle(p,value);}else s.pendingToggle=value;}
 public static void flushToggle(ServerPlayerEntity p){var s=SESSIONS.get(p.getUuid());if(s!=null&&s.pendingToggle!=null&&p.getServer().getTicks()-s.lastToggle>=5){boolean value=s.pendingToggle;s.pendingToggle=null;s.lastToggle=p.getServer().getTicks();toggle(p,value);}}
 public static JsonObject view(ServerPlayerEntity p){return view(p,SESSIONS.computeIfAbsent(p.getUuid(),id->new Session()));}
 private static JsonObject view(ServerPlayerEntity p,Session s){String dimension=p.getServerWorld().getRegistryKey().getValue().toString();if(!dimension.equals(s.dimension)){s.enabled=false;s.pendingToggle=null;s.dimension=dimension;}var j=new JsonObject();j.addProperty("enabled",s.enabled);j.addProperty("dimension",p.getServerWorld().getRegistryKey().getValue().toString());j.addProperty("hint",s.enabled?"search":"off");if(p.getHealth()<=0||p.isSpectator()){s.enabled=false;s.pendingToggle=null;j.addProperty("enabled",false);return j;}var c=active(p,s);if(c==null)return j;var i=c.investigation;j.addProperty("contract",c.id().toString());j.addProperty("stage",i.stage.name().toLowerCase(Locale.ROOT));j.addProperty("objective",i.stage==ContractInvestigation.Stage.TRACKING?(i.current==0?"first":"second"):i.stage.name().toLowerCase(Locale.ROOT));j.addProperty("target_dimension",i.dimension);j.addProperty("progress",i.current);j.addProperty("total",i.clues.size());if(c.hunt!=null&&c.hunt.status==SwampHunt.Status.WAITING)j.addProperty("hint","unavailable");if(i.current>0)j.addProperty("last",description(c,i.clues.get(i.current-1)));
  if(!i.dimension.equals(p.getServerWorld().getRegistryKey().getValue().toString())){j.addProperty("hint","dimension");return j;}
  var clue=i.next();var pos=i.stage==ContractInvestigation.Stage.TRACKING&&clue!=null?clue.position:i.lair;if(pos==null)return j;var area=new JsonObject();area.addProperty("x",Math.floorDiv(pos.getX(),16)*16+8);area.addProperty("z",Math.floorDiv(pos.getZ(),16)*16+8);area.addProperty("radius",20);j.add("area",area);double distance=p.squaredDistanceTo(pos.toCenterPos());if(distance<96*96){j.addProperty("hint",s.enabled?"area":"activate");if(s.enabled&&distance>settings.radius()*settings.radius()){var delta=pos.toCenterPos().subtract(p.getPos());int bearing=Math.floorMod((int)Math.floor(Math.toDegrees(Math.atan2(delta.x,-delta.z))/45+.5),8);j.addProperty("bearing",bearing);}}
  int radius=s.enabled?settings.radius():8;if(distance<=radius*radius&&p.getServerWorld().getChunkManager().getWorldChunk(pos.getX()>>4,pos.getZ()>>4)!=null){var point=new JsonObject();point.addProperty("x",pos.getX());point.addProperty("y",pos.getY());point.addProperty("z",pos.getZ());boolean tracking=i.stage==ContractInvestigation.Stage.TRACKING&&clue!=null;point.addProperty("type",tracking?clue.type.name():c.definition().target().getPath().equals("foglet")?"MIST_RESIDUE":"REMAINS");if(tracking){point.addProperty("id",clue.id.toString());var destination=clue.next==null?i.lair:i.clues.get(clue.order+1).position;var direction=destination.toCenterPos().subtract(pos.toCenterPos());point.addProperty("yaw",Math.toDegrees(Math.atan2(direction.x,direction.z)));}point.addProperty("lair",!tracking);j.add("point",point);j.addProperty("hint",tracking?"near":"lair");}
  if(i.stage==ContractInvestigation.Stage.COMPLETED)j.addProperty("hint","completed");return j;
 }
 public static String description(ContractInstance c,ContractClue clue){return "tracking.witchersbestiary.finding."+c.definition().target().getPath()+"."+clue.type.name().toLowerCase(Locale.ROOT);}
 private static void send(ServerPlayerEntity p,Session s){if(!ServerPlayNetworking.canSend(p,TrackingPackets.Snapshot.ID))return;String json=view(p,s).toString();if(!json.equals(s.sent)){s.sent=json;ServerPlayNetworking.send(p,new TrackingPackets.Snapshot(json));}}
 public static void initialize(){settings=InvestigationSettings.load();PayloadTypeRegistry.playC2S().register(TrackingPackets.Toggle.ID,TrackingPackets.Toggle.CODEC);PayloadTypeRegistry.playC2S().register(TrackingPackets.Inspect.ID,TrackingPackets.Inspect.CODEC);PayloadTypeRegistry.playS2C().register(TrackingPackets.Snapshot.ID,TrackingPackets.Snapshot.CODEC);
  ServerPlayNetworking.registerGlobalReceiver(TrackingPackets.Toggle.ID,(packet,ctx)->ctx.server().execute(()->{if(!ctx.player().isRemoved()&&ctx.player().getHealth()>0)requestToggle(ctx.player(),packet.enabled());}));
  ServerPlayNetworking.registerGlobalReceiver(TrackingPackets.Inspect.ID,(packet,ctx)->ctx.server().execute(()->{var p=ctx.player();if(p.isRemoved()||p.getHealth()<=0)return;var s=SESSIONS.computeIfAbsent(p.getUuid(),id->new Session());int tick=p.getServer().getTicks();if(tick-s.lastAction>=5){s.lastAction=tick;inspect(p,packet.clue());send(p,s);}}));
  ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTicks()%10!=0)return;for(var p:server.getPlayerManager().getPlayerList()){var s=SESSIONS.computeIfAbsent(p.getUuid(),id->new Session());String dim=p.getServerWorld().getRegistryKey().getValue().toString();if(!dim.equals(s.dimension)){s.dimension=dim;s.enabled=false;s.pendingToggle=null;s.sent="";}flushToggle(p);var c=active(p,s);if(c!=null&&c.investigation.stage==ContractInvestigation.Stage.LAIR_FOUND&&server.getTicks()>=s.nextSpawn){s.nextSpawn=server.getTicks()+100;trySpawn(p,c);}send(p,s);}});
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->disconnected(handler.player.getUuid()));ServerLifecycleEvents.SERVER_STOPPED.register(server->SESSIONS.clear());
 }
}
