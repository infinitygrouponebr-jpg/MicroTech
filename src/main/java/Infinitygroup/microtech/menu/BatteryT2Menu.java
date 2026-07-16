package Infinitygroup.microtech.menu;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.block.entity.BatteryT2BlockEntity;
import Infinitygroup.microtech.machine.MachineUpgradeHelper;
import Infinitygroup.microtech.machine.MachineUpgradeInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class BatteryT2Menu extends AbstractContainerMenu {
    private static final int UPGRADE_SLOT_COUNT = 2;
    public static final int SLOT_X = 80;
    public static final int SLOT_Y = 78;
    public static final int UPGRADE_X = 148;
    public static final int UPGRADE_Y = 20;
    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 108;
    public static final int HOTBAR_Y = 166;
    private static final String MACHINE_ID = "microtech:battery_t2";

    private final BatteryT2BlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final MachineUpgradeInventory upgradeInventory;
    private int energyStored;
    private int maxEnergy = BatteryT2BlockEntity.MAX_ENERGY;
    private int itemEnergyStored;
    private int itemMaxEnergy;
    private int chargingStatus = BatteryT2BlockEntity.STATUS_EMPTY;

    public BatteryT2Menu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, ContainerLevelAccess.NULL);
    }

    public BatteryT2Menu(int containerId, Inventory playerInventory, BatteryT2BlockEntity blockEntity, ContainerLevelAccess access) {
        super(Microtech.BATTERY_T2_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;
        this.upgradeInventory = this.blockEntity != null ? this.blockEntity.getUpgradeInventory() : new MachineUpgradeInventory(MACHINE_ID, UPGRADE_SLOT_COUNT, () -> false);

        if (this.blockEntity != null) {
            this.energyStored = this.blockEntity.getEnergyStored();
            this.maxEnergy = this.blockEntity.getMaxEnergy();
            this.itemEnergyStored = this.blockEntity.getChargingItemEnergyStored();
            this.itemMaxEnergy = this.blockEntity.getChargingItemMaxEnergy();
            this.chargingStatus = this.blockEntity.getChargingStatus();
        }

        if (this.blockEntity != null) {
            this.addSlot(new SlotItemHandler(this.blockEntity.getItemHandler(), 0, SLOT_X, SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM) != null || stack.is(Microtech.TECH_SWORD.get());
                }
            });
        } else {
            this.addSlot(new SlotItemHandler(new net.neoforged.neoforge.items.ItemStackHandler(1), 0, SLOT_X, SLOT_Y));
        }
        this.addSlot(new SlotItemHandler(this.upgradeInventory, 0, UPGRADE_X, UPGRADE_Y));
        this.addSlot(new SlotItemHandler(this.upgradeInventory, 1, UPGRADE_X, UPGRADE_Y + 18));

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return BatteryT2Menu.this.blockEntity != null ? BatteryT2Menu.this.blockEntity.getEnergyStored() : BatteryT2Menu.this.energyStored;
            }

            @Override
            public void set(int value) {
                BatteryT2Menu.this.energyStored = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return BatteryT2Menu.this.blockEntity != null ? BatteryT2Menu.this.blockEntity.getMaxEnergy() : BatteryT2Menu.this.maxEnergy;
            }

            @Override
            public void set(int value) {
                BatteryT2Menu.this.maxEnergy = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return BatteryT2Menu.this.blockEntity != null ? BatteryT2Menu.this.blockEntity.getChargingItemEnergyStored() : BatteryT2Menu.this.itemEnergyStored;
            }

            @Override
            public void set(int value) {
                BatteryT2Menu.this.itemEnergyStored = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return BatteryT2Menu.this.blockEntity != null ? BatteryT2Menu.this.blockEntity.getChargingItemMaxEnergy() : BatteryT2Menu.this.itemMaxEnergy;
            }

            @Override
            public void set(int value) {
                BatteryT2Menu.this.itemMaxEnergy = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return BatteryT2Menu.this.blockEntity != null ? BatteryT2Menu.this.blockEntity.getChargingStatus() : BatteryT2Menu.this.chargingStatus;
            }

            @Override
            public void set(int value) {
                BatteryT2Menu.this.chargingStatus = value;
            }
        });

        this.addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        SlotItemHandler batterySlot = (SlotItemHandler) this.slots.get(0);
        net.minecraft.world.inventory.Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < 3) {
            if (!this.moveItemStackTo(stack, 3, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.setChanged();
        } else if (MachineUpgradeHelper.isCompatibleUpgrade(MACHINE_ID, stack)) {
            if (!this.moveItemStackTo(stack, 1, 3, false)) {
                return ItemStack.EMPTY;
            }
        } else if (batterySlot.mayPlace(stack)) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Microtech.BATTERY_T2.get());
    }

    public int getEnergyStored() {
        return this.blockEntity != null ? this.blockEntity.getEnergyStored() : this.energyStored;
    }

    public int getMaxEnergy() {
        return this.blockEntity != null ? this.blockEntity.getMaxEnergy() : this.maxEnergy;
    }

    public int getChargingStatus() {
        return this.blockEntity != null ? this.blockEntity.getChargingStatus() : this.chargingStatus;
    }

    public int getChargingItemEnergyStored() {
        return this.blockEntity != null ? this.blockEntity.getChargingItemEnergyStored() : this.itemEnergyStored;
    }

    public int getChargingItemMaxEnergy() {
        return this.blockEntity != null ? this.blockEntity.getChargingItemMaxEnergy() : this.itemMaxEnergy;
    }

    public ItemStack getChargingStack() {
        return this.slots.get(0).getItem();
    }
}
