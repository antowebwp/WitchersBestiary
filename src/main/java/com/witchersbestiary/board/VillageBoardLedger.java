package com.witchersbestiary.board;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
/** Saved terminal village outcomes; no per-tick disk writes. */
public final class VillageBoardLedger extends PersistentState {
 public static final Type<VillageBoardLedger> TYPE=new Type<>(VillageBoardLedger::new,VillageBoardLedger::read,null);
 private NbtCompound future;
 public boolean readOnly(){return future!=null;}
 private final Map<String,String> outcomes=new HashMap<>();
 public static VillageBoardLedger get(MinecraftServer s){return s.getOverworld().getPersistentStateManager().getOrCreate(TYPE,"witchersbestiary_village_boards");}
 public String outcome(String key){return outcomes.get(key);}
 public void record(String key,String value){if(readOnly())throw new IllegalStateException("Future village ledger is read-only");if(!value.equals(outcomes.put(key,value)))markDirty();}
 public NbtCompound writeNbt(NbtCompound n,RegistryWrapper.WrapperLookup lookup){if(future!=null)return future.copy();n.putInt("Schema",1);var values=new NbtCompound();outcomes.forEach(values::putString);n.put("Outcomes",values);return n;}
 public static VillageBoardLedger read(NbtCompound n,RegistryWrapper.WrapperLookup lookup){var result=new VillageBoardLedger();if(n.getInt("Schema")!=1){result.future=n.copy();return result;}var values=n.getCompound("Outcomes");for(var key:values.getKeys())result.outcomes.put(key,values.getString(key));return result;}
}
