package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.CandleBundleBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class CandleBundleBlock extends CandleBlock implements EntityBlock {
    public CandleBundleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CandleBundleBlockEntity bundle) {
            int[] nonEmptyIndices = new int[4];
            int nonEmptyCount = 0;

            for (int i = 0; i < 4; i++) {
                ItemStack stack = bundle.getCandle(i);
                if (stack != null && !stack.isEmpty()) {
                    nonEmptyIndices[nonEmptyCount++] = i;
                }
            }

            if (nonEmptyCount > 0) {
                RandomSource random = (level instanceof Level l) ? l.getRandom() : RandomSource.create();
                int pickedIndex = nonEmptyIndices[random.nextInt(nonEmptyCount)];
                return bundle.getCandle(pickedIndex).copyWithCount(1);
            }
        }

        return new ItemStack(Items.CANDLE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (placer == null) {
            return;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CandleBundleBlockEntity bundleBe) {
            bundleBe.setFacing(bitsandbalance$bundleFacingFromPlacer(placer.getDirection()));
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof CandleBundleBlockEntity bundleBe) {
            List<ItemStack> drops = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) {
                ItemStack stack = bundleBe.getCandle(i);
                if (stack != null && !stack.isEmpty()) {
                    drops.add(stack.copyWithCount(1));
                }
            }
            if (!drops.isEmpty()) {
                return drops;
            }
        }

        int count = state.hasProperty(CandleBlock.CANDLES) ? state.getValue(CandleBlock.CANDLES) : 1;
        List<ItemStack> fallback = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            fallback.add(new ItemStack(Items.CANDLE));
        }
        return fallback;
    }

    private static Direction bitsandbalance$bundleFacingFromPlacer(Direction playerDirection) {
        if (playerDirection == null || !playerDirection.getAxis().isHorizontal()) {
            return Direction.SOUTH;
        }
        return playerDirection.getAxis() == Direction.Axis.X ? playerDirection : playerDirection.getOpposite();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return bitsandbalance$rotatedShape(super.getShape(state, level, pos, context), bitsandbalance$facing(level, pos));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return bitsandbalance$rotatedShape(super.getCollisionShape(state, level, pos, context), bitsandbalance$facing(level, pos));
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return bitsandbalance$rotatedShape(super.getInteractionShape(state, level, pos), bitsandbalance$facing(level, pos));
    }

    private static Direction bitsandbalance$facing(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CandleBundleBlockEntity bundleBe) {
            Direction facing = bundleBe.getFacing();
            if (facing != null && facing.getAxis().isHorizontal()) {
                return facing;
            }
        }
        return Direction.SOUTH;
    }

    private static VoxelShape bitsandbalance$rotatedShape(VoxelShape original, Direction facing) {
        if (facing == Direction.SOUTH) {
            return original;
        }

        VoxelShape[] buffer = new VoxelShape[] { original, Shapes.empty() };
        int turns = switch (facing) {
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };

        for (int i = 0; i < turns; i++) {
            VoxelShape rotated = Shapes.empty();
            for (var box : buffer[0].toAabbs()) {
                rotated = Shapes.or(rotated, Block.box(
                    box.minZ * 16.0D,
                    box.minY * 16.0D,
                    (1.0D - box.maxX) * 16.0D,
                    box.maxZ * 16.0D,
                    box.maxY * 16.0D,
                    (1.0D - box.minX) * 16.0D
                ));
            }
            buffer[0] = rotated;
        }

        return buffer[0];
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CandleBundleBlockEntity(pos, state);
    }
}