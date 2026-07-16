package Infinitygroup.microtech.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record EnergyEndpointInfo(
        BlockPos pos,
        Direction side,
        int available,
        int demand,
        boolean canExtract,
        boolean canReceive
) {
}
