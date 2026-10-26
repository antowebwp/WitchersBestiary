package com.witchersbestiary.config;

import java.util.*;

/** Restart-scoped settings; integer biome entries retain fractional rates by admission sampling. */
public final class NaturalSpawnSettings {
    public static final List<String> IDS=List.of("water_hag","drowner","ghoul","nekker","wraith","noonwraith","foglet","endrega_warrior","werewolf","bonecrusher","katakan","leshen","fiend","griffin","infernal_basilisk");
    private static final Set<String> RARE=Set.of("werewolf","bonecrusher","katakan","leshen","fiend","griffin","infernal_basilisk");
    private final Properties values=new Properties();
    private final double natural,rare;
    public NaturalSpawnSettings(Properties properties){values.putAll(properties);natural=scale("naturalSpawnMultiplier","spawnRateMultiplier");rare=scale("rareSpawnMultiplier","bossSpawnRateMultiplier");}
    private double scale(String key,String legacy){try{double n=Double.parseDouble(values.getProperty(key,values.getProperty(legacy,"1")));return Double.isFinite(n)?Math.clamp(n,0,10):1;}catch(NumberFormatException e){return 1;}}
    public boolean enabled(String id){return Boolean.parseBoolean(values.getProperty(id+"NaturalSpawnEnabled","true"));}
    public double effectiveWeight(String id,int fallback){
        int base=fallback;try{base=Math.clamp(Integer.parseInt(values.getProperty(id+"SpawnWeight",Integer.toString(fallback))),0,100);}catch(NumberFormatException ignored){}
        double encounter=id.equals("fiend")||id.equals("griffin")?.25:1;
        return enabled(id)?base*natural*(RARE.contains(id)?rare:1)*encounter:0;
    }
    public int weight(String id,int fallback){return (int)Math.ceil(effectiveWeight(id,fallback));}
    public double admissionChance(String id,int fallback){int n=weight(id,fallback);return n==0?0:effectiveWeight(id,fallback)/n;}
    public ModConfig.GroupSize group(String id,int min,int max){int a=groupValue(id+"MinGroupSize",min),b=groupValue(id+"MaxGroupSize",max);return id.equals("wraith")?new ModConfig.GroupSize(Math.min(a,2),Math.min(Math.max(a,b),2)):new ModConfig.GroupSize(a,Math.max(a,b));}
    private int groupValue(String key,int fallback){try{return Math.clamp(Integer.parseInt(values.getProperty(key,Integer.toString(fallback))),1,8);}catch(NumberFormatException e){return fallback;}}
}
