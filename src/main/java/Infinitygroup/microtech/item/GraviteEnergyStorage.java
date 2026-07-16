package Infinitygroup.microtech.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class GraviteEnergyStorage implements IEnergyStorage {
    private final ItemStack stack;

    public GraviteEnergyStorage(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return GraviteEnergyHelper.receiveEnergy(this.stack, maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return GraviteEnergyHelper.extractEnergy(this.stack, maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return GraviteEnergyHelper.getEnergyStored(this.stack);
    }

    @Override
    public int getMaxEnergyStored() {
        return GraviteEnergyHelper.getMaxEnergyStored(this.stack);
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return getEnergyStored() < getMaxEnergyStored();
    }
}
