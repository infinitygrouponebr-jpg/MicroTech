package Infinitygroup.microtech.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class TechSwordEnergyStorage implements IEnergyStorage {
    private final ItemStack stack;

    public TechSwordEnergyStorage(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (toReceive <= 0) {
            return 0;
        }

        SwordEvolutionTier tier = TechSwordData.getEvolutionTier(this.stack);
        int stored = TechSwordData.getEnergyStored(this.stack);
        int accepted = Math.min(tier.getMaxReceive(), Math.min(toReceive, tier.getEnergyCapacity() - stored));
        if (!simulate && accepted > 0) {
            TechSwordData.setEnergyStored(this.stack, stored + accepted);
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return TechSwordData.getEnergyStored(this.stack);
    }

    @Override
    public int getMaxEnergyStored() {
        return TechSwordData.getEvolutionTier(this.stack).getEnergyCapacity();
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
