package Infinitygroup.microtech.client.model.block;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.block.entity.TechCrusherBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class TechCrusherModel extends DefaultedBlockGeoModel<TechCrusherBlockEntity> {
    public TechCrusherModel() {
        super(ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "tech_crusher"));
    }
}
