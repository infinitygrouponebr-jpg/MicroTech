package Infinitygroup.microtech.block.entity;

import Infinitygroup.microtech.menu.BasicMachineMenu;
import Infinitygroup.microtech.machine.MachineUpgradeHost;
import Infinitygroup.microtech.machine.MachineUpgradeHelper;
import Infinitygroup.microtech.machine.MachineUpgradeInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class BasicMachineBlockEntity extends BlockEntity implements Container, MenuProvider, MachineUpgradeHost {
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    private static final int SLOT_COUNT = 2;
    private static final String MACHINE_ID = "microtech:energy_converter_t1";
    private static final String UPGRADE_TAG = "Upgrades";
    public static final int MAX_ENERGY = 10_000;
    public static final int ENERGY_PER_TICK = 20;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final IEnergyStorage energyStorage = new ConverterEnergyStorage();
    private final MachineUpgradeInventory upgradeInventory = new MachineUpgradeInventory(MACHINE_ID, 2, () -> {
        this.onUpgradesChanged();
        return true;
    });
    private int energyStored;
    private int pendingEnergy;

    public BasicMachineBlockEntity(BlockPos pos, BlockState state) {
        super(Infinitygroup.microtech.Microtech.ENERGY_CONVERTER_T1_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BasicMachineBlockEntity blockEntity) {
        if (level.isClientSide || blockEntity.isRemoved()) {
            return;
        }

        blockEntity.clampEnergyStored();
        boolean changed = false;

        if (blockEntity.pendingEnergy > 0 && blockEntity.energyStored < blockEntity.getMaxEnergy()) {
            int transferable = Math.min(blockEntity.getEffectiveGenerationPerTick(), Math.min(blockEntity.pendingEnergy, blockEntity.getMaxEnergy() - blockEntity.energyStored));
            if (transferable > 0) {
                blockEntity.addEnergyStored(transferable);
                blockEntity.pendingEnergy -= transferable;
                changed = true;
            }
        }

        if (blockEntity.pendingEnergy <= 0 && blockEntity.energyStored < blockEntity.getMaxEnergy()) {
            ItemStack input = blockEntity.items.get(SLOT_INPUT);
            if (!input.isEmpty() && isConvertible(input)) {
                int energy = getEnergyValue(input);
                if (energy > 0) {
                    int generatedEnergy = blockEntity.applyFuelEfficiency(energy);
                    if (input.is(Items.LAVA_BUCKET)) {
                        if (blockEntity.canAcceptBucketOutput()) {
                            blockEntity.consumeOneInputAndReturnBucket();
                            blockEntity.pendingEnergy = generatedEnergy;
                            changed = true;
                        }
                    } else {
                        input.shrink(1);
                        if (input.isEmpty()) {
                            blockEntity.items.set(SLOT_INPUT, ItemStack.EMPTY);
                        }
                        blockEntity.pendingEnergy = generatedEnergy;
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            blockEntity.setChanged();
            blockEntity.syncClient();
        }

        MicroTechMachineStateHelper.setMachineActive(blockEntity, blockEntity.pendingEnergy > 0);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.microtech.energy_converter_t1");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ContainerLevelAccess access = this.level == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(this.level, this.worldPosition);
        return new BasicMachineMenu(containerId, playerInventory, this, access);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        if (tag.contains(UPGRADE_TAG, Tag.TAG_COMPOUND)) {
            this.upgradeInventory.deserializeNBT(registries, tag.getCompound(UPGRADE_TAG));
        }
        this.energyStored = tag.getInt("EnergyStored");
        this.pendingEnergy = tag.getInt("PendingEnergy");
        if (this.energyStored < 0) {
            this.energyStored = 0;
        } else if (this.energyStored > MAX_ENERGY) {
            this.energyStored = MAX_ENERGY;
        }
        if (this.pendingEnergy < 0) {
            this.pendingEnergy = 0;
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.put(UPGRADE_TAG, this.upgradeInventory.serializeNBT(registries));
        tag.putInt("EnergyStored", this.energyStored);
        tag.putInt("PendingEnergy", this.pendingEnergy);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!copy.isEmpty() && copy.getCount() > getMaxStackSize()) {
            copy.setCount(getMaxStackSize());
        }
        items.set(slot, copy);
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return !this.isRemoved() && player.distanceToSqr((double) this.worldPosition.getX() + 0.5D, (double) this.worldPosition.getY() + 0.5D, (double) this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && isConvertible(stack);
    }

    public boolean canTakeItemThroughFace(int slot) {
        return slot == SLOT_OUTPUT || slot == SLOT_INPUT;
    }

    public boolean canPlaceItemThroughFace(int slot, ItemStack stack) {
        return canPlaceItem(slot, stack);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public int getEnergyStored() {
        return energyStored;
    }

    public int getMaxEnergy() {
        return this.getEffectiveMaxEnergy();
    }

    public int getPendingEnergy() {
        return pendingEnergy;
    }

    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
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

    public void addEnergyStored(int amount) {
        if (amount <= 0) {
            return;
        }

        int newValue = Math.min(this.getMaxEnergy(), this.energyStored + amount);
        if (newValue != this.energyStored) {
            this.energyStored = newValue;
            setChanged();
        }
    }

    public void setEnergyStored(int amount) {
        int clamped = Math.max(0, Math.min(this.getMaxEnergy(), amount));
        if (clamped != this.energyStored) {
            this.energyStored = clamped;
            setChanged();
        }
    }

    public void addPendingEnergy(int amount) {
        if (amount <= 0) {
            return;
        }

        this.pendingEnergy += amount;
        setChanged();
    }

    public void setPendingEnergy(int amount) {
        int clamped = Math.max(0, amount);
        if (clamped != this.pendingEnergy) {
            this.pendingEnergy = clamped;
            setChanged();
        }
    }

    public int getEffectiveMaxEnergy() {
        return MAX_ENERGY;
    }

    public int getEffectiveTransferLimit() {
        return ENERGY_PER_TICK;
    }

    public int getEffectiveGenerationPerTick() {
        return ENERGY_PER_TICK;
    }

    public int applyFuelEfficiency(int baseEnergy) {
        double efficiencyBonus = 1.0D / Math.max(0.50D, MachineUpgradeHelper.getEnergyCostMultiplier(this));
        return Math.max(1, (int) Math.round(baseEnergy * MachineUpgradeHelper.getFuelEfficiencyMultiplier(this) * efficiencyBonus));
    }

    public void onUpgradesChanged() {
        this.clampEnergyStored();
        this.syncClient();
    }

    private void clampEnergyStored() {
        int max = this.getEffectiveMaxEnergy();
        if (this.energyStored > max) {
            this.energyStored = max;
        }
        if (this.energyStored < 0) {
            this.energyStored = 0;
        }
    }

    public boolean canAcceptBucketOutput() {
        ItemStack output = items.get(SLOT_OUTPUT);
        return output.isEmpty() || (output.is(Items.BUCKET) && output.getCount() < output.getMaxStackSize());
    }

    private void consumeOneInputAndReturnBucket() {
        ItemStack input = items.get(SLOT_INPUT);
        if (!input.is(Items.LAVA_BUCKET)) {
            return;
        }

        input.shrink(1);
        if (input.isEmpty()) {
            items.set(SLOT_INPUT, ItemStack.EMPTY);
        }

        ItemStack output = items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            items.set(SLOT_OUTPUT, new ItemStack(Items.BUCKET));
        } else {
            output.grow(1);
        }
    }

    private void syncClient() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public static boolean isConvertible(ItemStack stack) {
        return getEnergyValue(stack) > 0;
    }

    public static int getEnergyValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        if (stack.is(Items.COAL)) {
            return 1_600;
        }
        if (stack.is(Items.CHARCOAL)) {
            return 1_600;
        }
        if (stack.is(Items.COAL_BLOCK)) {
            return 16_000;
        }
        if (stack.is(Items.REDSTONE)) {
            return 400;
        }
        if (stack.is(Items.BLAZE_POWDER)) {
            return 2_400;
        }
        if (stack.is(Items.BLAZE_ROD)) {
            return 4_800;
        }
        if (stack.is(Items.LAVA_BUCKET)) {
            return 20_000;
        }

        return 0;
    }

    private final class ConverterEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (toExtract <= 0) {
                return 0;
            }

            int extracted = Math.min(energyStored, Math.min(BasicMachineBlockEntity.this.getEffectiveTransferLimit(), toExtract));
            if (!simulate && extracted > 0) {
                energyStored -= extracted;
                BasicMachineBlockEntity.this.syncClient();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return energyStored;
        }

        @Override
        public int getMaxEnergyStored() {
            return BasicMachineBlockEntity.this.getEffectiveMaxEnergy();
        }

        @Override
        public boolean canExtract() {
            return energyStored > 0;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
