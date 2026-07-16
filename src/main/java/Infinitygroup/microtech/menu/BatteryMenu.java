package Infinitygroup.microtech.menu;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.block.entity.BatteryBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public class BatteryMenu extends AbstractContainerMenu {
    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 86;
    public static final int HOTBAR_Y = 144;

    private final BatteryBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private int energyStored;
    private int maxEnergy = BatteryBlockEntity.MAX_ENERGY;
    private int chargingStatus = BatteryBlockEntity.STATUS_IDLE;

    public BatteryMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, ContainerLevelAccess.NULL);
    }

    public BatteryMenu(int containerId, Inventory playerInventory, BatteryBlockEntity blockEntity, ContainerLevelAccess access) {
        super(Microtech.BATTERY_T1_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;

        if (this.blockEntity != null) {
            this.energyStored = this.blockEntity.getEnergyStored();
            this.maxEnergy = this.blockEntity.getMaxEnergy();
            this.chargingStatus = this.blockEntity.getChargingStatus();
        }

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return BatteryMenu.this.blockEntity != null ? BatteryMenu.this.blockEntity.getEnergyStored() : BatteryMenu.this.energyStored;
            }

            @Override
            public void set(int value) {
                BatteryMenu.this.energyStored = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return BatteryMenu.this.blockEntity != null ? BatteryMenu.this.blockEntity.getMaxEnergy() : BatteryMenu.this.maxEnergy;
            }

            @Override
            public void set(int value) {
                BatteryMenu.this.maxEnergy = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return BatteryMenu.this.blockEntity != null ? BatteryMenu.this.blockEntity.getChargingStatus() : BatteryMenu.this.chargingStatus;
            }

            @Override
            public void set(int value) {
                BatteryMenu.this.chargingStatus = value;
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
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Microtech.BATTERY_T1.get());
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
}
