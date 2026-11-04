package com.witchersbestiary.merchant;
import java.util.*;import net.minecraft.world.PersistentState;import net.minecraft.nbt.*;import net.minecraft.registry.RegistryWrapper;import net.minecraft.server.MinecraftServer;
/** Durable settlement claims: unloaded or dead merchants never trigger duplicate creation. */
public final class MerchantLedger extends PersistentState {
 public static final Type<MerchantLedger> TYPE=new Type<>(MerchantLedger::new,MerchantLedger::read,null);
 public static final class Claim {public UUID entity;public int attempts,searchVersion=2;public long next;}
 public final Map<String,Claim> villages=new HashMap<>();public long nextWander;private NbtCompound future;
 public boolean readOnly(){return future!=null;}
 public static MerchantLedger get(MinecraftServer s){return s.getOverworld().getPersistentStateManager().getOrCreate(TYPE,"witchersbestiary_arcane_merchants");}
 @Override public NbtCompound writeNbt(NbtCompound n,RegistryWrapper.WrapperLookup lookup){if(future!=null)return future.copy();n.putInt("Schema",2);n.putLong("NextWander",nextWander);var values=new NbtCompound();villages.forEach((key,claim)->{var row=new NbtCompound();if(claim.entity!=null)row.putUuid("Entity",claim.entity);row.putInt("Attempts",claim.attempts);row.putInt("SearchVersion",claim.searchVersion);row.putLong("Next",claim.next);values.put(key,row);});n.put("Villages",values);return n;}
 public static MerchantLedger read(NbtCompound n,RegistryWrapper.WrapperLookup lookup){var result=new MerchantLedger();if(n.getInt("Schema")!=1&&n.getInt("Schema")!=2){result.future=n.copy();return result;}result.nextWander=n.getLong("NextWander");var values=n.getCompound("Villages");for(var key:values.getKeys()){var row=values.getCompound(key);var claim=new Claim();claim.entity=row.containsUuid("Entity")?row.getUuid("Entity"):null;claim.attempts=Math.max(0,row.getInt("Attempts"));claim.searchVersion=row.contains("SearchVersion")?row.getInt("SearchVersion"):1;claim.next=row.getLong("Next");result.villages.put(key,claim);}return result;}
}
