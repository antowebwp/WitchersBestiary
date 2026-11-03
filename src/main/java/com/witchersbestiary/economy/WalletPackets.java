package com.witchersbestiary.economy;
import net.minecraft.network.*;import net.minecraft.network.codec.*;import net.minecraft.network.packet.CustomPayload;import net.minecraft.util.Identifier;
public final class WalletPackets {
 public record Action(int sync,long sequence,boolean deposit,long amount) implements CustomPayload {
  public static final Id<Action> ID=new Id<>(Identifier.of("witchersbestiary:wallet_action"));
  public static final PacketCodec<RegistryByteBuf,Action> CODEC=PacketCodec.of((p,b)->{b.writeVarInt(p.sync);b.writeLong(p.sequence);b.writeBoolean(p.deposit);b.writeLong(p.amount);},b->new Action(b.readVarInt(),b.readLong(),b.readBoolean(),b.readLong()));public Id<Action> getId(){return ID;}
 }
 public record State(int sync,long sequence,long balance,String status) implements CustomPayload {
  public static final Id<State> ID=new Id<>(Identifier.of("witchersbestiary:wallet_state"));public static final PacketCodec<RegistryByteBuf,State> CODEC=PacketCodec.of((p,b)->{b.writeVarInt(p.sync);b.writeLong(p.sequence);b.writeLong(p.balance);b.writeString(p.status);},b->new State(b.readVarInt(),b.readLong(),b.readLong(),b.readString(64)));public Id<State> getId(){return ID;}
 }
 public static void initialize(){net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(Action.ID,Action.CODEC);net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(State.ID,State.CODEC);net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(Action.ID,(a,c)->c.server().execute(()->{if(c.player().currentScreenHandler instanceof WalletHandler h)h.act(c.player(),a);}));}
}
