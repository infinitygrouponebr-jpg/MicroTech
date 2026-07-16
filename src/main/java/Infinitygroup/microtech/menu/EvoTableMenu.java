package Infinitygroup.microtech.menu;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.block.entity.EvoTableBlockEntity;
import Infinitygroup.microtech.block.entity.EvoTableBlockEntity.EvoStatus;
import Infinitygroup.microtech.item.TechArmorUpgradeHelper;
import Infinitygroup.microtech.item.TechChipItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;

public class EvoTableMenu extends AbstractContainerMenu {
    public static final int SLOT_INPUT_X = 53;
    public static final int SLOT_INPUT_Y = 52;
    public static final int SLOT_MATERIAL_X = 116;
    public static final int SLOT_MATERIAL_Y = 52;
    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 86;
    public static final int HOTBAR_Y = 144;

    private final EvoTableBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final net.minecraft.world.Container container;
    private int evolving;
    private int evolutionTicks;
    private int feedbackState;
    private int feedbackTicks;
    private int statusId;

    public EvoTableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, ContainerLevelAccess.NULL);
    }

    public EvoTableMenu(int containerId, Inventory playerInventory, EvoTableBlockEntity blockEntity, ContainerLevelAccess access) {
        super(Microtech.EVO_TABLE_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;
        this.container = this.blockEntity != null ? this.blockEntity : new SimpleContainer(EvoTableBlockEntity.SLOT_COUNT);

        if (this.blockEntity != null) {
            checkContainerSize(this.blockEntity, EvoTableBlockEntity.SLOT_COUNT);
            this.blockEntity.startOpen(playerInventory.player);
            this.evolving = this.blockEntity.isEvolving() ? 1 : 0;
            this.evolutionTicks = this.blockEntity.getEvolutionTicks();
            this.feedbackState = this.blockEntity.getFeedbackState();
            this.feedbackTicks = this.blockEntity.getFeedbackTicks();
            this.statusId = this.blockEntity.getStatus().getId();
        }

        this.addSlot(new Slot(this.container, EvoTableBlockEntity.SLOT_INPUT, SLOT_INPUT_X, SLOT_INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty()
                        && (stack.is(Microtech.TECH_SWORD.get()) || TechArmorUpgradeHelper.isTechArmorChestplate(stack));
            }

            @Override
            public boolean mayPickup(Player player) {
                return !EvoTableMenu.this.isEvolving();
            }
        });
        this.addSlot(new Slot(this.container, EvoTableBlockEntity.SLOT_MATERIAL, SLOT_MATERIAL_X, SLOT_MATERIAL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EvoTableMenu.isValidChip(stack);
            }

            @Override
            public boolean mayPickup(Player player) {
                return !EvoTableMenu.this.isEvolving();
            }
        });

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return EvoTableMenu.this.blockEntity != null ? (EvoTableMenu.this.blockEntity.isEvolving() ? 1 : 0) : EvoTableMenu.this.evolving;
            }

            @Override
            public void set(int value) {
                EvoTableMenu.this.evolving = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return EvoTableMenu.this.blockEntity != null ? EvoTableMenu.this.blockEntity.getEvolutionTicks() : EvoTableMenu.this.evolutionTicks;
            }

            @Override
            public void set(int value) {
                EvoTableMenu.this.evolutionTicks = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return EvoTableMenu.this.blockEntity != null ? EvoTableMenu.this.blockEntity.getFeedbackState() : EvoTableMenu.this.feedbackState;
            }

            @Override
            public void set(int value) {
                EvoTableMenu.this.feedbackState = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return EvoTableMenu.this.blockEntity != null ? EvoTableMenu.this.blockEntity.getFeedbackTicks() : EvoTableMenu.this.feedbackTicks;
            }

            @Override
            public void set(int value) {
                EvoTableMenu.this.feedbackTicks = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return EvoTableMenu.this.blockEntity != null ? EvoTableMenu.this.blockEntity.getStatus().getId() : EvoTableMenu.this.statusId;
            }

            @Override
            public void set(int value) {
                EvoTableMenu.this.statusId = value;
            }
        });

        addPlayerInventory(playerInventory);
        addHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
    }

    private void addHotbar(Inventory playerInventory) {
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

        ItemStack stackInSlot = slot.getItem();
        ItemStack copy = stackInSlot.copy();

        if (index < 2) {
            if (!this.moveItemStackTo(stackInSlot, 2, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stackInSlot, copy);
        } else if (stackInSlot.is(Microtech.TECH_SWORD.get()) || TechArmorUpgradeHelper.isTechArmorChestplate(stackInSlot)) {
            if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isValidChip(stackInSlot)) {
            if (!this.moveItemStackTo(stackInSlot, 1, 2, false)) {
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
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Microtech.EVO_TABLE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.blockEntity != null) {
            this.container.stopOpen(player);
        }
    }

    public boolean isEvolving() {
        return this.blockEntity != null ? this.blockEntity.isEvolving() : this.evolving > 0;
    }

    public int getEvolutionTicks() {
        return this.blockEntity != null ? this.blockEntity.getEvolutionTicks() : this.evolutionTicks;
    }

    public int getEvolutionDuration() {
        return this.blockEntity != null ? this.blockEntity.getEvolutionDuration() : EvoTableBlockEntity.SWORD_EVOLUTION_DURATION;
    }

    public int getFeedbackState() {
        return this.blockEntity != null ? this.blockEntity.getFeedbackState() : this.feedbackState;
    }

    public int getFeedbackTicks() {
        return this.blockEntity != null ? this.blockEntity.getFeedbackTicks() : this.feedbackTicks;
    }

    public EvoStatus getStatus() {
        return this.blockEntity != null ? this.blockEntity.getStatus() : EvoStatus.fromId(this.statusId);
    }

    public boolean canStartEvolution() {
        return this.blockEntity != null && this.blockEntity.canStartEvolution();
    }

    public boolean startEvolution() {
        return this.blockEntity != null && this.blockEntity.startEvolution();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && this.blockEntity != null) {
            return this.blockEntity.startEvolution();
        }
        return super.clickMenuButton(player, id);
    }

    public ItemStack getInputStack() {
        return this.slots.get(0).getItem();
    }

    public ItemStack getMaterialStack() {
        return this.slots.get(1).getItem();
    }

    private static boolean isValidChip(ItemStack stack) {
        return !stack.isEmpty()
                && ((stack.getItem() instanceof TechChipItem chipItem && chipItem.getChipType() != null)
                || TechArmorUpgradeHelper.isFlightChip(stack));
    }
}
