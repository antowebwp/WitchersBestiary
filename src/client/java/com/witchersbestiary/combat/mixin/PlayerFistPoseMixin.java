package com.witchersbestiary.combat.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;import net.minecraft.entity.LivingEntity;import net.minecraft.client.model.ModelPart;import net.minecraft.client.render.entity.model.*;
/** Server-confirmed poses only; normal players never enter this path. */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerFistPoseMixin extends BipedEntityModel<LivingEntity>{
 @org.spongepowered.asm.mixin.Unique private boolean wb$posed;@org.spongepowered.asm.mixin.Unique private float wb$roll;
 @Inject(method="setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",at=@At("HEAD"))private void reset(LivingEntity e,float a,float b,float c,float d,float f,CallbackInfo ci){if(wb$posed){body.roll=wb$roll;wb$posed=false;}}
 protected PlayerFistPoseMixin(ModelPart root){super(root);}
 @Inject(method="setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
 private void fists(LivingEntity e,float limb,float distance,float age,float yaw,float pitch,CallbackInfo ci){var pose=com.witchersbestiary.client.fist.FistClient.pose(e.getId());if(pose==null)return;wb$roll=body.roll;wb$posed=true;rightArm.pitch=-.8f;leftArm.pitch=-.8f;rightArm.roll=.15f;leftArm.roll=-.15f;
  switch(pose.action()){
   case "QUICK","COUNTER"->{float peak=pose.action().equals("QUICK")?4:5;float phase=Math.max(0,1-Math.abs(pose.ticks()-peak)/peak);rightArm.pitch=-.8f-phase*.9f;rightArm.yaw=-phase*.25f;}
   case "HEAVY"->{rightArm.pitch=pose.ticks()<14?-2.5f:-1.5f;leftArm.pitch=-1.1f;}
   case "BLOCK"->{rightArm.pitch=-1.5f;leftArm.pitch=-1.5f;rightArm.yaw=-.5f;leftArm.yaw=.5f;}
   case "DODGE"->{body.roll=.2f;rightArm.pitch=-1.2f;}
   case "STAGGER"->{body.pitch=.25f;head.pitch=.35f;}
   default->{}
  }
  var model=(PlayerEntityModel<?>)(Object)this;model.rightSleeve.copyTransform(rightArm);model.leftSleeve.copyTransform(leftArm);model.jacket.copyTransform(body);model.hat.copyTransform(head);
 }
}
