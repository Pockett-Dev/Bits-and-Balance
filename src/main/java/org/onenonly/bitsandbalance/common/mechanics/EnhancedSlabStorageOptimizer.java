package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.compat.SableCompat;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

/**
 * Collapses storage-only enhanced slab composites into true block states when no
 * mixed material data is needed anymore.
 */
public final class EnhancedSlabStorageOptimizer {

    private EnhancedSlabStorageOptimizer() {
    }

    public static boolean optimizeAt(Level level, BlockPos pos) {
        if (level == null || level.isClientSide()) return false;

        BlockState plotState = SableCompat.plotBlockState(level, pos);
        BlockState state = plotState == null ? level.getBlockState(pos) : plotState;
        BlockEntity plotBlockEntity = SableCompat.plotBlockEntity(level, pos);
        BlockEntity be = plotBlockEntity == null ? level.getBlockEntity(pos) : plotBlockEntity;
        Block block = state.getBlock();
        BlockState[] savedSlots = EnhancedSlabMaterialSavedData.get(level, pos.asLong());

        if (block instanceof MixedSlabBlock) {
            return optimizeMixedSlab(level, pos, savedSlots);
        }
        if (block instanceof VerticalSlabBlock) {
            BlockState[] slots = be instanceof VerticalSlabBlockEntity vertical
                    ? new BlockState[]{vertical.getFacingSlab(), vertical.getOppositeSlab(), null, null}
                    : savedSlots;
            return optimizeVerticalSlab(level, pos, state, slots);
        }
        if (block instanceof QuadStepBlock) {
            BlockState[] slots = be instanceof QuadStepBlockEntity quad ? quad.getAllSlabs() : savedSlots;
            return optimizeQuadStep(level, pos, state, slots);
        }
        if (block instanceof QuadVerticalStepBlock) {
            BlockState[] slots = be instanceof QuadVerticalStepBlockEntity quad ? quad.getAllSlabs() : savedSlots;
            return optimizeQuadVerticalStep(level, pos, state, slots);
        }
        return false;
    }

    private static boolean optimizeMixedSlab(Level level, BlockPos pos, @Nullable BlockState[] slots) {
        BlockState bottom = slot(slots, 0);
        BlockState top = slot(slots, 1);
        if (!sameSlabState(bottom, top)) return false;
        return setCollapsed(level, pos, slabState(bottom, SlabType.DOUBLE, false));
    }

    private static boolean optimizeVerticalSlab(Level level, BlockPos pos, BlockState state, @Nullable BlockState[] slots) {
        if (!state.hasProperty(VerticalSlabBlock.DOUBLE)) return false;

        BlockState facingSlab = slot(slots, 0);
        BlockState oppositeSlab = slot(slots, 1);
        Direction facing = state.getValue(VerticalSlabBlock.FACING);
        boolean waterlogged = state.hasProperty(VerticalSlabBlock.WATERLOGGED) && state.getValue(VerticalSlabBlock.WATERLOGGED);

        if (!state.getValue(VerticalSlabBlock.DOUBLE)) {
            if (!isDefaultSlabVariant(facingSlab)) return false;
            BlockState collapsed = fixedVerticalSlabState(facingSlab, facing, false, waterlogged);
            return collapsed != null && setCollapsed(level, pos, collapsed);
        }

        if (!sameSlabState(facingSlab, oppositeSlab) || !isDefaultSlabVariant(facingSlab)) return false;
        BlockState collapsed = fixedVerticalSlabState(facingSlab, facing, true, waterlogged);
        return collapsed != null && setCollapsed(level, pos, collapsed);
    }

    private static boolean optimizeQuadStep(Level level, BlockPos pos, BlockState state, @Nullable BlockState[] slabs) {
        if (slabs == null) return false;
        BlockState first = firstOccupied(slabs);
        if (first == null || !allOccupiedSame(slabs, first)) return false;

        boolean waterlogged = state.hasProperty(QuadStepBlock.WATERLOGGED) && state.getValue(QuadStepBlock.WATERLOGGED);
        Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
        int count = occupiedCount(slabs);

        if (count != 1 || !isDefaultSlabVariant(first)) {
            return false;
        }

        int index = lastOccupiedIndex(slabs);
        BlockState collapsed = fixedStepState(first, QuadStepBlock.sideForIndex(axis, index), QuadStepBlock.isTop(index), waterlogged);
        return collapsed != null && setCollapsed(level, pos, collapsed);
    }

    private static boolean optimizeQuadVerticalStep(Level level, BlockPos pos, BlockState state, @Nullable BlockState[] slabs) {
        if (slabs == null) return false;
        BlockState first = firstOccupied(slabs);
        if (first == null || !allOccupiedSame(slabs, first)) return false;

        boolean waterlogged = state.hasProperty(QuadVerticalStepBlock.WATERLOGGED)
                && state.getValue(QuadVerticalStepBlock.WATERLOGGED);
        int count = occupiedCount(slabs);

        if (count != 1 || !isDefaultSlabVariant(first)) {
            return false;
        }

        int index = lastOccupiedIndex(slabs);
        BlockState collapsed = fixedVerticalStepState(
                first,
                QuadVerticalStepBlock.facingForIndex(index),
                QuadVerticalStepBlock.sideForIndex(index),
                waterlogged);
        return collapsed != null && setCollapsed(level, pos, collapsed);
    }

    private static BlockState slabState(BlockState source, SlabType type, boolean waterlogged) {
        BlockState state = source;
        if (state.hasProperty(SlabBlock.TYPE)) {
            state = state.setValue(SlabBlock.TYPE, type);
        }
        if (state.hasProperty(SlabBlock.WATERLOGGED)) {
            state = state.setValue(SlabBlock.WATERLOGGED, type != SlabType.DOUBLE && waterlogged);
        }
        return state;
    }

    private static @Nullable BlockState fixedStepState(BlockState source, Direction facing, boolean top, boolean waterlogged) {
        Block step = StepDynamicRegistry.getStepForSlab(source.getBlock());
        if (!(step instanceof StepBlock) || step instanceof EntityBlock) return null;

        return step.defaultBlockState()
                .setValue(StepBlock.FACING, facing)
                .setValue(StepBlock.TOP, top)
                .setValue(StepBlock.WATERLOGGED, waterlogged);
    }

    private static @Nullable BlockState fixedVerticalSlabState(BlockState source, Direction facing, boolean isDouble, boolean waterlogged) {
        Block vertical = VerticalSlabDynamicRegistry.getVerticalForSlab(source.getBlock());
        if (!(vertical instanceof VerticalSlabBlock) || vertical instanceof EntityBlock) return null;

        return vertical.defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, facing)
                .setValue(VerticalSlabBlock.DOUBLE, isDouble)
                .setValue(VerticalSlabBlock.WATERLOGGED, waterlogged);
    }

    private static @Nullable BlockState fixedVerticalStepState(BlockState source, Direction facing, Direction side, boolean waterlogged) {
        Block vertical = VerticalSlabDynamicRegistry.getVerticalForSlab(source.getBlock());
        if (vertical == null) return null;

        Block verticalStep = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(vertical);
        if (!(verticalStep instanceof VerticalStepBlock) || verticalStep instanceof EntityBlock) return null;

        return verticalStep.defaultBlockState()
                .setValue(VerticalStepBlock.FACING, facing)
                .setValue(VerticalStepBlock.SIDE, side)
                .setValue(VerticalStepBlock.WATERLOGGED, waterlogged);
    }

    private static boolean setCollapsed(Level level, BlockPos pos, BlockState collapsed) {
        BlockState plotState = SableCompat.plotBlockState(level, pos);
        BlockState current = plotState == null ? level.getBlockState(pos) : plotState;
        if (collapsed == null || collapsed.equals(current)) return false;
        boolean changed = plotState == null
                ? level.setBlock(pos, collapsed, Block.UPDATE_ALL)
                : SableCompat.setPlotBlockState(level, pos, collapsed, Block.UPDATE_ALL);
        if (changed) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong());
        }
        return changed;
    }

    private static @Nullable BlockState slot(@Nullable BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    private static boolean allOccupiedSame(BlockState[] slabs, BlockState first) {
        for (BlockState slab : slabs) {
            if (slab != null && !sameSlabState(first, slab)) return false;
        }
        return true;
    }

    private static @Nullable BlockState firstOccupied(BlockState[] slabs) {
        for (BlockState slab : slabs) {
            if (slab != null) return slab;
        }
        return null;
    }

    private static int lastOccupiedIndex(BlockState[] slabs) {
        for (int i = slabs.length - 1; i >= 0; i--) {
            if (slabs[i] != null) return i;
        }
        return -1;
    }

    private static int occupiedCount(BlockState[] slabs) {
        int count = 0;
        for (BlockState slab : slabs) {
            if (slab != null) count++;
        }
        return count;
    }

    private static boolean isDefaultSlabVariant(BlockState state) {
        if (state == null || !(state.getBlock() instanceof SlabBlock)) return false;
        return sameSlabState(state, state.getBlock().defaultBlockState());
    }

    private static boolean sameSlabState(BlockState a, BlockState b) {
        if (a == null || b == null || a.getBlock() != b.getBlock()) return false;

        for (Property<?> property : a.getProperties()) {
            if (isIgnoredSlabProperty(property)) continue;
            if (!b.hasProperty(property) || !samePropertyValue(a, b, property)) return false;
        }
        for (Property<?> property : b.getProperties()) {
            if (!isIgnoredSlabProperty(property) && !a.hasProperty(property)) return false;
        }
        return true;
    }

    private static boolean isIgnoredSlabProperty(Property<?> property) {
        return property == SlabBlock.TYPE || property == SlabBlock.WATERLOGGED;
    }

    private static <T extends Comparable<T>> boolean samePropertyValue(BlockState a, BlockState b, Property<T> property) {
        return a.getValue(property).equals(b.getValue(property));
    }
}
