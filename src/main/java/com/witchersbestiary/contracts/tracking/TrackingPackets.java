package com.witchersbestiary.contracts.tracking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;
public final class TrackingPackets {
 public record Toggle(boolean enabled) implements CustomPayload {public static final Id<Toggle> ID=new Id<>(Identifier.of("witchersbestiary:senses_toggle"));public static final PacketCodec<RegistryByteBuf,Toggle> CODEC=PacketCodec.of((p,b)->b.writeBoolean(p.enabled),b->new Toggle(b.readBoolean()));public Id<Toggle> getId(){return ID;}}
 public record Inspect(UUID clue) implements CustomPayload {public static final Id<Inspect> ID=new Id<>(Identifier.of("witchersbestiary:clue_inspect"));public static final PacketCodec<RegistryByteBuf,Inspect> CODEC=PacketCodec.of((p,b)->b.writeUuid(p.clue),b->new Inspect(b.readUuid()));public Id<Inspect> getId(){return ID;}}
 public record Snapshot(String json) implements CustomPayload {public static final Id<Snapshot> ID=new Id<>(Identifier.of("witchersbestiary:senses_snapshot"));public static final PacketCodec<RegistryByteBuf,Snapshot> CODEC=PacketCodec.of((p,b)->b.writeString(p.json,8192),b->new Snapshot(b.readString(8192)));public Id<Snapshot> getId(){return ID;}}
}
