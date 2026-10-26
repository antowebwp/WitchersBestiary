package com.witchersbestiary.equipment.armory;
import com.witchersbestiary.combat.*;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import java.util.*;
/** Target rules for the vanilla item bonus hook; no second damage call. */
public final class ArmoryCombat {
 private ArmoryCombat(){}
 private record Primary(LivingEntity target,float amount){}
 private static final Map<DamageSource,Primary> PRIMARY=new WeakHashMap<>();
 public static void prepare(LivingEntity target,DamageSource source,float amount){if(!target.getWorld().isClient&&amount>0)PRIMARY.put(source,new Primary(target,amount));}
 /** Consume only on the originally selected victim. Sweep reuses this source, but not its bonus. */
 public static float apply(LivingEntity target,DamageSource source,float amount){if(target.getWorld().isClient)return amount;var hit=PRIMARY.get(source);if(hit==null||hit.target!=target)return amount;PRIMARY.remove(source);return Float.isFinite(amount)&&amount>0?amount+hit.amount:amount;}
 private static final Map<PlayerEntity,Long> CHILL_ATTEMPTS=new WeakHashMap<>();
 public static boolean monster(LivingEntity target){return target instanceof CategorizedMonster&&Registries.ENTITY_TYPE.getId(target.getType()).getNamespace().equals("witchersbestiary");}
 public static boolean graveTarget(LivingEntity target){return monster(target)&&target instanceof CategorizedMonster category&&(category.getMonsterCategory()==MonsterCategory.SPECTER||category.getMonsterCategory()==MonsterCategory.NECROPHAGE);}
 public static float bonus(ItemStack stack,LivingEntity target){if(!(stack.getItem() instanceof WitcherSwordItem sword))return 0;return sword.kind==WitcherSwordItem.Kind.SILVER&&monster(target)||sword.kind==WitcherSwordItem.Kind.GRAVE&&graveTarget(target)?4:0;}
 /** Sword postHit is called only for a successful primary hit; vanilla sweep does not call it. */
 public static void afterHit(WitcherSwordItem sword,LivingEntity target,LivingEntity attacker){
  if(sword.kind!=WitcherSwordItem.Kind.GRAVE||!(target.getWorld() instanceof ServerWorld world)||!(attacker instanceof PlayerEntity p)||!graveTarget(target)||!target.isAlive()||target.hurtTime<=0||target.getAttacker()!=p)return;
  long tick=world.getTime();if(Objects.equals(CHILL_ATTEMPTS.get(p),tick))return;CHILL_ATTEMPTS.put(p,tick);
  if(p.getRandom().nextFloat()>=.2f)return;
  target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,40,0),p);
  world.spawnParticles(ParticleTypes.SOUL,target.getX(),target.getBodyY(.6),target.getZ(),8,.25,.35,.25,.005);
 }
}
