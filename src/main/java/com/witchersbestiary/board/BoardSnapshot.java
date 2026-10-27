package com.witchersbestiary.board;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
public record BoardSnapshot(int syncId,String json) implements CustomPayload {
    public static final Id<BoardSnapshot> ID=new Id<>(Identifier.of("witchersbestiary:board_snapshot"));
    public static final PacketCodec<RegistryByteBuf,BoardSnapshot> CODEC=PacketCodec.of((p,b)->{b.writeVarInt(p.syncId);b.writeString(p.json,65536);},b->new BoardSnapshot(b.readVarInt(),b.readString(65536)));
    public Id<BoardSnapshot> getId(){return ID;}
}
