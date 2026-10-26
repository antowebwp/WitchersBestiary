package com.witchersbestiary.config;

import com.witchersbestiary.WitchersBestiary;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.Properties;

public final class ModConfig {
    private static int werewolfSpawnWeight=2;
    private static boolean natural=true,bossNatural=true;
    private static NaturalSpawnSettings spawnSettings=new NaturalSpawnSettings(new Properties());
    public record GroupSize(int min,int max){}
    public static GroupSize groupSize(String id,int min,int max){return spawnSettings.group(id,min,max);}
    public static int groupMaximum(String id,int fallback){return groupSize(id,1,fallback).max();}
    public static boolean naturalSpawning(boolean boss){return natural&&(!boss||bossNatural);}
    public static int spawnWeight(String id,int fallback,boolean boss){return naturalSpawning(boss)?spawnSettings.weight(id,fallback):0;}
    public static boolean spawnAllowed(String id,int fallback,boolean boss,net.minecraft.util.math.random.Random random){
        if(!naturalSpawning(boss)||!spawnSettings.enabled(id))return false;
        double chance=spawnSettings.admissionChance(id,fallback);return chance>=1||chance>0&&random.nextDouble()<chance;
    }
    private static double monsterSpeedMultiplier=1, attackCooldownMultiplier=1, abilityCooldownMultiplier=1, monsterDamageMultiplier=1;
    public static double speed(double base){return base*monsterSpeedMultiplier;}
    public static int attackTicks(int base){return Math.max(1,(int)Math.round(base*attackCooldownMultiplier));}
    public static int abilityTicks(int base){return Math.max(1,(int)Math.round(base*abilityCooldownMultiplier));}
    public static float damage(float base){return (float)(base*monsterDamageMultiplier);}
    private static double read(Properties p,String key){try{double v=Double.parseDouble(p.getProperty(key,"1.0"));return Double.isFinite(v)?Math.clamp(v,.25,3):1;}catch(NumberFormatException e){return 1;}}
    private static volatile ProgressiveSettings progressive=ProgressiveSettings.read(new Properties());
    public static ProgressiveSettings progressive(){return progressive;}
    public static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("witchersbestiary.properties");}
    public static void saveProgressive(ProgressiveOptions options)throws java.io.IOException{
        var updated=ProgressiveConfigFile.save(path(),options);progressive=updated;
    }
    private ModConfig() { }
    public static int werewolfSpawnWeight(){return werewolfSpawnWeight;}
    public static void initialize(){
        Path path=path();
        try{
            boolean created=!Files.exists(path);
            if(created){Files.createDirectories(path.getParent());Files.writeString(path,"# Werewolf spawn weight: 0 disables natural spawning; range 0-20. Restart required.\nwerewolf_spawn_weight=2\nenableNaturalSpawning=true\nnaturalSpawnMultiplier=1.0\nenableBossNaturalSpawning=true\nrareSpawnMultiplier=1.0\nmonsterSpeedMultiplier=1.0\nattackCooldownMultiplier=1.0\nabilityCooldownMultiplier=1.0\nmonsterDamageMultiplier=1.0\ndrownerMinGroupSize=3\ndrownerMaxGroupSize=5\nghoulMinGroupSize=2\nghoulMaxGroupSize=4\nwraithMinGroupSize=1\nwraithMaxGroupSize=2\nfiendSpawnWeight=1\n");}
            if(created){
                StringBuilder switches=new StringBuilder("\n# Individual natural-spawn switches; restart required.\n");
                for(String id:NaturalSpawnSettings.IDS)switches.append(id).append("NaturalSpawnEnabled=true\n");
                Files.writeString(path,switches.toString(),StandardOpenOption.APPEND);
            }
            Properties properties=new Properties();try(var in=Files.newInputStream(path)){properties.load(in);}
            // Append only missing progressive keys; preserve all user settings/comments.
            var defaults=new Properties();defaults.load(new java.io.StringReader(ProgressiveSettings.DEFAULTS));
            defaults.setProperty("water_hagNaturalSpawnEnabled","true");defaults.setProperty("water_hagSpawnWeight","4");defaults.setProperty("water_hagMinGroupSize","1");defaults.setProperty("water_hagMaxGroupSize","2");
            var additions=new StringBuilder("\n# Progressive difficulty (live server settings)\n");
            boolean missing=false;
            for(String key:new java.util.TreeSet<>(defaults.stringPropertyNames()))if(!properties.containsKey(key)){
                String value=defaults.getProperty(key);properties.setProperty(key,value);additions.append(key).append('=').append(value).append('\n');missing=true;
            }
            if(missing)try{Files.writeString(path,additions.toString(),StandardOpenOption.APPEND);}
            catch(java.io.IOException e){WitchersBestiary.LOGGER.warn("Could not append progressive defaults; loaded settings remain active",e);}
            progressive=ProgressiveSettings.read(properties);
            natural=Boolean.parseBoolean(properties.getProperty("enableNaturalSpawning","true"));
            bossNatural=Boolean.parseBoolean(properties.getProperty("enableBossNaturalSpawning","true"));
            spawnSettings=new NaturalSpawnSettings(properties);
            monsterSpeedMultiplier=read(properties,"monsterSpeedMultiplier");attackCooldownMultiplier=read(properties,"attackCooldownMultiplier");
            abilityCooldownMultiplier=read(properties,"abilityCooldownMultiplier");monsterDamageMultiplier=read(properties,"monsterDamageMultiplier");
            werewolfSpawnWeight=Math.max(0,Math.min(20,Integer.parseInt(properties.getProperty("werewolf_spawn_weight","2"))));
        }catch(Exception e){WitchersBestiary.LOGGER.warn("Could not read Werewolf spawn setting; using weight 2",e);werewolfSpawnWeight=2;}
    }
}
