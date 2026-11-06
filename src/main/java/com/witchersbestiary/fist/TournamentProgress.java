package com.witchersbestiary.fist;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/** Personal tournament history; live bouts are deliberately not restored after a restart. */
public final class TournamentProgress extends PersistentState {
 public enum Category {
  VILLAGE(3,150), CITY(4,400), CHAMPIONSHIP(5,1000);
  public final int rounds,reward;
  Category(int rounds,int reward){this.rounds=rounds;this.reward=reward;}
  public int tier(int round){return Math.min(2,ordinal()+round);}
 }
 public static final class Entry {
  public int titles,round,lastCategory=-1,lastRound;public boolean lastWon;public Category active;public boolean fighting;public final long[] paidUntil=new long[3];
  public boolean unlocked(Category c){return c==Category.VILLAGE||(titles&(1<<(c.ordinal()-1)))!=0;}
 }
 private final Map<UUID,Entry> players=new HashMap<>();
 private boolean readOnly;
 public static final Type<TournamentProgress> TYPE=new Type<>(TournamentProgress::new,TournamentProgress::read,null);
 public static TournamentProgress get(MinecraftServer s){return s.getOverworld().getPersistentStateManager().getOrCreate(TYPE,"witchersbestiary_tournaments");}
 public Entry entry(UUID id){return players.computeIfAbsent(id,k->new Entry());}
 public boolean enter(UUID id,Category c){var e=entry(id);if(readOnly||e.active!=null||!e.unlocked(c))return false;e.active=c;e.round=0;e.fighting=false;markDirty();return true;}
 public boolean begin(UUID id){var e=entry(id);if(readOnly||e.active==null||e.fighting)return false;e.fighting=true;markDirty();return true;}
 public void abandon(UUID id){if(readOnly)return;var e=entry(id);if(e.active!=null){e.lastCategory=e.active.ordinal();e.lastRound=e.round;e.lastWon=false;}e.active=null;e.fighting=false;markDirty();}
 /** Only a live official bout may complete a round. Replayed completion calls do nothing. */
 public boolean finish(UUID id,boolean won,long now,FistRewards rewards){
  var e=entry(id);if(readOnly||!e.fighting||e.active==null)return false;e.fighting=false;
  e.lastCategory=e.active.ordinal();e.lastRound=e.round;e.lastWon=won;
  if(!won){e.active=null;markDirty();return true;}
  var c=e.active;e.round++;e.lastRound=e.round;
  if(e.round==c.rounds){
   e.titles|=1<<c.ordinal();e.active=null;
   if(now>=0&&now<=Long.MAX_VALUE-72000&&now>=e.paidUntil[c.ordinal()]&&rewards.reserve(id,c.reward))e.paidUntil[c.ordinal()]=now+72000;
  }
  markDirty();return true;
 }
 private NbtCompound preserved;
 @Override public NbtCompound writeNbt(NbtCompound n,RegistryWrapper.WrapperLookup lookup){
  if(readOnly)return preserved.copy();n.putInt("Schema",1);var rows=new NbtList();
  players.forEach((id,e)->{var row=new NbtCompound();row.putUuid("Owner",id);row.putInt("Titles",e.titles);row.putInt("LastCategory",e.lastCategory);row.putInt("LastRound",e.lastRound);row.putBoolean("LastWon",e.lastWon);row.putInt("Round",e.round);row.putInt("Active",e.active==null?-1:e.active.ordinal());row.putBoolean("Fighting",e.fighting);row.putLongArray("PaidUntil",e.paidUntil);rows.add(row);});n.put("Players",rows);return n;
 }
 public static TournamentProgress read(NbtCompound n,RegistryWrapper.WrapperLookup lookup){
  var state=new TournamentProgress();if(n.getInt("Schema")!=1){state.readOnly=true;state.preserved=n.copy();return state;}
  var rows=n.getList("Players",10);
  for(int i=0;i<rows.size();i++){var row=rows.getCompound(i);if(!row.containsUuid("Owner"))continue;var e=new Entry();e.titles=row.getInt("Titles")&7;e.lastCategory=row.contains("LastCategory")?row.getInt("LastCategory"):-1;e.lastRound=row.getInt("LastRound");e.lastWon=row.getBoolean("LastWon");int a=row.getInt("Active");if(row.getBoolean("Fighting")&&a>=0&&a<3){e.lastCategory=a;e.lastRound=row.getInt("Round");e.lastWon=false;}if(a>=0&&a<3&&!row.getBoolean("Fighting")){e.active=Category.values()[a];e.round=Math.max(0,Math.min(e.active.rounds-1,row.getInt("Round")));}var paid=row.getLongArray("PaidUntil");for(int j=0;j<Math.min(3,paid.length);j++)e.paidUntil[j]=Math.max(0,paid[j]);state.players.put(row.getUuid("Owner"),e);}
  state.markDirty();return state;
 }
}
