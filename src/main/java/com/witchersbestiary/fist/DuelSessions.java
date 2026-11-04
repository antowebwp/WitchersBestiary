package com.witchersbestiary.fist;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;

/** PvP lifecycle adapter around the existing virtual fist engine. Never owns real health, inventory or rewards. */
public final class DuelSessions {
 public enum Stage { WAITING, CONFIRMING, COUNTDOWN, ACTIVE, FINISHED, CLEANUP }
 public enum Result { NONE, KNOCKOUT, SURRENDER, BOUNDARY, DISCONNECT, DIMENSION, TELEPORT, STONE_REMOVED, CHUNK_UNLOADED, EXTERNAL_DAMAGE, TIMEOUT, DECLINED, INVALID }
 public static final double RADIUS=12;public static final int WAIT=600,RETURN_GRACE=60;
 public static final class Session {
  public final UUID id=UUID.randomUUID(),a;public UUID b,winner;public final BlockPos stone;public final String dimension;
  public final FistFightSession fight=new FistFightSession(FistFightSession.Type.PLAYER_VS_PLAYER);
  public Stage stage=Stage.WAITING;public Result result=Result.NONE;public final long created;public long deadline;public long started=-1;public boolean acceptedA,acceptedB;public int finishedTicks,outA,outB;public Vec3d lastA,lastB;
  private final Map<UUID,Integer> controls=new HashMap<>(),inputs=new HashMap<>();
  Session(ServerPlayerEntity p,BlockPos stone){a=p.getUuid();this.stone=stone.toImmutable();dimension=p.getServerWorld().getRegistryKey().getValue().toString();created=p.getServer().getTicks();deadline=created+WAIT;}
  public boolean member(UUID p){return a.equals(p)||Objects.equals(b,p);}
 }
 private static final class Store {final Map<UUID,Session> sessions=new HashMap<>();final Map<UUID,UUID> owners=new HashMap<>();}
 private static final Map<MinecraftServer,Store> STORES=new WeakHashMap<>();
 public static Session get(ServerPlayerEntity p){var st=STORES.get(p.getServer());return st==null?null:st.sessions.get(st.owners.get(p.getUuid()));}
 public static Collection<Session> sessions(MinecraftServer s){var st=STORES.get(s);return st==null?List.of():List.copyOf(st.sessions.values());}
 public static boolean occupied(ServerPlayerEntity p){return get(p)!=null;}
 public static boolean restricted(Entity e){if(!(e instanceof ServerPlayerEntity p))return false;var d=get(p);return d!=null&&(d.stage==Stage.COUNTDOWN||d.stage==Stage.ACTIVE||d.stage==Stage.FINISHED&&d.started>=0);}
 private static ServerPlayerEntity player(MinecraftServer s,UUID id){return id==null?null:s.getPlayerManager().getPlayer(id);}
 private static boolean eligible(ServerPlayerEntity p){return p!=null&&!p.isRemoved()&&p.isAlive()&&!p.isSpectator()&&!p.hasVehicle()&&!p.isSleeping()&&p.getMainHandStack().isEmpty()&&p.getOffHandStack().isEmpty();}
 private static void message(ServerPlayerEntity p,String key){p.sendMessage(Text.translatable("duel.witchersbestiary."+key),false);}
 public static boolean interact(ServerPlayerEntity p,BlockPos stone){
  if(p.getServerWorld().getChunkManager().getWorldChunk(stone.getX()>>4,stone.getZ()>>4)==null||!p.getServerWorld().getBlockState(stone).isOf(DuelStone.BLOCK))return false;
  if(!p.getServer().isPvpEnabled()){message(p,"pvp_disabled");return false;}
  var existing=get(p);if(existing!=null){DuelPackets.send(p,existing);return existing.stone.equals(stone)&&existing.dimension.equals(p.getServerWorld().getRegistryKey().getValue().toString());}
  if(!eligible(p)||FistFights.active(p)!=null||p.squaredDistanceTo(stone.toCenterPos())>36){message(p,"empty_hands");return false;}
  var st=STORES.computeIfAbsent(p.getServer(),key->new Store());String dim=p.getServerWorld().getRegistryKey().getValue().toString();
  for(var d:st.sessions.values())if(d.dimension.equals(dim)&&d.stone.equals(stone)){
   if(d.stage!=Stage.WAITING||d.a.equals(p.getUuid())){message(p,"busy");return false;}
   var a=player(p.getServer(),d.a);if(!eligible(a)||a.getServerWorld()!=p.getServerWorld()||a.squaredDistanceTo(stone.toCenterPos())>36){finish(p.getServer(),d,null,Result.INVALID);return false;}
   d.b=p.getUuid();d.stage=Stage.CONFIRMING;d.deadline=p.getServer().getTicks()+400;st.owners.put(d.b,d.id);send(p.getServer(),d);return true;
  }
  for(var d:st.sessions.values())if(d.stage!=Stage.FINISHED&&d.dimension.equals(dim)&&d.stone.getSquaredDistance(stone)<(RADIUS*2)*(RADIUS*2)){message(p,"overlap");return false;}
  var d=new Session(p,stone);st.sessions.put(d.id,d);st.owners.put(d.a,d.id);DuelPackets.send(p,d);return true;
 }
 public static void control(ServerPlayerEntity p,UUID id,int action){var d=get(p);if(d==null||!d.id.equals(id)||!d.member(p.getUuid()))return;int tick=p.getServer().getTicks();if(tick-d.controls.getOrDefault(p.getUuid(),-100)<5)return;d.controls.put(p.getUuid(),tick);
  if(action==1&&(d.stage==Stage.WAITING||d.stage==Stage.CONFIRMING)){finish(p.getServer(),d,null,Result.DECLINED);return;}
  if(action==2){if(d.stage==Stage.ACTIVE||d.stage==Stage.COUNTDOWN)finish(p.getServer(),d,d.a.equals(p.getUuid())?d.b:d.a,Result.SURRENDER);return;}
  if(action!=0||d.stage!=Stage.CONFIRMING||tick>d.deadline||!eligible(p))return;
  if(d.a.equals(p.getUuid()))d.acceptedA=true;else d.acceptedB=true;
  if(d.acceptedA&&d.acceptedB)start(p.getServer(),d);else send(p.getServer(),d);
 }
 private static boolean safe(ServerWorld w,Vec3d pos){var feet=BlockPos.ofFloored(pos);for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)if(w.getChunkManager().getWorldChunk((feet.getX()+x)>>4,(feet.getZ()+z)>>4)==null)return false;
  var box=new Box(pos.x-.3,pos.y,pos.z-.3,pos.x+.3,pos.y+1.9,pos.z+.3);if(!w.getWorldBorder().contains(box)||w.isOutOfHeightLimit(feet.up(2))||!w.isSpaceEmpty(box)||w.containsFluid(box))return false;
  for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var floor=feet.add(x,-1,z);var block=w.getBlockState(floor);if(!block.isSideSolidFullSquare(w,floor,net.minecraft.util.math.Direction.UP)||!w.getFluidState(floor).isEmpty()||block.isOf(net.minecraft.block.Blocks.MAGMA_BLOCK)||block.isOf(net.minecraft.block.Blocks.CAMPFIRE)||block.isOf(net.minecraft.block.Blocks.SOUL_CAMPFIRE)||block.isOf(net.minecraft.block.Blocks.CACTUS))return false;}return true;
 }
 private static boolean free(ServerWorld w,Vec3d at,ServerPlayerEntity a,ServerPlayerEntity b){return w.getOtherEntities(null,new Box(at.x-.3,at.y,at.z-.3,at.x+.3,at.y+1.9,at.z+.3),e->e!=a&&e!=b&&e instanceof LivingEntity&&!e.isSpectator()).isEmpty();}
 private static void start(MinecraftServer s,Session d){var a=player(s,d.a);var b=player(s,d.b);if(!s.isPvpEnabled()||!eligible(a)||!eligible(b)||a.getServerWorld()!=b.getServerWorld()||!a.getServerWorld().getRegistryKey().getValue().toString().equals(d.dimension)||a.squaredDistanceTo(d.stone.toCenterPos())>36||b.squaredDistanceTo(d.stone.toCenterPos())>36){finish(s,d,null,Result.INVALID);return;}
  if(!DuelPermissions.allowed(a,b)||!DuelPermissions.allowed(b,a)){message(a,"protected");message(b,"protected");finish(s,d,null,Result.INVALID);return;}var w=a.getServerWorld();if(w.getChunkManager().getWorldChunk(d.stone.getX()>>4,d.stone.getZ()>>4)==null||!w.getBlockState(d.stone).isOf(DuelStone.BLOCK)){finish(s,d,null,Result.STONE_REMOVED);return;}Vec3d first=null,second=null;for(int radius:new int[]{3,4,5}){for(int direction=0;direction<8;direction++){var delta=new Vec3d(Math.cos(direction*Math.PI/4)*radius,0,Math.sin(direction*Math.PI/4)*radius);var center=new Vec3d(d.stone.getX()+.5,d.stone.getY(),d.stone.getZ()+.5);var one=center.add(delta);var two=center.subtract(delta);if(safe(w,one)&&safe(w,two)&&free(w,one,a,b)&&free(w,two,a,b)){first=one;second=two;break;}}if(first!=null)break;}
  if(first==null){message(a,"unsafe");message(b,"unsafe");finish(s,d,null,Result.INVALID);return;}
  teleportFacing(a,first,second);teleportFacing(b,second,first);d.lastA=first;d.lastB=second;d.stage=Stage.COUNTDOWN;d.started=s.getTicks();d.deadline=d.started+6060;send(s,d);
 }
 private static void teleportFacing(ServerPlayerEntity p,Vec3d pos,Vec3d target){var v=target.subtract(pos);p.teleport(p.getServerWorld(),pos.x,pos.y,pos.z,(float)Math.toDegrees(Math.atan2(-v.x,v.z)),0);}
 public static void input(ServerPlayerEntity p,UUID id,FistFightSession.Action action){var d=get(p);if(d==null||!d.id.equals(id)||d.stage!=Stage.ACTIVE||!eligible(p))return;int tick=p.getServer().getTicks();if(d.inputs.getOrDefault(p.getUuid(),-1)==tick)return;d.inputs.put(p.getUuid(),tick);boolean side=d.a.equals(p.getUuid());var f=side?d.fight.player:d.fight.opponent;
  if(action==FistFightSession.Action.IDLE){d.fight.releaseBlock(side);return;}
  if(action==FistFightSession.Action.QUICK&&f.counterTicks>0)action=FistFightSession.Action.COUNTER;
  if(d.fight.act(side,action)){if(action==FistFightSession.Action.DODGE)FistFights.dodge(p);else if(action==FistFightSession.Action.QUICK||action==FistFightSession.Action.COUNTER)p.swingHand(net.minecraft.util.Hand.MAIN_HAND);send(p.getServer(),d);}
 }
 public static boolean protect(LivingEntity victim,DamageSource source){var attacker=source.getAttacker();if(restricted(attacker))return true;if(restricted(victim)&&victim instanceof ServerPlayerEntity p){if(attacker instanceof ServerPlayerEntity)return true;var d=get(p);finish(p.getServer(),d,null,Result.EXTERNAL_DAMAGE);}return false;}
 public static void leave(ServerPlayerEntity p,Result result){var d=get(p);if(d!=null)finish(p.getServer(),d,d.b==null?null:d.a.equals(p.getUuid())?d.b:d.a,result);}
 private static boolean canHit(ServerPlayerEntity a,ServerPlayerEntity b){return a.squaredDistanceTo(b)<=2.8*2.8&&Math.abs(a.getY()-b.getY())<=1.25&&a.canSee(b)&&FistFights.facing(a,b)&&DuelPermissions.allowed(a,b);}
 private static void send(MinecraftServer s,Session d){var a=player(s,d.a);var b=player(s,d.b);if(a!=null)DuelPackets.send(a,d);if(b!=null)DuelPackets.send(b,d);}
 private static void finish(MinecraftServer s,Session d,UUID winner,Result result){if(d.stage==Stage.FINISHED||d.stage==Stage.CLEANUP)return;d.stage=Stage.FINISHED;d.result=result;d.winner=winner;d.deadline=s.getTicks()+(d.started<0?0:20);if(result!=Result.KNOCKOUT){d.fight.cancel();if(winner!=null){d.fight.player.action=d.a.equals(winner)?FistFightSession.Action.VICTORY:FistFightSession.Action.IDLE;d.fight.opponent.action=d.a.equals(winner)?FistFightSession.Action.IDLE:FistFightSession.Action.VICTORY;d.fight.player.actionTicks=d.fight.opponent.actionTicks=0;}}for(UUID id:new UUID[]{d.a,d.b}){var p=player(s,id);if(p!=null){var won=player(s,winner);p.sendMessage(won==null?Text.translatable("duel.witchersbestiary.ended"):Text.translatable("duel.witchersbestiary.winner",won.getName()),false);}}send(s,d);}
 public static void tick(MinecraftServer s){var store=STORES.get(s);if(store==null)return;for(var d:new ArrayList<>(store.sessions.values())){
  if(d.stage==Stage.FINISHED){if(s.getTicks()>=d.deadline){d.stage=Stage.CLEANUP;var a=player(s,d.a);var b=player(s,d.b);if(a!=null)DuelPackets.clear(a);if(b!=null)DuelPackets.clear(b);store.owners.remove(d.a,d.id);if(d.b!=null)store.owners.remove(d.b,d.id);store.sessions.remove(d.id);}continue;}
  if(!s.isPvpEnabled()){finish(s,d,null,Result.INVALID);continue;}var a=player(s,d.a);var b=player(s,d.b);if(a==null||d.b!=null&&b==null){finish(s,d,a==null?d.b:d.a,Result.DISCONNECT);continue;}
  var w=a.getServerWorld();if(!w.getRegistryKey().getValue().toString().equals(d.dimension)||b!=null&&b.getServerWorld()!=w){finish(s,d,null,Result.DIMENSION);continue;}
  if(w.getChunkManager().getWorldChunk(d.stone.getX()>>4,d.stone.getZ()>>4)==null){finish(s,d,null,Result.CHUNK_UNLOADED);continue;}
  if(!w.getBlockState(d.stone).isOf(DuelStone.BLOCK)){finish(s,d,null,Result.STONE_REMOVED);continue;}
  if(d.stage==Stage.WAITING||d.stage==Stage.CONFIRMING){if(!a.isAlive()||b!=null&&!b.isAlive()||s.getTicks()>d.deadline||a.squaredDistanceTo(d.stone.toCenterPos())>144||b!=null&&b.squaredDistanceTo(d.stone.toCenterPos())>144)finish(s,d,null,Result.TIMEOUT);else if(s.getTicks()%20==0)send(s,d);continue;}
  if(!eligible(a)||!eligible(b)||s.getTicks()%5==0&&(!DuelPermissions.allowed(a,b)||!DuelPermissions.allowed(b,a))){finish(s,d,null,Result.INVALID);continue;}
  if(a.getPos().squaredDistanceTo(d.lastA)>64||b.getPos().squaredDistanceTo(d.lastB)>64){finish(s,d,null,Result.TELEPORT);continue;}d.lastA=a.getPos();d.lastB=b.getPos();
  if(w.getChunkManager().getWorldChunk(a.getBlockX()>>4,a.getBlockZ()>>4)==null||w.getChunkManager().getWorldChunk(b.getBlockX()>>4,b.getBlockZ()>>4)==null){finish(s,d,null,Result.CHUNK_UNLOADED);continue;}
  d.outA=a.squaredDistanceTo(d.stone.toCenterPos())>RADIUS*RADIUS?d.outA+1:0;d.outB=b.squaredDistanceTo(d.stone.toCenterPos())>RADIUS*RADIUS?d.outB+1:0;
  if(d.outA==1)message(a,"boundary");if(d.outB==1)message(b,"boundary");if(d.outA>=RETURN_GRACE||d.outB>=RETURN_GRACE){finish(s,d,d.outA>=RETURN_GRACE?d.b:d.a,Result.BOUNDARY);continue;}
  d.fight.tick();if(d.fight.stage==FistFightSession.Stage.FIGHTING){if(d.stage==Stage.COUNTDOWN){message(a,"fight");message(b,"fight");}d.stage=Stage.ACTIVE;
   if(d.fight.contact(true)&&d.fight.hit(true,canHit(a,b),FistFights.facing(b,a)))FistFights.contact(b);
   if(d.fight.contact(false)&&d.fight.hit(false,canHit(b,a),FistFights.facing(a,b)))FistFights.contact(a);
  }
  if(d.fight.stage==FistFightSession.Stage.FINISHED)finish(s,d,d.fight.playerWon?d.a:d.b,Result.KNOCKOUT);else if(d.fight.stage==FistFightSession.Stage.CANCELLED)finish(s,d,null,Result.TIMEOUT);else if(s.getTicks()%5==0)send(s,d);
 }}
 public static void initialize(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(DuelSessions::tick);net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->leave(h.player,Result.DISCONNECT));net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->STORES.remove(s));}
}
