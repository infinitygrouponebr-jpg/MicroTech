package Infinitygroup.microtech.client.model.block;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.block.entity.TechTableBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class TechTableModel extends DefaultedBlockGeoModel<TechTableBlockEntity> {
    public TechTableModel() {
        super(ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "tech_table"));
    }
}
