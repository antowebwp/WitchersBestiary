package com.witchersbestiary.client.signs;

import java.nio.file.*;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/** Local, normalized HUD placement, independent of window resolution. */
public final class SignHudSettings {
 public static int corner=0;
 public static double scale=1, x=.8, y=.3;
 private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("witchersbestiary-sign-hud.properties");
 static {load();}
 public static void load(){try(var in=Files.newInputStream(FILE)){var p=new Properties();p.load(in);corner=Math.clamp(Integer.parseInt(p.getProperty("corner","0")),0,4);scale=finite(p.getProperty("scale","1"),1,.5,1.5);x=finite(p.getProperty("x",".8"),.8,0,1);y=finite(p.getProperty("y",".3"),.15,0,1);}catch(Exception ignored){}}
 private static double finite(String s,double fallback,double min,double max){double n=Double.parseDouble(s);return Double.isFinite(n)?Math.clamp(n,min,max):fallback;}
 public static int[] bounds(int w,int h){double s=Math.min(scale,Math.min(w/180.0,Math.max(1,h-48)/48.0));int bw=(int)Math.ceil(180*s),bh=(int)Math.ceil(48*s);int maxX=Math.max(0,w-bw),maxY=Math.max(0,h-48-bh);int px=corner==4?(int)(x*maxX):(corner<2?maxX-8:8);int py=corner==4?(int)(y*maxY):(corner%2==0?8:maxY);return new int[]{Math.clamp(px,0,maxX),Math.clamp(py,0,maxY),bw,bh,(int)(s*10000)};}
 public static void save(){var p=new Properties();p.setProperty("corner",""+corner);p.setProperty("scale",""+scale);p.setProperty("x",""+x);p.setProperty("y",""+y);try{Files.createDirectories(FILE.getParent());try(var out=Files.newOutputStream(FILE)){p.store(out,"Witcher's Bestiary signs HUD");}}catch(java.io.IOException e){org.slf4j.LoggerFactory.getLogger("witchersbestiary").warn("Cannot save signs HUD",e);}}
}
