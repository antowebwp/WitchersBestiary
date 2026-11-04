package com.witchersbestiary.combat.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;import net.minecraft.entity.LivingEntity;import net.minecraft.client.model.ModelPart;import net.minecraft.client.render.entity.model.*;
/** Server-confirmed poses only; normal players never enter this path. */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerFistPoseMixin extends BipedEntityModel<LivingEntity>{
 @org.spongepowered.asm.mixin.Unique private net.minecraft.client.model.ModelTransform[] wb$before;
 @Inject(method="setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",at=@At("HEAD"))private void reset(LivingEntity e,float a,float b,float c,float d,float f,CallbackInfo ci){if(wb$before!=null){var parts=new ModelPart[]{body,head,rightArm,leftArm,rightLeg,leftLeg};for(int i=0;i<parts.length;i++)parts[i].setTransform(wb$before[i]);wb$before=null;}}
 protected PlayerFistPoseMixin(ModelPart root){super(root);}
 @Inject(method="setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
 private void fists(LivingEntity e,float limb,float distance,float age,float yaw,float pitch,CallbackInfo ci){var pose=com.witchersbestiary.client.fist.FistClient.pose(e.getId());if(pose==null)return;wb$before=new net.minecraft.client.model.ModelTransform[]{body.getTransform(),head.getTransform(),rightArm.getTransform(),leftArm.getTransform(),rightLeg.getTransform(),leftLeg.getTransform()};rightArm.pitch=-.8f;leftArm.pitch=-.8f;rightArm.roll=.15f;leftArm.roll=-.15f;
  switch(pose.action()){
   case "QUICK","COUNTER"->{float peak=pose.action().equals("QUICK")?4:5;float phase=Math.max(0,1-Math.abs(pose.ticks()-peak)/peak);rightArm.pitch=-.8f-phase*.9f;rightArm.yaw=-phase*.25f;}
   case "HEAVY"->{rightArm.pitch=pose.ticks()<14?-2.5f:-1.5f;leftArm.pitch=-1.1f;}
   case "BLOCK"->{rightArm.pitch=-1.5f;leftArm.pitch=-1.5f;rightArm.yaw=-.5f;leftArm.yaw=.5f;}
   case "DODGE"->{body.roll=.2f;rightArm.pitch=-1.2f;}
   case "STAGGER"->{body.pitch=.25f;head.pitch=.35f;}
   case "KNOCKOUT"->{float bend=.5f,drop=24*(1-(float)Math.cos(bend));body.pitch=bend;body.pivotY+=drop;head.pivotY+=drop;head.pitch=.85f;rightArm.pitch=.65f;leftArm.pitch=.65f;rightArm.pivotY=leftArm.pivotY=drop+2*(float)Math.cos(bend);rightArm.pivotZ=leftArm.pivotZ=2*(float)Math.sin(bend);rightLeg.pitch=leftLeg.pitch=-bend;rightLeg.pivotY=leftLeg.pivotY=drop+12*(float)Math.cos(bend);rightLeg.pivotZ=leftLeg.pivotZ=12*(float)Math.sin(bend);}
   case "VICTORY"->{rightArm.pitch=-2.8f;leftArm.pitch=-2.8f;rightArm.roll=.3f;leftArm.roll=-.3f;}
   default->{}
  }
  var model=(PlayerEntityModel<?>)(Object)this;model.rightSleeve.copyTransform(rightArm);model.leftSleeve.copyTransform(leftArm);model.jacket.copyTransform(body);model.hat.copyTransform(head);model.rightPants.copyTransform(rightLeg);model.leftPants.copyTransform(leftLeg);
 }
}
