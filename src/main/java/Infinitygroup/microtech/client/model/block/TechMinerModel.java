package Infinitygroup.microtech.client.model.block;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.block.entity.TechMinerBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TechMinerModel extends GeoModel<TechMinerBlockEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "geo/tech_miner.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "textures/block/tech_miner.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "animations/block/tech_miner.animation.json");

    @Override
    public ResourceLocation getModelResource(TechMinerBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TechMinerBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TechMinerBlockEntity animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(TechMinerBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
