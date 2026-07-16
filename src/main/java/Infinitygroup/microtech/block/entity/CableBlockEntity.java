package Infinitygroup.microtech.block.entity;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.energy.EnergyNetworkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CableBlockEntity extends BlockEntity {
    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(Microtech.CABLE_T1_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CableBlockEntity blockEntity) {
        EnergyNetworkHelper.tickCableNetwork(level, pos);
    }
}
