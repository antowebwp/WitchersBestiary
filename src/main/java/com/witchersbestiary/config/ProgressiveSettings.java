package com.witchersbestiary.config;

import java.util.*;
/** Live server settings from the existing witchersbestiary.properties. */
public record ProgressiveSettings(boolean enabled, int interval, double healthIncrement,
        double damageIncrement, double bypassIncrement, double normalCap, double eliteCap,
        double damageCap, double bypassCap, Set<String> excluded, Set<String> elites) {
    public static final String DEFAULTS = """

        # Progressive wild WB monsters; live settings. Overworld calendar days.
        progressiveEnabled=true
        progressiveDayInterval=25
        progressiveHealthIncrement=0.30
        progressiveDamageIncrement=0.15
        progressiveArmorBypassIncrement=0.05
        progressiveNormalHealthCap=700
        progressiveEliteHealthCap=3500
        progressiveDamageCap=4.0
        progressiveArmorBypassCap=0.85
        # Comma-separated registry IDs; unqualified IDs use witchersbestiary.
        progressiveExcluded=
        progressiveElites=leshen,fiend,griffin,infernal_basilisk,bonecrusher,noonwraith,werewolf,katakan
        """;
    public static ProgressiveSettings read(Properties p) {
        return new ProgressiveSettings(Boolean.parseBoolean(p.getProperty("progressiveEnabled","true")),
            (int)number(p,"progressiveDayInterval",25,1,1000000),
            number(p,"progressiveHealthIncrement",.30,0,100),number(p,"progressiveDamageIncrement",.15,0,100),
            number(p,"progressiveArmorBypassIncrement",.05,0,1),number(p,"progressiveNormalHealthCap",700,1,1000000),
            number(p,"progressiveEliteHealthCap",3500,1,1000000),number(p,"progressiveDamageCap",4,1,100),
            number(p,"progressiveArmorBypassCap",.85,0,1),ids(p.getProperty("progressiveExcluded","")),
            ids(p.getProperty("progressiveElites","leshen,fiend,griffin,infernal_basilisk,bonecrusher,noonwraith,werewolf,katakan")));
    }
    private static double number(Properties p,String key,double fallback,double min,double max) {
        try { double n=Double.parseDouble(p.getProperty(key,""+fallback));return Double.isFinite(n)?Math.clamp(n,min,max):fallback; }
        catch(NumberFormatException e){return fallback;}
    }
    private static Set<String> ids(String text) {
        var result=new HashSet<String>();
        for(String id:text.split(",")){id=id.trim();if(!id.isEmpty())result.add(id.contains(":")?id:"witchersbestiary:"+id);}
        return Set.copyOf(result);
    }
    public long steps(long day){return enabled?Math.max(0,day)/interval:0;}
    public double health(double base,long steps,boolean elite){return Math.max(base,Math.min(base*(1+steps*healthIncrement),elite?eliteCap:normalCap));}
    public double damage(long steps){return Math.min(1+steps*damageIncrement,damageCap);}
    public double bypass(long steps){return Math.min(steps*bypassIncrement,bypassCap);}
}
