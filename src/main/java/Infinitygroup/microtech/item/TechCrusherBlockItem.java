package Infinitygroup.microtech.item;

import Infinitygroup.microtech.client.renderer.item.TechCrusherItemRenderer;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TechCrusherBlockItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TechCrusherBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MicroTechTooltipHelper.addHeader(tooltip, "tooltip.microtech.tech_crusher.type");
        tooltip.add(Component.translatable(
                "tooltip.microtech.tech_crusher.energy",
                MicroTechTooltipHelper.formatFE(Infinitygroup.microtech.block.entity.TechCrusherBlockEntity.MAX_ENERGY)
        ).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                "tooltip.microtech.tech_crusher.cost",
                MicroTechTooltipHelper.formatFE(Infinitygroup.microtech.block.entity.TechCrusherBlockEntity.PROCESS_COST)
        ).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(
                "tooltip.microtech.tech_crusher.time",
                Component.literal("5s")
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.tech_crusher.slots").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.tech_crusher.upgrades").withStyle(ChatFormatting.DARK_GRAY));

        if (MicroTechTooltipHelper.isShiftDown()) {
            tooltip.add(Component.translatable("tooltip.microtech.tech_crusher.shift_1").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.microtech.tech_crusher.shift_2").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.microtech.tech_crusher.shift_3").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            MicroTechTooltipHelper.addHoldShiftHint(tooltip);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final BlockEntityWithoutLevelRenderer renderer = new TechCrusherItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return this.renderer;
            }
        });
    }
}
