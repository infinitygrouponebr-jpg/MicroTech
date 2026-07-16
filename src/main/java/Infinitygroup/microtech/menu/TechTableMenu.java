package Infinitygroup.microtech.menu;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.block.entity.TechTableBlockEntity;
import Infinitygroup.microtech.machine.TechTableRecipeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TechTableMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = TechTableBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INV_START = SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;
    private static final String MACHINE_ID = "microtech:tech_table";

    private final Container container;
    private final ContainerLevelAccess access;
    private final TechTableBlockEntity blockEntity;
    private int blockPosX;
    private int blockPosY;
    private int blockPosZ;

    public TechTableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, ContainerLevelAccess.NULL);
    }

    public TechTableMenu(int containerId, Inventory playerInventory, TechTableBlockEntity blockEntity, ContainerLevelAccess access) {
        super(Microtech.TECH_TABLE_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;
        this.container = blockEntity != null ? blockEntity : new SimpleContainer(SLOT_COUNT);
        this.container.startOpen(playerInventory.player);

        if (blockEntity != null) {
            this.blockPosX = blockEntity.getBlockPos().getX();
            this.blockPosY = blockEntity.getBlockPos().getY();
            this.blockPosZ = blockEntity.getBlockPos().getZ();
        }

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return TechTableMenu.this.blockEntity != null ? TechTableMenu.this.blockEntity.getBlockPos().getX() : TechTableMenu.this.blockPosX;
            }

            @Override
            public void set(int value) {
                TechTableMenu.this.blockPosX = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return TechTableMenu.this.blockEntity != null ? TechTableMenu.this.blockEntity.getBlockPos().getY() : TechTableMenu.this.blockPosY;
            }

            @Override
            public void set(int value) {
                TechTableMenu.this.blockPosY = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return TechTableMenu.this.blockEntity != null ? TechTableMenu.this.blockEntity.getBlockPos().getZ() : TechTableMenu.this.blockPosZ;
            }

            @Override
            public void set(int value) {
                TechTableMenu.this.blockPosZ = value;
            }
        });

        this.addSlot(new Slot(this.container, TechTableBlockEntity.SLOT_INPUT, 56, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return (TechTableMenu.this.blockEntity == null || !TechTableMenu.this.blockEntity.isWorking()) && TechTableRecipeHelper.isValidInput(stack);
            }

            @Override
            public boolean mayPickup(Player playerIn) {
                return TechTableMenu.this.blockEntity == null || !TechTableMenu.this.blockEntity.isWorking();
            }
        });
        this.addSlot(new Slot(this.container, TechTableBlockEntity.SLOT_OUTPUT, 116, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addPlayerInventory(playerInventory);
        addHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 110 + row * 18));
            }
        }
    }

    private void addHotbar(Inventory playerInventory) {
        for (int column = 0; column < 9; ++column) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 168));
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

        if (index < SLOT_COUNT) {
            if (!this.moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stackInSlot, copy);
        } else if (TechTableRecipeHelper.isValidInput(stackInSlot)) {
            if (!this.moveItemStackTo(stackInSlot, TechTableBlockEntity.SLOT_INPUT, TechTableBlockEntity.SLOT_INPUT + 1, false)) {
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
        if (this.blockEntity == null) {
            return super.clickMenuButton(player, id);
        }

        if (id == 0) {
            if (this.blockEntity.isWorking()) {
                if (!this.blockEntity.isSessionOwner(player)) {
                    return false;
                }
                this.blockEntity.cancelSession(false);
                return true;
            }
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                return this.blockEntity.startSession(serverPlayer);
            }
        }

        return super.clickMenuButton(player, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Microtech.TECH_TABLE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    public TechTableBlockEntity getBlockEntity(net.minecraft.world.level.Level level) {
        if (this.blockEntity != null) {
            return this.blockEntity;
        }
        if (level == null) {
            return null;
        }
        BlockPos pos = this.getBlockPos();
        if (level.getBlockEntity(pos) instanceof TechTableBlockEntity tableBlockEntity) {
            return tableBlockEntity;
        }
        return null;
    }

    public BlockPos getBlockPos() {
        return new BlockPos(this.blockPosX, this.blockPosY, this.blockPosZ);
    }

    public boolean isFor(BlockPos pos) {
        return this.getBlockPos().equals(pos);
    }
}
