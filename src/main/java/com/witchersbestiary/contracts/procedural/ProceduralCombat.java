package com.witchersbestiary.contracts.procedural;
import net.minecraft.nbt.NbtCompound;
/** Only active named entities allocate runtime ability state. Vanilla entity NBT owns this state. */
public final class ProceduralCombat {
 public final ProceduralProfile profile;public final net.minecraft.text.Text displayName;public int poison,rage;public boolean rageUsed;
 public ProceduralCombat(ProceduralProfile profile){this.profile=profile;this.displayName=profile.displayName();}
 public NbtCompound write(){var n=profile.write();n.putInt("PoisonCooldown",poison);n.putInt("RageTicks",rage);n.putBoolean("RageUsed",rageUsed);return n;}
 public static ProceduralCombat read(NbtCompound n){var s=new ProceduralCombat(ProceduralProfile.read(n));s.poison=Math.clamp(n.getInt("PoisonCooldown"),0,100);s.rage=Math.clamp(n.getInt("RageTicks"),0,300);s.rageUsed=n.getBoolean("RageUsed");return s;}
}
