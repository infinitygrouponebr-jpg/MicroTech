package Infinitygroup.microtech.client.model.block;

import Infinitygroup.microtech.block.entity.EvoTableBlockEntity;
import Infinitygroup.microtech.Microtech;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class EvoTableModel extends DefaultedBlockGeoModel<EvoTableBlockEntity> {
    public EvoTableModel() {
        super(ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "evo_table"));
    }
}
