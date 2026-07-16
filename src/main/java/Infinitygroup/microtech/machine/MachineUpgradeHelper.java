package Infinitygroup.microtech.machine;

import Infinitygroup.microtech.block.entity.BasicMachineBlockEntity;
import Infinitygroup.microtech.block.entity.BatteryT2BlockEntity;
import Infinitygroup.microtech.block.entity.ElectricFurnaceBlockEntity;
import Infinitygroup.microtech.block.entity.EvoTableBlockEntity;
import Infinitygroup.microtech.block.entity.SolarPanelBlockEntity;
import Infinitygroup.microtech.block.entity.TechCrusherBlockEntity;
import Infinitygroup.microtech.block.entity.TechMinerBlockEntity;
import Infinitygroup.microtech.item.MicroTechMachineBlockItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class MachineUpgradeHelper {
    private static final int SPEED_EFFECTIVE_MAX = 32;
    private static final int EFFICIENCY_EFFECTIVE_MAX = 32;
    private static final int INPUT_EFFECTIVE_MAX = 64;
    private static final int OUTPUT_EFFECTIVE_MAX = 64;
    private static final int RANGE_EFFECTIVE_MAX = 16;
    private static final int AREA_EFFECTIVE_MAX = 32;
    private static final int FILTER_EFFECTIVE_MAX = 16;
    private static final int FORTUNE_EFFECTIVE_MAX = 64;
    private static final int SOLAR_EFFECTIVE_MAX = 32;
    private static final int MINER_SCAN_BASE_RADIUS = 6;
    private static final int MINER_SCAN_BASE_DEPTH = 8;
    private static final int MINER_SCAN_BASE_TARGETS = 64;
    private static final int MINER_SCAN_MAX_RADIUS = 38;
    private static final int MINER_SCAN_MAX_DEPTH = 72;
    private static final int MINER_SCAN_MAX_TARGETS = 1088;
    private static final MachineUpgradeType[] FINAL_ORDER = {
            MachineUpgradeType.SPEED,
            MachineUpgradeType.EFFICIENCY,
            MachineUpgradeType.INPUT,
            MachineUpgradeType.OUTPUT,
            MachineUpgradeType.RANGE,
            MachineUpgradeType.AREA,
            MachineUpgradeType.FILTER,
            MachineUpgradeType.FORTUNE,
            MachineUpgradeType.SOLAR_FOCUS
    };

    private MachineUpgradeHelper() {
    }

    public static boolean isCompatibleUpgrade(String machineId, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!(stack.getItem() instanceof MachineUpgradeItem upgradeItem)) {
            return false;
        }
        return upgradeItem.getUpgradeType().supportsMachine(machineId);
    }

    public static int countUpgrade(BlockEntity blockEntity, MachineUpgradeType upgradeType) {
        if (blockEntity instanceof MachineUpgradeHost host) {
            return countUpgrade(host.getUpgradeInventory(), upgradeType);
        }
        return 0;
    }

    public static int countUpgrade(ItemStackHandler inventory, MachineUpgradeType upgradeType) {
        MachineUpgradeType canonicalTarget = canonicalType(upgradeType);
        if (inventory == null || canonicalTarget == null) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof MachineUpgradeItem upgradeItem)) {
                continue;
            }

            MachineUpgradeType canonicalStack = canonicalType(upgradeItem.getUpgradeType());
            if (canonicalStack == canonicalTarget) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int getInstalledCount(MachineUpgradeHost host, MachineUpgradeType type) {
        if (host == null) {
            return 0;
        }
        return countUpgrade(host.getUpgradeInventory(), type);
    }

    public static int getInstalledCount(BlockEntity blockEntity, MachineUpgradeType type) {
        if (!(blockEntity instanceof MachineUpgradeHost host)) {
            return 0;
        }
        return getInstalledCount(host, type);
    }

    public static int getEffectiveCount(MachineUpgradeHost host, MachineUpgradeType type, int max) {
        return Math.min(getInstalledCount(host, type), Math.max(0, max));
    }

    public static int getEffectiveCount(BlockEntity blockEntity, MachineUpgradeType type, int max) {
        if (!(blockEntity instanceof MachineUpgradeHost host)) {
            return 0;
        }
        return getEffectiveCount(host, type, max);
    }

    public static int getEffectiveCount(BlockEntity blockEntity, MachineUpgradeType type) {
        return getEffectiveCount(blockEntity, type, getUpgradeCap(type));
    }

    public static int getUpgradeLevel(BlockEntity blockEntity, MachineUpgradeType upgradeType) {
        return getEffectiveCount(blockEntity, upgradeType);
    }

    public static boolean hasUpgrade(BlockEntity blockEntity, MachineUpgradeType upgradeType) {
        return getUpgradeLevel(blockEntity, upgradeType) > 0;
    }

    public static int getSpeedLevel(BlockEntity blockEntity) {
        return getEffectiveCount(blockEntity, MachineUpgradeType.SPEED, SPEED_EFFECTIVE_MAX);
    }

    public static double getSpeedMultiplier(BlockEntity blockEntity) {
        int count = getSpeedLevel(blockEntity);
        return 1.0D + (count * 0.12D);
    }

    public static int getEfficiencyLevel(BlockEntity blockEntity) {
        return getEffectiveCount(blockEntity, MachineUpgradeType.EFFICIENCY, EFFICIENCY_EFFECTIVE_MAX);
    }

    public static double getEnergyCostMultiplier(BlockEntity blockEntity) {
        int count = getEfficiencyLevel(blockEntity);
        return Math.max(0.20D, 1.0D - (count * 0.035D));
    }

    public static int getInputLevel(BlockEntity blockEntity) {
        return getEffectiveCount(blockEntity, MachineUpgradeType.INPUT, INPUT_EFFECTIVE_MAX);
    }

    public static boolean getAutoInputEnabled(BlockEntity blockEntity) {
        return getInputLevel(blockEntity) > 0;
    }

    public static int getInputBatchSize(BlockEntity blockEntity) {
        int count = getInputLevel(blockEntity);
        return count <= 0 ? 0 : Math.min(64, 1 + count);
    }

    public static int getInputInterval(BlockEntity blockEntity) {
        int count = getInputLevel(blockEntity);
        return count <= 0 ? 20 : Math.max(2, 20 - (count / 4));
    }

    public static int getOutputLevel(BlockEntity blockEntity) {
        return getEffectiveCount(blockEntity, MachineUpgradeType.OUTPUT, OUTPUT_EFFECTIVE_MAX);
    }

    public static boolean getAutoOutputEnabled(BlockEntity blockEntity) {
        return getOutputLevel(blockEntity) > 0;
    }

    public static int getOutputBatchSize(BlockEntity blockEntity) {
        int count = getOutputLevel(blockEntity);
        return count <= 0 ? 0 : Math.min(64, 1 + count);
    }

    public static int getOutputInterval(BlockEntity blockEntity) {
        int count = getOutputLevel(blockEntity);
        return count <= 0 ? 20 : Math.max(2, 20 - (count / 4));
    }

    public static int getRangeLevel(BlockEntity blockEntity) {
        return getEffectiveCount(blockEntity, MachineUpgradeType.RANGE, RANGE_EFFECTIVE_MAX);
    }

    public static int getRangeBonus(BlockEntity blockEntity) {
        int count = getRangeLevel(blockEntity);
        return Math.min(32, count * 2);
    }

    public static int getDepthBonus(BlockEntity blockEntity) {
        int count = getRangeLevel(blockEntity);
        return Math.min(64, count * 4);
    }

    public static int getAreaLevel(BlockEntity blockEntity) {
        return getEffectiveCount(blockEntity, MachineUpgradeType.AREA, AREA_EFFECTIVE_MAX);
    }

    public static int getAreaBonus(BlockEntity blockEntity) {
        int count = getAreaLevel(blockEntity);
        return Math.min(1024, count * 32);
    }

    public static int getAreaTargetLimit(BlockEntity blockEntity) {
        return Math.min(MINER_SCAN_MAX_TARGETS, MINER_SCAN_BASE_TARGETS + getAreaBonus(blockEntity));
    }

    public static int getFilterLevel(BlockEntity blockEntity) {
        return getEffectiveCount(blockEntity, MachineUpgradeType.FILTER, FILTER_EFFECTIVE_MAX);
    }

    public static int getFilterTier(BlockEntity blockEntity) {
        if (!(blockEntity instanceof MachineUpgradeHost host)) {
            return 0;
        }
        return getFilterTier(host.getUpgradeInventory());
    }

    public static int getFilterTier(ItemStackHandler inventory) {
        if (inventory == null) {
            return 0;
        }

        int tier = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof MachineUpgradeItem upgradeItem)) {
                continue;
            }

            tier = Math.max(tier, getFilterTier(upgradeItem.getUpgradeType(), stack.getCount()));
        }
        return Math.min(4, tier);
    }

    public static int getFilterCapacity(BlockEntity blockEntity) {
        return getFilterCapacityFromTier(getFilterTier(blockEntity));
    }

    public static int getFilterCapacityFromTier(int tier) {
        return switch (Math.max(0, Math.min(4, tier))) {
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> 6;
            case 4 -> 9;
            default -> 0;
        };
    }

    public static boolean isFilterUpgrade(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof MachineUpgradeItem upgradeItem)) {
            return false;
        }
        return canonicalType(upgradeItem.getUpgradeType()) == MachineUpgradeType.FILTER;
    }

    public static int getFilterTier(MachineUpgradeType type, int count) {
        if (type == null || count <= 0) {
            return 0;
        }
        return switch (type) {
            case FILTER_UPGRADE_T1 -> 1;
            case FILTER_UPGRADE_T2 -> 2;
            case FILTER_UPGRADE_T3 -> 3;
            case FILTER_UPGRADE_T4 -> 4;
            case FILTER, PRIORITY -> Math.min(4, count);
            default -> 0;
        };
    }

    public static int getMinerPriorityRank(BlockState state) {
        if (state == null) {
            return Integer.MAX_VALUE;
        }
        if (state.is(net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS)) {
            return -1;
        }
        if (state.is(net.minecraft.tags.BlockTags.DIAMOND_ORES)) return 1;
        if (state.is(net.minecraft.tags.BlockTags.EMERALD_ORES)) return 2;
        if (state.is(net.minecraft.tags.BlockTags.GOLD_ORES)) return 3;
        if (state.is(net.minecraft.tags.BlockTags.REDSTONE_ORES)) return 4;
        if (state.is(net.minecraft.tags.BlockTags.LAPIS_ORES)) return 5;
        if (state.is(net.minecraft.tags.BlockTags.IRON_ORES)) return 6;
        if (state.is(net.minecraft.tags.BlockTags.COPPER_ORES)) return 7;
        if (state.is(net.minecraft.tags.BlockTags.COAL_ORES)) return 8;
        return Integer.MAX_VALUE - 1;
    }

    public static boolean getSilenced(BlockEntity blockEntity) {
        return false;
    }

    public static int getSmeltBoostLevel(BlockEntity blockEntity) {
        return getSpeedLevel(blockEntity);
    }

    public static double getSmeltBoostMultiplier(BlockEntity blockEntity) {
        return getSpeedMultiplier(blockEntity);
    }

    public static double getHeatControlMultiplier(BlockEntity blockEntity) {
        return getEnergyCostMultiplier(blockEntity);
    }

    public static double getDoubleSmeltChance(BlockEntity blockEntity) {
        return getFortuneLevel(blockEntity) > 0 ? 1.0D : 0.0D;
    }

    public static double getCrusherYieldChance(BlockEntity blockEntity) {
        return getFortuneLevel(blockEntity) > 0 ? 1.0D : 0.0D;
    }

    public static double getCrusherFortuneChance(BlockEntity blockEntity) {
        return getFortuneLevel(blockEntity) > 0 ? 1.0D : 0.0D;
    }

    public static double getFineDustEfficiencyMultiplier(BlockEntity blockEntity) {
        int count = getFortuneLevel(blockEntity);
        if (count <= 0) {
            return 1.0D;
        }
        return Math.max(0.85D, 1.0D - (Math.min(count, 64) * 0.0025D));
    }

    public static int getFortuneLevel(BlockEntity blockEntity) {
        return getEffectiveCount(blockEntity, MachineUpgradeType.FORTUNE, FORTUNE_EFFECTIVE_MAX);
    }

    public static double getFortuneChance(BlockEntity blockEntity) {
        int count = getFortuneLevel(blockEntity);
        if (count <= 0) {
            return 0.0D;
        }
        int remainder = count % 8;
        return remainder == 0 ? 0.0D : remainder / 8.0D;
    }

    public static double getChargeSpeedMultiplier(BlockEntity blockEntity) {
        return getSpeedMultiplier(blockEntity);
    }

    public static boolean getWirelessChargeEnabled(BlockEntity blockEntity) {
        return false;
    }

    public static boolean getEquipmentPriorityEnabled(BlockEntity blockEntity) {
        return false;
    }

    public static int getSolarFocusLevel(BlockEntity blockEntity) {
        return getEffectiveCount(blockEntity, MachineUpgradeType.SOLAR_FOCUS, SOLAR_EFFECTIVE_MAX);
    }

    public static double getGenerationMultiplier(BlockEntity blockEntity) {
        int count = getSolarFocusLevel(blockEntity);
        if (count <= 0) {
            return 1.0D;
        }
        return Math.min(6.0D, 1.0D + (count * 0.15D));
    }

    public static double getFuelEfficiencyMultiplier(BlockEntity blockEntity) {
        int count = getSolarFocusLevel(blockEntity);
        if (count <= 0) {
            return 1.0D;
        }
        return 1.0D + (Math.min(count, 16) * 0.08D);
    }

    public static double getSolarFocusMultiplier(BlockEntity blockEntity) {
        return getGenerationMultiplier(blockEntity);
    }

    public static double getNightStorageMultiplier(BlockEntity blockEntity) {
        int count = getSolarFocusLevel(blockEntity);
        if (count >= 32) {
            return 0.35D;
        }
        if (count >= 16) {
            return 0.20D;
        }
        if (count >= 8) {
            return 0.10D;
        }
        return 0.0D;
    }

    public static int getCapacityMultiplierLevel(BlockEntity blockEntity) {
        return getUpgradeLevel(blockEntity, MachineUpgradeType.CAPACITY);
    }

    public static double getCapacityMultiplier(BlockEntity blockEntity) {
        return 1.0D;
    }

    public static int getTransferBonus(BlockEntity blockEntity) {
        return 0;
    }

    public static MachineUpgradeType canonicalType(MachineUpgradeType type) {
        return type == null ? null : type.getCanonicalType();
    }

    public static MachineUpgradeType displayType(MachineUpgradeType type) {
        if (type == null) {
            return null;
        }
        MachineUpgradeType display = type.getDisplayType();
        return display != null ? display : type;
    }

    public static void dropInventory(Level level, BlockPos pos, ItemStackHandler inventory) {
        if (level == null || inventory == null || level.isClientSide) {
            return;
        }
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    public static List<Component> getInstalledUpgrades(BlockEntity blockEntity) {
        List<Component> lines = new ArrayList<>();
        if (!(blockEntity instanceof MachineUpgradeHost host)) {
            return lines;
        }

        Map<MachineUpgradeType, Integer> counts = getCanonicalUpgradeCounts(host.getUpgradeInventory());
        for (MachineUpgradeType type : FINAL_ORDER) {
            int count = counts.getOrDefault(type, 0);
            if (count > 0) {
                int effective = getEffectiveCount(host, type, getUpgradeCap(type));
                lines.add(Component.translatable("item.microtech." + type.getId() + "_chip")
                        .append(Component.literal(" x" + count))
                        .append(Component.literal(" (eff " + effective + ")")));
            }
        }
        return lines;
    }

    public static List<Component> getUpgradeEffectSummary(BlockEntity blockEntity) {
        List<Component> lines = new ArrayList<>();
        if (!(blockEntity instanceof MachineUpgradeHost host)) {
            return lines;
        }

        Map<MachineUpgradeType, Integer> counts = getCanonicalUpgradeCounts(host.getUpgradeInventory());
        appendIfPresent(lines, "Speed", counts.getOrDefault(MachineUpgradeType.SPEED, 0), getSpeedSummary(host));
        appendIfPresent(lines, "Efficiency", counts.getOrDefault(MachineUpgradeType.EFFICIENCY, 0), getEfficiencySummary(host));
        appendIfPresent(lines, "Input", counts.getOrDefault(MachineUpgradeType.INPUT, 0), getInputSummary(host));
        appendIfPresent(lines, "Output", counts.getOrDefault(MachineUpgradeType.OUTPUT, 0), getOutputSummary(host));
        appendIfPresent(lines, "Range", counts.getOrDefault(MachineUpgradeType.RANGE, 0), getRangeSummary(host));
        appendIfPresent(lines, "Area", counts.getOrDefault(MachineUpgradeType.AREA, 0), getAreaSummary(host));
        appendIfPresent(lines, "Filter", counts.getOrDefault(MachineUpgradeType.FILTER, 0), getFilterSummary(host));
        appendIfPresent(lines, "Fortune", counts.getOrDefault(MachineUpgradeType.FORTUNE, 0), getFortuneSummary(host));
        appendIfPresent(lines, "Solar Focus", counts.getOrDefault(MachineUpgradeType.SOLAR_FOCUS, 0), getSolarSummary(host));
        return lines;
    }

    public static List<Component> getUpgradeTooltipDetails(MachineUpgradeType type) {
        List<Component> lines = new ArrayList<>();
        MachineUpgradeType resolvedType = displayType(type);
        if (resolvedType == null) {
            return lines;
        }

        switch (resolvedType) {
            case SPEED -> {
                lines.add(Component.literal("Stacks increase speed by 12% each."));
                lines.add(Component.literal("32 chips = 4.84x speed, clamped to 32 used chips."));
            }
            case EFFICIENCY -> {
                lines.add(Component.literal("Stacks reduce energy cost by 3.5% each."));
                lines.add(Component.literal("32 chips = 20% of base cost."));
            }
            case INPUT -> {
                lines.add(Component.literal("Stacks increase pull size and shorten the interval."));
                lines.add(Component.literal("64 chips = up to 64 items every 4 ticks."));
            }
            case OUTPUT -> {
                lines.add(Component.literal("Stacks increase push size and shorten the interval."));
                lines.add(Component.literal("64 chips = up to 64 items every 4 ticks."));
            }
            case RANGE -> {
                lines.add(Component.literal("Stacks extend mine range by +2 radius and +4 depth each."));
                lines.add(Component.literal("16 chips = radius 38, depth 72."));
            }
            case AREA -> {
                lines.add(Component.literal("Stacks increase scan targets by 32 each."));
                lines.add(Component.literal("32 chips = up to 1088 targets."));
            }
            case FILTER -> {
                lines.add(Component.literal("Unlocks the Tech Miner allowlist filter."));
                lines.add(Component.literal("Tier capacity: T1=1, T2=3, T3=6, T4=9 entries."));
            }
            case FILTER_UPGRADE_T1, FILTER_UPGRADE_T2, FILTER_UPGRADE_T3, FILTER_UPGRADE_T4 -> {
                lines.add(Component.literal("Unlocks the Tech Miner allowlist filter."));
                lines.add(Component.literal("Only the highest installed tier is used."));
            }
            case FORTUNE -> {
                lines.add(Component.literal("Crusher: +1 output per 8 chips, with a small remainder chance."));
                lines.add(Component.literal("64 chips = +8 guaranteed outputs in Crusher, +4 in Furnace."));
            }
            case SOLAR_FOCUS -> {
                lines.add(Component.literal("Solar output scales by 15% per chip, up to 6x."));
                lines.add(Component.literal("Night output unlocks at 8/16/32 chips."));
            }
            default -> {
            }
        }
        return lines;
    }

    private static void appendIfPresent(List<Component> lines, String label, int count, String detail) {
        if (count > 0) {
            lines.add(Component.literal(String.format(Locale.ROOT, " - %s x%d (%s)", label, count, detail)));
        }
    }

    private static Map<MachineUpgradeType, Integer> getCanonicalUpgradeCounts(ItemStackHandler inventory) {
        Map<MachineUpgradeType, Integer> counts = new EnumMap<>(MachineUpgradeType.class);
        if (inventory == null) {
            return counts;
        }

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof MachineUpgradeItem upgradeItem)) {
                continue;
            }

            MachineUpgradeType canonical = canonicalType(upgradeItem.getUpgradeType());
            if (canonical == null || !canonical.isVisibleInCreative()) {
                continue;
            }

            counts.merge(canonical, stack.getCount(), Integer::sum);
        }

        return counts;
    }

    private static String getSpeedSummary(MachineUpgradeHost host) {
        int count = getInstalledCount(host, MachineUpgradeType.SPEED);
        int effective = getEffectiveCount(host, MachineUpgradeType.SPEED, SPEED_EFFECTIVE_MAX);
        return String.format(Locale.ROOT, "raw %d, eff %d, %.2fx speed", count, effective, 1.0D + (effective * 0.12D));
    }

    private static String getEfficiencySummary(MachineUpgradeHost host) {
        int count = getInstalledCount(host, MachineUpgradeType.EFFICIENCY);
        int effective = getEffectiveCount(host, MachineUpgradeType.EFFICIENCY, EFFICIENCY_EFFECTIVE_MAX);
        return String.format(Locale.ROOT, "raw %d, eff %d, %.0f%% cost", count, effective, Math.max(20.0D, (1.0D - (effective * 0.035D)) * 100.0D));
    }

    private static String getInputSummary(MachineUpgradeHost host) {
        int count = getInstalledCount(host, MachineUpgradeType.INPUT);
        int effective = getEffectiveCount(host, MachineUpgradeType.INPUT, INPUT_EFFECTIVE_MAX);
        return String.format(Locale.ROOT, "raw %d, eff %d, %d items / %dt", count, effective, Math.min(64, 1 + effective), Math.max(2, 20 - (effective / 4)));
    }

    private static String getOutputSummary(MachineUpgradeHost host) {
        int count = getInstalledCount(host, MachineUpgradeType.OUTPUT);
        int effective = getEffectiveCount(host, MachineUpgradeType.OUTPUT, OUTPUT_EFFECTIVE_MAX);
        return String.format(Locale.ROOT, "raw %d, eff %d, %d items / %dt", count, effective, Math.min(64, 1 + effective), Math.max(2, 20 - (effective / 4)));
    }

    private static String getRangeSummary(MachineUpgradeHost host) {
        int count = getInstalledCount(host, MachineUpgradeType.RANGE);
        int effective = getEffectiveCount(host, MachineUpgradeType.RANGE, RANGE_EFFECTIVE_MAX);
        return String.format(Locale.ROOT, "raw %d, eff %d, radius +%d / depth +%d", count, effective, Math.min(32, effective * 2), Math.min(64, effective * 4));
    }

    private static String getAreaSummary(MachineUpgradeHost host) {
        int count = getInstalledCount(host, MachineUpgradeType.AREA);
        int effective = getEffectiveCount(host, MachineUpgradeType.AREA, AREA_EFFECTIVE_MAX);
        return String.format(Locale.ROOT, "raw %d, eff %d, max targets %d", count, effective, Math.min(MINER_SCAN_MAX_TARGETS, MINER_SCAN_BASE_TARGETS + Math.min(1024, effective * 32)));
    }

    private static String getFilterSummary(MachineUpgradeHost host) {
        int tier = getFilterTier(host.getUpgradeInventory());
        int capacity = getFilterCapacityFromTier(tier);
        return String.format(Locale.ROOT, "tier %d, capacity %d filter entries", tier, capacity);
    }

    private static String getFortuneSummary(MachineUpgradeHost host) {
        int count = getInstalledCount(host, MachineUpgradeType.FORTUNE);
        int effective = getEffectiveCount(host, MachineUpgradeType.FORTUNE, FORTUNE_EFFECTIVE_MAX);
        int guaranteedCrusherBonus = effective / 8;
        int furnaceBonus = getElectricFurnaceFortuneBonus(effective);
        return String.format(Locale.ROOT, "raw %d, eff %d, Crusher +%d base, Furnace +%d", count, effective, guaranteedCrusherBonus, furnaceBonus);
    }

    private static String getSolarSummary(MachineUpgradeHost host) {
        int count = getInstalledCount(host, MachineUpgradeType.SOLAR_FOCUS);
        int effective = getEffectiveCount(host, MachineUpgradeType.SOLAR_FOCUS, SOLAR_EFFECTIVE_MAX);
        return String.format(Locale.ROOT, "raw %d, eff %d, %.2fx day / %.0f%% night", count, effective, Math.min(6.0D, 1.0D + (effective * 0.15D)), getNightStorageSummaryPercent(effective));
    }

    private static int getElectricFurnaceFortuneBonus(int count) {
        if (count >= 64) {
            return 4;
        }
        if (count >= 32) {
            return 3;
        }
        if (count >= 16) {
            return 2;
        }
        if (count >= 8) {
            return 1;
        }
        return 0;
    }

    private static int getNightStorageSummaryPercent(int count) {
        if (count >= 32) {
            return 35;
        }
        if (count >= 16) {
            return 20;
        }
        if (count >= 8) {
            return 10;
        }
        return 0;
    }

    public static int getUpgradeCap(MachineUpgradeType type) {
        MachineUpgradeType canonical = canonicalType(type);
        if (canonical == null) {
            return 0;
        }
        return switch (canonical) {
            case SPEED -> SPEED_EFFECTIVE_MAX;
            case EFFICIENCY -> EFFICIENCY_EFFECTIVE_MAX;
            case INPUT -> INPUT_EFFECTIVE_MAX;
            case OUTPUT -> OUTPUT_EFFECTIVE_MAX;
            case RANGE -> RANGE_EFFECTIVE_MAX;
            case AREA -> AREA_EFFECTIVE_MAX;
            case FILTER -> FILTER_EFFECTIVE_MAX;
            case FORTUNE -> FORTUNE_EFFECTIVE_MAX;
            case SOLAR_FOCUS -> SOLAR_EFFECTIVE_MAX;
            default -> canonical.getMaxLevel();
        };
    }

    public static String getMachineId(BlockEntity blockEntity) {
        if (blockEntity instanceof MachineUpgradeHost host) {
            return host.getMachineUpgradeId();
        }
        if (blockEntity instanceof TechMinerBlockEntity) return "microtech:tech_miner";
        if (blockEntity instanceof TechCrusherBlockEntity) return "microtech:tech_crusher";
        if (blockEntity instanceof ElectricFurnaceBlockEntity) return "microtech:electric_furnace_t1";
        if (blockEntity instanceof BatteryT2BlockEntity) return "microtech:battery_t2";
        if (blockEntity instanceof BasicMachineBlockEntity) return "microtech:energy_converter_t1";
        if (blockEntity instanceof SolarPanelBlockEntity) return "microtech:solar_panel_t1";
        if (blockEntity instanceof EvoTableBlockEntity) return "microtech:evo_table";
        return "";
    }
}
