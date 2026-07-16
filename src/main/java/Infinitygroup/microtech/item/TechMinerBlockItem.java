package Infinitygroup.microtech.item;

import Infinitygroup.microtech.block.entity.TechMinerBlockEntity;
import Infinitygroup.microtech.client.renderer.item.TechMinerItemRenderer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
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

import java.util.function.Consumer;

public class TechMinerBlockItem extends MicroTechMachineBlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TechMinerBlockItem(Block block, Item.Properties properties) {
        super(block, properties, TooltipProfile.TECH_MINER);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MicroTechTooltipHelper.addHeader(tooltip, "tooltip.microtech.tech_miner.type");
        tooltip.add(Component.translatable(
                "tooltip.microtech.tech_miner.energy",
                MicroTechTooltipHelper.formatFE(TechMinerBlockEntity.getEnergyFromStack(stack)),
                MicroTechTooltipHelper.formatFE(TechMinerBlockEntity.MAX_ENERGY)
        ).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                "tooltip.microtech.tech_miner.area",
                MicroTechTooltipHelper.formatFE(TechMinerBlockEntity.SCAN_RADIUS),
                MicroTechTooltipHelper.formatFE(TechMinerBlockEntity.SCAN_DEPTH)
        ).withStyle(ChatFormatting.GRAY));

        if (MicroTechTooltipHelper.isShiftDown()) {
            tooltip.add(Component.translatable("tooltip.microtech.tech_miner.shift_1").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.microtech.tech_miner.shift_2").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.microtech.tech_miner.shift_3").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.microtech.tech_miner.shift_4").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.microtech.tech_miner.shift_5").withStyle(ChatFormatting.DARK_GRAY));
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
            private final BlockEntityWithoutLevelRenderer renderer = new TechMinerItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return this.renderer;
            }
        });
    }
}
