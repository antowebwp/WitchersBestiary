package com.witchersbestiary.ward;
import net.minecraft.world.PersistentState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import java.util.*;
/** Per-dimension persistent spatial index. Installed wards remain active in unloaded chunks. No tickets or tick scans. */
public final class SpiritWardState extends PersistentState {
 public static final int RADIUS=128;
 public static final Type<SpiritWardState> TYPE=new Type<>(SpiritWardState::new,SpiritWardState::read,null);
 private record Cell(int x,int y,int z){}
 private final Map<Cell,Set<BlockPos>> cells=new HashMap<>();
 private static Cell cell(BlockPos p){return new Cell(Math.floorDiv(p.getX(),128),Math.floorDiv(p.getY(),128),Math.floorDiv(p.getZ(),128));}
 public static SpiritWardState get(ServerWorld w){return w.getPersistentStateManager().getOrCreate(TYPE,"witchersbestiary_spirit_wards");}
 public void add(BlockPos p){if(cells.computeIfAbsent(cell(p),k->new HashSet<>()).add(p.toImmutable()))markDirty();}
 public void remove(BlockPos p){var bucket=cells.get(cell(p));if(bucket!=null&&bucket.remove(p)){if(bucket.isEmpty())cells.remove(cell(p));markDirty();}}
 public boolean protects(BlockPos p){var c=cell(p);for(int x=c.x-1;x<=c.x+1;x++)for(int y=c.y-1;y<=c.y+1;y++)for(int z=c.z-1;z<=c.z+1;z++)for(var ward:cells.getOrDefault(new Cell(x,y,z),Set.of()))if(ward.getSquaredDistance(p)<=128d*128)return true;return false;}
 @Override public NbtCompound writeNbt(NbtCompound n,RegistryWrapper.WrapperLookup lookup){n.putLongArray("Wards",cells.values().stream().flatMap(Set::stream).mapToLong(BlockPos::asLong).toArray());return n;}
 public static SpiritWardState read(NbtCompound n,RegistryWrapper.WrapperLookup lookup){var s=new SpiritWardState();for(long p:n.getLongArray("Wards"))s.add(BlockPos.fromLong(p));return s;}
}
