package com.witchersbestiary.client.workshop;
import com.witchersbestiary.workshop.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.*;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.RotationAxis;
public final class WorkshopRenderer implements BlockEntityRenderer<WorkshopInventory> {
 public WorkshopRenderer(BlockEntityRendererFactory.Context context){}
 @Override public void render(WorkshopInventory be,float delta,MatrixStack matrices,VertexConsumerProvider consumers,int light,int overlay){
  if(be.kind()==WorkshopKind.WITCHER_CHEST){
   matrices.push();matrices.translate(.5,0,.5);matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180-be.getCachedState().get(Properties.HORIZONTAL_FACING).asRotation()));matrices.translate(-.5,0,-.5);
   float progress=be.lidProgress(delta);progress=1-(1-progress)*(1-progress)*(1-progress);
   matrices.translate(0,9.0/16,14.0/16);matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(85*progress));matrices.translate(0,-9.0/16,-14.0/16);
   var client=MinecraftClient.getInstance();var model=client.getBakedModelManager().getModel(ModWorkshop.id("block/witcher_chest_lid"));
   client.getBlockRenderManager().getModelRenderer().render(matrices.peek(),consumers.getBuffer(RenderLayers.getEntityBlockLayer(be.getCachedState(),false)),be.getCachedState(),model,1,1,1,light,overlay);
   matrices.pop();return;
  }
  if(!be.kind().rack())return;
  matrices.push();matrices.translate(.5,.5,.5);matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180-be.getCachedState().get(Properties.HORIZONTAL_FACING).asRotation()));
  for(int slot=0;slot<be.size();slot++)if(!be.getStack(slot).isEmpty()){
   matrices.push();matrices.translate((slot-(be.size()-1)/2.0)*(be.size()==2?.43:.28),.02,be.kind()==WorkshopKind.WALL_SWORD_RACK?.09:0);
   var renderer=MinecraftClient.getInstance().getItemRenderer();boolean volumetric=renderer.getModel(be.getStack(slot),be.getWorld(),null,slot).hasDepth();
   if(!volumetric)matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-45));
   float scale=be.kind()==WorkshopKind.WALL_SWORD_RACK?.55f:volumetric?.45f:.5f;matrices.scale(scale,scale,scale);
   MinecraftClient.getInstance().getItemRenderer().renderItem(be.getStack(slot),ModelTransformationMode.FIXED,light,overlay,matrices,consumers,be.getWorld(),slot);matrices.pop();
  }matrices.pop();
 }
}
