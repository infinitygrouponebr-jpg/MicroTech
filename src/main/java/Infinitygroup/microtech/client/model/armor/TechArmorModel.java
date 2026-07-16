package Infinitygroup.microtech.client.model.armor;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.item.TechArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TechArmorModel extends GeoModel<TechArmorItem> {
    @Override
    public ResourceLocation getModelResource(TechArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "geo/armor/tech_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TechArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "textures/armor/tech_armor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TechArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "animations/armor/tech_armor.animation.json");
    }
}
