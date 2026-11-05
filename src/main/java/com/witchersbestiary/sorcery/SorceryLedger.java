package com.witchersbestiary.sorcery;
import java.util.*;import net.minecraft.world.PersistentState;import net.minecraft.nbt.*;import net.minecraft.registry.RegistryWrapper;import net.minecraft.server.MinecraftServer;
public final class SorceryLedger extends PersistentState {
 public static final Type<SorceryLedger> TYPE=new Type<>(SorceryLedger::new,SorceryLedger::read,null);
 public static final class Progress {public boolean learned;public int cooldown;}
 private final Map<UUID,Progress> players=new HashMap<>();
 public static SorceryLedger get(MinecraftServer s){return s.getOverworld().getPersistentStateManager().getOrCreate(TYPE,"witchersbestiary_sorcery");}
 public Progress player(UUID id){return players.computeIfAbsent(id,k->new Progress());}
 public NbtCompound writeNbt(NbtCompound n,RegistryWrapper.WrapperLookup lookup){players.forEach((id,p)->{var value=new NbtCompound();value.putBoolean("icy_grasp",p.learned);value.putInt("cooldown",p.cooldown);n.put(id.toString(),value);});return n;}
 public static SorceryLedger read(NbtCompound n,RegistryWrapper.WrapperLookup lookup){var l=new SorceryLedger();for(String key:n.getKeys())try{var p=l.player(UUID.fromString(key));var value=n.getCompound(key);p.learned=value.getBoolean("icy_grasp");p.cooldown=Math.clamp(value.getInt("cooldown"),0,160);}catch(IllegalArgumentException ignored){}return l;}
}
