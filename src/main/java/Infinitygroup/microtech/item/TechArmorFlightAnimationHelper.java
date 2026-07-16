package Infinitygroup.microtech.item;

import Infinitygroup.microtech.Microtech;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class TechArmorFlightAnimationHelper {
    private TechArmorFlightAnimationHelper() {
    }

    public static boolean hasFlightChipInChestplate(Player player) {
        return TechArmorFlightVisualHelper.hasFlightChipInChestplate(player);
    }

    public static boolean hasChestplateEnergyForFlight(Player player) {
        return TechArmorFlightVisualHelper.hasChestplateEnergyForFlight(player);
    }

    public static boolean hasChestplateEnergyForFlightTick(Player player) {
        return TechArmorFlightVisualHelper.hasChestplateEnergyForFlightTick(player);
    }

    public static boolean isFlightChipActive(Player player) {
        return hasFlightChipInChestplate(player) && hasChestplateEnergyForFlight(player);
    }

    public static boolean isTechArmorFlightActive(Player player) {
        return TechArmorFlightVisualHelper.isTechFlightActive(player);
    }

    public static boolean shouldOpenWings(Player player) {
        return isTechArmorFlightActive(player);
    }

    public static boolean shouldAnimateWings(Entity entity, ItemStack stack, EquipmentSlot slot) {
        if (!(entity instanceof Player player) || stack == null || stack.isEmpty() || slot != EquipmentSlot.CHEST) {
            return false;
        }

        return isTechArmorFlightActive(player)
                && stack.is(Microtech.TECH_ARMOR_CHESTPLATE.get())
                && TechArmorUpgradeHelper.hasFlightChip(stack);
    }
}
