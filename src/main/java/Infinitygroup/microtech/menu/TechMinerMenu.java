package Infinitygroup.microtech.menu;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.block.TechMinerBlock;
import Infinitygroup.microtech.block.entity.TechMinerBlockEntity;
import Infinitygroup.microtech.machine.MachineStatus;
import Infinitygroup.microtech.machine.MachineUpgradeHelper;
import Infinitygroup.microtech.machine.MachineUpgradeInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.IntSupplier;

public class TechMinerMenu extends AbstractContainerMenu {
    public static final int SLOT_SIZE = 18;
    public static final int[] UPGRADE_SLOT_X = {28, 8, 48, 68};
    public static final int UPGRADE_Y = 26;
    public static final int UPGRADE_SLOT_COUNT = TechMinerBlockEntity.UPGRADE_SLOTS;
    public static final int PLAYER_INV_X = 6;
    public static final int PLAYER_INV_Y = 156;
    public static final int HOTBAR_Y = 214;
    private static final String MACHINE_ID = "microtech:tech_miner";
    private static final int BUTTON_SCAN = 0;
    private static final int BUTTON_START = 1;
    private static final int BUTTON_STOP = 2;
    private static final int BUTTON_OUTPUT = 3;
    private static final int BUTTON_FILTER = 4;
    private static final int FLAG_PROCESSING_ACTIVE = 1;
    private static final int FLAG_HAS_SCAN_RESULT = 1 << 1;
    private static final int FLAG_MANUALLY_PAUSED = 1 << 2;
    private static final int FLAG_CAN_START_SCAN = 1 << 3;
    private static final int FLAG_CAN_START_MINING = 1 << 4;

    private final ContainerLevelAccess access;
    private final TechMinerBlockEntity blockEntity;
    private final MachineUpgradeInventory upgradeInventory;
    private final SyncedInt energyStored = new SyncedInt();
    private final SyncedInt maxEnergy = new SyncedInt();
    private final SyncedInt processTicks = new SyncedInt();
    private final SyncedInt processDuration = new SyncedInt();
    private final SyncedInt targetCount = new SyncedInt();
    private int syncedFilterCapacity;
    private int syncedActiveFilterEntries;
    private int syncedStatusOrdinal = MachineStatus.IDLE.ordinal();
    private int syncedFlags;
    private int blockPosX;
    private int blockPosY;
    private int blockPosZ;

    public TechMinerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, ContainerLevelAccess.NULL);
    }

    public TechMinerMenu(int containerId, Inventory playerInventory, TechMinerBlockEntity blockEntity, ContainerLevelAccess access) {
        super(Microtech.TECH_MINER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;
        this.upgradeInventory = this.blockEntity != null
                ? this.blockEntity.getUpgradeInventory()
                : new MachineUpgradeInventory(MACHINE_ID, UPGRADE_SLOT_COUNT, () -> false);

        if (this.blockEntity != null) {
            this.blockEntity.startOpen(playerInventory.player);
            this.blockPosX = this.blockEntity.getBlockPos().getX();
            this.blockPosY = this.blockEntity.getBlockPos().getY();
            this.blockPosZ = this.blockEntity.getBlockPos().getZ();
        }

        this.addPositionDataSlots();
        this.addMachineDataSlots();

        for (int slot = 0; slot < UPGRADE_SLOT_COUNT; slot++) {
            this.addSlot(new SlotItemHandler(this.upgradeInventory, slot, UPGRADE_SLOT_X[slot], UPGRADE_Y));
        }

        this.addPlayerInventory(playerInventory);
        this.addHotbar(playerInventory);
    }

    private void addPositionDataSlots() {
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return TechMinerMenu.this.blockEntity != null ? TechMinerMenu.this.blockEntity.getBlockPos().getX() : TechMinerMenu.this.blockPosX;
            }

            @Override
            public void set(int value) {
                TechMinerMenu.this.blockPosX = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return TechMinerMenu.this.blockEntity != null ? TechMinerMenu.this.blockEntity.getBlockPos().getY() : TechMinerMenu.this.blockPosY;
            }

            @Override
            public void set(int value) {
                TechMinerMenu.this.blockPosY = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return TechMinerMenu.this.blockEntity != null ? TechMinerMenu.this.blockEntity.getBlockPos().getZ() : TechMinerMenu.this.blockPosZ;
            }

            @Override
            public void set(int value) {
                TechMinerMenu.this.blockPosZ = value;
            }
        });
    }

    private void addMachineDataSlots() {
        this.addSyncedInt(() -> this.blockEntity != null ? this.blockEntity.getEnergyStored() : this.energyStored.get(), this.energyStored);
        this.addSyncedInt(() -> this.blockEntity != null ? this.blockEntity.getMaxEnergy() : this.maxEnergy.get(), this.maxEnergy);
        this.addSyncedInt(() -> this.blockEntity != null ? this.blockEntity.getProcessTicks() : this.processTicks.get(), this.processTicks);
        this.addSyncedInt(() -> this.blockEntity != null ? this.blockEntity.getProcessDuration() : this.processDuration.get(), this.processDuration);
        this.addSyncedInt(() -> this.blockEntity != null ? this.blockEntity.getTargetPositions().size() : this.targetCount.get(), this.targetCount);
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return TechMinerMenu.this.blockEntity != null ? TechMinerMenu.this.blockEntity.getFilterCapacity() : TechMinerMenu.this.syncedFilterCapacity;
            }

            @Override
            public void set(int value) {
                TechMinerMenu.this.syncedFilterCapacity = Math.max(0, value);
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return TechMinerMenu.this.blockEntity != null ? TechMinerMenu.this.blockEntity.getActiveFilterEntryCount() : TechMinerMenu.this.syncedActiveFilterEntries;
            }

            @Override
            public void set(int value) {
                TechMinerMenu.this.syncedActiveFilterEntries = Math.max(0, value);
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return TechMinerMenu.this.blockEntity != null ? TechMinerMenu.this.blockEntity.getStatus().ordinal() : TechMinerMenu.this.syncedStatusOrdinal;
            }

            @Override
            public void set(int value) {
                TechMinerMenu.this.syncedStatusOrdinal = Mth.clamp(value, 0, MachineStatus.values().length - 1);
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return TechMinerMenu.this.blockEntity != null ? TechMinerMenu.this.buildServerFlags() : TechMinerMenu.this.syncedFlags;
            }

            @Override
            public void set(int value) {
                TechMinerMenu.this.syncedFlags = value;
            }
        });
    }

    private void addSyncedInt(IntSupplier getter, SyncedInt syncedInt) {
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getter.getAsInt() & 0xFFFF;
            }

            @Override
            public void set(int value) {
                syncedInt.setLow(value);
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (getter.getAsInt() >>> 16) & 0xFFFF;
            }

            @Override
            public void set(int value) {
                syncedInt.setHigh(value);
            }
        });
    }

    private int buildServerFlags() {
        if (this.blockEntity == null) {
            return this.syncedFlags;
        }

        int flags = 0;
        if (this.blockEntity.isProcessing()) {
            flags |= FLAG_PROCESSING_ACTIVE;
        }
        if (this.blockEntity.hasScanResult()) {
            flags |= FLAG_HAS_SCAN_RESULT;
        }
        if (this.blockEntity.isManuallyPaused()) {
            flags |= FLAG_MANUALLY_PAUSED;
        }
        if (this.blockEntity.canStartScan()) {
            flags |= FLAG_CAN_START_SCAN;
        }
        if (this.blockEntity.canStartMining()) {
            flags |= FLAG_CAN_START_MINING;
        }
        return flags;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, PLAYER_INV_X + column * SLOT_SIZE, PLAYER_INV_Y + row * SLOT_SIZE));
            }
        }
    }

    private void addHotbar(Inventory playerInventory) {
        for (int column = 0; column < 9; ++column) {
            this.addSlot(new Slot(playerInventory, column, PLAYER_INV_X + column * SLOT_SIZE, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack copy = stackInSlot.copy();

        if (index < UPGRADE_SLOT_COUNT) {
            if (!this.moveItemStackTo(stackInSlot, UPGRADE_SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stackInSlot, copy);
        } else if (MachineUpgradeHelper.isCompatibleUpgrade(MACHINE_ID, stackInSlot)) {
            if (!this.moveItemStackTo(stackInSlot, 0, UPGRADE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stackInSlot.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stackInSlot);
        return copy;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide) {
            return id >= BUTTON_SCAN && id <= BUTTON_FILTER;
        }

        if (this.blockEntity == null || !this.stillValid(player)) {
            if (id == BUTTON_OUTPUT) {
                player.displayClientMessage(Component.translatable("message.microtech.tech_miner.open_output_failed"), true);
            }
            return super.clickMenuButton(player, id);
        }

        if (id == BUTTON_SCAN) {
            return this.blockEntity.startScan(player);
        }
        if (id == BUTTON_START) {
            return this.blockEntity.startMining(player);
        }
        if (id == BUTTON_STOP) {
            return this.blockEntity.stopMining();
        }
        if (id == BUTTON_OUTPUT && player instanceof ServerPlayer serverPlayer) {
            this.openOutputMenu(serverPlayer);
            return true;
        }
        if (id == BUTTON_FILTER && player instanceof ServerPlayer serverPlayer) {
            if (!this.blockEntity.hasFilterUpgrade()) {
                player.displayClientMessage(Component.translatable("message.microtech.tech_miner.filter_requires_upgrade"), true);
                return false;
            }
            this.openFilterMenu(serverPlayer);
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    private void openOutputMenu(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new TechMinerOutputMenu(containerId, inventory, this.blockEntity, this.access),
                Component.translatable("container.microtech.tech_miner.output")
        ));
    }

    private void openFilterMenu(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new TechMinerFilterMenu(containerId, inventory, this.blockEntity, this.access),
                Component.translatable("container.microtech.tech_miner.filter")
        ));
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.blockEntity == null) {
            return stillValid(this.access, player, Microtech.TECH_MINER.get());
        }
        return isUsable(this.blockEntity, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.blockEntity != null) {
            this.blockEntity.stopOpen(player);
        }
    }

    public static boolean isUsable(TechMinerBlockEntity blockEntity, Player player) {
        if (blockEntity == null || blockEntity.isRemoved() || blockEntity.getLevel() == null) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        BlockState state = blockEntity.getLevel().getBlockState(pos);
        return state.is(Microtech.TECH_MINER.get())
                && blockEntity.stillValid(player)
                && TechMinerBlock.canUseFromOperationSide(state, pos, player);
    }

    public TechMinerBlockEntity getBlockEntity(Level level) {
        if (this.blockEntity != null) {
            return this.blockEntity;
        }
        if (level == null) {
            return null;
        }
        BlockPos pos = this.getBlockPos();
        if (level.getBlockEntity(pos) instanceof TechMinerBlockEntity techMinerBlockEntity) {
            return techMinerBlockEntity;
        }
        return null;
    }

    public BlockPos getBlockPos() {
        return new BlockPos(this.blockPosX, this.blockPosY, this.blockPosZ);
    }

    public MachineStatus getStatus() {
        if (this.blockEntity != null) {
            return this.blockEntity.getStatus();
        }
        return MachineStatus.values()[Mth.clamp(this.syncedStatusOrdinal, 0, MachineStatus.values().length - 1)];
    }

    public Component getStatusText() {
        MachineStatus status = this.getStatus();
        return status != null ? status.getText() : Component.translatable("status.microtech.idle");
    }

    public MachineStatus getStatus(Level level) {
        return this.getStatus();
    }

    public int getEnergyStored() {
        return this.blockEntity != null ? this.blockEntity.getEnergyStored() : Math.max(0, this.energyStored.get());
    }

    public int getEnergyStored(Level level) {
        return this.getEnergyStored();
    }

    public int getMaxEnergy() {
        return this.blockEntity != null ? this.blockEntity.getMaxEnergy() : Math.max(0, this.maxEnergy.get());
    }

    public int getMaxEnergy(Level level) {
        return this.getMaxEnergy();
    }

    public int getProcessTicks() {
        return this.blockEntity != null ? this.blockEntity.getProcessTicks() : Math.max(0, this.processTicks.get());
    }

    public int getProcessTicks(Level level) {
        return this.getProcessTicks();
    }

    public int getProcessDuration() {
        return this.blockEntity != null ? this.blockEntity.getProcessDuration() : Math.max(0, this.processDuration.get());
    }

    public int getProcessDuration(Level level) {
        return this.getProcessDuration();
    }

    public int getTargetCount() {
        return this.blockEntity != null ? this.blockEntity.getTargetPositions().size() : Math.max(0, this.targetCount.get());
    }

    public int getTargetCount(Level level) {
        return this.getTargetCount();
    }

    public int getFilterCapacity() {
        return this.blockEntity != null ? this.blockEntity.getFilterCapacity() : Math.max(0, this.syncedFilterCapacity);
    }

    public int getFilterCapacity(Level level) {
        return this.getFilterCapacity();
    }

    public int getActiveFilterEntryCount() {
        return this.blockEntity != null ? this.blockEntity.getActiveFilterEntryCount() : Math.max(0, this.syncedActiveFilterEntries);
    }

    public Component getFilterStatusText() {
        int capacity = this.getFilterCapacity();
        if (capacity <= 0) {
            return Component.translatable("gui.microtech.tech_miner.filter_disabled_value");
        }
        return Component.translatable("gui.microtech.tech_miner.filter_active_value", this.getActiveFilterEntryCount(), capacity);
    }

    public Component getNextTargetText() {
        return Component.translatable("gui.microtech.tech_miner.no_targets");
    }

    public int getActiveFilterEntryCount(Level level) {
        return this.getActiveFilterEntryCount();
    }

    public boolean isProcessing() {
        return this.blockEntity != null ? this.blockEntity.isProcessing() : (this.syncedFlags & FLAG_PROCESSING_ACTIVE) != 0;
    }

    public boolean hasScanResult() {
        return this.blockEntity != null ? this.blockEntity.hasScanResult() : (this.syncedFlags & FLAG_HAS_SCAN_RESULT) != 0;
    }

    public boolean isManuallyPaused() {
        return this.blockEntity != null ? this.blockEntity.isManuallyPaused() : (this.syncedFlags & FLAG_MANUALLY_PAUSED) != 0;
    }

    public boolean canStartScan() {
        return this.blockEntity != null ? this.blockEntity.canStartScan() : (this.syncedFlags & FLAG_CAN_START_SCAN) != 0;
    }

    public boolean canStartMining() {
        return this.blockEntity != null ? this.blockEntity.canStartMining() : (this.syncedFlags & FLAG_CAN_START_MINING) != 0;
    }

    public int getProgressPercent() {
        if (!this.isProcessing() || this.getTargetCount() <= 0 || this.getProcessTicks() <= 0) {
            return 0;
        }

        int duration = this.getProcessDuration();
        if (duration <= 0) {
            return 0;
        }

        int remaining = Mth.clamp(this.getProcessTicks(), 0, duration);
        int completed = duration - remaining;
        return Mth.clamp(completed * 100 / duration, 0, 100);
    }

    public int getProgressScaled(int width) {
        if (width <= 0) {
            return 0;
        }
        return Mth.clamp(this.getProgressPercent() * width / 100, 0, width);
    }

    private static final class SyncedInt {
        private int low;
        private int high;

        private int get() {
            return (this.high << 16) | this.low;
        }

        private void setLow(int value) {
            this.low = value & 0xFFFF;
        }

        private void setHigh(int value) {
            this.high = value & 0xFFFF;
        }
    }
}
