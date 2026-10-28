package com.witchersbestiary.contracts.tracking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;
/** Logical clue, never a world entity or block. Ownership is persisted and validated server-side. */
public final class ContractClue {
 public enum Type {FOOTPRINTS,BLOOD_TRACE,CLAW_MARKS,REMAINS,MIST_RESIDUE}
 public final UUID id,contract,owner,next;public final Type type;public final BlockPos position;public final String dimension;public final int order;public boolean studied;
 public ContractClue(UUID id,UUID contract,UUID owner,Type type,BlockPos position,String dimension,int order,UUID next){this.id=id;this.contract=contract;this.owner=owner;this.type=type;this.position=position.toImmutable();this.dimension=dimension;this.order=order;this.next=next;}
 public NbtCompound write(){var n=new NbtCompound();n.putUuid("Id",id);n.putUuid("Contract",contract);n.putUuid("Owner",owner);n.putString("Type",type.name());n.putLong("Position",position.asLong());n.putString("Dimension",dimension);n.putInt("Order",order);n.putBoolean("Studied",studied);if(next!=null)n.putUuid("Next",next);return n;}
 public static ContractClue read(NbtCompound n){var c=new ContractClue(n.getUuid("Id"),n.getUuid("Contract"),n.getUuid("Owner"),Type.valueOf(n.getString("Type")),BlockPos.fromLong(n.getLong("Position")),n.getString("Dimension"),n.getInt("Order"),n.containsUuid("Next")?n.getUuid("Next"):null);c.studied=n.getBoolean("Studied");return c;}
}
