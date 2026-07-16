package Infinitygroup.microtech.block;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.block.entity.TechMinerBlockEntity;
import Infinitygroup.microtech.machine.MachineUpgradeHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TechMinerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<TechMinerBlock> CODEC = simpleCodec(TechMinerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final double SIDE_TOLERANCE = 0.08D;
    private static final double MAX_USE_DISTANCE_SQR = 64.0D;

    private static final VoxelShape OUTLINE_SHAPE = Shapes.or(
            Block.box(1.0D, 2.0D, 1.0D, 15.0D, 14.0D, 15.0D),
            Block.box(2.0D, 14.0D, 2.0D, 14.0D, 16.0D, 14.0D)
    );
    private static final VoxelShape COLLISION_SHAPE = OUTLINE_SHAPE;

    public TechMinerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TechMinerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide || blockEntityType != Microtech.TECH_MINER_BLOCK_ENTITY.get()
                ? null
                : (level1, pos, blockState, blockEntity) -> TechMinerBlockEntity.tick(level1, pos, blockState, (TechMinerBlockEntity) blockEntity);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof TechMinerBlockEntity blockEntity) {
            if (!canUseFromOperationSide(state, pos, player)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.microtech.tech_miner.wrong_side"), true);
                }
                return InteractionResult.FAIL;
            }

            if (!level.isClientSide) {
                player.openMenu(blockEntity);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof TechMinerBlockEntity blockEntity) {
            blockEntity.setEnergyStored(TechMinerBlockEntity.getEnergyFromStack(stack));
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);

        if (blockEntity instanceof TechMinerBlockEntity techMinerBlockEntity) {
            net.minecraft.world.Containers.dropContents(level, pos, techMinerBlockEntity);
            popResource(level, pos, techMinerBlockEntity.createItemStackWithEnergy());
        } else {
            popResource(level, pos, new ItemStack(Microtech.TECH_MINER_ITEM.get()));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof TechMinerBlockEntity blockEntity) {
                MachineUpgradeHelper.dropInventory(level, pos, blockEntity.getUpgradeInventory());
            }
            level.invalidateCapabilities(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return OUTLINE_SHAPE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    public static Direction getLocalPositiveXDirection(BlockState state) {
        Direction facing = state.getValue(FACING);
        return facing.getCounterClockWise();
    }

    public static boolean canUseFromOperationSide(BlockState state, BlockPos pos, Player player) {
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_USE_DISTANCE_SQR) {
            return false;
        }

        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 toPlayer = player.position().subtract(center);
        double dx = toPlayer.x;
        double dz = toPlayer.z;

        Direction localPositiveX = getLocalPositiveXDirection(state);
        double sideDot = dx * localPositiveX.getStepX() + dz * localPositiveX.getStepZ();

        Direction facing = state.getValue(FACING);
        double frontBackDot = dx * facing.getStepX() + dz * facing.getStepZ();

        return sideDot > SIDE_TOLERANCE && sideDot > Math.abs(frontBackDot) + SIDE_TOLERANCE;
    }
}
