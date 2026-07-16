package Infinitygroup.microtech.client.model.item;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.item.TechTableBlockItem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TechTableItemModel extends GeoModel<TechTableBlockItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "geo/item/tech_table_item.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "textures/block/tech_table.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "animations/item/tech_table.animation.json");

    @Override
    public ResourceLocation getModelResource(TechTableBlockItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TechTableBlockItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TechTableBlockItem animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(TechTableBlockItem animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
