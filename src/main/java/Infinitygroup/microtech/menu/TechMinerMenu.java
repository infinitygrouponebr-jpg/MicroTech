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

public class TechMinerMenu extends AbstractContainerMenu {
    public static final int UPGRADE_X = 184;
    public static final int UPGRADE_Y = 28;
    public static final int UPGRADE_SLOT_COUNT = TechMinerBlockEntity.UPGRADE_SLOTS;
    public static final int PLAYER_INV_X = 28;
    public static final int PLAYER_INV_Y = 142;
    public static final int HOTBAR_Y = 200;
    private static final String MACHINE_ID = "microtech:tech_miner";

    private final ContainerLevelAccess access;
    private final TechMinerBlockEntity blockEntity;
    private final MachineUpgradeInventory upgradeInventory;
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

        for (int slot = 0; slot < UPGRADE_SLOT_COUNT; slot++) {
            this.addSlot(new SlotItemHandler(this.upgradeInventory, slot, UPGRADE_X, UPGRADE_Y + slot * 20));
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

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, PLAYER_INV_X + column * 18, PLAYER_INV_Y + row * 18));
            }
        }
    }

    private void addHotbar(Inventory playerInventory) {
        for (int column = 0; column < 9; ++column) {
            this.addSlot(new Slot(playerInventory, column, PLAYER_INV_X + column * 18, HOTBAR_Y));
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
        if (this.blockEntity == null || !this.stillValid(player)) {
            return super.clickMenuButton(player, id);
        }

        if (id == 0) {
            return this.blockEntity.startScan(player);
        }
        if (id == 1) {
            return this.blockEntity.startMining(player);
        }
        if (id == 2) {
            return this.blockEntity.stopMining();
        }
        if (id == 3 && player instanceof ServerPlayer serverPlayer) {
            this.openOutputMenu(serverPlayer);
            return true;
        }
        if (id == 4 && player instanceof ServerPlayer serverPlayer) {
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

    public MachineStatus getStatus(Level level) {
        TechMinerBlockEntity blockEntity = this.getBlockEntity(level);
        return blockEntity != null ? blockEntity.getStatus() : MachineStatus.IDLE;
    }

    public int getEnergyStored(Level level) {
        TechMinerBlockEntity blockEntity = this.getBlockEntity(level);
        return blockEntity != null ? blockEntity.getEnergyStored() : 0;
    }

    public int getMaxEnergy(Level level) {
        TechMinerBlockEntity blockEntity = this.getBlockEntity(level);
        return blockEntity != null ? blockEntity.getMaxEnergy() : TechMinerBlockEntity.MAX_ENERGY;
    }

    public int getProcessTicks(Level level) {
        TechMinerBlockEntity blockEntity = this.getBlockEntity(level);
        return blockEntity != null ? blockEntity.getProcessTicks() : 0;
    }

    public int getProcessDuration(Level level) {
        TechMinerBlockEntity blockEntity = this.getBlockEntity(level);
        return blockEntity != null ? blockEntity.getProcessDuration() : TechMinerBlockEntity.PROCESS_TICKS;
    }

    public int getTargetCount(Level level) {
        TechMinerBlockEntity blockEntity = this.getBlockEntity(level);
        return blockEntity != null ? blockEntity.getTargetPositions().size() : 0;
    }

    public int getFilterCapacity(Level level) {
        TechMinerBlockEntity blockEntity = this.getBlockEntity(level);
        return blockEntity != null ? blockEntity.getFilterCapacity() : 0;
    }

    public int getActiveFilterEntryCount(Level level) {
        TechMinerBlockEntity blockEntity = this.getBlockEntity(level);
        return blockEntity != null ? blockEntity.getActiveFilterEntryCount() : 0;
    }
}
