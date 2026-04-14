package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.mechanics.GlowGooRuntime;

public class GooSplatterBlock extends Block implements SimpleWaterloggedBlock {
    private static final VoxelShape FLOOR_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
    private static final VoxelShape CEILING_SHAPE = Block.box(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape NORTH_SHAPE = Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
    private static final VoxelShape WEST_SHAPE = Block.box(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape EAST_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public GooSplatterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(ROTATION, 0)
                .setValue(WATERLOGGED, Boolean.FALSE));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> CEILING_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            case UP -> FLOOR_SHAPE;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return Block.canSupportCenter(level, supportPos, facing);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(ROTATION, bitsandbalance$getPlacementRotation(context))
                .setValue(WATERLOGGED, context.getLevel().getFluidState(pos).getType() == Fluids.WATER);
        return state.canSurvive(context.getLevel(), pos) ? state : null;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotatedState = state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        if (state.getValue(FACING).getAxis() == Direction.Axis.Y) {
            rotatedState = rotatedState.setValue(ROTATION, bitsandbalance$rotateQuarterTurns(state.getValue(ROTATION), rotation));
        }
        return rotatedState;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirroredState = state.rotate(mirror.getRotation(state.getValue(FACING)));
        if (state.getValue(FACING).getAxis() == Direction.Axis.Y) {
            mirroredState = mirroredState.setValue(ROTATION, bitsandbalance$mirrorQuarterTurns(state.getValue(ROTATION), mirror));
        }
        return mirroredState;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!GlowGooRuntime.enabled || !GlowGooRuntime.splatterAmbientParticlesEnabled || random.nextInt(3) != 0) {
            return;
        }

        Direction facing = state.getValue(FACING);
        int particleCount = 1 + random.nextInt(2);
        for (int index = 0; index < particleCount; index++) {
            Vec3 particlePos = bitsandbalance$getAmbientParticlePos(pos, facing, random);
            level.addParticle(
                    ParticleTypes.GLOW,
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public static int rotationFromIncomingDirection(Direction facing, Vec3 incomingDirection) {
        return bitsandbalance$getQuarterTurns(incomingDirection.z, -incomingDirection.x, 0);
    }

    private static Vec3 bitsandbalance$getAmbientParticlePos(BlockPos pos, Direction facing, RandomSource random) {
        double x = pos.getX() + 0.2D + random.nextDouble() * 0.6D;
        double y = pos.getY() + 0.2D + random.nextDouble() * 0.6D;
        double z = pos.getZ() + 0.2D + random.nextDouble() * 0.6D;
        double surfaceOffset = 0.0625D + random.nextDouble() * 0.06D;

        switch (facing) {
            case UP -> y = pos.getY() + surfaceOffset;
            case DOWN -> y = pos.getY() + 1.0D - surfaceOffset;
            case NORTH -> z = pos.getZ() + 1.0D - surfaceOffset;
            case SOUTH -> z = pos.getZ() + surfaceOffset;
            case WEST -> x = pos.getX() + 1.0D - surfaceOffset;
            case EAST -> x = pos.getX() + surfaceOffset;
        }

        return new Vec3(x, y, z);
    }

    private static int bitsandbalance$getPlacementRotation(BlockPlaceContext context) {
        return Mth.floor((double) (context.getRotation() * 4.0F / 360.0F) + 0.5D) & 3;
    }

    private static int bitsandbalance$getQuarterTurns(double primaryAxis, double secondaryAxis, int fallbackTurns) {
        double primaryAbs = Math.abs(primaryAxis);
        double secondaryAbs = Math.abs(secondaryAxis);
        if (primaryAbs < 1.0E-4D && secondaryAbs < 1.0E-4D) {
            return fallbackTurns;
        }
        if (primaryAbs >= secondaryAbs) {
            return primaryAxis >= 0.0D ? 0 : 2;
        }
        return secondaryAxis >= 0.0D ? 1 : 3;
    }

    private static int bitsandbalance$rotateQuarterTurns(int turns, Rotation rotation) {
        return switch (rotation) {
            case NONE -> turns;
            case CLOCKWISE_90 -> (turns + 1) & 3;
            case CLOCKWISE_180 -> (turns + 2) & 3;
            case COUNTERCLOCKWISE_90 -> (turns + 3) & 3;
        };
    }

    private static int bitsandbalance$mirrorQuarterTurns(int turns, Mirror mirror) {
        return switch (mirror) {
            case NONE -> turns;
            case LEFT_RIGHT -> switch (turns) {
                case 1 -> 3;
                case 3 -> 1;
                default -> turns;
            };
            case FRONT_BACK -> switch (turns) {
                case 0 -> 2;
                case 2 -> 0;
                default -> turns;
            };
        };
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level,
                                     ScheduledTickAccess scheduledTickAccess, BlockPos pos,
                                     Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROTATION, WATERLOGGED);
    }
}