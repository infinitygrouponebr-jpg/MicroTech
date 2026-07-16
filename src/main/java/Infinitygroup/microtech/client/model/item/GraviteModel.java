package Infinitygroup.microtech.client.model.item;

import Infinitygroup.microtech.item.GraviteItem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GraviteModel extends GeoModel<GraviteItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("microtech", "geo/item/gravite.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("microtech", "textures/item/gravite.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("microtech", "animations/item/gravite.animation.json");

    @Override
    public ResourceLocation getModelResource(GraviteItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GraviteItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(GraviteItem animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(GraviteItem animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
