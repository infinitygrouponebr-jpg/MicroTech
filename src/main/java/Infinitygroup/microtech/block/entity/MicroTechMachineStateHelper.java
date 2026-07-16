package Infinitygroup.microtech.block.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MicroTechMachineStateHelper {
    public static final String MACHINE_ACTIVE_TAG = "microtech_active";

    private MicroTechMachineStateHelper() {
    }

    public static boolean setMachineActive(BlockEntity blockEntity, boolean active) {
        if (blockEntity == null) {
            return false;
        }

        CompoundTag persistentData = blockEntity.getPersistentData();
        if (persistentData.getBoolean(MACHINE_ACTIVE_TAG) == active) {
            return false;
        }

        persistentData.putBoolean(MACHINE_ACTIVE_TAG, active);
        blockEntity.setChanged();
        return true;
    }

    public static boolean isMachineActive(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }

        return blockEntity.getPersistentData().getBoolean(MACHINE_ACTIVE_TAG);
    }
}
