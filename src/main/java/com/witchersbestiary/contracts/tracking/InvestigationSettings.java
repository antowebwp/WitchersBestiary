package com.witchersbestiary.contracts.tracking;
import com.google.gson.*;
import java.nio.file.*;
/** Bounded independent configuration; malformed user files are preserved, with safe defaults. */
public record InvestigationSettings(int common,int rare,int epic,int legendary,int radius) {
 public static final InvestigationSettings DEFAULT=new InvestigationSettings(2,3,4,5,24);
 public InvestigationSettings {common=Math.clamp(common,1,5);rare=Math.clamp(rare,1,5);epic=Math.clamp(epic,1,5);legendary=Math.clamp(legendary,1,5);radius=Math.clamp(radius,8,32);}
 public static InvestigationSettings load(){var path=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("witchersbestiary-investigations.json");try{if(!Files.exists(path)){Files.createDirectories(path.getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(DEFAULT));return DEFAULT;}var j=JsonParser.parseString(Files.readString(path)).getAsJsonObject();return new InvestigationSettings(number(j,"common",2),number(j,"rare",3),number(j,"epic",4),number(j,"legendary",5),number(j,"radius",24));}catch(Exception e){com.witchersbestiary.WitchersBestiary.LOGGER.warn("Invalid investigation configuration; using safe defaults");return DEFAULT;}}
 private static int number(JsonObject j,String key,int fallback){return j.has(key)?j.get(key).getAsInt():fallback;}
}
