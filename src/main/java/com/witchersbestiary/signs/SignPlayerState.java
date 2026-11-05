package com.witchersbestiary.signs;
import net.minecraft.nbt.NbtCompound;
/** Server-owned persistent resource. Time advances only while this state is ticked online. */
public final class SignPlayerState {
 private int learned;private SignType selected=SignType.IGNI;private double concentration=100;private int regenDelay;private final int[] cooldowns=new int[5];private long lastRequest=Long.MIN_VALUE;
 public int learned(){return learned;}public SignType selected(){return selected;}public double concentration(){return concentration;}public int cooldown(SignType sign){return cooldowns[sign.ordinal()];}
 public boolean knows(SignType sign){return (learned&(1<<sign.ordinal()))!=0;}
 public boolean learn(SignType sign){if(knows(sign))return false;learned|=1<<sign.ordinal();return true;}
 public boolean select(SignType sign){if(sign==null||!knows(sign))return false;selected=sign;return true;}
 /** Call only after the cast manager has validated sender, world, duel rules and target. */
 public boolean commit(SignType sign,long request){if(sign==null||request<=lastRequest||!knows(sign)||cooldown(sign)>0||concentration<sign.cost)return false;lastRequest=request;concentration=Math.max(0,concentration-sign.cost);cooldowns[sign.ordinal()]=sign.cooldown;regenDelay=30;return true;}
 public void tick(){for(int i=0;i<cooldowns.length;i++)if(cooldowns[i]>0)cooldowns[i]--;if(regenDelay>0)regenDelay--;else concentration=Math.min(100,concentration+.25);}
 public NbtCompound write(){var n=new NbtCompound();n.putInt("schema",1);n.putInt("learned",learned);n.putInt("selected",selected.ordinal());n.putDouble("concentration",concentration);n.putInt("regenDelay",regenDelay);n.putIntArray("cooldowns",cooldowns);return n;}
 public static SignPlayerState read(NbtCompound n){var s=new SignPlayerState();s.learned=n.getInt("learned")&31;var selected=SignType.byIndex(n.getInt("selected"));if(selected!=null)s.selected=selected;double value=n.contains("concentration")?n.getDouble("concentration"):100;s.concentration=Double.isFinite(value)?Math.clamp(value,0,100):0;s.regenDelay=Math.clamp(n.getInt("regenDelay"),0,30);int[] saved=n.getIntArray("cooldowns");for(int i=0;i<Math.min(saved.length,5);i++)s.cooldowns[i]=Math.clamp(saved[i],0,SignType.values()[i].cooldown);return s;}
}
