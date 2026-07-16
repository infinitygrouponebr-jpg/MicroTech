package Infinitygroup.microtech.block.entity;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.machine.MachineUpgradeHelper;
import Infinitygroup.microtech.machine.MachineUpgradeHost;
import Infinitygroup.microtech.machine.MachineUpgradeInventory;
import Infinitygroup.microtech.menu.SolarPanelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class SolarPanelBlockEntity extends BlockEntity implements MenuProvider, MachineUpgradeHost {
    public static final int MAX_ENERGY = 5_000;
    public static final int MAX_EXTRACT = 20;
    public static final int DAY_GENERATION = 4;
    public static final int RAIN_GENERATION = 1;
    private static final String ENERGY_TAG = "EnergyStored";
    private static final String UPGRADE_TAG = "Upgrades";
    private static final String MACHINE_ID = "microtech:solar_panel_t1";

    private final SolarEnergyStorage energyStorage = new SolarEnergyStorage();
    private final MachineUpgradeInventory upgradeInventory = new MachineUpgradeInventory(MACHINE_ID, 2, () -> {
        this.onUpgradesChanged();
        return true;
    });
    private int generationPerTick;
    private SolarStatus status = SolarStatus.NO_SUN;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(Microtech.SOLAR_PANEL_T1_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity blockEntity) {
        if (level.isClientSide || blockEntity.isRemoved()) {
            return;
        }

        blockEntity.clampEnergyStored();
        SolarStatus newStatus = blockEntity.computeStatus(level, pos);
        int newGeneration = blockEntity.getEffectiveGenerationPerTick(newStatus);

        boolean changed = false;
        if (newStatus != blockEntity.status) {
            blockEntity.status = newStatus;
            changed = true;
        }
        if (newGeneration != blockEntity.generationPerTick) {
            blockEntity.generationPerTick = newGeneration;
            changed = true;
        }

        if (newGeneration > 0 && blockEntity.energyStorage.getEnergyStored() < blockEntity.getEffectiveMaxEnergy()) {
            int generated = Math.min(newGeneration, blockEntity.getEffectiveMaxEnergy() - blockEntity.energyStorage.getEnergyStored());
            if (generated > 0) {
                blockEntity.energyStorage.addEnergy(generated);
                changed = true;
            }
        }

        if (changed) {
            blockEntity.setChanged();
            blockEntity.syncClient();
        }
    }

    private SolarStatus computeStatus(Level level, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasFixedTime()) {
            return SolarStatus.NO_SUN;
        }

        if (!level.canSeeSky(pos.above())) {
            return SolarStatus.BLOCKED;
        }

        if (!level.isDay()) {
            return SolarStatus.NIGHT;
        }

        if (level.isRainingAt(pos.above())) {
            return SolarStatus.RAIN;
        }

        return SolarStatus.GENERATING;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.microtech.solar_panel_t1");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ContainerLevelAccess access = this.level == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(this.level, this.worldPosition);
        return new SolarPanelMenu(containerId, playerInventory, this, access);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(UPGRADE_TAG, Tag.TAG_COMPOUND)) {
            this.upgradeInventory.deserializeNBT(registries, tag.getCompound(UPGRADE_TAG));
        }
        this.energyStorage.setEnergyStored(tag.getInt(ENERGY_TAG));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(ENERGY_TAG, this.energyStorage.getEnergyStored());
        tag.put(UPGRADE_TAG, this.upgradeInventory.serializeNBT(registries));
    }

    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public int getEnergyStored() {
        return this.energyStorage.getEnergyStored();
    }

    public int getMaxEnergy() {
        return this.getEffectiveMaxEnergy();
    }

    public int getGenerationPerTick() {
        return this.generationPerTick;
    }

    public int getEffectiveMaxEnergy() {
        return MAX_ENERGY;
    }

    public int getEffectiveExtractLimit() {
        return MAX_EXTRACT;
    }

    public int getEffectiveGenerationPerTick(SolarStatus status) {
        if (status == SolarStatus.NO_SUN || status == SolarStatus.BLOCKED) {
            return 0;
        }
        double dayMultiplier = MachineUpgradeHelper.getGenerationMultiplier(this);
        double nightMultiplier = MachineUpgradeHelper.getNightStorageMultiplier(this);
        if (status == SolarStatus.NIGHT) {
            if (nightMultiplier <= 0.0D) {
                return 0;
            }
            return Math.max(1, (int) Math.ceil(DAY_GENERATION * nightMultiplier));
        }
        double base = status.generation;
        return Math.max(0, (int) Math.round(base * dayMultiplier));
    }

    public MachineUpgradeInventory getUpgradeInventory() {
        return this.upgradeInventory;
    }

    @Override
    public String getMachineUpgradeId() {
        return MACHINE_ID;
    }

    @Override
    public int getUpgradeSlotCount() {
        return this.upgradeInventory.getSlots();
    }

    public SolarStatus getStatus() {
        return this.status;
    }

    public void setEnergyStored(int energy) {
        int before = this.energyStorage.getEnergyStored();
        this.energyStorage.setEnergyStored(energy);
        if (before != this.energyStorage.getEnergyStored()) {
            this.syncClient();
        }
    }

    public void onUpgradesChanged() {
        this.clampEnergyStored();
        this.syncClient();
    }

    public void syncClient() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void clampEnergyStored() {
        int max = this.getEffectiveMaxEnergy();
        if (this.energyStorage.getEnergyStored() > max) {
            this.energyStorage.setEnergyStored(max);
        }
    }

    private final class SolarEnergyStorage extends EnergyStorage {
        private SolarEnergyStorage() {
            super(MAX_ENERGY, 0, MAX_EXTRACT, 0);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int limit = SolarPanelBlockEntity.this.getEffectiveExtractLimit();
            int extracted = Math.max(0, Math.min(toExtract, Math.min(limit, this.energy)));
            if (!simulate && extracted > 0) {
                this.energy -= extracted;
                SolarPanelBlockEntity.this.syncClient();
            }
            return extracted;
        }

        @Override
        public int getMaxEnergyStored() {
            return SolarPanelBlockEntity.this.getEffectiveMaxEnergy();
        }

        @Override
        public boolean canExtract() {
            return this.energy > 0;
        }

        private void setEnergyStored(int energy) {
            this.energy = Mth.clamp(energy, 0, SolarPanelBlockEntity.this.getEffectiveMaxEnergy());
        }

        private void addEnergy(int amount) {
            this.energy = Mth.clamp(this.energy + amount, 0, SolarPanelBlockEntity.this.getEffectiveMaxEnergy());
        }
    }

    public enum SolarStatus {
        GENERATING(4, "gui.microtech.solar_panel_t1.generating"),
        RAIN(1, "gui.microtech.solar_panel_t1.rain"),
        NIGHT(0, "gui.microtech.solar_panel_t1.night"),
        BLOCKED(0, "gui.microtech.solar_panel_t1.blocked"),
        NO_SUN(0, "gui.microtech.solar_panel_t1.no_sun");

        private final int generation;
        private final String translationKey;

        SolarStatus(int generation, String translationKey) {
            this.generation = generation;
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }
    }
}
