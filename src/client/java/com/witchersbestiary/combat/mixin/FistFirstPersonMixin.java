package com.witchersbestiary.combat.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.item.HeldItemRenderer;import net.minecraft.client.network.AbstractClientPlayerEntity;import net.minecraft.client.util.math.MatrixStack;import net.minecraft.client.render.VertexConsumerProvider;import net.minecraft.item.ItemStack;import net.minecraft.util.Hand;
/** Empty-hand poses only, inside vanilla's balanced matrix push/pop; no item model transforms. */
@Mixin(HeldItemRenderer.class)
public final class FistFirstPersonMixin{
 @Inject(method="renderFirstPersonItem",at=@At(value="INVOKE",target="Lnet/minecraft/client/util/math/MatrixStack;push()V",shift=At.Shift.AFTER,ordinal=0))
 private void pose(AbstractClientPlayerEntity player,float delta,float pitch,Hand hand,float swing,ItemStack item,float equip,MatrixStack matrices,VertexConsumerProvider vertices,int light,CallbackInfo ci){if(hand!=Hand.MAIN_HAND||!item.isEmpty())return;var pose=com.witchersbestiary.client.fist.FistClient.pose(player.getId());if(pose==null)return;float sign=player.getMainArm()==net.minecraft.util.Arm.RIGHT?1:-1;float t=pose.ticks()+delta;
  switch(pose.action()){
   case "HEAVY"->{float windup=Math.min(1,t/10);if(t>14)windup=Math.max(0,1-(t-14)/14);float strike=t<14?Math.clamp((t-12)/2,0,1):Math.max(0,1-(t-14)/8);matrices.translate(sign*.08*windup,.2*windup-.2*strike,.12*windup-.5*strike);matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(sign*(15*windup-5*strike)));}
   case "BLOCK"->{matrices.translate(-sign*.22,.25,-.12);matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(sign*20));}
   case "DODGE"->{float amount=(float)Math.sin(Math.PI*Math.clamp(t/10,0,1));matrices.translate(sign*.12*amount,0,0);}
   case "COUNTER"->{float amount=Math.max(0,1-Math.abs(t-5)/5);matrices.translate(0,.05*amount,-.3*amount);}
   case "STAGGER"->matrices.translate(0,-.1,.1);
   default->{}
  }
 }
}
