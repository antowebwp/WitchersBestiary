package com.witchersbestiary.world;

import com.google.gson.GsonBuilder;
import com.witchersbestiary.WitchersBestiary;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/** Частота появления задаётся в config/witchersbestiary.json и ограничивается безопасным диапазоном. */
public final class DrownerSpawnConfig {
    private static int weight=24;
    private DrownerSpawnConfig(){}
    public static int weight(){return weight;}
    public static void load(){
        Path path=FabricLoader.getInstance().getConfigDir().resolve("witchersbestiary.json");
        try{
            if(Files.exists(path)){
                var json=com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                if(json.has("drownerSpawnWeight"))weight=Math.clamp(json.get("drownerSpawnWeight").getAsInt(),0,100);
            }else{
                Files.createDirectories(path.getParent());
                Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(java.util.Map.of("drownerSpawnWeight",weight))+"\n");
            }
        }catch(Exception e){WitchersBestiary.LOGGER.warn("Не удалось прочитать {}. Используется spawnWeight={}",path,weight,e);}
    }
}
