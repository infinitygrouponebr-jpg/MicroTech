package Infinitygroup.microtech.client.model.item;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.item.TechMinerBlockItem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TechMinerItemModel extends GeoModel<TechMinerBlockItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "geo/tech_miner.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "textures/block/tech_miner.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "animations/block/tech_miner.animation.json");

    @Override
    public ResourceLocation getModelResource(TechMinerBlockItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TechMinerBlockItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TechMinerBlockItem animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(TechMinerBlockItem animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
