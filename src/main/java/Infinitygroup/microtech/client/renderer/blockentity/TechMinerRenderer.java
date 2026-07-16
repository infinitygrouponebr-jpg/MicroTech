package Infinitygroup.microtech.client.renderer.blockentity;

import Infinitygroup.microtech.block.entity.TechMinerBlockEntity;
import Infinitygroup.microtech.client.model.block.TechMinerModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TechMinerRenderer extends GeoBlockRenderer<TechMinerBlockEntity> {
    public TechMinerRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
        super(new TechMinerModel());
    }
}
