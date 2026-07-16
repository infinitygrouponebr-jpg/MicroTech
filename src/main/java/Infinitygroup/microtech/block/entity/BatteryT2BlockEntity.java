package Infinitygroup.microtech.block.entity;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.menu.BatteryT2Menu;
import Infinitygroup.microtech.machine.MachineUpgradeHost;
import Infinitygroup.microtech.machine.MachineUpgradeHelper;
import Infinitygroup.microtech.machine.MachineUpgradeInventory;
import Infinitygroup.microtech.item.TechSwordEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

public class BatteryT2BlockEntity extends BlockEntity implements MenuProvider, MachineUpgradeHost {
    public static final int MAX_ENERGY = 50_000;
    public static final int MAX_RECEIVE = 200;
    public static final int MAX_EXTRACT = 200;
    public static final int ITEM_CHARGE_PER_TICK = 80;
    public static final int STATUS_EMPTY = 0;
    public static final int STATUS_INCOMPATIBLE = 1;
    public static final int STATUS_CHARGING = 2;
    public static final int STATUS_FULL = 3;
    private static final String ENERGY_TAG = "EnergyStored";
    private static final String INVENTORY_TAG = "Inventory";
    private static final String STATUS_TAG = "ChargingStatus";
    private static final String UPGRADE_TAG = "Upgrades";
    private static final String MACHINE_ID = "microtech:battery_t2";

    private final BatteryEnergyStorage energyStorage = new BatteryEnergyStorage();
    private int chargingStatus = STATUS_EMPTY;
    private int lastObservedEnergy = -1;
    private final MachineUpgradeInventory upgradeInventory = new MachineUpgradeInventory(MACHINE_ID, 2, () -> {
        this.onUpgradesChanged();
        return true;
    });
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null || stack.is(Microtech.TECH_SWORD.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            BatteryT2BlockEntity.this.syncClient();
        }
    };

    public BatteryT2BlockEntity(BlockPos pos, BlockState state) {
        super(Microtech.BATTERY_T2_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BatteryT2BlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        blockEntity.clampEnergyStored();
        boolean changed = false;

        if (level.getGameTime() % 20L == 0L) {
            if (MachineUpgradeHelper.getAutoInputEnabled(blockEntity)) {
                changed |= blockEntity.tryAutoInput(level);
            }
            int inputInterval = MachineUpgradeHelper.getInputInterval(blockEntity);
            int inputBatch = MachineUpgradeHelper.getInputBatchSize(blockEntity);
            if (inputBatch > 0 && level.getGameTime() % inputInterval == 0L) {
                changed |= blockEntity.tryAutoInput(level, inputBatch);
            }
            int outputInterval = MachineUpgradeHelper.getOutputInterval(blockEntity);
            int outputBatch = MachineUpgradeHelper.getOutputBatchSize(blockEntity);
            if (outputBatch > 0 && level.getGameTime() % outputInterval == 0L) {
                changed |= blockEntity.tryAutoOutput(level, outputBatch);
            }
            if (MachineUpgradeHelper.getWirelessChargeEnabled(blockEntity)) {
                changed |= blockEntity.wirelessChargeNearby(level);
            }
        }

        ItemStack stack = blockEntity.itemHandler.getStackInSlot(0);
        blockEntity.updateChargingStatus(stack);
        if (stack.isEmpty()) {
            blockEntity.updateMachineActivity();
            if (changed) {
                blockEntity.syncClient();
            }
            return;
        }

        IEnergyStorage itemEnergy = resolveItemEnergyStorage(stack);
        if (itemEnergy == null || !itemEnergy.canReceive()) {
            blockEntity.updateMachineActivity();
            if (changed) {
                blockEntity.syncClient();
            }
            return;
        }

        int available = Math.min(blockEntity.getEffectiveChargePerTick(), blockEntity.energyStorage.getEnergyStored());
        if (available <= 0) {
            blockEntity.updateMachineActivity();
            if (changed) {
                blockEntity.syncClient();
            }
            return;
        }

        int simulatedReceive = itemEnergy.receiveEnergy(available, true);
        int transferable = Math.min(available, simulatedReceive);
        if (transferable <= 0) {
            blockEntity.updateMachineActivity();
            if (changed) {
                blockEntity.syncClient();
            }
            return;
        }

        int extracted = blockEntity.energyStorage.extractEnergy(transferable, false);
        if (extracted <= 0) {
            return;
        }

        int inserted = itemEnergy.receiveEnergy(extracted, false);
        if (inserted < extracted) {
            int refunded = extracted - inserted;
            if (refunded > 0) {
                blockEntity.energyStorage.receiveEnergy(refunded, false);
            }
        }

        changed = true;
        blockEntity.syncClient();
        blockEntity.updateChargingStatus(stack);
        blockEntity.updateMachineActivity();
        if (inserted > 0 && level.getGameTime() % 10L == 0L) {
            blockEntity.spawnChargeEffects((net.minecraft.server.level.ServerLevel) level, inserted >= blockEntity.getEffectiveChargePerTick());
        }
        if (changed) {
            blockEntity.setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.microtech.battery_t2");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ContainerLevelAccess access = this.level == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(this.level, this.worldPosition);
        return new BatteryT2Menu(containerId, playerInventory, this, access);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(UPGRADE_TAG, Tag.TAG_COMPOUND)) {
            this.upgradeInventory.deserializeNBT(registries, tag.getCompound(UPGRADE_TAG));
        }
        this.energyStorage.setEnergyStored(tag.getInt(ENERGY_TAG));
        this.chargingStatus = tag.contains(STATUS_TAG, Tag.TAG_INT) ? tag.getInt(STATUS_TAG) : STATUS_EMPTY;
        if (tag.contains(INVENTORY_TAG, Tag.TAG_COMPOUND)) {
            this.itemHandler.deserializeNBT(registries, tag.getCompound(INVENTORY_TAG));
        }
        this.lastObservedEnergy = this.energyStorage.getEnergyStored();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(ENERGY_TAG, this.energyStorage.getEnergyStored());
        tag.putInt(STATUS_TAG, this.chargingStatus);
        tag.put(INVENTORY_TAG, this.itemHandler.serializeNBT(registries));
        tag.put(UPGRADE_TAG, this.upgradeInventory.serializeNBT(registries));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt(ENERGY_TAG, this.energyStorage.getEnergyStored());
        tag.putInt(STATUS_TAG, this.chargingStatus);
        tag.put(INVENTORY_TAG, this.itemHandler.serializeNBT(registries));
        tag.put(UPGRADE_TAG, this.upgradeInventory.serializeNBT(registries));
        return tag;
    }

    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    public MachineUpgradeInventory getUpgradeInventory() {
        return this.upgradeInventory;
    }

    public ItemStack getChargingStack() {
        return this.itemHandler.getStackInSlot(0);
    }

    public int getEnergyStored() {
        return this.energyStorage.getEnergyStored();
    }

    public int getMaxEnergy() {
        return this.getEffectiveMaxEnergy();
    }

    public int getChargingStatus() {
        return this.chargingStatus;
    }

    public int getChargingItemEnergyStored() {
        IEnergyStorage itemEnergy = resolveItemEnergyStorage(this.getChargingStack());
        return itemEnergy != null ? itemEnergy.getEnergyStored() : 0;
    }

    public int getChargingItemMaxEnergy() {
        IEnergyStorage itemEnergy = resolveItemEnergyStorage(this.getChargingStack());
        return itemEnergy != null ? itemEnergy.getMaxEnergyStored() : 0;
    }

    public int getEffectiveMaxEnergy() {
        return MAX_ENERGY;
    }

    public int getEffectiveReceiveLimit() {
        return MAX_RECEIVE;
    }

    public int getEffectiveExtractLimit() {
        return MAX_EXTRACT;
    }

    public int getEffectiveChargePerTick() {
        return Math.max(1, (int) Math.round(ITEM_CHARGE_PER_TICK * MachineUpgradeHelper.getSpeedMultiplier(this)));
    }

    public void onUpgradesChanged() {
        this.clampEnergyStored();
        this.syncClient();
    }

    @Override
    public String getMachineUpgradeId() {
        return MACHINE_ID;
    }

    @Override
    public int getUpgradeSlotCount() {
        return this.upgradeInventory.getSlots();
    }

    public void setEnergyStored(int energy) {
        int before = this.energyStorage.getEnergyStored();
        this.energyStorage.setEnergyStored(energy);
        if (this.energyStorage.getEnergyStored() != before) {
            this.lastObservedEnergy = this.energyStorage.getEnergyStored();
            this.syncClient();
        }
    }

    public ItemStack createItemStackWithEnergy() {
        ItemStack stack = new ItemStack(Microtech.BATTERY_T2_ITEM.get());
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
            this.lastObservedEnergy = this.energyStorage.getEnergyStored();
        }
    }

    private void updateMachineActivity() {
        int currentEnergy = this.energyStorage.getEnergyStored();
        boolean active = this.lastObservedEnergy >= 0 && currentEnergy != this.lastObservedEnergy;
        this.lastObservedEnergy = currentEnergy;
        MicroTechMachineStateHelper.setMachineActive(this, active);
    }

    private boolean tryAutoInput(Level level) {
        ItemStack current = this.itemHandler.getStackInSlot(0);
        if (!current.isEmpty()) {
            return false;
        }

        for (Direction direction : Direction.values()) {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, this.worldPosition.relative(direction), direction.getOpposite());
            if (handler == null) {
                continue;
            }

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty() || resolveItemEnergyStorage(stack) == null) {
                    continue;
                }

                ItemStack extracted = handler.extractItem(slot, 1, false);
                if (extracted.isEmpty()) {
                    continue;
                }

                this.itemHandler.setStackInSlot(0, extracted.copy());
                this.syncClient();
                return true;
            }
        }

        return false;
    }

    private boolean tryAutoInput(Level level, int attempts) {
        boolean moved = false;
        for (int i = 0; i < attempts; i++) {
            moved |= this.tryAutoInput(level);
        }
        return moved;
    }

    private boolean tryAutoOutput(Level level) {
        ItemStack stack = this.itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) {
            return false;
        }

        IEnergyStorage itemEnergy = resolveItemEnergyStorage(stack);
        if (itemEnergy == null) {
            return false;
        }

        if (itemEnergy.getEnergyStored() < itemEnergy.getMaxEnergyStored()) {
            return false;
        }

        boolean moved = false;
        for (Direction direction : Direction.values()) {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, this.worldPosition.relative(direction), direction.getOpposite());
            if (handler == null) {
                continue;
            }

            ItemStack simulated = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), true);
            int transferable = stack.getCount() - simulated.getCount();
            if (transferable <= 0) {
                continue;
            }

            ItemStack toMove = stack.copy();
            toMove.setCount(transferable);
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(handler, toMove, false);
            int inserted = transferable - remaining.getCount();
            if (inserted > 0) {
                stack.shrink(inserted);
                moved = true;
                if (stack.isEmpty()) {
                    this.itemHandler.setStackInSlot(0, ItemStack.EMPTY);
                }
                break;
            }
        }

        if (moved) {
            this.syncClient();
        }
        return moved;
    }

    private boolean tryAutoOutput(Level level, int attempts) {
        boolean moved = false;
        for (int i = 0; i < attempts; i++) {
            moved |= this.tryAutoOutput(level);
        }
        return moved;
    }

    private boolean wirelessChargeNearby(Level level) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }

        int transferBudget = Math.min(this.getEffectiveChargePerTick(), this.energyStorage.getEnergyStored());
        if (transferBudget <= 0) {
            return false;
        }

        double range = 4.0D;
        boolean priority = MachineUpgradeHelper.getEquipmentPriorityEnabled(this);
        boolean moved = false;
        for (net.minecraft.world.entity.player.Player player : serverLevel.players()) {
            if (player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D) > range * range) {
                continue;
            }
            int used = this.chargePlayerInventory(player, transferBudget, priority);
            if (used > 0) {
                moved = true;
                transferBudget -= used;
            }
            if (transferBudget <= 0) {
                break;
            }
        }

        if (moved) {
            this.syncClient();
        }
        return moved;
    }

    private int chargePlayerInventory(net.minecraft.world.entity.player.Player player, int budget, boolean priority) {
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        if (priority) {
            stacks.addAll(player.getInventory().armor);
            stacks.addAll(player.getInventory().offhand);
            stacks.addAll(player.getInventory().items);
        } else {
            stacks.addAll(player.getInventory().items);
            stacks.addAll(player.getInventory().armor);
            stacks.addAll(player.getInventory().offhand);
        }

        int used = 0;
        int remainingBudget = budget;
        for (ItemStack stack : stacks) {
            if (remainingBudget <= 0) {
                break;
            }
            IEnergyStorage itemEnergy = resolveItemEnergyStorage(stack);
            if (itemEnergy == null || !itemEnergy.canReceive()) {
                continue;
            }

            int simulated = itemEnergy.receiveEnergy(remainingBudget, true);
            if (simulated <= 0) {
                continue;
            }

            int extracted = this.energyStorage.extractEnergy(simulated, false);
            if (extracted <= 0) {
                continue;
            }

            int inserted = itemEnergy.receiveEnergy(extracted, false);
            if (inserted < extracted) {
                this.energyStorage.receiveEnergy(extracted - inserted, false);
            }
            remainingBudget -= inserted;
            used += inserted;
        }

        return used;
    }

    private void updateChargingStatus(ItemStack stack) {
        int previous = this.chargingStatus;
        int next;
        if (stack.isEmpty()) {
            next = STATUS_EMPTY;
        } else {
            IEnergyStorage itemEnergy = resolveItemEnergyStorage(stack);
            if (itemEnergy == null) {
                next = STATUS_INCOMPATIBLE;
            } else if (itemEnergy.getEnergyStored() >= itemEnergy.getMaxEnergyStored()) {
                next = STATUS_FULL;
            } else if (this.energyStorage.getEnergyStored() <= 0) {
                next = STATUS_EMPTY;
            } else {
                next = STATUS_CHARGING;
            }
        }

        if (previous != next) {
            this.chargingStatus = next;
            this.syncClient();
        }
    }

    private void spawnChargeEffects(net.minecraft.server.level.ServerLevel level, boolean pulse) {
        if (MachineUpgradeHelper.getSilenced(this)) {
            return;
        }
        level.sendParticles(
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.15F, 0.85F, 1.0F), 1.0F),
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 1.25D,
                this.worldPosition.getZ() + 0.5D,
                pulse ? 6 : 3,
                0.12D,
                0.08D,
                0.12D,
                0.0D
        );
        if (pulse) {
            level.playSound(null, this.worldPosition, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS, 0.08F, 1.6F);
        }
    }

    private final class BatteryEnergyStorage extends EnergyStorage {
        private BatteryEnergyStorage() {
            super(MAX_ENERGY, MAX_RECEIVE, MAX_EXTRACT, 0);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int limit = BatteryT2BlockEntity.this.getEffectiveReceiveLimit();
            int space = BatteryT2BlockEntity.this.getEffectiveMaxEnergy() - this.energy;
            int received = Math.max(0, Math.min(toReceive, Math.min(limit, space)));
            if (!simulate && received > 0) {
                this.energy += received;
                BatteryT2BlockEntity.this.syncClient();
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int limit = BatteryT2BlockEntity.this.getEffectiveExtractLimit();
            int extracted = Math.max(0, Math.min(toExtract, Math.min(limit, this.energy)));
            if (!simulate && extracted > 0) {
                this.energy -= extracted;
                BatteryT2BlockEntity.this.syncClient();
            }
            return extracted;
        }

        @Override
        public int getMaxEnergyStored() {
            return BatteryT2BlockEntity.this.getEffectiveMaxEnergy();
        }

        @Override
        public boolean canReceive() {
            return this.energy < BatteryT2BlockEntity.this.getEffectiveMaxEnergy();
        }

        @Override
        public boolean canExtract() {
            return this.energy > 0;
        }

        private void setEnergyStored(int energy) {
            this.energy = Mth.clamp(energy, 0, BatteryT2BlockEntity.this.getEffectiveMaxEnergy());
        }
    }

    private static IEnergyStorage resolveItemEnergyStorage(ItemStack stack) {
        IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (storage != null) {
            return storage;
        }

        if (stack.is(Microtech.TECH_SWORD.get())) {
            return new TechSwordEnergyStorage(stack);
        }

        return null;
    }
}
