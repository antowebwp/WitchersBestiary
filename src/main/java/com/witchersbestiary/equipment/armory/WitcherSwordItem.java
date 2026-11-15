package com.witchersbestiary.equipment.armory;
import net.minecraft.item.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.component.type.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.*;
import net.minecraft.block.Block;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.item.tooltip.TooltipType;
import java.util.List;
public final class WitcherSwordItem extends SwordItem {
 public enum Kind {STEEL,SILVER,GRAVE}
 public final Kind kind;
 public WitcherSwordItem(Kind kind,int damage,double speed,int durability){super(material(kind,durability),new Settings().rarity(kind==Kind.GRAVE?Rarity.RARE:kind==Kind.SILVER?Rarity.UNCOMMON:Rarity.COMMON).attributeModifiers(AttributeModifiersComponent.builder()
  .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID,damage-1,EntityAttributeModifier.Operation.ADD_VALUE),AttributeModifierSlot.MAINHAND)
  .add(EntityAttributes.GENERIC_ATTACK_SPEED,new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID,speed-4,EntityAttributeModifier.Operation.ADD_VALUE),AttributeModifierSlot.MAINHAND).build()));this.kind=kind;}
 private static ToolMaterial material(Kind kind,int durability){return new ToolMaterial(){
  public int getDurability(){return durability;}
  public float getMiningSpeedMultiplier(){return 6;}
  public float getAttackDamage(){return 0;}
  public TagKey<Block> getInverseTag(){return BlockTags.INCORRECT_FOR_IRON_TOOL;}
  public int getEnchantability(){return 14;}
  public Ingredient getRepairIngredient(){return Ingredient.ofItems(kind==Kind.GRAVE?com.witchersbestiary.registry.ModItems.GRAVEBOUND_STEEL_FRAGMENT:kind==Kind.SILVER?Items.GOLD_INGOT:Items.IRON_INGOT);}
 };}
 /** Capture before sweep calculation; the shared damage wrapper applies one flat primary-only bonus. */
 @Override public float getBonusAttackDamage(Entity target,float baseDamage,net.minecraft.entity.damage.DamageSource source){
  if(!(target instanceof LivingEntity living)||!(source.getAttacker() instanceof net.minecraft.entity.player.PlayerEntity player)||source.getSource()!=player||!source.isOf(net.minecraft.entity.damage.DamageTypes.PLAYER_ATTACK))return 0;
  double full=player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
  float bonus=ArmoryCombat.bonus(new ItemStack(this),living)*(float)Math.clamp(full>0?baseDamage/full:0,0,1);
  double fraction = this == com.witchersbestiary.registry.ModItems.BLOOD_LEGACY ? .0065 : switch (kind) {
   case STEEL -> .0035;
   case SILVER -> .005;
   case GRAVE -> 0;
  };
  bonus += com.witchersbestiary.combat.MonsterHealthScaling.bonus(living,fraction,8);
  ArmoryCombat.prepare(living,source,bonus);return 0;
 }
 @Override public boolean postHit(ItemStack stack,LivingEntity target,LivingEntity attacker){boolean result=super.postHit(stack,target,attacker);ArmoryCombat.afterHit(this,target,attacker);
  if(this==com.witchersbestiary.registry.ModItems.BLOOD_LEGACY&&!target.getWorld().isClient
      &&target.isAlive()&&target.hurtTime>0&&target.getAttacker()==attacker)
   com.witchersbestiary.alchemy.BleedingEffect.inflict(target,1);
  return result;}
 @Override public void appendTooltip(ItemStack stack,TooltipContext context,List<Text> tooltip,TooltipType type){super.appendTooltip(stack,context,tooltip,type);if(this==com.witchersbestiary.registry.ModItems.BLOOD_LEGACY)
  tooltip.add(Text.translatable("tooltip.witchersbestiary.blood_legacy").formatted(Formatting.RED));if(kind!=Kind.STEEL)tooltip.add(Text.translatable("tooltip.witchersbestiary."+(kind==Kind.SILVER?"silver_witcher_sword":"grave_blade")).formatted(Formatting.AQUA));}
}
