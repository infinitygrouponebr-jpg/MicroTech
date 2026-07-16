package Infinitygroup.microtech.client.renderer.armor;

import Infinitygroup.microtech.client.model.armor.TechArmorModel;
import Infinitygroup.microtech.item.TechArmorItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class TechArmorRenderer extends GeoArmorRenderer<TechArmorItem> {
    public TechArmorRenderer() {
        super(new TechArmorModel());
    }

    @Override
    public GeoBone getHeadBone(GeoModel<TechArmorItem> model) {
        return model.getBone("armorHead").orElse(null);
    }

    @Override
    public GeoBone getBodyBone(GeoModel<TechArmorItem> model) {
        return model.getBone("armorBody").orElse(null);
    }

    @Override
    public GeoBone getRightArmBone(GeoModel<TechArmorItem> model) {
        return model.getBone("bipedRightArm").orElse(null);
    }

    @Override
    public GeoBone getLeftArmBone(GeoModel<TechArmorItem> model) {
        return model.getBone("armorLeftArm").orElse(null);
    }

    @Override
    public GeoBone getRightLegBone(GeoModel<TechArmorItem> model) {
        return model.getBone("armorRightLeg").orElse(null);
    }

    @Override
    public GeoBone getLeftLegBone(GeoModel<TechArmorItem> model) {
        return model.getBone("armorLeftLeg").orElse(null);
    }

    @Override
    public GeoBone getRightBootBone(GeoModel<TechArmorItem> model) {
        return model.getBone("armorRightBoot").orElse(null);
    }

    @Override
    public GeoBone getLeftBootBone(GeoModel<TechArmorItem> model) {
        return model.getBone("armorLeftBoot").orElse(null);
    }
}
