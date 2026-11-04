package com.witchersbestiary.client.fist;
import com.witchersbestiary.fist.FistNpc;import net.minecraft.util.Identifier;import software.bernie.geckolib.model.GeoModel;
public final class FistRenderer extends software.bernie.geckolib.renderer.GeoEntityRenderer<FistNpc>{
 public FistRenderer(net.minecraft.client.render.entity.EntityRendererFactory.Context c){this(c,false);}
 public FistRenderer(net.minecraft.client.render.entity.EntityRendererFactory.Context c,boolean troll){super(c,new GeoModel<FistNpc>(){
  public Identifier getModelResource(FistNpc n){return Identifier.of("witchersbestiary",n instanceof com.witchersbestiary.fist.RockTrollEntity?"geo/rock_troll.geo.json":"geo/fighter_"+n.tier+".geo.json");}
  public Identifier getTextureResource(FistNpc n){return Identifier.of("witchersbestiary",n instanceof com.witchersbestiary.fist.RockTrollEntity?"textures/entity/rock_troll.png":"textures/entity/fighter_"+n.tier+".png");}
  public Identifier getAnimationResource(FistNpc n){return Identifier.of("witchersbestiary",n instanceof com.witchersbestiary.fist.RockTrollEntity?"animations/rock_troll.animation.json":"animations/fighter.animation.json");}
 });shadowRadius=troll?.85f:.4f;}
 @Override protected float getDeathMaxRotation(FistNpc npc){return npc instanceof com.witchersbestiary.fist.RockTrollEntity?0:super.getDeathMaxRotation(npc);}
}
