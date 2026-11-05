package com.witchersbestiary.signs;
import java.util.*;import net.minecraft.world.PersistentState;import net.minecraft.nbt.*;import net.minecraft.registry.RegistryWrapper;import net.minecraft.server.MinecraftServer;
public final class SignLedger extends PersistentState {
 public static final Type<SignLedger> TYPE=new Type<>(SignLedger::new,SignLedger::read,null);private final Map<UUID,SignPlayerState> players=new HashMap<>();
 public static SignLedger get(MinecraftServer s){return s.getOverworld().getPersistentStateManager().getOrCreate(TYPE,"witchersbestiary_signs");}
 public SignPlayerState player(UUID id){return players.computeIfAbsent(id,k->new SignPlayerState());}
 public NbtCompound writeNbt(NbtCompound n,RegistryWrapper.WrapperLookup lookup){players.forEach((id,state)->n.put(id.toString(),state.write()));return n;}
 public static SignLedger read(NbtCompound n,RegistryWrapper.WrapperLookup lookup){var l=new SignLedger();for(String key:n.getKeys())try{l.players.put(UUID.fromString(key),SignPlayerState.read(n.getCompound(key)));}catch(IllegalArgumentException ignored){}return l;}
}
