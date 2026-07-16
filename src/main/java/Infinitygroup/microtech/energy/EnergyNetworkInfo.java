package Infinitygroup.microtech.energy;

import java.util.List;
import net.minecraft.core.BlockPos;

public record EnergyNetworkInfo(
        int cableCount,
        int maxCableCount,
        int sourceCount,
        int targetCount,
        int totalAvailable,
        int totalDemand,
        int networkLimit,
        int estimatedTransfer,
        BlockPos controller,
        boolean overLimit,
        List<EnergyEndpointInfo> sources,
        List<EnergyEndpointInfo> targets
) {
}
