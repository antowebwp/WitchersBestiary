package com.witchersbestiary.lure;

import net.minecraft.world.PersistentState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/** Server-wide journal. Terminal nonces remain tombstones: stale block NBT cannot summon/refund twice. */
public final class LureState extends PersistentState {
 public static final long COOLDOWN=24000,APPROACH=1200;
 public static final double AREA_RADIUS=192;
 public static final Type<LureState> TYPE=new Type<>(LureState::new,LureState::read,null);
 public static final class Entry {
  public final UUID nonce,owner;public final String dimension;public final BlockPos pos;public final LureKind kind;public final long created,due;
  public int status;public UUID boss;public long completed;
  Entry(UUID nonce,UUID owner,String dimension,BlockPos pos,LureKind kind,long created,long due){this.nonce=nonce;this.owner=owner;this.dimension=dimension;this.pos=pos.toImmutable();this.kind=kind;this.created=created;this.due=due;}
  public boolean matches(ServerWorld w,BlockPos p){return dimension.equals(w.getRegistryKey().getValue().toString())&&pos.equals(p);}
 }
 private long clock;
 private final Map<UUID,Entry> entries=new HashMap<>();
 private final Map<UUID,Entry> bosses=new HashMap<>();
 public static LureState get(ServerWorld w){var state=w.getServer().getOverworld().getPersistentStateManager().getOrCreate(TYPE,"witchersbestiary_lures");state.clock=now(w);return state;}
 public static long now(ServerWorld w){return w.getServer().getOverworld().getTime();}
 public Entry get(UUID id){return entries.get(id);}
 public Entry approach(UUID boss,long now){var e=bosses.get(boss);return e!=null&&now<e.completed+APPROACH?e:null;}
 public String reservationError(UUID owner,String dimension,BlockPos pos,long now){
  clock=now;prune();
  // This is the compact saved lure journal, not a scan of entities/chunks. Runs only on placement.
  for(var e:entries.values()){
   boolean active=e.status==0,cooling=e.status==1&&now<e.completed+COOLDOWN;
   if(!active&&!cooling)continue;
   if(e.owner.equals(owner))return active?"active":"cooldown";
   if(e.dimension.equals(dimension)&&horizontalSquared(e.pos,pos)<AREA_RADIUS*AREA_RADIUS)return "area";
  }return null;
 }
 public Entry reserve(UUID owner,ServerWorld w,BlockPos pos,LureKind kind){long now=now(w);if(reservationError(owner,w.getRegistryKey().getValue().toString(),pos,now)!=null)return null;
  var e=new Entry(UUID.randomUUID(),owner,w.getRegistryKey().getValue().toString(),pos,kind,now,now+2400+w.random.nextInt(1201));entries.put(e.nonce,e);markDirty();return e;
 }
 public boolean refund(Entry e){if(e==null||e.status!=0)return false;e.status=2;e.completed=clock;markDirty();return true;}
 // Complete the encounter, not the life of its boss. Vanilla PersistenceRequired survives entity NBT.
 public boolean finish(Entry e,net.minecraft.entity.mob.MobEntity boss,long now){
  if(!finish(e,boss.getUuid(),now))return false;boss.setPersistent();return true;
 }
 public boolean isHuntBoss(UUID id){return bosses.containsKey(id);}
 public boolean finish(Entry e,UUID boss,long now){if(e==null||e.status!=0)return false;e.status=1;e.boss=boss;e.completed=now;bosses.put(boss,e);markDirty();return true;}
 private static double horizontalSquared(BlockPos a,BlockPos b){double x=(double)a.getX()-b.getX(),z=(double)a.getZ()-b.getZ();return x*x+z*z;}
 private void prune(){var it=entries.values().iterator();while(it.hasNext()){var e=it.next();if(e.status!=0&&clock>e.completed+(e.status==1?COOLDOWN:APPROACH)){it.remove();if(e.boss!=null)bosses.remove(e.boss,e);}}}
 @Override public NbtCompound writeNbt(NbtCompound n,RegistryWrapper.WrapperLookup registries){
  prune();var list=new NbtList();for(var e:entries.values()){var a=new NbtCompound();a.putUuid("Nonce",e.nonce);a.putUuid("Owner",e.owner);a.putString("Dimension",e.dimension);a.putLong("Pos",e.pos.asLong());a.putString("Kind",e.kind.name());a.putLong("Created",e.created);a.putLong("Due",e.due);a.putInt("Status",e.status);a.putLong("Completed",e.completed);if(e.boss!=null)a.putUuid("Boss",e.boss);list.add(a);}n.putInt("Version",1);n.put("Entries",list);return n;
 }
 public static LureState read(NbtCompound n,RegistryWrapper.WrapperLookup registries){var state=new LureState();var list=n.getList("Entries",NbtElement.COMPOUND_TYPE);for(int i=0;i<list.size();i++){var a=list.getCompound(i);try{
  var e=new Entry(a.getUuid("Nonce"),a.getUuid("Owner"),a.getString("Dimension"),BlockPos.fromLong(a.getLong("Pos")),LureKind.valueOf(a.getString("Kind")),a.getLong("Created"),a.getLong("Due"));e.status=Math.clamp(a.getInt("Status"),0,2);e.completed=a.getLong("Completed");if(a.containsUuid("Boss"))e.boss=a.getUuid("Boss");state.entries.put(e.nonce,e);if(e.status==1&&e.boss!=null)state.bosses.merge(e.boss,e,(older,newer)->older.completed>=newer.completed?older:newer);
 }catch(IllegalArgumentException ignored){}}return state;}
}
