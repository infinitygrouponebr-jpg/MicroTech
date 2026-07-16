package Infinitygroup.microtech.client.model.item;

import Infinitygroup.microtech.item.TechSwordItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

import org.jetbrains.annotations.Nullable;

public class TechSwordModel extends GeoModel<TechSwordItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("microtech", "geo/item/tech_sword.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("microtech", "textures/item/tech_sword.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("microtech", "animations/item/tech_sword.animation.json");

    @Override
    public ResourceLocation getModelResource(TechSwordItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TechSwordItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TechSwordItem animatable) {
        return ANIMATION;
    }

    @Override
    public net.minecraft.client.renderer.RenderType getRenderType(TechSwordItem animatable, ResourceLocation texture) {
        return net.minecraft.client.renderer.RenderType.entityCutoutNoCull(texture);
    }
}
