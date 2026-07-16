package Infinitygroup.microtech.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class TechMinerTargetHelper {
    private TechMinerTargetHelper() {
    }

    public static boolean isValidTarget(BlockState state) {
        return !state.isAir()
                && !state.hasBlockEntity()
                && (state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(Blocks.ANCIENT_DEBRIS));
    }

    public static boolean isValidTarget(Level level, BlockPos pos, BlockState state) {
        return isValidTarget(state)
                && state.getDestroySpeed(level, pos) >= 0.0F
                && state.getFluidState().isEmpty();
    }

    public static String getBlockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    public static Component getDisplayName(BlockState state) {
        return state.getBlock().getName();
    }
}
