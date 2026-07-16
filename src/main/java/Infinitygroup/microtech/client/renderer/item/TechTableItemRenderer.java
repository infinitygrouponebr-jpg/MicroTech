package Infinitygroup.microtech.client.renderer.item;

import Infinitygroup.microtech.client.model.item.TechTableItemModel;
import Infinitygroup.microtech.item.TechTableBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TechTableItemRenderer extends GeoItemRenderer<TechTableBlockItem> {
    public TechTableItemRenderer() {
        super(new TechTableItemModel());
    }
}
