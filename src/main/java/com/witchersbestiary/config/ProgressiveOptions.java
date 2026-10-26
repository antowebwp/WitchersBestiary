package com.witchersbestiary.config;
import com.google.gson.*;
import java.util.*;

/** The nine editable options, mapped to the existing settings; exclusions remain server-owned. */
public record ProgressiveOptions(boolean enabled,List<Double> values){
 public record Field(String key,double fallback,double min,double max,double scale,boolean integer){}
 public static final List<Field> FIELDS=List.of(
  new Field("progressiveDayInterval",25,1,1000000,1,true),
  new Field("progressiveHealthIncrement",.3,0,100,100,false),
  new Field("progressiveDamageIncrement",.15,0,100,100,false),
  new Field("progressiveArmorBypassIncrement",.05,0,1,100,false),
  new Field("progressiveNormalHealthCap",700,1,1000000,1,false),
  new Field("progressiveEliteHealthCap",3500,1,1000000,1,false),
  new Field("progressiveDamageCap",4,1,100,1,false),
  new Field("progressiveArmorBypassCap",.85,0,1,100,false));
 public ProgressiveOptions{
  values=List.copyOf(values);if(values.size()!=FIELDS.size())throw new IllegalArgumentException("options");
  for(int i=0;i<values.size();i++){double n=values.get(i);var f=FIELDS.get(i);if(!Double.isFinite(n)||n<f.min||n>f.max||f.integer&&n!=Math.rint(n))throw new IllegalArgumentException(f.key);}
 }
 public static ProgressiveOptions defaults(){return from(ProgressiveSettings.read(new Properties()));}
 public static ProgressiveOptions from(ProgressiveSettings s){return new ProgressiveOptions(s.enabled(),List.of((double)s.interval(),s.healthIncrement(),s.damageIncrement(),s.bypassIncrement(),s.normalCap(),s.eliteCap(),s.damageCap(),s.bypassCap()));}
 public Properties properties(){var p=new Properties();p.setProperty("progressiveEnabled",Boolean.toString(enabled));for(int i=0;i<FIELDS.size();i++)p.setProperty(FIELDS.get(i).key,Double.toString(values.get(i)));return p;}
 public ProgressiveSettings applyTo(ProgressiveSettings base){return new ProgressiveSettings(enabled,values.get(0).intValue(),values.get(1),values.get(2),values.get(3),values.get(4),values.get(5),values.get(6),values.get(7),base.excluded(),base.elites());}
 public JsonObject json(){var j=new JsonObject();j.addProperty("progressiveEnabled",enabled);for(int i=0;i<FIELDS.size();i++)j.addProperty(FIELDS.get(i).key,values.get(i));return j;}
 public static ProgressiveOptions parse(JsonObject j){
  if(j.size()!=9||!j.has("progressiveEnabled")||!j.get("progressiveEnabled").isJsonPrimitive()||!j.getAsJsonPrimitive("progressiveEnabled").isBoolean())throw new IllegalArgumentException("enabled");
  var numbers=new ArrayList<Double>();for(var f:FIELDS){var n=j.get(f.key);if(n==null||!n.isJsonPrimitive()||!n.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(f.key);numbers.add(n.getAsDouble());}
  return new ProgressiveOptions(j.get("progressiveEnabled").getAsBoolean(),numbers);
 }
}
