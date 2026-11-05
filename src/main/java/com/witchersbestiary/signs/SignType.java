package com.witchersbestiary.signs;
import net.minecraft.util.Identifier;
/** Only basic forms; ordinal values are stable save/network identifiers. */
public enum SignType {
 IGNI(30,60,6,60,0xffff7430), AARD(25,60,5,0,0xff75d5ff), QUEN(40,160,0,240,0xffffcd55), YRDEN(35,140,3,200,0xffbe73ed), AXII(35,200,6,80,0xff87cb63);
 public final int cost,cooldown,duration,color;public final double range;public final Identifier id;
 SignType(int cost,int cooldown,double range,int duration,int color){this.cost=cost;this.cooldown=cooldown;this.range=range;this.duration=duration;this.color=color;id=Identifier.of("witchersbestiary",name().toLowerCase(java.util.Locale.ROOT));}
 public static SignType byIndex(int index){return index>=0&&index<values().length?values()[index]:null;}
}
