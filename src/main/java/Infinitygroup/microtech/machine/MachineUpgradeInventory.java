package Infinitygroup.microtech.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.BooleanSupplier;

public class MachineUpgradeInventory extends ItemStackHandler {
    private static final int UPGRADE_STACK_LIMIT = 64;
    private final String machineId;
    private final BooleanSupplier changeCallback;

    public MachineUpgradeInventory(String machineId, int slots, BooleanSupplier changeCallback) {
        super(slots);
        this.machineId = machineId;
        this.changeCallback = changeCallback;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (!MachineUpgradeHelper.isCompatibleUpgrade(this.machineId, stack)) {
            return false;
        }
        if (!MachineUpgradeHelper.isFilterUpgrade(stack)) {
            return true;
        }

        for (int i = 0; i < this.getSlots(); i++) {
            if (i == slot) {
                continue;
            }
            if (MachineUpgradeHelper.isFilterUpgrade(this.getStackInSlot(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getSlotLimit(int slot) {
        return UPGRADE_STACK_LIMIT;
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (this.changeCallback != null) {
            this.changeCallback.getAsBoolean();
        }
    }

    public String getMachineId() {
        return this.machineId;
    }
}
