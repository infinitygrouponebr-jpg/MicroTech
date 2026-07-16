package Infinitygroup.microtech.client.renderer.item;

import Infinitygroup.microtech.client.model.item.TechMinerItemModel;
import Infinitygroup.microtech.item.TechMinerBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TechMinerItemRenderer extends GeoItemRenderer<TechMinerBlockItem> {
    public TechMinerItemRenderer() {
        super(new TechMinerItemModel());
    }
}
