package com.witchersbestiary.fist;
import java.util.UUID;
import net.minecraft.network.*;import net.minecraft.network.codec.*;import net.minecraft.network.packet.CustomPayload;import net.minecraft.util.Identifier;import net.minecraft.server.network.ServerPlayerEntity;
public final class TournamentPackets {
 public record Control(UUID token,int action) implements CustomPayload {
  public static final Id<Control> ID=new Id<>(Identifier.of("witchersbestiary:tournament_control"));public static final PacketCodec<RegistryByteBuf,Control> CODEC=PacketCodec.of((p,b)->{b.writeUuid(p.token);b.writeVarInt(p.action);},b->new Control(b.readUuid(),b.readVarInt()));public Id<Control> getId(){return ID;}
 }
 public record View(UUID token,int titles,int category,int round,boolean fighting,boolean open,int lastCategory,int lastRound,boolean lastWon) implements CustomPayload {
  public static final Id<View> ID=new Id<>(Identifier.of("witchersbestiary:tournament_view"));public static final PacketCodec<RegistryByteBuf,View> CODEC=PacketCodec.of((p,b)->{b.writeUuid(p.token);b.writeVarInt(p.titles);b.writeVarInt(p.category);b.writeVarInt(p.round);b.writeBoolean(p.fighting);b.writeBoolean(p.open);b.writeVarInt(p.lastCategory);b.writeVarInt(p.lastRound);b.writeBoolean(p.lastWon);},b->new View(b.readUuid(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readBoolean()));public Id<View> getId(){return ID;}
 }
 public static void send(ServerPlayerEntity p,View v){if(net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p,View.ID))net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p,v);}
 public static void initialize(){net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(Control.ID,Control.CODEC);net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(View.ID,View.CODEC);net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(Control.ID,(packet,c)->c.server().execute(()->Tournaments.request(c.player(),packet)));}
}
