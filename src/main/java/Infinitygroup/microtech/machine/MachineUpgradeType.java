package Infinitygroup.microtech.machine;

import java.util.List;

public enum MachineUpgradeType {
    SPEED("speed", MachineUpgradeCategory.UNIVERSAL, 3, false, "tooltip.microtech.machine_upgrade.speed", "microtech:tech_miner", "microtech:tech_crusher", "microtech:electric_furnace_t1", "microtech:battery_t2"),
    EFFICIENCY("efficiency", MachineUpgradeCategory.UNIVERSAL, 3, false, "tooltip.microtech.machine_upgrade.efficiency", "microtech:tech_miner", "microtech:tech_crusher", "microtech:electric_furnace_t1", "microtech:energy_converter_t1", "microtech:battery_t2"),
    INPUT("input", MachineUpgradeCategory.UNIVERSAL, 3, false, "tooltip.microtech.machine_upgrade.input", "microtech:tech_crusher", "microtech:electric_furnace_t1", "microtech:battery_t2"),
    OUTPUT("output", MachineUpgradeCategory.UNIVERSAL, 3, false, "tooltip.microtech.machine_upgrade.output", "microtech:tech_miner", "microtech:tech_crusher", "microtech:electric_furnace_t1", "microtech:battery_t2"),
    RANGE("range", MachineUpgradeCategory.MINING, 3, false, "tooltip.microtech.machine_upgrade.range", "microtech:tech_miner"),
    AREA("area", MachineUpgradeCategory.MINING, 3, false, "tooltip.microtech.machine_upgrade.area", "microtech:tech_miner"),
    FILTER("filter", MachineUpgradeCategory.MINING, 3, false, "tooltip.microtech.machine_upgrade.filter", "microtech:tech_miner"),
    FILTER_UPGRADE_T1("filter_upgrade_t1", MachineUpgradeCategory.MINING, 1, false, "tooltip.microtech.machine_upgrade.filter_upgrade_t1", "microtech:tech_miner"),
    FILTER_UPGRADE_T2("filter_upgrade_t2", MachineUpgradeCategory.MINING, 1, false, "tooltip.microtech.machine_upgrade.filter_upgrade_t2", "microtech:tech_miner"),
    FILTER_UPGRADE_T3("filter_upgrade_t3", MachineUpgradeCategory.MINING, 1, false, "tooltip.microtech.machine_upgrade.filter_upgrade_t3", "microtech:tech_miner"),
    FILTER_UPGRADE_T4("filter_upgrade_t4", MachineUpgradeCategory.MINING, 1, false, "tooltip.microtech.machine_upgrade.filter_upgrade_t4", "microtech:tech_miner"),
    FORTUNE("fortune", MachineUpgradeCategory.PROCESSING, 3, false, "tooltip.microtech.machine_upgrade.fortune", "microtech:tech_crusher", "microtech:electric_furnace_t1"),
    SOLAR_FOCUS("solar_focus", MachineUpgradeCategory.GENERATION, 3, false, "tooltip.microtech.machine_upgrade.solar_focus", "microtech:energy_converter_t1", "microtech:solar_panel_t1"),
    CAPACITY("capacity", MachineUpgradeCategory.UNIVERSAL, 3, true, "tooltip.microtech.machine_upgrade.capacity", "microtech:tech_miner", "microtech:tech_crusher", "microtech:electric_furnace_t1", "microtech:energy_converter_t1", "microtech:battery_t2"),
    TRANSFER("transfer", MachineUpgradeCategory.UNIVERSAL, 3, true, "tooltip.microtech.machine_upgrade.transfer", "microtech:tech_miner", "microtech:tech_crusher", "microtech:electric_furnace_t1", "microtech:energy_converter_t1", "microtech:solar_panel_t1", "microtech:battery_t2"),
    AUTO_INPUT("auto_input", MachineUpgradeCategory.UNIVERSAL, 3, true, "tooltip.microtech.machine_upgrade.auto_input", "microtech:tech_crusher", "microtech:electric_furnace_t1", "microtech:battery_t2"),
    AUTO_OUTPUT("auto_output", MachineUpgradeCategory.UNIVERSAL, 3, true, "tooltip.microtech.machine_upgrade.auto_output", "microtech:tech_miner", "microtech:tech_crusher", "microtech:electric_furnace_t1", "microtech:battery_t2"),
    SILENCE("silence", MachineUpgradeCategory.UNIVERSAL, 3, true, "tooltip.microtech.machine_upgrade.silence", "microtech:tech_miner", "microtech:tech_crusher", "microtech:electric_furnace_t1", "microtech:energy_converter_t1", "microtech:solar_panel_t1", "microtech:battery_t2"),
    DEPTH("depth", MachineUpgradeCategory.MINING, 3, true, "tooltip.microtech.machine_upgrade.depth", "microtech:tech_miner"),
    PRIORITY("priority", MachineUpgradeCategory.MINING, 3, true, "tooltip.microtech.machine_upgrade.priority", "microtech:tech_miner"),
    YIELD("yield", MachineUpgradeCategory.PROCESSING, 3, true, "tooltip.microtech.machine_upgrade.yield", "microtech:tech_crusher"),
    FINE_DUST("fine_dust", MachineUpgradeCategory.PROCESSING, 3, true, "tooltip.microtech.machine_upgrade.fine_dust", "microtech:tech_crusher"),
    SMELT_BOOST("smelt_boost", MachineUpgradeCategory.PROCESSING, 3, true, "tooltip.microtech.machine_upgrade.smelt_boost", "microtech:electric_furnace_t1"),
    HEAT_CONTROL("heat_control", MachineUpgradeCategory.PROCESSING, 3, true, "tooltip.microtech.machine_upgrade.heat_control", "microtech:electric_furnace_t1"),
    DOUBLE_SMELT("double_smelt", MachineUpgradeCategory.PROCESSING, 3, true, "tooltip.microtech.machine_upgrade.double_smelt", "microtech:electric_furnace_t1"),
    CHARGE_SPEED("charge_speed", MachineUpgradeCategory.ENERGY_STORAGE, 3, true, "tooltip.microtech.machine_upgrade.charge_speed", "microtech:battery_t2"),
    WIRELESS_CHARGE("wireless_charge", MachineUpgradeCategory.ENERGY_STORAGE, 3, true, "tooltip.microtech.machine_upgrade.wireless_charge", "microtech:battery_t2"),
    EQUIPMENT_PRIORITY("equipment_priority", MachineUpgradeCategory.ENERGY_STORAGE, 3, true, "tooltip.microtech.machine_upgrade.equipment_priority", "microtech:battery_t2"),
    GENERATION("generation", MachineUpgradeCategory.GENERATION, 3, true, "tooltip.microtech.machine_upgrade.generation", "microtech:energy_converter_t1", "microtech:solar_panel_t1"),
    FUEL_EFFICIENCY("fuel_efficiency", MachineUpgradeCategory.GENERATION, 3, true, "tooltip.microtech.machine_upgrade.fuel_efficiency", "microtech:energy_converter_t1"),
    NIGHT_STORAGE("night_storage", MachineUpgradeCategory.GENERATION, 3, true, "tooltip.microtech.machine_upgrade.night_storage", "microtech:solar_panel_t1");

    private final String id;
    private final MachineUpgradeCategory category;
    private final int maxLevel;
    private final boolean legacy;
    private final String descriptionKey;
    private final List<String> compatibleMachines;

    MachineUpgradeType(String id, MachineUpgradeCategory category, int maxLevel, boolean legacy, String descriptionKey, String... compatibleMachines) {
        this.id = id;
        this.category = category;
        this.maxLevel = maxLevel;
        this.legacy = legacy;
        this.descriptionKey = descriptionKey;
        this.compatibleMachines = List.of(compatibleMachines);
    }

    public String getId() {
        return this.id;
    }

    public MachineUpgradeCategory getCategory() {
        return this.category;
    }

    public int getMaxLevel() {
        return this.maxLevel;
    }

    public boolean isLegacy() {
        return this.legacy;
    }

    public boolean isVisibleInCreative() {
        return !this.legacy;
    }

    public String getDescriptionKey() {
        return this.descriptionKey;
    }

    public List<String> getCompatibleMachines() {
        return this.compatibleMachines;
    }

    public boolean supportsMachine(String machineId) {
        return this.compatibleMachines.contains(machineId);
    }

    public MachineUpgradeType getDisplayType() {
        MachineUpgradeType canonical = this.getCanonicalType();
        return canonical != null ? canonical : this;
    }

    public MachineUpgradeType getCanonicalType() {
        return switch (this) {
            case AUTO_INPUT -> INPUT;
            case AUTO_OUTPUT -> OUTPUT;
            case DEPTH -> RANGE;
            case PRIORITY, FILTER_UPGRADE_T1, FILTER_UPGRADE_T2, FILTER_UPGRADE_T3, FILTER_UPGRADE_T4 -> FILTER;
            case YIELD, FINE_DUST, DOUBLE_SMELT -> FORTUNE;
            case SMELT_BOOST, CHARGE_SPEED -> SPEED;
            case HEAT_CONTROL -> EFFICIENCY;
            case GENERATION, FUEL_EFFICIENCY, NIGHT_STORAGE -> SOLAR_FOCUS;
            case CAPACITY, TRANSFER, SILENCE, WIRELESS_CHARGE, EQUIPMENT_PRIORITY -> null;
            default -> this;
        };
    }
}
