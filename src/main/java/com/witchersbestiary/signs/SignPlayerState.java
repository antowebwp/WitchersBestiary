package com.witchersbestiary.signs;
import net.minecraft.nbt.NbtCompound;
/** Server-owned persistent resource. Time advances only while this state is ticked online. */
public final class SignPlayerState {
 public enum Upgrade { POWER, RANGE, DURATION, EFFECT, COST, REGEN }
 private final int[] upgrades=new int[30], unlocks=new int[5], slots=new int[10];
 private int points;
 private long combatAwardAfter;
 private final java.util.Map<java.util.UUID,Long> recentTargets=new java.util.HashMap<>();
 public boolean awardCombat(java.util.UUID target,long tick){
  if(tick<combatAwardAfter||recentTargets.getOrDefault(target,0L)>tick)return false;
  recentTargets.values().removeIf(until->until<=tick);
  if(recentTargets.size()>=32)return false;
  recentTargets.put(target,tick+1200);combatAwardAfter=tick+120;award(1);return true;
 }
 public int points(){return points;}
 public int level(SignType sign,Upgrade node){return upgrades[sign.ordinal()*6+node.ordinal()];}
 public int form(SignType sign,int slot){return slots[sign.ordinal()*2+slot];}
 public int unlocked(SignType sign){return unlocks[sign.ordinal()]|1;}
 public void award(int amount){points=Math.min(100000,points+Math.max(0,amount));}
 public boolean upgrade(SignType sign,Upgrade node){
  int index=sign.ordinal()*6+node.ordinal(), level=upgrades[index], price=2+level*2;
  if(!knows(sign)||level>=3||points<price)return false;
  points-=price;upgrades[index]++;return true;
 }
 public boolean unlock(SignType sign,int form){
  if(form<1||form>3||!knows(sign)||(unlocked(sign)&(1<<form))!=0||points<2+form)return false;
  int total=0;for(var node:Upgrade.values())total+=level(sign,node);
  if(total<form*2)return false;
  points-=2+form;unlocks[sign.ordinal()]|=1<<form;return true;
 }
 public boolean equip(SignType sign,int slot,int form){
  if(!knows(sign)||slot<0||slot>1||form<0||form>3||(unlocked(sign)&(1<<form))==0)return false;
  slots[sign.ordinal()*2+slot]=form;return true;
 }
 public int cost(SignType sign,int form){return sign.cost-level(sign,Upgrade.COST)*2+(form==0?0:4);}
 private int learned;private SignType selected=SignType.IGNI;private double concentration=100;private int regenDelay;private final int[] cooldowns=new int[5];private long lastRequest=Long.MIN_VALUE;
 public void resetProtocol(){lastRequest=Long.MIN_VALUE;}
 public int learned(){return learned;}public SignType selected(){return selected;}public double concentration(){return concentration;}public int cooldown(SignType sign){return cooldowns[sign.ordinal()];}
 public boolean knows(SignType sign){return (learned&(1<<sign.ordinal()))!=0;}
 public boolean learn(SignType sign){if(knows(sign))return false;learned|=1<<sign.ordinal();return true;}
 public boolean select(SignType sign){if(sign==null||!knows(sign))return false;selected=sign;return true;}
 /** Call only after the cast manager has validated sender, world, duel rules and target. */
 public boolean commit(SignType sign,long request){return commit(sign,request,0);}
 public boolean commit(SignType sign,long request,int form){if(sign==null||request<=lastRequest||!knows(sign)||cooldown(sign)>0||concentration<cost(sign,form))return false;lastRequest=request;concentration=Math.max(0,concentration-cost(sign,form));cooldowns[sign.ordinal()]=sign.cooldown;regenDelay=30;return true;}
 public void tick(){tick(1);}
 public boolean spendBreathTick(){
  if(concentration<.25){concentration=0;regenDelay=30;return false;}
  concentration-=.25;regenDelay=30;return true;
 }
 public void tick(double regeneration){for(int i=0;i<cooldowns.length;i++)if(cooldowns[i]>0)cooldowns[i]--;if(regenDelay>0)regenDelay--;else concentration=Math.min(100,concentration+(.25+.02*level(selected,Upgrade.REGEN))*regeneration);}
 public NbtCompound write(){var n=new NbtCompound();n.putInt("schema",2);n.putInt("points",points);n.putLong("combatAwardAfter",combatAwardAfter);var combat=new NbtCompound();recentTargets.forEach((target,until)->combat.putLong(target.toString(),until));n.put("recentTargets",combat);n.putIntArray("upgrades",upgrades);n.putIntArray("unlocks",unlocks);n.putIntArray("slots",slots);n.putInt("learned",learned);n.putInt("selected",selected.ordinal());n.putDouble("concentration",concentration);n.putInt("regenDelay",regenDelay);n.putIntArray("cooldowns",cooldowns);return n;}
 public static SignPlayerState read(NbtCompound n){var s=new SignPlayerState();s.learned=n.getInt("learned")&31;var selected=SignType.byIndex(n.getInt("selected"));if(selected!=null)s.selected=selected;double value=n.contains("concentration")?n.getDouble("concentration"):100;s.concentration=Double.isFinite(value)?Math.clamp(value,0,100):0;s.regenDelay=Math.clamp(n.getInt("regenDelay"),0,30);int[] saved=n.getIntArray("cooldowns");for(int i=0;i<Math.min(saved.length,5);i++)s.cooldowns[i]=Math.clamp(saved[i],0,SignType.values()[i].cooldown);s.points=Math.clamp(n.getInt("points"),0,100000);
  s.combatAwardAfter=Math.max(0,n.getLong("combatAwardAfter"));
  var combat=n.getCompound("recentTargets");
  for(String key:combat.getKeys()){
   if(s.recentTargets.size()>=32)break;
   try{s.recentTargets.put(java.util.UUID.fromString(key),Math.max(0,combat.getLong(key)));}catch(IllegalArgumentException ignored){}
  }
  int[] levels=n.getIntArray("upgrades"), unlocked=n.getIntArray("unlocks"), equipped=n.getIntArray("slots");
  for(int i=0;i<Math.min(levels.length,30);i++)s.upgrades[i]=Math.clamp(levels[i],0,3);
  for(int i=0;i<Math.min(unlocked.length,5);i++)s.unlocks[i]=unlocked[i]&14;
  for(int i=0;i<Math.min(equipped.length,10);i++){int f=Math.clamp(equipped[i],0,3);if((s.unlocked(SignType.values()[i/2])&(1<<f))!=0)s.slots[i]=f;}
  return s;}
}
