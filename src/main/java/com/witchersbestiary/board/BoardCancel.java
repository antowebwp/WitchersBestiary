package com.witchersbestiary.board;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
/** Confirmed cancellation carries an instance identity, never an offer/row index. */
public record BoardCancel(int syncId,UUID instance) implements CustomPayload {
 public static final Id<BoardCancel> ID=new Id<>(Identifier.of("witchersbestiary:board_cancel"));
 public static final PacketCodec<RegistryByteBuf,BoardCancel> CODEC=PacketCodec.of((p,b)->{b.writeVarInt(p.syncId);b.writeUuid(p.instance);},b->new BoardCancel(b.readVarInt(),b.readUuid()));
 public Id<BoardCancel> getId(){return ID;}
}
