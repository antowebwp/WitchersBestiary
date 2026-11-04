package com.witchersbestiary.enchantment;
import com.witchersbestiary.combat.CategorizedMonster;import com.witchersbestiary.config.ModConfig;
import net.minecraft.component.ComponentType;import net.minecraft.enchantment.*;import net.minecraft.entity.*;import net.minecraft.entity.damage.*;import net.minecraft.entity.mob.*;import net.minecraft.entity.passive.*;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.item.*;import net.minecraft.particle.*;import net.minecraft.registry.*;import net.minecraft.registry.tag.TagKey;import net.minecraft.server.network.ServerPlayerEntity;import net.minecraft.server.world.ServerWorld;import net.minecraft.sound.*;import net.minecraft.util.*;import net.minecraft.util.math.*;import java.util.*;
/** Vanilla registry definitions, with custom effects committed exactly once after real server damage. */
public final class SwordEnchantments {
 public static final RegistryKey<Enchantment> BANE=key("monsters_bane"),FROST=key("frost_blade"),BLOOD=key("blood_harvest"),RUNIC=key("runic_resonance");
 public static final RegistryKey<DamageType> PULSE=RegistryKey.of(RegistryKeys.DAMAGE_TYPE,Identifier.of("witchersbestiary:runic_pulse"));
 public static final TagKey<Item> SWORDS=TagKey.of(RegistryKeys.ITEM,Identifier.of("witchersbestiary:enchantable/hunting_swords"));
 public static final ComponentType<RunicState> RUNIC_STATE=Registry.register(Registries.DATA_COMPONENT_TYPE,Identifier.of("witchersbestiary:runic_state"),ComponentType.<RunicState>builder().codec(RunicState.CODEC).packetCodec(RunicState.PACKET).build());
 private static RegistryKey<Enchantment> key(String id){return RegistryKey.of(RegistryKeys.ENCHANTMENT,Identifier.of("witchersbestiary",id));}
 public static int level(ItemStack stack,RegistryKey<Enchantment> key,LivingEntity owner){return owner.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(key).map(e->Math.clamp(EnchantmentHelper.getLevel(e,stack),0,3)).orElse(0);}
 public static boolean sword(ItemStack stack){return !stack.isEmpty()&&stack.isIn(SWORDS)&&(stack.getItem() instanceof SwordItem||com.witchersbestiary.alchemy.WeaponOilItem.supportsMeleeDamage(stack));}
 public static final class Attack {
  final PlayerEntity player;DamageSource source;ItemStack weapon;final Set<UUID> processed=new HashSet<>();
  Attack(PlayerEntity p){player=p;}
 }
 private static final ThreadLocal<Attack> ATTACK=new ThreadLocal<>();private static final ThreadLocal<Boolean> SECONDARY=ThreadLocal.withInitial(()->false);
 public static Attack begin(PlayerEntity p){var previous=ATTACK.get();ATTACK.set(new Attack(p));return previous;}
 public static void end(Attack previous){if(previous==null)ATTACK.remove();else ATTACK.set(previous);}
 public record Hit(Attack attack,PlayerEntity player,ItemStack weapon,int bane,int frost,int blood,int runic,boolean baneTarget){}
 public static Hit capture(LivingEntity victim,DamageSource source){
  var attack=ATTACK.get();if(attack==null||SECONDARY.get()||victim.getWorld().isClient||!(source.getAttacker() instanceof PlayerEntity p)||attack.player!=p||source.getSource()!=p||!source.isOf(DamageTypes.PLAYER_ATTACK)||friendly(p,victim))return null;
  // A unique sword's secondary ability may use another player-attack source inside postHit.
  // Only the real vanilla attack source (also reused by its sweep) belongs to this attack scope.
  if(attack.source==null){attack.source=source;attack.weapon=source.getWeaponStack();}
  if(attack.source!=source||attack.weapon==null||!sword(attack.weapon))return null;
  var stack=attack.weapon;return new Hit(attack,p,stack,level(stack,BANE,p),level(stack,FROST,p),level(stack,BLOOD,p),level(stack,RUNIC,p),victim instanceof CategorizedMonster&&Registries.ENTITY_TYPE.getId(victim.getType()).getNamespace().equals("witchersbestiary"));
 }
 private static boolean friendly(PlayerEntity p,LivingEntity v){return v==p||p.isTeammate(v)||v instanceof TameableEntity tame&&tame.isTamed()||v instanceof VillagerEntity||v instanceof IronGolemEntity||v instanceof net.minecraft.entity.decoration.ArmorStandEntity||v instanceof com.witchersbestiary.merchant.ArcaneMerchant||v instanceof com.witchersbestiary.fist.FistNpc&&!(v instanceof com.witchersbestiary.fist.RockTrollEntity)||com.witchersbestiary.fist.FistFights.busy(p)||com.witchersbestiary.fist.FistFights.busy(v)||v instanceof com.witchersbestiary.entity.monster.FogletEntity f&&f.isIllusion()||v instanceof com.witchersbestiary.entity.monster.NoonwraithEntity n&&n.isIllusion()||v instanceof com.witchersbestiary.entity.monster.KatakanEntity k&&k.isBatForm()||v instanceof com.witchersbestiary.entity.monster.WerewolfEntity w&&w.isHumanForm();}
 private static boolean enemy(PlayerEntity p,LivingEntity v,boolean pulse){if(friendly(p,v))return false;if(v instanceof PlayerEntity)return !pulse&&p.getServer()!=null&&p.getServer().isPvpEnabled();boolean provoked=v.getAttacker()==p||v instanceof MobEntity m&&m.getTarget()==p;if(v instanceof Angerable angry&&!angry.shouldAngerAt(p)&&!provoked)return false;if(v instanceof PiglinEntity&&!provoked)return false;return v instanceof Monster||v instanceof CategorizedMonster||v instanceof com.witchersbestiary.fist.RockTrollEntity&&provoked||v instanceof Angerable&&provoked;}
 public static float damage(Hit hit,float amount){if(hit==null||hit.bane==0||!hit.baneTarget||!Float.isFinite(amount)||amount<=0)return amount;return (float)(amount*(1+ModConfig.enchantments().bane(hit.bane)));}
 public static void after(LivingEntity victim,Hit hit,float actual){
  if(hit==null||!Float.isFinite(actual)||actual<=0||!hit.attack.processed.add(victim.getUuid())||!(victim.getWorld() instanceof ServerWorld world))return;
  var p=hit.player;var config=ModConfig.enchantments();long now=p.getServer().getOverworld().getTime();var target=(SwordEnchantmentState)victim;var owner=(SwordEnchantmentState)p;
  if(hit.bane>0&&hit.baneTarget)world.spawnParticles(ParticleTypes.ENCHANTED_HIT,victim.getX(),victim.getBodyY(.55),victim.getZ(),4,.18,.2,.18,.015);
  if(hit.frost>0&&victim.isAlive()&&now>=target.bestiary$frostReady()){
   target.bestiary$frostReady(now+config.frostCooldown);victim.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS,config.frost(hit.frost),hit.frost==3?1:0),p);
   world.spawnParticles(ParticleTypes.SNOWFLAKE,victim.getX(),victim.getBodyY(.6),victim.getZ(),5,.2,.3,.2,.015);world.playSound(null,victim.getBlockPos(),SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,SoundCategory.PLAYERS,.25f,1.8f);
  }
  if(!enemy(p,victim,false))return;
  if(hit.blood>0&&now>=owner.bestiary$harvestReady()&&p.getHealth()<p.getMaxHealth()){
   float heal=Math.min(actual,Math.min(config.heal(hit.blood),p.getMaxHealth()-p.getHealth()));if(heal>0){owner.bestiary$harvestReady(now+config.bloodCooldown);p.heal(heal);world.spawnParticles(new DustParticleEffect(new org.joml.Vector3f(.85f,.08f,.14f),.7f),p.getX(),p.getBodyY(.55),p.getZ(),4,.15,.2,.15,0);}
  }
  if(hit.runic<=0)return;var state=hit.weapon.getOrDefault(RUNIC_STATE,RunicState.EMPTY);if(state.lastHit()==now)return;
  int threshold=config.hits(hit.runic),charges=Math.min(state.charges(),threshold-1);long ready=Math.max(state.readyAt(),owner.bestiary$pulseReady());
  if(now<ready)return;
  if(++charges<threshold){hit.weapon.set(RUNIC_STATE,new RunicState(charges,ready,now));p.getInventory().markDirty();world.spawnParticles(ParticleTypes.ENCHANT,p.getX(),p.getBodyY(.6),p.getZ(),1,.1,.1,.1,0);return;}
  hit.weapon.set(RUNIC_STATE,new RunicState(0,now+config.runicCooldown,now));p.getInventory().markDirty();owner.bestiary$pulseReady(now+config.runicCooldown);pulse(world,p,victim,hit.runic);
 }
 private static void pulse(ServerWorld world,PlayerEntity p,LivingEntity primary,int level){var config=ModConfig.enchantments();double radius=config.radius(level);var center=primary.getPos();
  for(int n=0;n<12;n++){double angle=n*Math.PI/6;world.spawnParticles(ParticleTypes.ENCHANT,center.x+Math.cos(angle)*radius,center.y+.2,center.z+Math.sin(angle)*radius,1,0,.05,0,0);}
  world.playSound(null,primary.getBlockPos(),SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,SoundCategory.PLAYERS,.45f,1.25f);
  var victims=world.getEntitiesByClass(LivingEntity.class,primary.getBoundingBox().expand(radius),e->e!=primary&&e.isAlive()&&e.squaredDistanceTo(center)<=radius*radius&&enemy(p,e,true)&&primary.canSee(e));
  var source=new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(PULSE),p);boolean previous=SECONDARY.get();SECONDARY.set(true);try{for(int i=0;i<Math.min(config.maxTargets,victims.size());i++)victims.get(i).damage(source,config.pulseDamage);}finally{if(previous)SECONDARY.set(true);else SECONDARY.remove();}
 }
 public static void initialize(){}
}
