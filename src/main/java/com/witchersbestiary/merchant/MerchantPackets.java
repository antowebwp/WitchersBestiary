package com.witchersbestiary.merchant;
import java.util.UUID;
import net.minecraft.network.*;import net.minecraft.network.codec.PacketCodec;import net.minecraft.network.packet.CustomPayload;import net.minecraft.util.Identifier;
public final class MerchantPackets {
 public record Buy(int sync,UUID session,long sequence,String offer) implements CustomPayload {
  public static final Id<Buy> ID=new Id<>(Identifier.of("witchersbestiary:merchant_buy"));public static final PacketCodec<RegistryByteBuf,Buy> CODEC=PacketCodec.of((p,b)->{b.writeVarInt(p.sync);b.writeUuid(p.session);b.writeLong(p.sequence);b.writeString(p.offer);},b->new Buy(b.readVarInt(),b.readUuid(),b.readLong(),b.readString(128)));public Id<Buy> getId(){return ID;}
 }
 public record State(int sync,UUID session,long sequence,String json) implements CustomPayload {
  public static final Id<State> ID=new Id<>(Identifier.of("witchersbestiary:merchant_state"));public static final PacketCodec<RegistryByteBuf,State> CODEC=PacketCodec.of((p,b)->{b.writeVarInt(p.sync);b.writeUuid(p.session);b.writeLong(p.sequence);b.writeString(p.json);},b->new State(b.readVarInt(),b.readUuid(),b.readLong(),b.readString(16384)));public Id<State> getId(){return ID;}
 }
 public static void initialize(){var c=net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S();c.register(Buy.ID,Buy.CODEC);net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(State.ID,State.CODEC);net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(Buy.ID,(packet,ctx)->ctx.server().execute(()->{if(ctx.player().currentScreenHandler instanceof MerchantHandler h)h.buy(ctx.player(),packet);}));}
}
