package Infinitygroup.microtech.item;

import Infinitygroup.microtech.client.renderer.armor.TechArmorRenderer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TechArmorItem extends ArmorItem implements GeoItem {
    private static final String WING_CONTROLLER = "wing_controller";
    private static final RawAnimation WINGS_OPEN_ANIMATION = RawAnimation.begin().thenPlay("animation.tech_armor.wings_open");
    private static final RawAnimation WINGS_LOOP_ANIMATION = RawAnimation.begin().thenLoop("fly_on");
    private static final RawAnimation WINGS_CLOSE_ANIMATION = RawAnimation.begin().thenPlay("animation.tech_armor.wings_close");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TechArmorItem(Holder<net.minecraft.world.item.ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> PlayState.STOP));
        controllers.add(new AnimationController<>(this, WING_CONTROLLER, 0, state -> {
            Entity entity = state.getData(DataTickets.ENTITY);
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            EquipmentSlot slot = state.getData(DataTickets.EQUIPMENT_SLOT);
            AnimationController<TechArmorItem> controller = state.getController();

            if (!TechArmorFlightAnimationHelper.shouldAnimateWings(entity, stack, slot)) {
                if (controller.getCurrentRawAnimation() != null
                        && !controller.getCurrentRawAnimation().equals(WINGS_CLOSE_ANIMATION)) {
                    controller.setAnimation(WINGS_CLOSE_ANIMATION);
                    return PlayState.CONTINUE;
                }

                return PlayState.STOP;
            }

            if (controller.getCurrentRawAnimation() == null || controller.getCurrentRawAnimation().equals(WINGS_CLOSE_ANIMATION)) {
                controller.setAnimation(WINGS_OPEN_ANIMATION);
                return PlayState.CONTINUE;
            }

            if (controller.getCurrentRawAnimation().equals(WINGS_OPEN_ANIMATION) && controller.hasAnimationFinished()) {
                controller.setAnimation(WINGS_LOOP_ANIMATION);
                return PlayState.CONTINUE;
            }

            if (controller.getCurrentRawAnimation().equals(WINGS_LOOP_ANIMATION)) {
                return PlayState.CONTINUE;
            }

            controller.setAnimation(WINGS_OPEN_ANIMATION);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide) {
            TechArmorEnergyHelper.ensureInitialized(stack);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        if (!TechArmorEnergyHelper.isTechArmorPiece(stack)) {
            return false;
        }
        return TechArmorEnergyHelper.getEnergyStored(stack) < TechArmorEnergyHelper.getMaxEnergyStored(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int max = TechArmorEnergyHelper.getMaxEnergyStored(stack);
        if (max <= 0) {
            return 0;
        }
        return Mth.clamp(Math.round(13.0F * TechArmorEnergyHelper.getEnergyStored(stack) / (float) max), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int percent = TechArmorEnergyHelper.getEnergyPercent(stack);
        if (percent < 25) {
            return 0xFF5C5C;
        }
        if (percent < 60) {
            return 0xFFD85C;
        }
        return 0x53DDFF;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(java.util.function.Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final TechArmorRenderer renderer = new TechArmorRenderer();

            @Override
            public <T extends net.minecraft.world.entity.LivingEntity> HumanoidModel<?> getGeoArmorRenderer(T livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<T> original) {
                return this.renderer;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        MicroTechTooltipHelper.addHeader(tooltip, "tooltip.microtech.tech_armor.type");
        tooltip.add(Component.translatable("tooltip.microtech.tech_armor.tier", "T1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.microtech.tech_armor.energy",
                MicroTechTooltipHelper.formatFE(TechArmorEnergyHelper.getEnergyStoredForTooltip(stack)),
                MicroTechTooltipHelper.formatFE(TechArmorEnergyHelper.getMaxEnergyStored(stack))
        ).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                "tooltip.microtech.tech_armor.charge",
                MicroTechTooltipHelper.formatPercent(TechArmorEnergyHelper.getEnergyStoredForTooltip(stack), TechArmorEnergyHelper.getMaxEnergyStored(stack))
        ).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.microtech.tech_armor.energy_replaces_durability").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.tech_armor.full_set").withStyle(ChatFormatting.GRAY));

        if (this.getType() == Type.CHESTPLATE) {
            tooltip.add(Component.translatable("tooltip.microtech.tech_armor.flight_chip").withStyle(ChatFormatting.AQUA));
            tooltip.add(TechArmorUpgradeHelper.getUpgradeTooltip(stack).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("tooltip.microtech.tech_armor.flight_chip_note").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.microtech.tech_armor.flight_chip_only_chest").withStyle(ChatFormatting.DARK_GRAY));
        }

        if (MicroTechTooltipHelper.isShiftDown()) {
            tooltip.add(Component.translatable("tooltip.microtech.tech_armor.shift_1").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.microtech.tech_armor.shift_2").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
