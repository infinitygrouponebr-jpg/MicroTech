package Infinitygroup.microtech.client.renderer.blockentity;

import Infinitygroup.microtech.block.entity.EvoTableBlockEntity;
import Infinitygroup.microtech.client.model.block.EvoTableModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class EvoTableBlockEntityRenderer extends GeoBlockRenderer<EvoTableBlockEntity> {
    public EvoTableBlockEntityRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
        super(new EvoTableModel());
    }
}
