package Infinitygroup.microtech.menu;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.block.entity.BasicMachineBlockEntity;
import Infinitygroup.microtech.machine.MachineUpgradeHelper;
import Infinitygroup.microtech.machine.MachineUpgradeInventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class BasicMachineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = 2;
    private static final int UPGRADE_SLOT_COUNT = 2;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT + UPGRADE_SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;
    private static final String MACHINE_ID = "microtech:energy_converter_t1";

    private final Container container;
    private final ContainerLevelAccess access;
    private final BasicMachineBlockEntity blockEntity;
    private final MachineUpgradeInventory upgradeInventory;
    private int energyStored;
    private int maxEnergy = BasicMachineBlockEntity.MAX_ENERGY;
    private int pendingEnergy;

    public BasicMachineMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(MACHINE_SLOT_COUNT), ContainerLevelAccess.NULL);
    }

    public BasicMachineMenu(int containerId, Inventory playerInventory, Container container, ContainerLevelAccess access) {
        super(Microtech.ENERGY_CONVERTER_T1_MENU.get(), containerId);
        this.container = container;
        this.access = access;
        this.blockEntity = container instanceof BasicMachineBlockEntity be ? be : null;
        this.upgradeInventory = this.blockEntity != null ? this.blockEntity.getUpgradeInventory() : new MachineUpgradeInventory(MACHINE_ID, UPGRADE_SLOT_COUNT, () -> false);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        container.startOpen(playerInventory.player);

        if (this.blockEntity != null) {
            this.energyStored = this.blockEntity.getEnergyStored();
            this.maxEnergy = this.blockEntity.getMaxEnergy();
            this.pendingEnergy = this.blockEntity.getPendingEnergy();
        }

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (BasicMachineMenu.this.blockEntity != null) {
                    return BasicMachineMenu.this.blockEntity.getEnergyStored();
                }
                return BasicMachineMenu.this.energyStored;
            }

            @Override
            public void set(int value) {
                BasicMachineMenu.this.energyStored = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (BasicMachineMenu.this.blockEntity != null) {
                    return BasicMachineMenu.this.blockEntity.getMaxEnergy();
                }
                return BasicMachineMenu.this.maxEnergy;
            }

            @Override
            public void set(int value) {
                BasicMachineMenu.this.maxEnergy = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (BasicMachineMenu.this.blockEntity != null) {
                    return BasicMachineMenu.this.blockEntity.getPendingEnergy();
                }
                return BasicMachineMenu.this.pendingEnergy;
            }

            @Override
            public void set(int value) {
                BasicMachineMenu.this.pendingEnergy = value;
            }
        });

        this.addSlot(new Slot(container, 0, 56, 66) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BasicMachineBlockEntity.isConvertible(stack);
            }
        });
        this.addSlot(new Slot(container, 1, 116, 66) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        this.addSlot(new SlotItemHandler(this.upgradeInventory, 0, 148, 20));
        this.addSlot(new SlotItemHandler(this.upgradeInventory, 1, 148, 38));

        addPlayerInventory(playerInventory);
        addHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
    }

    private void addHotbar(Inventory playerInventory) {
        for (int column = 0; column < 9; ++column) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
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

        if (index < MACHINE_SLOT_COUNT + UPGRADE_SLOT_COUNT) {
            if (!this.moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stackInSlot, copy);
        } else if (MachineUpgradeHelper.isCompatibleUpgrade(MACHINE_ID, stackInSlot)) {
            if (!this.moveItemStackTo(stackInSlot, MACHINE_SLOT_COUNT, MACHINE_SLOT_COUNT + UPGRADE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
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
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Microtech.ENERGY_CONVERTER_T1.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    public int getEnergyStored() {
        return this.energyStored;
    }

    public int getMaxEnergy() {
        return this.maxEnergy;
    }

    public int getPendingEnergy() {
        return this.pendingEnergy;
    }

    public ItemStack getInputStack() {
        return this.slots.get(0).getItem();
    }

    public ItemStack getOutputStack() {
        return this.slots.get(1).getItem();
    }
}
