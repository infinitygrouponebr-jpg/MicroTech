package Infinitygroup.microtech.menu;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.block.entity.TechMinerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TechMinerOutputMenu extends AbstractContainerMenu {
    public static final int OUTPUT_SLOT_COUNT = TechMinerBlockEntity.OUTPUT_SLOTS;
    public static final int OUTPUT_X = 27;
    public static final int OUTPUT_Y = 28;
    public static final int PLAYER_INV_X = 27;
    public static final int PLAYER_INV_Y = 100;
    public static final int HOTBAR_Y = 158;

    private final ContainerLevelAccess access;
    private final TechMinerBlockEntity blockEntity;
    private final Container outputContainer;
    private int blockPosX;
    private int blockPosY;
    private int blockPosZ;

    public TechMinerOutputMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, ContainerLevelAccess.NULL);
    }

    public TechMinerOutputMenu(int containerId, Inventory playerInventory, TechMinerBlockEntity blockEntity, ContainerLevelAccess access) {
        super(Microtech.TECH_MINER_OUTPUT_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;
        this.outputContainer = this.blockEntity != null ? this.blockEntity : new SimpleContainer(OUTPUT_SLOT_COUNT);

        if (this.blockEntity != null) {
            checkContainerSize(this.blockEntity, OUTPUT_SLOT_COUNT);
            this.blockEntity.startOpen(playerInventory.player);
            this.blockPosX = this.blockEntity.getBlockPos().getX();
            this.blockPosY = this.blockEntity.getBlockPos().getY();
            this.blockPosZ = this.blockEntity.getBlockPos().getZ();
        }

        this.addPositionDataSlots();

        for (int slot = 0; slot < OUTPUT_SLOT_COUNT; slot++) {
            int column = slot % 9;
            int row = slot / 9;
            this.addSlot(new Slot(this.outputContainer, slot, OUTPUT_X + column * 18, OUTPUT_Y + row * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        this.addPlayerInventory(playerInventory);
        this.addHotbar(playerInventory);
    }

    private void addPositionDataSlots() {
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return TechMinerOutputMenu.this.blockEntity != null ? TechMinerOutputMenu.this.blockEntity.getBlockPos().getX() : TechMinerOutputMenu.this.blockPosX;
            }

            @Override
            public void set(int value) {
                TechMinerOutputMenu.this.blockPosX = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return TechMinerOutputMenu.this.blockEntity != null ? TechMinerOutputMenu.this.blockEntity.getBlockPos().getY() : TechMinerOutputMenu.this.blockPosY;
            }

            @Override
            public void set(int value) {
                TechMinerOutputMenu.this.blockPosY = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return TechMinerOutputMenu.this.blockEntity != null ? TechMinerOutputMenu.this.blockEntity.getBlockPos().getZ() : TechMinerOutputMenu.this.blockPosZ;
            }

            @Override
            public void set(int value) {
                TechMinerOutputMenu.this.blockPosZ = value;
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

        if (index < OUTPUT_SLOT_COUNT) {
            if (!this.moveItemStackTo(stackInSlot, OUTPUT_SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stackInSlot, copy);
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
            return id == 0;
        }

        if (id == 0 && this.blockEntity != null && player instanceof ServerPlayer serverPlayer && this.stillValid(player)) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new TechMinerMenu(containerId, inventory, this.blockEntity, this.access),
                    Component.translatable("container.microtech.tech_miner")
            ));
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.blockEntity == null) {
            return stillValid(this.access, player, Microtech.TECH_MINER.get());
        }
        return TechMinerMenu.isUsable(this.blockEntity, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.blockEntity != null) {
            this.blockEntity.stopOpen(player);
        }
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
}
