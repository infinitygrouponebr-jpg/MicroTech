package Infinitygroup.microtech.item;

import Infinitygroup.microtech.Microtech;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class TechArmorUpgradeHelper {
    private static final String FLIGHT_CHIP_TAG = "HasFlightChip";

    private TechArmorUpgradeHelper() {
    }

    public static boolean isTechArmorChestplate(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Microtech.TECH_ARMOR_CHESTPLATE.get());
    }

    public static boolean hasFlightChip(ItemStack stack) {
        if (!isTechArmorChestplate(stack)) {
            return false;
        }

        CompoundTag tag = getUpgradeData(stack);
        return tag.getBoolean(FLIGHT_CHIP_TAG);
    }

    public static boolean isFlightChip(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Microtech.TECH_FLIGHT_CHIP.get());
    }

    public static boolean canInstallFlightChip(ItemStack chestplate) {
        return isTechArmorChestplate(chestplate) && !hasFlightChip(chestplate);
    }

    public static boolean installFlightChip(ItemStack chestplate) {
        if (!canInstallFlightChip(chestplate)) {
            return false;
        }

        CompoundTag tag = getUpgradeData(chestplate);
        tag.putBoolean(FLIGHT_CHIP_TAG, true);
        chestplate.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }

    public static MutableComponent getUpgradeTooltip(ItemStack stack) {
        if (!isTechArmorChestplate(stack)) {
            return Component.translatable("tooltip.microtech.tech_flight_chip.invalid");
        }

        return hasFlightChip(stack)
                ? Component.translatable("tooltip.microtech.tech_armor.flight_chip.installed")
                : Component.translatable("tooltip.microtech.tech_armor.flight_chip.not_installed");
    }

    private static CompoundTag getUpgradeData(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }
}
