package Infinitygroup.microtech.block;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.block.entity.CableBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class CableBlock extends Block implements EntityBlock {
    public static final MapCodec<CableBlock> CODEC = simpleCodec(CableBlock::new);
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    private static final VoxelShape CORE_SHAPE = Block.box(6.0D, 6.0D, 6.0D, 10.0D, 10.0D, 10.0D);
    private static final VoxelShape NORTH_SHAPE = Block.box(7.0D, 7.0D, 0.0D, 9.0D, 9.0D, 6.0D);
    private static final VoxelShape SOUTH_SHAPE = Block.box(7.0D, 7.0D, 10.0D, 9.0D, 9.0D, 16.0D);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0D, 7.0D, 7.0D, 6.0D, 9.0D, 9.0D);
    private static final VoxelShape EAST_SHAPE = Block.box(10.0D, 7.0D, 7.0D, 16.0D, 9.0D, 9.0D);
    private static final VoxelShape DOWN_SHAPE = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 6.0D, 9.0D);
    private static final VoxelShape UP_SHAPE = Block.box(7.0D, 10.0D, 7.0D, 9.0D, 16.0D, 9.0D);
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        for (int mask = 0; mask < SHAPES.length; mask++) {
            VoxelShape shape = CORE_SHAPE;
            if ((mask & 1) != 0) {
                shape = Shapes.or(shape, NORTH_SHAPE);
            }
            if ((mask & 2) != 0) {
                shape = Shapes.or(shape, SOUTH_SHAPE);
            }
            if ((mask & 4) != 0) {
                shape = Shapes.or(shape, EAST_SHAPE);
            }
            if ((mask & 8) != 0) {
                shape = Shapes.or(shape, WEST_SHAPE);
            }
            if ((mask & 16) != 0) {
                shape = Shapes.or(shape, UP_SHAPE);
            }
            if ((mask & 32) != 0) {
                shape = Shapes.or(shape, DOWN_SHAPE);
            }
            SHAPES[mask] = shape.optimize();
        }
    }

    public CableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide || blockEntityType != Microtech.CABLE_T1_BLOCK_ENTITY.get()
                ? null
                : (level1, pos, blockState, blockEntity) -> CableBlockEntity.tick(level1, pos, blockState, (CableBlockEntity) blockEntity);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.updateConnections(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[getShapeIndex(state)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[getShapeIndex(state)];
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPES[getShapeIndex(state)];
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            BlockState updated = this.updateConnections(level, pos, state);
            if (!updated.equals(state)) {
                level.setBlock(pos, updated, 3);
            }
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide) {
            BlockState updated = this.updateConnections(level, pos, state);
            if (!updated.equals(state)) {
                level.setBlock(pos, updated, 3);
            }
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.phys.BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    private BlockState updateConnections(Level level, BlockPos pos, BlockState state) {
        return state
                .setValue(NORTH, isConnected(level, pos, Direction.NORTH))
                .setValue(SOUTH, isConnected(level, pos, Direction.SOUTH))
                .setValue(EAST, isConnected(level, pos, Direction.EAST))
                .setValue(WEST, isConnected(level, pos, Direction.WEST))
                .setValue(UP, isConnected(level, pos, Direction.UP))
                .setValue(DOWN, isConnected(level, pos, Direction.DOWN));
    }

    private boolean isConnected(Level level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        if (level.getBlockState(neighborPos).is(Microtech.CABLE_T1.get())) {
            return true;
        }

        return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite()) != null;
    }

    private static int getShapeIndex(BlockState state) {
        int mask = 0;
        if (state.getValue(NORTH)) {
            mask |= 1;
        }
        if (state.getValue(SOUTH)) {
            mask |= 2;
        }
        if (state.getValue(EAST)) {
            mask |= 4;
        }
        if (state.getValue(WEST)) {
            mask |= 8;
        }
        if (state.getValue(UP)) {
            mask |= 16;
        }
        if (state.getValue(DOWN)) {
            mask |= 32;
        }
        return mask;
    }
}
