package com.witchersbestiary.artifact;
import com.witchersbestiary.accessory.HunterAccessories;
import com.witchersbestiary.combat.RootBinding;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
public final class Artifacts {
 private Artifacts(){}
 private static final Identifier SWIM=Identifier.of("witchersbestiary","drowner_swim");
 private static final TagKey<EntityType<?>> RESISTANT=TagKey.of(RegistryKeys.ENTITY_TYPE,Identifier.of("witchersbestiary","artifact_root_resistant"));
 public static boolean melee(LivingEntity victim,DamageSource s){return s.isOf(DamageTypes.PLAYER_ATTACK)&&s.getSource()==s.getAttacker()&&s.getAttacker() instanceof PlayerEntity&&!(victim instanceof PlayerEntity)&&victim.isAlive()&&(victim instanceof HostileEntity||victim instanceof com.witchersbestiary.entity.monster.InfernalBasiliskEntity b&&!b.isTamed())&&!(victim instanceof com.witchersbestiary.entity.monster.FogletEntity f&&f.isIllusion())&&!(victim instanceof com.witchersbestiary.entity.monster.NoonwraithEntity n&&n.isIllusion())&&!(victim instanceof com.witchersbestiary.entity.monster.WerewolfEntity w&&w.isHumanForm())&&!(victim instanceof com.witchersbestiary.entity.monster.KatakanEntity k&&k.isBatForm());}
 public static void afterDamage(LivingEntity v,DamageSource s,float loss){
  if(v.getWorld().isClient||!Float.isFinite(loss)||loss<=0||!melee(v,s))return;
  PlayerEntity p=(PlayerEntity)s.getAttacker();var state=(ArtifactCooldowns)p;long now=p.getWorld().getTime();
  if(HunterAccessories.equipped(p,ModArtifacts.LESHEN_RING)&&now>=state.bestiary$rootsReady()&&p.getRandom().nextFloat()<.12f&&root(v))state.bestiary$rootsReady(now+400);
  if(HunterAccessories.equipped(p,ModArtifacts.WEREWOLF_RING)&&p.getHealth()<p.getMaxHealth()&&now>=state.bestiary$wolfReady()&&p.getRandom().nextFloat()<.10f){p.heal(2);state.bestiary$wolfReady(now+300);}
 }
 public static boolean root(LivingEntity v){if(v.getType().isIn(RESISTANT)||v.getMaxHealth()>=100){return v.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,60,1));}return RootBinding.bind(v,60);}
 public static float protection(LivingEntity v,DamageSource source,float amount){return !v.getWorld().isClient&&source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_FALL)&&HunterAccessories.equipped(v,ModArtifacts.GRIFFIN_CHARM)?amount*.65f:amount;}
 public static int air(LivingEntity v,int before,int after){return after<before&&v.age%2==0&&HunterAccessories.equipped(v,ModArtifacts.DROWNER_CHARM)?before:after;}
 public static void updateSwim(PlayerEntity p){var a=p.getAttributeInstance(EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY);if(a==null)return;boolean active=HunterAccessories.equipped(p,ModArtifacts.DROWNER_CHARM);if(active&&!a.hasModifier(SWIM))a.addTemporaryModifier(new EntityAttributeModifier(SWIM,.2,EntityAttributeModifier.Operation.ADD_VALUE));else if(!active)a.removeModifier(SWIM);}
 public static void initialize(){
  // Twenty-tick event, not a per-tick inventory scan. A fixed modifier ID prevents stacking.
  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTicks()%20!=0)return;for(var p:s.getPlayerManager().getPlayerList())updateSwim(p);});
 }
}
