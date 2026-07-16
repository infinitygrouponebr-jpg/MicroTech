package Infinitygroup.microtech.machine;

import net.neoforged.neoforge.items.ItemStackHandler;

public interface MachineUpgradeHost {
    String getMachineUpgradeId();
    ItemStackHandler getUpgradeInventory();
    int getUpgradeSlotCount();
}
