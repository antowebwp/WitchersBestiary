package com.witchersbestiary.sorcery;
import java.lang.reflect.*;import net.minecraft.entity.LivingEntity;import net.minecraft.entity.damage.DamageSource;import net.fabricmc.loader.api.FabricLoader;
/** Public Spell Power API only, lazily resolved so either optional mod may be absent. */
public final class SpellPowerBridge {
 private static boolean checked,enabled;private static Object frost;private static Method power,base,damage;
 private static void load(){if(checked)return;checked=true;if(!FabricLoader.getInstance().isModLoaded("spell_power"))return;try{var school=Class.forName("net.spell_power.api.SpellSchool");frost=Class.forName("net.spell_power.api.SpellSchools").getField("FROST").get(null);power=Class.forName("net.spell_power.api.SpellPower").getMethod("getSpellPower",school,LivingEntity.class);base=Class.forName("net.spell_power.api.SpellPower$Result").getMethod("baseValue");damage=Class.forName("net.spell_power.api.SpellDamageSource").getMethod("create",school,LivingEntity.class);enabled=true;}catch(ReflectiveOperationException|LinkageError e){com.witchersbestiary.WitchersBestiary.LOGGER.warn("Optional frost API unavailable; using standalone magic",e);}}
 public static boolean available(){load();return enabled;}
 public static float amount(LivingEntity caster){load();if(enabled)try{double value=((Number)base.invoke(power.invoke(null,frost,caster))).doubleValue();return Double.isFinite(value)?(float)Math.clamp(6+value*.5,6,12):6;}catch(ReflectiveOperationException e){enabled=false;}return 6;}
 public static DamageSource source(LivingEntity caster,DamageSource fallback){load();if(enabled)try{return (DamageSource)damage.invoke(null,frost,caster);}catch(ReflectiveOperationException e){enabled=false;}return fallback;}
}
