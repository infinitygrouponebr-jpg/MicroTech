package Infinitygroup.microtech;

import Infinitygroup.microtech.block.BasicMachineBlock;
import Infinitygroup.microtech.block.CableBlock;
import Infinitygroup.microtech.block.BatteryBlock;
import Infinitygroup.microtech.block.BatteryT2Block;
import Infinitygroup.microtech.block.EvoTableBlock;
import Infinitygroup.microtech.block.ElectricFurnaceBlock;
import Infinitygroup.microtech.block.SolarPanelBlock;
import Infinitygroup.microtech.block.TechMinerBlock;
import Infinitygroup.microtech.block.TechCrusherBlock;
import Infinitygroup.microtech.block.TechTableBlock;
import Infinitygroup.microtech.block.entity.BasicMachineBlockEntity;
import Infinitygroup.microtech.block.entity.CableBlockEntity;
import Infinitygroup.microtech.block.entity.BatteryBlockEntity;
import Infinitygroup.microtech.block.entity.BatteryT2BlockEntity;
import Infinitygroup.microtech.block.entity.EvoTableBlockEntity;
import Infinitygroup.microtech.block.entity.ElectricFurnaceBlockEntity;
import Infinitygroup.microtech.block.entity.SolarPanelBlockEntity;
import Infinitygroup.microtech.block.entity.TechMinerBlockEntity;
import Infinitygroup.microtech.block.entity.TechCrusherBlockEntity;
import Infinitygroup.microtech.block.entity.TechTableBlockEntity;
import Infinitygroup.microtech.item.TechArmorItem;
import Infinitygroup.microtech.item.TechArmorEnergyStorage;
import Infinitygroup.microtech.item.TechArmorMaterial;
import Infinitygroup.microtech.item.TechArmorFlightHandler;
import Infinitygroup.microtech.item.MicroTechMachineBlockItem;
import Infinitygroup.microtech.item.GraviteEnergyStorage;
import Infinitygroup.microtech.item.GraviteItem;
import Infinitygroup.microtech.item.TechSwordEnergyStorage;
import Infinitygroup.microtech.item.TechChipItem;
import Infinitygroup.microtech.item.TechChipType;
import Infinitygroup.microtech.item.TechFlightChipItem;
import Infinitygroup.microtech.item.TechSwordItem;
import Infinitygroup.microtech.item.TechSwordData;
import Infinitygroup.microtech.item.EnergyDebugToolItem;
import Infinitygroup.microtech.item.ControllerChipItem;
import Infinitygroup.microtech.item.AdvancedControllerChipItem;
import Infinitygroup.microtech.item.TechMinerBlockItem;
import Infinitygroup.microtech.item.TechCrusherBlockItem;
import Infinitygroup.microtech.item.TechTableBlockItem;
import Infinitygroup.microtech.entity.control.ControlledMobEvents;
import Infinitygroup.microtech.entity.control.ControlledMobDebugCommand;
import Infinitygroup.microtech.machine.MachineUpgradeItem;
import Infinitygroup.microtech.machine.MachineUpgradeType;
import Infinitygroup.microtech.network.TechMinerFilterPayload;
import Infinitygroup.microtech.network.TechSwordAbilitySelectionPayload;
import Infinitygroup.microtech.client.screen.BasicMachineScreen;
import Infinitygroup.microtech.client.screen.BatteryScreen;
import Infinitygroup.microtech.client.screen.BatteryT2Screen;
import Infinitygroup.microtech.client.screen.EvoTableScreen;
import Infinitygroup.microtech.client.screen.ElectricFurnaceScreen;
import Infinitygroup.microtech.client.screen.SolarPanelScreen;
import Infinitygroup.microtech.client.screen.TechMinerFilterScreen;
import Infinitygroup.microtech.client.screen.TechMinerOutputScreen;
import Infinitygroup.microtech.client.screen.TechMinerScreen;
import Infinitygroup.microtech.client.screen.TechCrusherScreen;
import Infinitygroup.microtech.client.TechArmorClientConfig;
import Infinitygroup.microtech.client.renderer.blockentity.ElectricFurnaceRenderer;
import Infinitygroup.microtech.client.renderer.blockentity.TechTableRenderer;
import Infinitygroup.microtech.menu.BasicMachineMenu;
import Infinitygroup.microtech.menu.BatteryMenu;
import Infinitygroup.microtech.menu.BatteryT2Menu;
import Infinitygroup.microtech.menu.EvoTableMenu;
import Infinitygroup.microtech.menu.ElectricFurnaceMenu;
import Infinitygroup.microtech.menu.SolarPanelMenu;
import Infinitygroup.microtech.menu.TechMinerFilterMenu;
import Infinitygroup.microtech.menu.TechMinerMenu;
import Infinitygroup.microtech.menu.TechMinerOutputMenu;
import Infinitygroup.microtech.menu.TechCrusherMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Microtech.MODID)
public class Microtech {
    public static final String MODID = "microtech";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<BasicMachineBlock> ENERGY_CONVERTER_T1 = BLOCKS.register("energy_converter_t1",
            () -> new BasicMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(4.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> ENERGY_CONVERTER_T1_ITEM = ITEMS.register("energy_converter_t1",
            () -> new MicroTechMachineBlockItem(ENERGY_CONVERTER_T1.get(), new net.minecraft.world.item.Item.Properties(), MicroTechMachineBlockItem.TooltipProfile.ENERGY_CONVERTER_T1));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BasicMachineBlockEntity>> ENERGY_CONVERTER_T1_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("energy_converter_t1",
            () -> BlockEntityType.Builder.of(BasicMachineBlockEntity::new, ENERGY_CONVERTER_T1.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<BasicMachineMenu>> ENERGY_CONVERTER_T1_MENU = MENUS.register("energy_converter_t1_menu",
            () -> new MenuType<>(BasicMachineMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredBlock<CableBlock> CABLE_T1 = BLOCKS.register("cable_t1",
            () -> new CableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> CABLE_T1_ITEM = ITEMS.register("cable_t1",
            () -> new MicroTechMachineBlockItem(CABLE_T1.get(), new net.minecraft.world.item.Item.Properties(), MicroTechMachineBlockItem.TooltipProfile.CABLE_T1));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> CABLE_T1_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("cable_t1",
            () -> BlockEntityType.Builder.of(CableBlockEntity::new, CABLE_T1.get()).build(null));

    public static final DeferredBlock<BatteryBlock> BATTERY_T1 = BLOCKS.register("battery_t1",
            () -> new BatteryBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> BATTERY_T1_ITEM = ITEMS.register("battery_t1",
            () -> new MicroTechMachineBlockItem(BATTERY_T1.get(), new net.minecraft.world.item.Item.Properties(), MicroTechMachineBlockItem.TooltipProfile.BATTERY_T1));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> BATTERY_T1_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("battery_t1",
            () -> BlockEntityType.Builder.of(BatteryBlockEntity::new, BATTERY_T1.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<BatteryMenu>> BATTERY_T1_MENU = MENUS.register("battery_t1_menu",
            () -> new MenuType<>(BatteryMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredBlock<BatteryT2Block> BATTERY_T2 = BLOCKS.register("battery_t2",
            () -> new BatteryT2Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.5F, 6.5F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> BATTERY_T2_ITEM = ITEMS.register("battery_t2",
            () -> new MicroTechMachineBlockItem(BATTERY_T2.get(), new net.minecraft.world.item.Item.Properties(), MicroTechMachineBlockItem.TooltipProfile.BATTERY_T1));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryT2BlockEntity>> BATTERY_T2_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("battery_t2",
            () -> BlockEntityType.Builder.of(BatteryT2BlockEntity::new, BATTERY_T2.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<BatteryT2Menu>> BATTERY_T2_MENU = MENUS.register("battery_t2_menu",
            () -> new MenuType<>(BatteryT2Menu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredBlock<EvoTableBlock> EVO_TABLE = BLOCKS.register("evo_table",
            () -> new EvoTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> EVO_TABLE_ITEM = ITEMS.register("evo_table",
            () -> new MicroTechMachineBlockItem(EVO_TABLE.get(), new net.minecraft.world.item.Item.Properties(), MicroTechMachineBlockItem.TooltipProfile.EVO_TABLE));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EvoTableBlockEntity>> EVO_TABLE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("evo_table",
            () -> BlockEntityType.Builder.of(EvoTableBlockEntity::new, EVO_TABLE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<EvoTableMenu>> EVO_TABLE_MENU = MENUS.register("evo_table_menu",
            () -> new MenuType<>(EvoTableMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredBlock<TechTableBlock> TECH_TABLE = BLOCKS.register("tech_table",
            () -> new TechTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0F, 5.5F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL)));
    public static final DeferredItem<TechTableBlockItem> TECH_TABLE_ITEM = ITEMS.register("tech_table",
            () -> new TechTableBlockItem(TECH_TABLE.get(), new net.minecraft.world.item.Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TechTableBlockEntity>> TECH_TABLE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("tech_table",
            () -> BlockEntityType.Builder.of(TechTableBlockEntity::new, TECH_TABLE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<Infinitygroup.microtech.menu.TechTableMenu>> TECH_TABLE_MENU = MENUS.register("tech_table_menu",
            () -> new MenuType<>(Infinitygroup.microtech.menu.TechTableMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredBlock<ElectricFurnaceBlock> ELECTRIC_FURNACE_T1 = BLOCKS.register("electric_furnace_t1",
            () -> new ElectricFurnaceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5F, 4.5F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> ELECTRIC_FURNACE_T1_ITEM = ITEMS.register("electric_furnace_t1",
            () -> new MicroTechMachineBlockItem(ELECTRIC_FURNACE_T1.get(), new net.minecraft.world.item.Item.Properties(), MicroTechMachineBlockItem.TooltipProfile.ELECTRIC_FURNACE_T1));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_T1_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("electric_furnace_t1",
            () -> BlockEntityType.Builder.of(ElectricFurnaceBlockEntity::new, ELECTRIC_FURNACE_T1.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE_T1_MENU = MENUS.register("electric_furnace_t1_menu",
            () -> new MenuType<>(ElectricFurnaceMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL_T1 = BLOCKS.register("solar_panel_t1",
            () -> new SolarPanelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 4.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> SOLAR_PANEL_T1_ITEM = ITEMS.register("solar_panel_t1",
            () -> new MicroTechMachineBlockItem(SOLAR_PANEL_T1.get(), new net.minecraft.world.item.Item.Properties(), MicroTechMachineBlockItem.TooltipProfile.SOLAR_PANEL_T1));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T1_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("solar_panel_t1",
            () -> BlockEntityType.Builder.of(SolarPanelBlockEntity::new, SOLAR_PANEL_T1.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<SolarPanelMenu>> SOLAR_PANEL_T1_MENU = MENUS.register("solar_panel_t1_menu",
            () -> new MenuType<>(SolarPanelMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredBlock<TechMinerBlock> TECH_MINER = BLOCKS.register("tech_miner",
            () -> new TechMinerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.5F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL)));
    public static final DeferredItem<TechMinerBlockItem> TECH_MINER_ITEM = ITEMS.register("tech_miner",
            () -> new TechMinerBlockItem(TECH_MINER.get(), new net.minecraft.world.item.Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TechMinerBlockEntity>> TECH_MINER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("tech_miner",
            () -> BlockEntityType.Builder.of(TechMinerBlockEntity::new, TECH_MINER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<TechMinerMenu>> TECH_MINER_MENU = MENUS.register("tech_miner_menu",
            () -> new MenuType<>(TechMinerMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<TechMinerOutputMenu>> TECH_MINER_OUTPUT_MENU = MENUS.register("tech_miner_output_menu",
            () -> new MenuType<>(TechMinerOutputMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<TechMinerFilterMenu>> TECH_MINER_FILTER_MENU = MENUS.register("tech_miner_filter_menu",
            () -> new MenuType<>(TechMinerFilterMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredBlock<TechCrusherBlock> TECH_CRUSHER = BLOCKS.register("tech_crusher",
            () -> new TechCrusherBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.2F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL)));
    public static final DeferredItem<TechCrusherBlockItem> TECH_CRUSHER_ITEM = ITEMS.register("tech_crusher",
            () -> new TechCrusherBlockItem(TECH_CRUSHER.get(), new net.minecraft.world.item.Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TechCrusherBlockEntity>> TECH_CRUSHER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("tech_crusher",
            () -> BlockEntityType.Builder.of(TechCrusherBlockEntity::new, TECH_CRUSHER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<TechCrusherMenu>> TECH_CRUSHER_MENU = MENUS.register("tech_crusher_menu",
            () -> new MenuType<>(TechCrusherMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredItem<Item> IRON_DUST = ITEMS.register("iron_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.register("copper_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_DUST = ITEMS.register("gold_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COAL_DUST = ITEMS.register("coal_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LAPIS_DUST = ITEMS.register("lapis_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DIAMOND_DUST = ITEMS.register("diamond_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> EMERALD_DUST = ITEMS.register("emerald_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NETHERITE_DUST = ITEMS.register("netherite_dust",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> IRON_PLATE = ITEMS.register("iron_plate",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_PLATE = ITEMS.register("copper_plate",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_PLATE = ITEMS.register("gold_plate",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<MachineUpgradeItem> SPEED_CHIP = registerUpgradeItem("speed_chip", MachineUpgradeType.SPEED);
    public static final DeferredItem<MachineUpgradeItem> EFFICIENCY_CHIP = registerUpgradeItem("efficiency_chip", MachineUpgradeType.EFFICIENCY);
    public static final DeferredItem<MachineUpgradeItem> INPUT_CHIP = registerUpgradeItem("input_chip", MachineUpgradeType.INPUT);
    public static final DeferredItem<MachineUpgradeItem> OUTPUT_CHIP = registerUpgradeItem("output_chip", MachineUpgradeType.OUTPUT);
    public static final DeferredItem<MachineUpgradeItem> CAPACITY_CHIP = registerUpgradeItem("capacity_chip", MachineUpgradeType.CAPACITY);
    public static final DeferredItem<MachineUpgradeItem> TRANSFER_CHIP = registerUpgradeItem("transfer_chip", MachineUpgradeType.TRANSFER);
    public static final DeferredItem<MachineUpgradeItem> AUTO_INPUT_CHIP = registerUpgradeItem("auto_input_chip", MachineUpgradeType.AUTO_INPUT);
    public static final DeferredItem<MachineUpgradeItem> AUTO_OUTPUT_CHIP = registerUpgradeItem("auto_output_chip", MachineUpgradeType.AUTO_OUTPUT);
    public static final DeferredItem<MachineUpgradeItem> SILENCE_CHIP = registerUpgradeItem("silence_chip", MachineUpgradeType.SILENCE);
    public static final DeferredItem<MachineUpgradeItem> RANGE_CHIP = registerUpgradeItem("range_chip", MachineUpgradeType.RANGE);
    public static final DeferredItem<MachineUpgradeItem> DEPTH_CHIP = registerUpgradeItem("depth_chip", MachineUpgradeType.DEPTH);
    public static final DeferredItem<MachineUpgradeItem> FILTER_CHIP = registerUpgradeItem("filter_chip", MachineUpgradeType.FILTER);
    public static final DeferredItem<MachineUpgradeItem> FILTER_UPGRADE_T1 = registerUpgradeItem("filter_upgrade_t1", MachineUpgradeType.FILTER_UPGRADE_T1, 1);
    public static final DeferredItem<MachineUpgradeItem> FILTER_UPGRADE_T2 = registerUpgradeItem("filter_upgrade_t2", MachineUpgradeType.FILTER_UPGRADE_T2, 1);
    public static final DeferredItem<MachineUpgradeItem> FILTER_UPGRADE_T3 = registerUpgradeItem("filter_upgrade_t3", MachineUpgradeType.FILTER_UPGRADE_T3, 1);
    public static final DeferredItem<MachineUpgradeItem> FILTER_UPGRADE_T4 = registerUpgradeItem("filter_upgrade_t4", MachineUpgradeType.FILTER_UPGRADE_T4, 1);
    public static final DeferredItem<MachineUpgradeItem> PRIORITY_CHIP = registerUpgradeItem("priority_chip", MachineUpgradeType.PRIORITY);
    public static final DeferredItem<MachineUpgradeItem> AREA_CHIP = registerUpgradeItem("area_chip", MachineUpgradeType.AREA);
    public static final DeferredItem<MachineUpgradeItem> YIELD_CHIP = registerUpgradeItem("yield_chip", MachineUpgradeType.YIELD);
    public static final DeferredItem<MachineUpgradeItem> FORTUNE_CHIP = registerUpgradeItem("fortune_chip", MachineUpgradeType.FORTUNE);
    public static final DeferredItem<MachineUpgradeItem> FINE_DUST_CHIP = registerUpgradeItem("fine_dust_chip", MachineUpgradeType.FINE_DUST);
    public static final DeferredItem<MachineUpgradeItem> SMELT_BOOST_CHIP = registerUpgradeItem("smelt_boost_chip", MachineUpgradeType.SMELT_BOOST);
    public static final DeferredItem<MachineUpgradeItem> HEAT_CONTROL_CHIP = registerUpgradeItem("heat_control_chip", MachineUpgradeType.HEAT_CONTROL);
    public static final DeferredItem<MachineUpgradeItem> DOUBLE_SMELT_CHIP = registerUpgradeItem("double_smelt_chip", MachineUpgradeType.DOUBLE_SMELT);
    public static final DeferredItem<MachineUpgradeItem> CHARGE_SPEED_CHIP = registerUpgradeItem("charge_speed_chip", MachineUpgradeType.CHARGE_SPEED);
    public static final DeferredItem<MachineUpgradeItem> WIRELESS_CHARGE_CHIP = registerUpgradeItem("wireless_charge_chip", MachineUpgradeType.WIRELESS_CHARGE);
    public static final DeferredItem<MachineUpgradeItem> EQUIPMENT_PRIORITY_CHIP = registerUpgradeItem("equipment_priority_chip", MachineUpgradeType.EQUIPMENT_PRIORITY);
    public static final DeferredItem<MachineUpgradeItem> GENERATION_CHIP = registerUpgradeItem("generation_chip", MachineUpgradeType.GENERATION);
    public static final DeferredItem<MachineUpgradeItem> FUEL_EFFICIENCY_CHIP = registerUpgradeItem("fuel_efficiency_chip", MachineUpgradeType.FUEL_EFFICIENCY);
    public static final DeferredItem<MachineUpgradeItem> SOLAR_FOCUS_CHIP = registerUpgradeItem("solar_focus_chip", MachineUpgradeType.SOLAR_FOCUS);
    public static final DeferredItem<MachineUpgradeItem> NIGHT_STORAGE_CHIP = registerUpgradeItem("night_storage_chip", MachineUpgradeType.NIGHT_STORAGE);

    public static final DeferredItem<TechSwordItem> TECH_SWORD = ITEMS.register("tech_sword",
            () -> new TechSwordItem(new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<TechArmorItem> TECH_ARMOR_HELMET = ITEMS.register("tech_armor_helmet",
            () -> new TechArmorItem(TechArmorMaterial.TECH_ARMOR_MATERIAL, net.minecraft.world.item.ArmorItem.Type.HELMET, new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<TechArmorItem> TECH_ARMOR_CHESTPLATE = ITEMS.register("tech_armor_chestplate",
            () -> new TechArmorItem(TechArmorMaterial.TECH_ARMOR_MATERIAL, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<TechArmorItem> TECH_ARMOR_LEGGINGS = ITEMS.register("tech_armor_leggings",
            () -> new TechArmorItem(TechArmorMaterial.TECH_ARMOR_MATERIAL, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<TechArmorItem> TECH_ARMOR_BOOTS = ITEMS.register("tech_armor_boots",
            () -> new TechArmorItem(TechArmorMaterial.TECH_ARMOR_MATERIAL, net.minecraft.world.item.ArmorItem.Type.BOOTS, new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<TechChipItem> ENERGY_CUT_CHIP = ITEMS.register("energy_cut_chip",
            () -> new TechChipItem(TechChipType.ENERGY_CUT, new net.minecraft.world.item.Item.Properties().stacksTo(64).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<TechChipItem> SHOCK_DISCHARGE_CHIP = ITEMS.register("shock_discharge_chip",
            () -> new TechChipItem(TechChipType.SHOCK_DISCHARGE, new net.minecraft.world.item.Item.Properties().stacksTo(64).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<TechChipItem> OVERLOAD_CHIP = ITEMS.register("overload_chip",
            () -> new TechChipItem(TechChipType.OVERLOAD, new net.minecraft.world.item.Item.Properties().stacksTo(64).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<ControllerChipItem> CONTROLLER_CHIP = ITEMS.register("controller_chip",
            () -> new ControllerChipItem(new net.minecraft.world.item.Item.Properties().stacksTo(64).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<AdvancedControllerChipItem> ADVANCED_CONTROLLER_CHIP = ITEMS.register("advanced_controller_chip",
            () -> new AdvancedControllerChipItem(new net.minecraft.world.item.Item.Properties().stacksTo(64).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<TechFlightChipItem> TECH_FLIGHT_CHIP = ITEMS.register("tech_flight_chip",
            () -> new TechFlightChipItem(new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<GraviteItem> GRAVITE = ITEMS.register("gravite",
            () -> new GraviteItem(new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<EnergyDebugToolItem> ENERGY_DEBUG_TOOL = ITEMS.register("energy_debug_tool",
            () -> new EnergyDebugToolItem(new net.minecraft.world.item.Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MICROTECH_TAB = CREATIVE_MODE_TABS.register("microtech_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.microtech"))
                    .icon(() -> SOLAR_PANEL_T1_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(CABLE_T1_ITEM.get());
                        output.accept(BATTERY_T1_ITEM.get());
                        output.accept(BATTERY_T2_ITEM.get());
                        output.accept(EVO_TABLE_ITEM.get());
                        output.accept(ENERGY_CUT_CHIP.get());
                        output.accept(SHOCK_DISCHARGE_CHIP.get());
                        output.accept(OVERLOAD_CHIP.get());
                        output.accept(CONTROLLER_CHIP.get());
                        output.accept(ADVANCED_CONTROLLER_CHIP.get());
                        output.accept(TECH_FLIGHT_CHIP.get());
                        output.accept(GRAVITE.get());
                        output.accept(ENERGY_DEBUG_TOOL.get());
                        output.accept(ELECTRIC_FURNACE_T1_ITEM.get());
                        output.accept(SOLAR_PANEL_T1_ITEM.get());
                        output.accept(ENERGY_CONVERTER_T1_ITEM.get());
                        output.accept(TECH_TABLE_ITEM.get());
                        output.accept(TECH_MINER_ITEM.get());
                        output.accept(TECH_CRUSHER_ITEM.get());
                        output.accept(IRON_DUST.get());
                        output.accept(COPPER_DUST.get());
                        output.accept(GOLD_DUST.get());
                        output.accept(COAL_DUST.get());
                        output.accept(LAPIS_DUST.get());
                        output.accept(DIAMOND_DUST.get());
                        output.accept(EMERALD_DUST.get());
                        output.accept(NETHERITE_DUST.get());
                        output.accept(IRON_PLATE.get());
                        output.accept(COPPER_PLATE.get());
                        output.accept(GOLD_PLATE.get());
                        output.accept(SPEED_CHIP.get());
                        output.accept(EFFICIENCY_CHIP.get());
                        output.accept(INPUT_CHIP.get());
                        output.accept(OUTPUT_CHIP.get());
                        output.accept(RANGE_CHIP.get());
                        output.accept(AREA_CHIP.get());
                        output.accept(FILTER_CHIP.get());
                        output.accept(FILTER_UPGRADE_T1.get());
                        output.accept(FILTER_UPGRADE_T2.get());
                        output.accept(FILTER_UPGRADE_T3.get());
                        output.accept(FILTER_UPGRADE_T4.get());
                        output.accept(FORTUNE_CHIP.get());
                        output.accept(SOLAR_FOCUS_CHIP.get());
                        output.accept(TECH_ARMOR_HELMET.get());
                        output.accept(TECH_ARMOR_CHESTPLATE.get());
                        output.accept(TECH_ARMOR_LEGGINGS.get());
                        output.accept(TECH_ARMOR_BOOTS.get());
                        output.accept(TECH_SWORD.get());
                    })
                    .build());

    public Microtech(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerPayloads);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, TechArmorClientConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(TechSwordItem::onLivingHurt);
        NeoForge.EVENT_BUS.addListener(TechSwordItem::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(TechSwordItem::onServerTick);
        NeoForge.EVENT_BUS.addListener(TechArmorFlightHandler::onServerTick);
        NeoForge.EVENT_BUS.addListener(TechSwordItem::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(TechSwordItem::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(TechArmorFlightHandler::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(TechArmorFlightHandler::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(TechTableBlockEntity::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(TechTableBlockEntity::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(TechTableBlockEntity::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(ControlledMobEvents::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(ControlledMobEvents::onEntityLeaveLevel);
        NeoForge.EVENT_BUS.addListener(ControlledMobEvents::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(ControlledMobEvents::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(ControlledMobEvents::onLivingChangeTarget);
        NeoForge.EVENT_BUS.addListener(ControlledMobEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(ControlledMobEvents::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(ControlledMobEvents::onProjectileImpact);
        NeoForge.EVENT_BUS.addListener(ControlledMobEvents::onExplosionDetonate);
        NeoForge.EVENT_BUS.addListener(ControlledMobEvents::onEntityMobGriefing);
        NeoForge.EVENT_BUS.addListener(ControlledMobEvents::onMobEffectApplicable);
        NeoForge.EVENT_BUS.addListener(ControlledMobDebugCommand::onRegisterCommands);
    }

    private static DeferredItem<MachineUpgradeItem> registerUpgradeItem(String name, MachineUpgradeType upgradeType) {
        return ITEMS.register(name, () -> new MachineUpgradeItem(upgradeType, new Item.Properties().stacksTo(64).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    }

    private static DeferredItem<MachineUpgradeItem> registerUpgradeItem(String name, MachineUpgradeType upgradeType, int stackSize) {
        return ITEMS.register(name, () -> new MachineUpgradeItem(upgradeType, new Item.Properties().stacksTo(stackSize).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(MODID).playToServer(TechSwordAbilitySelectionPayload.TYPE, TechSwordAbilitySelectionPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }

                    var stack = player.getMainHandItem();
                    if (stack.isEmpty() || stack.getItem() != TECH_SWORD.get()) {
                        return;
                    }

                    var activeAbilities = TechSwordData.getInstalledActiveAbilities(stack);
                    if (activeAbilities.isEmpty()) {
                        TechSwordData.setSelectedActiveAbility(stack, "");
                        player.getInventory().setChanged();
                        player.containerMenu.broadcastChanges();
                        return;
                    }

                    if (!activeAbilities.contains(payload.selectedAbility())) {
                        return;
                    }

                    TechSwordData.setSelectedActiveAbility(stack, payload.selectedAbility());
                    player.getInventory().setChanged();
                    player.containerMenu.broadcastChanges();
                }));
        event.registrar(MODID).playToServer(TechMinerFilterPayload.TYPE, TechMinerFilterPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }
                    if (!(player.containerMenu instanceof TechMinerFilterMenu menu)) {
                        return;
                    }

                    BlockPos pos = new BlockPos(payload.x(), payload.y(), payload.z());
                    if (!pos.equals(menu.getBlockPos())) {
                        return;
                    }
                    if (!(player.level().getBlockEntity(pos) instanceof TechMinerBlockEntity blockEntity)) {
                        return;
                    }
                    if (!TechMinerMenu.isUsable(blockEntity, player)) {
                        return;
                    }
                    if (!blockEntity.hasFilterUpgrade()) {
                        player.displayClientMessage(Component.translatable("message.microtech.tech_miner.filter_requires_upgrade"), true);
                        return;
                    }

                    if (payload.action() == TechMinerFilterPayload.ACTION_CLEAR) {
                        blockEntity.clearFilterEntries();
                        return;
                    }
                    if (payload.index() < 0 || payload.index() >= TechMinerBlockEntity.MAX_FILTER_ENTRIES) {
                        return;
                    }
                    if (payload.action() == TechMinerFilterPayload.ACTION_REMOVE) {
                        blockEntity.removeFilterEntry(payload.index());
                        return;
                    }
                    if (payload.action() != TechMinerFilterPayload.ACTION_SET || payload.index() >= blockEntity.getFilterCapacity()) {
                        return;
                    }

                    ResourceLocation blockId;
                    try {
                        blockId = ResourceLocation.parse(payload.blockId());
                    } catch (Exception ignored) {
                        return;
                    }
                    blockEntity.setFilterEntry(payload.index(), blockId, player);
                }));
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ENERGY_CONVERTER_T1_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BATTERY_T1_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BATTERY_T2_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ELECTRIC_FURNACE_T1_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, SOLAR_PANEL_T1_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TECH_MINER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TECH_CRUSHER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage());
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> new TechSwordEnergyStorage(stack),
                TECH_SWORD.get());
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> new GraviteEnergyStorage(stack),
                GRAVITE.get());
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> new TechArmorEnergyStorage(stack),
                TECH_ARMOR_HELMET.get(),
                TECH_ARMOR_CHESTPLATE.get(),
                TECH_ARMOR_LEGGINGS.get(),
                TECH_ARMOR_BOOTS.get());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BATTERY_T2_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getItemHandler());
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
            event.register(Microtech.ENERGY_CONVERTER_T1_MENU.get(), BasicMachineScreen::new);
            event.register(Microtech.BATTERY_T1_MENU.get(), BatteryScreen::new);
            event.register(Microtech.BATTERY_T2_MENU.get(), BatteryT2Screen::new);
            event.register(Microtech.EVO_TABLE_MENU.get(), EvoTableScreen::new);
            event.register(Microtech.ELECTRIC_FURNACE_T1_MENU.get(), ElectricFurnaceScreen::new);
            event.register(Microtech.SOLAR_PANEL_T1_MENU.get(), SolarPanelScreen::new);
            event.register(Microtech.TECH_MINER_MENU.get(), TechMinerScreen::new);
            event.register(Microtech.TECH_MINER_OUTPUT_MENU.get(), TechMinerOutputScreen::new);
            event.register(Microtech.TECH_MINER_FILTER_MENU.get(), TechMinerFilterScreen::new);
            event.register(Microtech.TECH_CRUSHER_MENU.get(), TechCrusherScreen::new);
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(Infinitygroup.microtech.client.TechSwordClientKeybinds.OPEN_ABILITY_SELECTOR);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(Microtech.BATTERY_T2_BLOCK_ENTITY.get(), Infinitygroup.microtech.client.renderer.blockentity.BatteryT2BlockEntityRenderer::new);
            event.registerBlockEntityRenderer(Microtech.EVO_TABLE_BLOCK_ENTITY.get(), Infinitygroup.microtech.client.renderer.blockentity.EvoTableBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(Microtech.ELECTRIC_FURNACE_T1_BLOCK_ENTITY.get(), ElectricFurnaceRenderer::new);
            event.registerBlockEntityRenderer(Microtech.TECH_MINER_BLOCK_ENTITY.get(), Infinitygroup.microtech.client.renderer.blockentity.TechMinerRenderer::new);
            event.registerBlockEntityRenderer(Microtech.TECH_CRUSHER_BLOCK_ENTITY.get(), Infinitygroup.microtech.client.renderer.blockentity.TechCrusherRenderer::new);
            event.registerBlockEntityRenderer(Microtech.TECH_TABLE_BLOCK_ENTITY.get(), TechTableRenderer::new);
        }
    }
}
