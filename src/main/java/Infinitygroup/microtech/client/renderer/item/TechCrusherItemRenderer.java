package Infinitygroup.microtech.client.renderer.item;

import Infinitygroup.microtech.client.model.item.TechCrusherItemModel;
import Infinitygroup.microtech.item.TechCrusherBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TechCrusherItemRenderer extends GeoItemRenderer<TechCrusherBlockItem> {
    public TechCrusherItemRenderer() {
        super(new TechCrusherItemModel());
    }
}
