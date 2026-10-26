package com.witchersbestiary.client;
import com.witchersbestiary.entity.monster.InfernalBasiliskEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
public final class InfernalBasiliskRenderer extends GeoEntityRenderer<InfernalBasiliskEntity> {
    public InfernalBasiliskRenderer(EntityRendererFactory.Context context){super(context,new InfernalBasiliskModel());shadowRadius=1.3f;addRenderLayer(new AutoGlowingGeoLayer<>(this));}
}
