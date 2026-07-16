package Infinitygroup.microtech.menu;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.block.entity.SolarPanelBlockEntity;
import Infinitygroup.microtech.machine.MachineUpgradeHelper;
import Infinitygroup.microtech.machine.MachineUpgradeInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class SolarPanelMenu extends AbstractContainerMenu {
    private static final int UPGRADE_SLOT_COUNT = 2;
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int UPGRADE_X = 148;
    private static final int UPGRADE_Y = 20;
    private static final String MACHINE_ID = "microtech:solar_panel_t1";

    private final SolarPanelBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final MachineUpgradeInventory upgradeInventory;
    private int energyStored;
    private int generationPerTick;
    private int statusOrdinal;

    public SolarPanelMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, ContainerLevelAccess.NULL);
    }

    public SolarPanelMenu(int containerId, Inventory playerInventory, SolarPanelBlockEntity blockEntity, ContainerLevelAccess access) {
        super(Microtech.SOLAR_PANEL_T1_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;
        this.upgradeInventory = this.blockEntity != null ? this.blockEntity.getUpgradeInventory() : new MachineUpgradeInventory(MACHINE_ID, UPGRADE_SLOT_COUNT, () -> false);

        if (this.blockEntity != null) {
            this.energyStored = this.blockEntity.getEnergyStored();
            this.generationPerTick = this.blockEntity.getGenerationPerTick();
            this.statusOrdinal = this.blockEntity.getStatus().ordinal();
        }

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return SolarPanelMenu.this.blockEntity != null ? SolarPanelMenu.this.blockEntity.getEnergyStored() : SolarPanelMenu.this.energyStored;
            }

            @Override
            public void set(int value) {
                SolarPanelMenu.this.energyStored = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return SolarPanelMenu.this.blockEntity != null ? SolarPanelMenu.this.blockEntity.getGenerationPerTick() : SolarPanelMenu.this.generationPerTick;
            }

            @Override
            public void set(int value) {
                SolarPanelMenu.this.generationPerTick = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return SolarPanelMenu.this.blockEntity != null ? SolarPanelMenu.this.blockEntity.getStatus().ordinal() : SolarPanelMenu.this.statusOrdinal;
            }

            @Override
            public void set(int value) {
                SolarPanelMenu.this.statusOrdinal = value;
            }
        });

        this.addSlot(new SlotItemHandler(this.upgradeInventory, 0, UPGRADE_X, UPGRADE_Y));
        this.addSlot(new SlotItemHandler(this.upgradeInventory, 1, UPGRADE_X, UPGRADE_Y + 18));

        this.addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < UPGRADE_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, UPGRADE_SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (MachineUpgradeHelper.isCompatibleUpgrade(MACHINE_ID, stack)) {
            if (!this.moveItemStackTo(stack, 0, UPGRADE_SLOT_COUNT, false)) {
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

        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Microtech.SOLAR_PANEL_T1.get());
    }

    public int getEnergyStored() {
        return this.blockEntity != null ? this.blockEntity.getEnergyStored() : this.energyStored;
    }

    public int getMaxEnergy() {
        return SolarPanelBlockEntity.MAX_ENERGY;
    }

    public int getGenerationPerTick() {
        return this.blockEntity != null ? this.blockEntity.getGenerationPerTick() : this.generationPerTick;
    }

    public SolarPanelBlockEntity.SolarStatus getStatus() {
        SolarPanelBlockEntity.SolarStatus[] values = SolarPanelBlockEntity.SolarStatus.values();
        int index = this.blockEntity != null ? this.blockEntity.getStatus().ordinal() : this.statusOrdinal;
        if (index < 0 || index >= values.length) {
            return SolarPanelBlockEntity.SolarStatus.NO_SUN;
        }
        return values[index];
    }

    public String getStatusTranslationKey() {
        return getStatus().getTranslationKey();
    }
}
