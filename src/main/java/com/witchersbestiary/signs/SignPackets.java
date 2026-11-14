package com.witchersbestiary.signs;
import java.util.*;import net.minecraft.network.*;import net.minecraft.network.codec.PacketCodec;import net.minecraft.network.packet.CustomPayload;import net.minecraft.util.Identifier;import net.fabricmc.fabric.api.networking.v1.*;
public final class SignPackets {
 public record Intent(UUID session,long sequence,int action,int sign,String dimension) implements CustomPayload {public static final Id<Intent>ID=new Id<>(Identifier.of("witchersbestiary:sign_intent"));public static final PacketCodec<RegistryByteBuf,Intent> CODEC=PacketCodec.of((p,b)->{b.writeUuid(p.session);b.writeLong(p.sequence);b.writeVarInt(p.action);b.writeVarInt(p.sign);b.writeString(p.dimension);},b->new Intent(b.readUuid(),b.readLong(),b.readVarInt(),b.readVarInt(),b.readString(128)));public Id<Intent>getId(){return ID;}}
 public record View(UUID session,int learned,int selected,float energy,int cooldown,float shield,int shieldTicks,int trapTicks,int c0,int c1,int c2,int c3,int c4) implements CustomPayload {public static final Id<View>ID=new Id<>(Identifier.of("witchersbestiary:sign_view"));public static final PacketCodec<RegistryByteBuf,View> CODEC=PacketCodec.of((p,b)->{b.writeUuid(p.session);b.writeVarInt(p.learned);b.writeVarInt(p.selected);b.writeFloat(p.energy);b.writeVarInt(p.cooldown);b.writeFloat(p.shield);b.writeVarInt(p.shieldTicks);b.writeVarInt(p.trapTicks);b.writeVarInt(p.c0);b.writeVarInt(p.c1);b.writeVarInt(p.c2);b.writeVarInt(p.c3);b.writeVarInt(p.c4);},b->new View(b.readUuid(),b.readVarInt(),b.readVarInt(),b.readFloat(),b.readVarInt(),b.readFloat(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt()));public int remaining(int sign){return switch(sign){case 0->c0;case 1->c1;case 2->c2;case 3->c3;default->c4;};}public Id<View>getId(){return ID;}}
 public record Mastery(int points, List<Integer> values) implements CustomPayload {
  public static final Id<Mastery> ID=new Id<>(Identifier.of("witchersbestiary:sign_mastery"));
  public static final PacketCodec<RegistryByteBuf,Mastery> CODEC=PacketCodec.of((p,b)->{
   b.writeVarInt(p.points);for(int value:p.values)b.writeByte(value);
  },b->{int points=b.readVarInt();var values=new ArrayList<Integer>(45);for(int i=0;i<45;i++)values.add((int)b.readUnsignedByte());return new Mastery(points,values);});
  public Mastery { values=List.copyOf(values); }
  public int level(int sign,int node){return values.get(sign*9+node);}
  public int unlocked(int sign){return values.get(sign*9+6);}
  public int form(int sign,int slot){return values.get(sign*9+7+slot);}
  public Id<Mastery> getId(){return ID;}
 }
 public record QuenShell(int entityId,int form,int ticks) implements CustomPayload {
  public static final Id<QuenShell> ID=new Id<>(Identifier.of("witchersbestiary:quen_shell"));
  public static final PacketCodec<RegistryByteBuf,QuenShell> CODEC=PacketCodec.of((p,b)->{
   b.writeVarInt(p.entityId);b.writeByte(p.form);b.writeByte(p.ticks);
  },b->new QuenShell(b.readVarInt(),b.readUnsignedByte(),b.readUnsignedByte()));
  public Id<QuenShell> getId(){return ID;}
 }
 public record Visual(int owner,int sign,int form,int ticks,net.minecraft.util.math.Vec3d from,
                      net.minecraft.util.math.Vec3d to,float radius) implements CustomPayload {
  public static final Id<Visual> ID=new Id<>(Identifier.of("witchersbestiary:sign_visual"));
  public static final PacketCodec<RegistryByteBuf,Visual> CODEC=PacketCodec.of((p,b)->{
   b.writeVarInt(p.owner);b.writeByte(p.sign);b.writeByte(p.form);b.writeVarInt(p.ticks);
   b.writeVec3d(p.from);b.writeVec3d(p.to);b.writeFloat(p.radius);
  },b->new Visual(b.readVarInt(),b.readUnsignedByte(),b.readUnsignedByte(),b.readVarInt(),b.readVec3d(),b.readVec3d(),b.readFloat()));
  public Id<Visual> getId(){return ID;}
 }
 public static void initialize(){PayloadTypeRegistry.playS2C().register(Visual.ID,Visual.CODEC);PayloadTypeRegistry.playC2S().register(Intent.ID,Intent.CODEC);PayloadTypeRegistry.playS2C().register(View.ID,View.CODEC);PayloadTypeRegistry.playS2C().register(QuenShell.ID,QuenShell.CODEC);PayloadTypeRegistry.playS2C().register(Mastery.ID,Mastery.CODEC);ServerPlayNetworking.registerGlobalReceiver(Intent.ID,(p,c)->c.server().execute(()->SignManager.intent(c.player(),p)));}
}
