package Infinitygroup.microtech.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

public final class TechSwordData {
    private static final String ENERGY_TAG = "EnergyStored";
    private static final String TIER_TAG = "EvolutionTier";
    private static final String ENERGY_CUT_LEVEL_TAG = "EnergyCutLevel";
    private static final String SHOCK_DISCHARGE_LEVEL_TAG = "ShockDischargeLevel";
    private static final String OVERLOAD_LEVEL_TAG = "OverloadLevel";
    private static final String OVERLOAD_ARMED_UNTIL_TAG = "OverloadArmedUntil";
    private static final String OVERLOAD_COOLDOWN_UNTIL_TAG = "OverloadCooldownUntil";
    private static final String SHOCK_DISCHARGE_COOLDOWN_TAG = "ShockDischargeCooldown";
    private static final String SELECTED_ACTIVE_CHIP_TAG = "selected_active_chip";
    private static final String ACTIVE_ABILITIES_TAG = "active_abilities";

    private TechSwordData() {
    }

    public static SwordEvolutionTier getEvolutionTier(ItemStack stack) {
        return SwordEvolutionTier.fromId(getStoredTierId(stack));
    }

    public static void setEvolutionTier(ItemStack stack, SwordEvolutionTier tier) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        tag.putInt(TIER_TAG, tier.getId());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getEnergyStored(ItemStack stack) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        SwordEvolutionTier tier = getEvolutionTier(stack);
        return Mth.clamp(tag.getInt(ENERGY_TAG), 0, tier.getEnergyCapacity());
    }

    public static void setEnergyStored(ItemStack stack, int energy) {
        SwordEvolutionTier tier = getEvolutionTier(stack);
        CompoundTag tag = getOrCreateMicrotechData(stack);
        tag.putInt(ENERGY_TAG, Mth.clamp(energy, 0, tier.getEnergyCapacity()));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getEnergyCutLevel(ItemStack stack) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        return Mth.clamp(tag.getInt(ENERGY_CUT_LEVEL_TAG), 0, TechChipType.ENERGY_CUT.getMaxLevel());
    }

    public static void setEnergyCutLevel(ItemStack stack, int level) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        tag.putInt(ENERGY_CUT_LEVEL_TAG, Mth.clamp(level, 0, TechChipType.ENERGY_CUT.getMaxLevel()));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getShockDischargeLevel(ItemStack stack) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        return Mth.clamp(tag.getInt(SHOCK_DISCHARGE_LEVEL_TAG), 0, TechChipType.SHOCK_DISCHARGE.getMaxLevel());
    }

    public static void setShockDischargeLevel(ItemStack stack, int level) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        tag.putInt(SHOCK_DISCHARGE_LEVEL_TAG, Mth.clamp(level, 0, TechChipType.SHOCK_DISCHARGE.getMaxLevel()));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getOverloadLevel(ItemStack stack) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        return Mth.clamp(tag.getInt(OVERLOAD_LEVEL_TAG), 0, TechChipType.OVERLOAD.getMaxLevel());
    }

    public static void setOverloadLevel(ItemStack stack, int level) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        tag.putInt(OVERLOAD_LEVEL_TAG, Mth.clamp(level, 0, TechChipType.OVERLOAD.getMaxLevel()));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getShockDischargeCooldown(ItemStack stack) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        return Math.max(0, tag.getInt(SHOCK_DISCHARGE_COOLDOWN_TAG));
    }

    public static void setShockDischargeCooldown(ItemStack stack, int ticks) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        if (ticks <= 0) {
            tag.remove(SHOCK_DISCHARGE_COOLDOWN_TAG);
        } else {
            tag.putInt(SHOCK_DISCHARGE_COOLDOWN_TAG, ticks);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static long getOverloadArmedUntil(ItemStack stack) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        return tag.contains(OVERLOAD_ARMED_UNTIL_TAG, Tag.TAG_LONG) ? tag.getLong(OVERLOAD_ARMED_UNTIL_TAG) : 0L;
    }

    public static void setOverloadArmedUntil(ItemStack stack, long gameTime) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        if (gameTime <= 0L) {
            tag.remove(OVERLOAD_ARMED_UNTIL_TAG);
        } else {
            tag.putLong(OVERLOAD_ARMED_UNTIL_TAG, gameTime);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static long getOverloadCooldownUntil(ItemStack stack) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        return tag.contains(OVERLOAD_COOLDOWN_UNTIL_TAG, Tag.TAG_LONG) ? tag.getLong(OVERLOAD_COOLDOWN_UNTIL_TAG) : 0L;
    }

    public static void setOverloadCooldownUntil(ItemStack stack, long gameTime) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        if (gameTime <= 0L) {
            tag.remove(OVERLOAD_COOLDOWN_UNTIL_TAG);
        } else {
            tag.putLong(OVERLOAD_COOLDOWN_UNTIL_TAG, gameTime);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isOverloadArmed(ItemStack stack, long gameTime) {
        return getOverloadLevel(stack) > 0
                && getOverloadArmedUntil(stack) > gameTime;
    }

    public static boolean isOverloadOnCooldown(ItemStack stack, long gameTime) {
        return getOverloadCooldownUntil(stack) > gameTime;
    }

    public static int getChipLevel(ItemStack stack, TechChipType type) {
        return switch (type) {
            case ENERGY_CUT -> getEnergyCutLevel(stack);
            case SHOCK_DISCHARGE -> getShockDischargeLevel(stack);
            case OVERLOAD -> getOverloadLevel(stack);
        };
    }

    public static void setChipLevel(ItemStack stack, TechChipType type, int level) {
        switch (type) {
            case ENERGY_CUT -> setEnergyCutLevel(stack, level);
            case SHOCK_DISCHARGE -> setShockDischargeLevel(stack, level);
            case OVERLOAD -> setOverloadLevel(stack, level);
        }
    }

    public static void addInstalledActiveAbility(ItemStack stack, String abilityId) {
        if (abilityId == null || abilityId.isBlank()) {
            return;
        }

        List<String> abilities = new ArrayList<>(getInstalledActiveAbilities(stack));
        if (!abilities.contains(abilityId)) {
            abilities.add(abilityId);
            setInstalledActiveAbilities(stack, abilities);
        }
    }

    public static List<String> getInstalledActiveAbilities(ItemStack stack) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        if (!tag.contains(ACTIVE_ABILITIES_TAG, Tag.TAG_LIST)) {
            return List.of();
        }

        ListTag listTag = tag.getList(ACTIVE_ABILITIES_TAG, Tag.TAG_STRING);
        List<String> abilities = new ArrayList<>(listTag.size());
        for (int i = 0; i < listTag.size(); i++) {
            String abilityId = listTag.getString(i);
            if (!abilityId.isBlank()) {
                abilities.add(abilityId);
            }
        }
        return List.copyOf(abilities);
    }

    public static void setInstalledActiveAbilities(ItemStack stack, List<String> abilityIds) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        if (abilityIds == null || abilityIds.isEmpty()) {
            tag.remove(ACTIVE_ABILITIES_TAG);
        } else {
            ListTag listTag = new ListTag();
            for (String abilityId : abilityIds) {
                if (abilityId != null && !abilityId.isBlank()) {
                    listTag.add(StringTag.valueOf(abilityId));
                }
            }
            if (listTag.isEmpty()) {
                tag.remove(ACTIVE_ABILITIES_TAG);
            } else {
                tag.put(ACTIVE_ABILITIES_TAG, listTag);
            }
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static String getSelectedActiveAbility(ItemStack stack) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        return tag.contains(SELECTED_ACTIVE_CHIP_TAG) ? tag.getString(SELECTED_ACTIVE_CHIP_TAG) : "";
    }

    public static void setSelectedActiveAbility(ItemStack stack, String abilityId) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        if (abilityId == null || abilityId.isBlank()) {
            tag.remove(SELECTED_ACTIVE_CHIP_TAG);
        } else {
            tag.putString(SELECTED_ACTIVE_CHIP_TAG, abilityId);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static String getSelectedActiveChipId(ItemStack stack) {
        return getSelectedActiveAbility(stack);
    }

    public static void setSelectedActiveChipId(ItemStack stack, String chipId) {
        setSelectedActiveAbility(stack, chipId);
    }

    public static Component formatEnergyText(int energy) {
        return Component.literal(Integer.toString(energy));
    }

    private static int getStoredTierId(ItemStack stack) {
        CompoundTag tag = getOrCreateMicrotechData(stack);
        return tag.contains(TIER_TAG) ? tag.getInt(TIER_TAG) : SwordEvolutionTier.TIER_1.getId();
    }

    public static CompoundTag getOrCreateMicrotechData(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }
}
