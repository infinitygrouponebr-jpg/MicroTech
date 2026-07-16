package Infinitygroup.microtech.item;

import Infinitygroup.microtech.Microtech;
import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class TechArmorEnergyHelper {
    public static final int HELMET_CAPACITY = 25_000;
    public static final int CHESTPLATE_CAPACITY = 50_000;
    public static final int LEGGINGS_CAPACITY = 40_000;
    public static final int BOOTS_CAPACITY = 25_000;
    public static final int FE_PER_DAMAGE = 250;

    private static final String ENERGY_TAG = "EnergyStored";

    private TechArmorEnergyHelper() {
    }

    public static boolean isTechArmorPiece(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return stack.is(Microtech.TECH_ARMOR_HELMET.get())
                || stack.is(Microtech.TECH_ARMOR_CHESTPLATE.get())
                || stack.is(Microtech.TECH_ARMOR_LEGGINGS.get())
                || stack.is(Microtech.TECH_ARMOR_BOOTS.get());
    }

    public static boolean isFullTechArmorSet(Player player) {
        if (player == null) {
            return false;
        }

        return player.getItemBySlot(EquipmentSlot.HEAD).is(Microtech.TECH_ARMOR_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(Microtech.TECH_ARMOR_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(Microtech.TECH_ARMOR_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(Microtech.TECH_ARMOR_BOOTS.get());
    }

    public static int getEnergyStored(ItemStack stack) {
        if (!isTechArmorPiece(stack)) {
            return 0;
        }

        CompoundTag tag = getMicrotechArmorData(stack);
        return Mth.clamp(tag.contains(ENERGY_TAG) ? tag.getInt(ENERGY_TAG) : getMaxEnergyStored(stack), 0, getMaxEnergyStored(stack));
    }

    public static int getEnergyStoredForTooltip(ItemStack stack) {
        if (!isTechArmorPiece(stack)) {
            return 0;
        }

        CompoundTag tag = getMicrotechArmorData(stack);
        return Mth.clamp(tag.contains(ENERGY_TAG) ? tag.getInt(ENERGY_TAG) : 0, 0, getMaxEnergyStored(stack));
    }

    public static boolean hasEnergyData(ItemStack stack) {
        if (!isTechArmorPiece(stack)) {
            return false;
        }

        CompoundTag tag = getMicrotechArmorData(stack);
        return tag.contains(ENERGY_TAG);
    }

    public static int getMaxEnergyStored(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem)) {
            return 0;
        }

        ArmorItem.Type type = armorItem.getType();
        return switch (type) {
            case HELMET -> HELMET_CAPACITY;
            case CHESTPLATE -> CHESTPLATE_CAPACITY;
            case LEGGINGS -> LEGGINGS_CAPACITY;
            case BOOTS -> BOOTS_CAPACITY;
            default -> 0;
        };
    }

    public static int receiveEnergy(ItemStack stack, int amount, boolean simulate) {
        if (!isTechArmorPiece(stack) || amount <= 0) {
            return 0;
        }

        int stored = getEnergyStored(stack);
        int max = getMaxEnergyStored(stack);
        int accepted = Math.min(amount, max - stored);
        if (!simulate && accepted > 0) {
            setEnergyStored(stack, stored + accepted);
        }
        return accepted;
    }

    public static int extractEnergy(ItemStack stack, int amount, boolean simulate) {
        if (!isTechArmorPiece(stack) || amount <= 0) {
            return 0;
        }

        int stored = getEnergyStored(stack);
        int extracted = Math.min(amount, stored);
        if (!simulate && extracted > 0) {
            setEnergyStored(stack, stored - extracted);
        }
        return extracted;
    }

    public static int getTotalEnergy(Player player) {
        if (player == null) {
            return 0;
        }

        return getEnergyStored(player.getItemBySlot(EquipmentSlot.HEAD))
                + getEnergyStored(player.getItemBySlot(EquipmentSlot.CHEST))
                + getEnergyStored(player.getItemBySlot(EquipmentSlot.LEGS))
                + getEnergyStored(player.getItemBySlot(EquipmentSlot.FEET));
    }

    public static int getTotalMaxEnergy(Player player) {
        if (player == null) {
            return 0;
        }

        return getMaxEnergyStored(player.getItemBySlot(EquipmentSlot.HEAD))
                + getMaxEnergyStored(player.getItemBySlot(EquipmentSlot.CHEST))
                + getMaxEnergyStored(player.getItemBySlot(EquipmentSlot.LEGS))
                + getMaxEnergyStored(player.getItemBySlot(EquipmentSlot.FEET));
    }

    public static ItemStack getChestplate(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }

        return player.getItemBySlot(EquipmentSlot.CHEST);
    }

    public static int getChestplateEnergy(Player player) {
        return getEnergyStored(getChestplate(player));
    }

    public static int getChestplateMaxEnergy(Player player) {
        return getMaxEnergyStored(getChestplate(player));
    }

    public static int consumeEnergyFromChestplate(Player player, int amount) {
        if (player == null || amount <= 0) {
            return 0;
        }

        return extractEnergy(getChestplate(player), amount, false);
    }

    public static int getEnergyPercent(Player player) {
        int max = getTotalMaxEnergy(player);
        if (max <= 0) {
            return 0;
        }

        return Mth.clamp(Math.round(getTotalEnergy(player) * 100.0F / max), 0, 100);
    }

    public static int getEnergyPercent(ItemStack stack) {
        int max = getMaxEnergyStored(stack);
        if (max <= 0) {
            return 0;
        }

        return Mth.clamp(Math.round(getEnergyStored(stack) * 100.0F / max), 0, 100);
    }

    public static int consumeEnergyFromArmor(Player player, int amount) {
        if (player == null || amount <= 0) {
            return 0;
        }

        int remaining = amount;
        remaining = drainSlot(player.getItemBySlot(EquipmentSlot.CHEST), remaining);
        remaining = drainSlot(player.getItemBySlot(EquipmentSlot.LEGS), remaining);
        remaining = drainSlot(player.getItemBySlot(EquipmentSlot.HEAD), remaining);
        remaining = drainSlot(player.getItemBySlot(EquipmentSlot.FEET), remaining);
        return amount - remaining;
    }

    public static void ensureInitialized(ItemStack stack) {
        if (!isTechArmorPiece(stack)) {
            return;
        }

        CompoundTag tag = getMicrotechArmorData(stack);
        if (!tag.contains(ENERGY_TAG)) {
            tag.putInt(ENERGY_TAG, getMaxEnergyStored(stack));
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static void setEnergyStored(ItemStack stack, int amount) {
        if (!isTechArmorPiece(stack)) {
            return;
        }

        CompoundTag tag = getMicrotechArmorData(stack);
        tag.putInt(ENERGY_TAG, Mth.clamp(amount, 0, getMaxEnergyStored(stack)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static String getStatus(Player player) {
        int percent = getEnergyPercent(player);
        if (percent <= 0) {
            return "OFFLINE";
        }
        if (percent < 25) {
            return "CRITICAL";
        }
        if (percent < 60) {
            return "LOW POWER";
        }
        return "ONLINE";
    }

    private static int drainSlot(ItemStack stack, int remaining) {
        if (remaining <= 0 || !isTechArmorPiece(stack)) {
            return remaining;
        }

        return remaining - extractEnergy(stack, remaining, false);
    }

    private static CompoundTag getMicrotechArmorData(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return Objects.requireNonNullElseGet(data.copyTag(), CompoundTag::new);
    }
}
