package Infinitygroup.microtech.item;

import Infinitygroup.microtech.Microtech;
import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class GraviteEnergyHelper {
    public static final int MAX_ENERGY = 100_000;
    public static final int BASE_USE_COST = 100;
    public static final int BLOCK_BREAK_COST = 50;

    private static final String ENERGY_TAG = "EnergyStored";

    private GraviteEnergyHelper() {
    }

    public static boolean isGravite(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Microtech.GRAVITE.get());
    }

    public static int getEnergyStored(ItemStack stack) {
        if (!isGravite(stack)) {
            return 0;
        }

        CompoundTag tag = getMicrotechData(stack);
        return Mth.clamp(tag.contains(ENERGY_TAG) ? tag.getInt(ENERGY_TAG) : MAX_ENERGY, 0, MAX_ENERGY);
    }

    public static int getEnergyStoredForTooltip(ItemStack stack) {
        if (!isGravite(stack)) {
            return 0;
        }

        CompoundTag tag = getMicrotechData(stack);
        return Mth.clamp(tag.contains(ENERGY_TAG) ? tag.getInt(ENERGY_TAG) : 0, 0, MAX_ENERGY);
    }

    public static boolean hasEnergyData(ItemStack stack) {
        if (!isGravite(stack)) {
            return false;
        }

        CompoundTag tag = getMicrotechData(stack);
        return tag.contains(ENERGY_TAG);
    }

    public static int getMaxEnergyStored(ItemStack stack) {
        return isGravite(stack) ? MAX_ENERGY : 0;
    }

    public static int receiveEnergy(ItemStack stack, int amount, boolean simulate) {
        if (!isGravite(stack) || amount <= 0) {
            return 0;
        }

        int stored = getEnergyStored(stack);
        int accepted = Math.min(amount, MAX_ENERGY - stored);
        if (!simulate && accepted > 0) {
            setEnergyStored(stack, stored + accepted);
        }
        return accepted;
    }

    public static int extractEnergy(ItemStack stack, int amount, boolean simulate) {
        if (!isGravite(stack) || amount <= 0) {
            return 0;
        }

        int stored = getEnergyStored(stack);
        int extracted = Math.min(amount, stored);
        if (!simulate && extracted > 0) {
            setEnergyStored(stack, stored - extracted);
        }
        return extracted;
    }

    public static boolean hasEnoughEnergy(ItemStack stack, int amount) {
        return isGravite(stack) && amount > 0 && getEnergyStored(stack) >= amount;
    }

    public static int getEnergyPercent(ItemStack stack) {
        int max = getMaxEnergyStored(stack);
        if (max <= 0) {
            return 0;
        }

        return Mth.clamp(Math.round(getEnergyStored(stack) * 100.0F / max), 0, 100);
    }

    public static void ensureInitialized(ItemStack stack) {
        if (!isGravite(stack)) {
            return;
        }

        CompoundTag tag = getMicrotechData(stack);
        if (!tag.contains(ENERGY_TAG)) {
            tag.putInt(ENERGY_TAG, MAX_ENERGY);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static void setEnergyStored(ItemStack stack, int amount) {
        if (!isGravite(stack)) {
            return;
        }

        CompoundTag tag = getMicrotechData(stack);
        tag.putInt(ENERGY_TAG, Mth.clamp(amount, 0, MAX_ENERGY));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static CompoundTag getMicrotechData(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return Objects.requireNonNullElseGet(data.copyTag(), CompoundTag::new);
    }
}
