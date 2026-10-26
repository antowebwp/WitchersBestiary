package com.witchersbestiary.equipment;

import com.witchersbestiary.registry.ModDataComponents;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.particle.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.util.List;

/** One stable sword ID. Better Combat owns normal hits; this is a separate ground ability. */
public final class OgroidHammerItem extends SwordItem {
 public static final RegistryKey<DamageType> SLAM=RegistryKey.of(RegistryKeys.DAMAGE_TYPE,Identifier.of("witchersbestiary","ogroid_slam"));
 public static final int WINDUP=10, COOLDOWN=240;
 public OgroidHammerItem(){super(ToolMaterials.IRON,new Settings().maxDamage(800).rarity(Rarity.RARE).attributeModifiers(
  AttributeModifiersComponent.builder()
   .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID,11,EntityAttributeModifier.Operation.ADD_VALUE),AttributeModifierSlot.MAINHAND)
   .add(EntityAttributes.GENERIC_ATTACK_SPEED,new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID,-3.5,EntityAttributeModifier.Operation.ADD_VALUE),AttributeModifierSlot.MAINHAND)
   .add(EntityAttributes.GENERIC_MOVEMENT_SPEED,new EntityAttributeModifier(Identifier.of("witchersbestiary","hammer_weight"),-.08,EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),AttributeModifierSlot.MAINHAND).build()));}
 // Better Combat masks getOffHandStack for two-handed weapons; enforce the physical slot.
 private static boolean offhandOccupied(PlayerEntity player){return !player.getInventory().offHand.get(0).isEmpty();}
 @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand){
  var stack=player.getStackInHand(hand);
  if(hand!=Hand.MAIN_HAND||offhandOccupied(player)||!player.isOnGround()||player.hasVehicle()||player.isTouchingWater()||player.isInLava()||player.getItemCooldownManager().isCoolingDown(this))return TypedActionResult.fail(stack);
  long remaining=stack.getOrDefault(ModDataComponents.HAMMER_READY_AT,0L)-world.getTime();
  if(remaining>0&&remaining<=COOLDOWN){if(!world.isClient)player.getItemCooldownManager().set(this,(int)remaining);return TypedActionResult.fail(stack);}
  player.setCurrentHand(hand);return TypedActionResult.consume(stack);
 }
 @Override public int getMaxUseTime(ItemStack stack,LivingEntity user){return WINDUP+2;}
 @Override public UseAction getUseAction(ItemStack stack){return UseAction.NONE;}
 @Override public void usageTick(World world,LivingEntity user,ItemStack stack,int remainingUseTicks){
  if(!(world instanceof ServerWorld server)||!(user instanceof PlayerEntity player))return;
  if(!player.isOnGround()||player.hasVehicle()||player.isTouchingWater()||player.isInLava()||offhandOccupied(player)){player.stopUsingItem();return;}
  int elapsed=getMaxUseTime(stack,user)-remainingUseTicks;
  if(elapsed>0&&elapsed<WINDUP&&elapsed%3==0){server.spawnParticles(ParticleTypes.CRIT,player.getX(),player.getY()+.12,player.getZ(),5,.65,.08,.65,.02);server.playSound(null,player.getBlockPos(),SoundEvents.BLOCK_STONE_HIT,SoundCategory.PLAYERS,.3f,.7f);}
  if(elapsed==WINDUP){slam(server,player,stack);player.stopUsingItem();}
 }
 public boolean slam(ServerWorld world,PlayerEntity player,ItemStack stack){
  if(!stack.isOf(this)||player.getMainHandStack()!=stack||!player.isOnGround()||player.hasVehicle()||player.isTouchingWater()||player.isInLava()||offhandOccupied(player)||player.getItemCooldownManager().isCoolingDown(this))return false;
  long now=world.getTime(),ready=stack.getOrDefault(ModDataComponents.HAMMER_READY_AT,0L);if(ready>now&&ready-now<=COOLDOWN)return false;
  player.getItemCooldownManager().set(this,COOLDOWN);stack.set(ModDataComponents.HAMMER_READY_AT,now+COOLDOWN);
  var source=new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(SLAM),player);
  for(var target:world.getEntitiesByClass(LivingEntity.class,player.getBoundingBox().expand(3,1.2,3),e->e.isAlive()&&(e instanceof HostileEntity||e instanceof com.witchersbestiary.entity.monster.InfernalBasiliskEntity basilisk&&!basilisk.isTamed())&&!(e instanceof PlayerEntity)&&!(e instanceof com.witchersbestiary.entity.monster.WerewolfEntity wolf&&wolf.isHumanForm()))){
   if(target instanceof TameableEntity pet&&pet.isTamed()||player.squaredDistanceTo(target)>9||Math.abs(target.getY()-player.getY())>1.5)continue;
   Vec3d from=player.getPos().add(0,.3,0),to=target.getPos().add(0,.45,0);
   if(world.raycast(new RaycastContext(from,to,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,player)).getType()!=net.minecraft.util.hit.HitResult.Type.MISS)continue;
   if(target.damage(source,9))target.takeKnockback(.8,player.getX()-target.getX(),player.getZ()-target.getZ());
  }
  for(int i=0;i<40;i++){double a=i*Math.PI*2/40;world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,world.getBlockState(player.getBlockPos().down())),player.getX()+Math.cos(a)*2.8,player.getY()+.1,player.getZ()+Math.sin(a)*2.8,2,.12,.1,.12,.15);}
  world.playSound(null,player.getBlockPos(),SoundEvents.BLOCK_ANVIL_LAND,SoundCategory.PLAYERS,.8f,.6f);player.swingHand(Hand.MAIN_HAND,true);stack.damage(3,player,EquipmentSlot.MAINHAND);return true;
 }
 @Override public void appendTooltip(ItemStack stack,TooltipContext context,List<Text> lines,TooltipType type){
  super.appendTooltip(stack,context,lines,type);lines.add(Text.translatable("tooltip.witchersbestiary.ogroid_slam").formatted(Formatting.GOLD));lines.add(Text.translatable("tooltip.witchersbestiary.ogroid_slam_limits").formatted(Formatting.DARK_GRAY));
 }
}
