package Infinitygroup.microtech.client.renderer.item;

import Infinitygroup.microtech.client.model.item.GraviteModel;
import Infinitygroup.microtech.item.GraviteItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GraviteRenderer extends GeoItemRenderer<GraviteItem> {
    public GraviteRenderer() {
        super(new GraviteModel());
    }
}
