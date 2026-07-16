package Infinitygroup.microtech.client.model.item;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.item.TechCrusherBlockItem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TechCrusherItemModel extends GeoModel<TechCrusherBlockItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "geo/item/tech_crusher_item.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "textures/item/tech_crusher.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "animations/block/tech_crusher.animation.json");

    @Override
    public ResourceLocation getModelResource(TechCrusherBlockItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TechCrusherBlockItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TechCrusherBlockItem animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(TechCrusherBlockItem animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
