package Infinitygroup.microtech.client.renderer.item;

import Infinitygroup.microtech.client.model.item.TechSwordModel;
import Infinitygroup.microtech.item.TechSwordItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TechSwordRenderer extends GeoItemRenderer<TechSwordItem> {
    public TechSwordRenderer() {
        super(new TechSwordModel());
    }
}
