package com.witchersbestiary.enchantment;
import com.mojang.serialization.*;import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;import net.minecraft.network.codec.PacketCodec;
/** The charge/cooldown belongs to this actual ItemStack and survives ordinary saving. */
public record RunicState(int charges,long readyAt,long lastHit) {
 public static final RunicState EMPTY=new RunicState(0,0,-1);
 public static final Codec<RunicState> CODEC=RecordCodecBuilder.create(i->i.group(Codec.intRange(0,7).fieldOf("charges").forGetter(RunicState::charges),Codec.LONG.fieldOf("ready_at").forGetter(RunicState::readyAt),Codec.LONG.fieldOf("last_hit").forGetter(RunicState::lastHit)).apply(i,RunicState::new));
 public static final PacketCodec<RegistryByteBuf,RunicState> PACKET=PacketCodec.of((s,b)->{b.writeVarInt(s.charges);b.writeLong(s.readyAt);b.writeLong(s.lastHit);},b->new RunicState(Math.clamp(b.readVarInt(),0,7),b.readLong(),b.readLong()));
}
