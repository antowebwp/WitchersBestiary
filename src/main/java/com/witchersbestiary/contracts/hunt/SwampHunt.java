package com.witchersbestiary.contracts.hunt;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;
/** Stored inside the authoritative contract, not in a second PersistentState. */
public final class SwampHunt {
 public enum Status { SEARCHING, WAITING, RESERVED, ACTIVE, DEAD, CANCELLED }
 public final String dimension; public final BlockPos origin; public UUID target;
 public BlockPos position; public Status status=Status.SEARCHING; public int attempts;
 public SwampHunt(String dimension,BlockPos origin){this.dimension=dimension;this.origin=origin.toImmutable();}
 public NbtCompound write(){var n=new NbtCompound();n.putString("Dimension",dimension);n.putLong("Origin",origin.asLong());n.putString("Status",status.name());n.putInt("Attempts",attempts);if(target!=null)n.putUuid("Target",target);if(position!=null)n.putLong("Position",position.asLong());return n;}
 public static SwampHunt read(NbtCompound n){var h=new SwampHunt(n.getString("Dimension"),BlockPos.fromLong(n.getLong("Origin")));h.status=Status.valueOf(n.getString("Status"));h.attempts=n.getInt("Attempts");if(n.containsUuid("Target"))h.target=n.getUuid("Target");if(n.contains("Position"))h.position=BlockPos.fromLong(n.getLong("Position"));if((h.status==Status.ACTIVE||h.status==Status.RESERVED||h.status==Status.DEAD)&&(h.target==null||h.position==null))throw new IllegalArgumentException("Hunt binding missing UUID/position");if(net.minecraft.util.Identifier.tryParse(h.dimension)==null)throw new IllegalArgumentException("Invalid hunt dimension");return h;}
}
