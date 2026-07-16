package Infinitygroup.microtech.item;

import Infinitygroup.microtech.block.entity.BasicMachineBlockEntity;
import Infinitygroup.microtech.block.entity.BatteryBlockEntity;
import Infinitygroup.microtech.block.entity.BatteryT2BlockEntity;
import Infinitygroup.microtech.block.entity.ElectricFurnaceBlockEntity;
import Infinitygroup.microtech.block.entity.TechMinerBlockEntity;
import Infinitygroup.microtech.energy.EnergyNetworkHelper;
import Infinitygroup.microtech.block.entity.SolarPanelBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class MicroTechMachineBlockItem extends BlockItem {
    private final TooltipProfile profile;

    public MicroTechMachineBlockItem(Block block, Properties properties, TooltipProfile profile) {
        super(block, properties);
        this.profile = profile;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        switch (this.profile) {
            case BATTERY_T1 -> addBatteryTooltip(stack, tooltip, false);
            case BATTERY_T2 -> addBatteryTooltip(stack, tooltip, true);
            case CABLE_T1 -> addCableTooltip(tooltip);
            case ENERGY_CONVERTER_T1 -> addEnergyConverterTooltip(tooltip);
            case SOLAR_PANEL_T1 -> addSolarTooltip(tooltip);
            case EVO_TABLE -> addEvoTableTooltip(tooltip);
            case TECH_TABLE -> addTechTableTooltip(tooltip);
            case ELECTRIC_FURNACE_T1 -> addFurnaceTooltip(tooltip);
            case TECH_MINER -> addTechMinerTooltip(stack, tooltip);
        }

        MicroTechTooltipHelper.addHoldShiftHint(tooltip);
        if (MicroTechTooltipHelper.isShiftDown()) {
            addShiftDetails(tooltip);
        }
    }

    private static void addBatteryTooltip(ItemStack stack, List<Component> tooltip, boolean tier2) {
        MicroTechTooltipHelper.addHeader(tooltip, "tooltip.microtech.battery.type");
        int maxEnergy = tier2 ? BatteryT2BlockEntity.MAX_ENERGY : BatteryBlockEntity.MAX_ENERGY;
        int maxReceive = tier2 ? BatteryT2BlockEntity.MAX_RECEIVE : BatteryBlockEntity.MAX_RECEIVE;
        int maxExtract = tier2 ? BatteryT2BlockEntity.MAX_EXTRACT : BatteryBlockEntity.MAX_EXTRACT;
        tooltip.add(Component.translatable("tooltip.microtech.battery.tier", tier2 ? "T2" : "T1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.microtech.battery.capacity",
                MicroTechTooltipHelper.formatFE(maxEnergy)
        ).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(
                "tooltip.microtech.battery.input",
                MicroTechTooltipHelper.formatFE(maxReceive)
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.microtech.battery.output",
                MicroTechTooltipHelper.formatFE(maxExtract)
        ).withStyle(ChatFormatting.GRAY));

        if (stack.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA)) {
            tooltip.add(Component.translatable(
                    "tooltip.microtech.battery.stored",
                    MicroTechTooltipHelper.formatFE(tier2 ? BatteryT2BlockEntity.getEnergyFromStack(stack) : BatteryBlockEntity.getEnergyFromStack(stack)),
                    MicroTechTooltipHelper.formatFE(maxEnergy)
            ).withStyle(ChatFormatting.AQUA));
        }
    }

    private static void addCableTooltip(List<Component> tooltip) {
        MicroTechTooltipHelper.addHeader(tooltip, "tooltip.microtech.cable.type");
        tooltip.add(Component.translatable("tooltip.microtech.cable.tier", "T1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.microtech.cable.transfer",
                MicroTechTooltipHelper.formatFE(EnergyNetworkHelper.CABLE_T1_TRANSFER_PER_CABLE)
        ).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(
                "tooltip.microtech.cable.network_limit",
                MicroTechTooltipHelper.formatFE(EnergyNetworkHelper.CABLE_T1_MAX_NETWORK_TRANSFER)
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.microtech.cable.network_size",
                MicroTechTooltipHelper.formatFE(EnergyNetworkHelper.MAX_NETWORK_CABLES)
        ).withStyle(ChatFormatting.GRAY));
    }

    private static void addEnergyConverterTooltip(List<Component> tooltip) {
        MicroTechTooltipHelper.addHeader(tooltip, "tooltip.microtech.energy_converter.type");
        tooltip.add(Component.translatable("tooltip.microtech.energy_converter.tier", "T1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.microtech.energy_converter.generate",
                MicroTechTooltipHelper.formatFE(BasicMachineBlockEntity.ENERGY_PER_TICK)
        ).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(
                "tooltip.microtech.energy_converter.capacity",
                MicroTechTooltipHelper.formatFE(BasicMachineBlockEntity.MAX_ENERGY)
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.energy_converter.fuel_based").withStyle(ChatFormatting.GRAY));
    }

    private static void addSolarTooltip(List<Component> tooltip) {
        MicroTechTooltipHelper.addHeader(tooltip, "tooltip.microtech.solar_panel.type");
        tooltip.add(Component.translatable("tooltip.microtech.solar_panel.tier", "T1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.microtech.solar_panel.generate",
                MicroTechTooltipHelper.formatFE(SolarPanelBlockEntity.DAY_GENERATION)
        ).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.microtech.solar_panel.sky_access").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.microtech.solar_panel.capacity",
                MicroTechTooltipHelper.formatFE(SolarPanelBlockEntity.MAX_ENERGY)
        ).withStyle(ChatFormatting.GRAY));
    }

    private static void addEvoTableTooltip(List<Component> tooltip) {
        MicroTechTooltipHelper.addHeader(tooltip, "tooltip.microtech.evo_table.type");
        tooltip.add(Component.translatable("tooltip.microtech.evo_table.tier", "T1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.evo_table.use").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("tooltip.microtech.evo_table.supports").withStyle(ChatFormatting.GRAY));
    }

    private static void addTechTableTooltip(List<Component> tooltip) {
        MicroTechTooltipHelper.addHeader(tooltip, "tooltip.microtech.tech_table.type");
        tooltip.add(Component.translatable("tooltip.microtech.tech_table.tier").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.tech_table.input_output").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("tooltip.microtech.tech_table.hammering").withStyle(ChatFormatting.GRAY));
    }

    private static void addFurnaceTooltip(List<Component> tooltip) {
        MicroTechTooltipHelper.addHeader(tooltip, "tooltip.microtech.electric_furnace.type");
        tooltip.add(Component.translatable("tooltip.microtech.electric_furnace.tier", "T1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.microtech.electric_furnace.capacity",
                MicroTechTooltipHelper.formatFE(ElectricFurnaceBlockEntity.MAX_ENERGY)
        ).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(
                "tooltip.microtech.electric_furnace.input",
                MicroTechTooltipHelper.formatFE(ElectricFurnaceBlockEntity.MAX_RECEIVE)
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.electric_furnace.smelts_using_energy").withStyle(ChatFormatting.GRAY));
    }

    private static void addTechMinerTooltip(ItemStack stack, List<Component> tooltip) {
        MicroTechTooltipHelper.addHeader(tooltip, "tooltip.microtech.tech_miner.type");
        tooltip.add(Component.translatable("tooltip.microtech.tech_miner.tier").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.microtech.tech_miner.energy",
                MicroTechTooltipHelper.formatFE(TechMinerBlockEntity.MAX_ENERGY)
        ).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                "tooltip.microtech.tech_miner.scan_cost",
                MicroTechTooltipHelper.formatFE(TechMinerBlockEntity.SCAN_COST)
        ).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(
                "tooltip.microtech.tech_miner.mining_cost",
                MicroTechTooltipHelper.formatFE(TechMinerBlockEntity.MINE_COST)
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.microtech.tech_miner.area",
                MicroTechTooltipHelper.formatFE(TechMinerBlockEntity.SCAN_RADIUS),
                MicroTechTooltipHelper.formatFE(TechMinerBlockEntity.SCAN_DEPTH)
        ).withStyle(ChatFormatting.GRAY));

        if (stack.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA)) {
            tooltip.add(Component.translatable(
                    "tooltip.microtech.tech_miner.stored",
                    MicroTechTooltipHelper.formatFE(TechMinerBlockEntity.getEnergyFromStack(stack)),
                    MicroTechTooltipHelper.formatFE(TechMinerBlockEntity.MAX_ENERGY)
            ).withStyle(ChatFormatting.AQUA));
        }
    }

    private void addShiftDetails(List<Component> tooltip) {
        switch (this.profile) {
            case BATTERY_T1 -> {
                tooltip.add(Component.translatable("tooltip.microtech.battery.shift_1").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.microtech.battery.shift_2").withStyle(ChatFormatting.DARK_GRAY));
            }
            case CABLE_T1 -> {
                tooltip.add(Component.translatable("tooltip.microtech.cable.shift_1").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.microtech.cable.shift_2").withStyle(ChatFormatting.DARK_GRAY));
            }
            case ENERGY_CONVERTER_T1 -> tooltip.add(Component.translatable("tooltip.microtech.energy_converter.shift").withStyle(ChatFormatting.DARK_GRAY));
            case SOLAR_PANEL_T1 -> tooltip.add(Component.translatable("tooltip.microtech.solar_panel.shift").withStyle(ChatFormatting.DARK_GRAY));
            case EVO_TABLE -> tooltip.add(Component.translatable("tooltip.microtech.evo_table.shift").withStyle(ChatFormatting.DARK_GRAY));
            case TECH_TABLE -> {
                tooltip.add(Component.translatable("tooltip.microtech.tech_table.shift_1").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.microtech.tech_table.shift_2").withStyle(ChatFormatting.DARK_GRAY));
            }
            case ELECTRIC_FURNACE_T1 -> tooltip.add(Component.translatable("tooltip.microtech.electric_furnace.shift").withStyle(ChatFormatting.DARK_GRAY));
            case TECH_MINER -> {
                tooltip.add(Component.translatable("tooltip.microtech.tech_miner.shift_1").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.microtech.tech_miner.shift_2").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    public enum TooltipProfile {
        BATTERY_T1,
        BATTERY_T2,
        CABLE_T1,
        ENERGY_CONVERTER_T1,
        SOLAR_PANEL_T1,
        EVO_TABLE,
        TECH_TABLE,
        ELECTRIC_FURNACE_T1,
        TECH_MINER
    }
}
