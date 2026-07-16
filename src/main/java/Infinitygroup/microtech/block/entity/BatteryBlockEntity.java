package Infinitygroup.microtech.block.entity;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.menu.BatteryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class BatteryBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int MAX_ENERGY = 20_000;
    public static final int MAX_RECEIVE = 40;
    public static final int MAX_EXTRACT = 100;
    public static final int STATUS_IDLE = 0;
    public static final int STATUS_CHARGING = 1;
    public static final int STATUS_FULL = 2;
    public static final int STATUS_NO_POWER = 3;
    public static final int STATUS_DISCHARGING = 4;

    private static final String ENERGY_TAG = "EnergyStored";
    private static final String STATUS_TAG = "BatteryStatus";
    private static final String LEGACY_STATUS_TAG = "ChargingStatus";

    private final BatteryEnergyStorage energyStorage = new BatteryEnergyStorage();
    private int batteryStatus = STATUS_IDLE;
    private int lastObservedEnergy = -1;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(Microtech.BATTERY_T1_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BatteryBlockEntity blockEntity) {
        if (level.isClientSide || blockEntity.isRemoved()) {
            return;
        }

        blockEntity.updateMachineActivity();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.microtech.battery_t1");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ContainerLevelAccess access = this.level == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(this.level, this.worldPosition);
        return new BatteryMenu(containerId, playerInventory, this, access);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.energyStorage.setEnergyStored(tag.getInt(ENERGY_TAG));
        if (tag.contains(STATUS_TAG, Tag.TAG_INT)) {
            this.batteryStatus = tag.getInt(STATUS_TAG);
        } else if (tag.contains(LEGACY_STATUS_TAG, Tag.TAG_INT)) {
            this.batteryStatus = tag.getInt(LEGACY_STATUS_TAG);
        } else {
            this.refreshStatusFromEnergy();
        }
        this.lastObservedEnergy = this.energyStorage.getEnergyStored();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(ENERGY_TAG, this.energyStorage.getEnergyStored());
        tag.putInt(STATUS_TAG, this.batteryStatus);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt(ENERGY_TAG, this.energyStorage.getEnergyStored());
        tag.putInt(STATUS_TAG, this.batteryStatus);
        return tag;
    }

    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public int getEnergyStored() {
        return this.energyStorage.getEnergyStored();
    }

    public int getMaxEnergy() {
        return MAX_ENERGY;
    }

    public int getChargingStatus() {
        return this.batteryStatus;
    }

    public void setEnergyStored(int energy) {
        int before = this.energyStorage.getEnergyStored();
        this.energyStorage.setEnergyStored(energy);
        if (this.energyStorage.getEnergyStored() != before) {
            this.refreshStatusFromEnergy();
            this.lastObservedEnergy = this.energyStorage.getEnergyStored();
            this.setChanged();
            this.syncClient();
        }
    }

    public ItemStack createItemStackWithEnergy() {
        ItemStack stack = new ItemStack(Microtech.BATTERY_T1_ITEM.get());
        saveEnergyToStack(stack, this.energyStorage.getEnergyStored());
        return stack;
    }

    public static void saveEnergyToStack(ItemStack stack, int energy) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(ENERGY_TAG, Mth.clamp(energy, 0, MAX_ENERGY));
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
    }

    public static int getEnergyFromStack(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return Mth.clamp(tag.getInt(ENERGY_TAG), 0, MAX_ENERGY);
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // Battery T1 has no item slot.
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.isRemoved()
                && player.distanceToSqr((double) this.worldPosition.getX() + 0.5D, (double) this.worldPosition.getY() + 0.5D, (double) this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        // Battery T1 has no item slot.
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    public boolean canTakeItemThroughFace(int slot) {
        return false;
    }

    public boolean canPlaceItemThroughFace(int slot, ItemStack stack) {
        return false;
    }

    private void refreshStatusFromEnergy() {
        if (this.energyStorage.getEnergyStored() <= 0) {
            this.batteryStatus = STATUS_NO_POWER;
        } else if (this.energyStorage.getEnergyStored() >= MAX_ENERGY) {
            this.batteryStatus = STATUS_FULL;
        } else if (this.batteryStatus != STATUS_CHARGING && this.batteryStatus != STATUS_DISCHARGING) {
            this.batteryStatus = STATUS_IDLE;
        }
    }

    private void updateMachineActivity() {
        int currentEnergy = this.energyStorage.getEnergyStored();
        boolean active = this.lastObservedEnergy >= 0 && currentEnergy != this.lastObservedEnergy;
        this.lastObservedEnergy = currentEnergy;
        MicroTechMachineStateHelper.setMachineActive(this, active);
    }

    private void setBatteryStatus(int status) {
        if (this.batteryStatus != status) {
            this.batteryStatus = status;
            this.setChanged();
            this.syncClient();
        }
    }

    private void syncClient() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private final class BatteryEnergyStorage extends EnergyStorage {
        private BatteryEnergyStorage() {
            super(MAX_ENERGY, MAX_RECEIVE, MAX_EXTRACT, 0);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = super.receiveEnergy(toReceive, simulate);
            if (!simulate && received > 0) {
                BatteryBlockEntity.this.setBatteryStatus(BatteryBlockEntity.this.energyStorage.getEnergyStored() >= MAX_ENERGY
                        ? STATUS_FULL
                        : STATUS_CHARGING);
                BatteryBlockEntity.this.setChanged();
                BatteryBlockEntity.this.syncClient();
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int extracted = super.extractEnergy(toExtract, simulate);
            if (!simulate && extracted > 0) {
                BatteryBlockEntity.this.setBatteryStatus(BatteryBlockEntity.this.energyStorage.getEnergyStored() <= 0
                        ? STATUS_NO_POWER
                        : STATUS_DISCHARGING);
                BatteryBlockEntity.this.setChanged();
                BatteryBlockEntity.this.syncClient();
            }
            return extracted;
        }

        private void setEnergyStored(int energy) {
            this.energy = Mth.clamp(energy, 0, MAX_ENERGY);
        }
    }
}
