package Infinitygroup.microtech.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class TechArmorEnergyStorage implements IEnergyStorage {
    private final ItemStack stack;

    public TechArmorEnergyStorage(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return TechArmorEnergyHelper.receiveEnergy(this.stack, maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return TechArmorEnergyHelper.extractEnergy(this.stack, maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return TechArmorEnergyHelper.getEnergyStored(this.stack);
    }

    @Override
    public int getMaxEnergyStored() {
        return TechArmorEnergyHelper.getMaxEnergyStored(this.stack);
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
