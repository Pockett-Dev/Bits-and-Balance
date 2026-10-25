package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior - Quad Step Block Entity Renderer (Fabric)
 *
 * Renders up to 4 quadrant steps stored in a {@link QuadStepBlockEntity},
 * delegating per-quadrant geometry to {@link StepBlockEntityRenderer#submitQuadrant}.
 */
public class QuadStepBlockEntityRenderer
        implements BlockEntityRenderer<QuadStepBlockEntity, QuadStepBlockEntityRenderer.State> {
    private static final int HORIZONTAL_DIRECTIONS = bitsandbalance$directionMask(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);

    public QuadStepBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return EnhancedSlabTerrainRenderHelper.quadStepBlockEntityViewDistance();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(QuadStepBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakOverlay);
        state.level = blockEntity.getLevel();
        state.ownerState = blockEntity.getBlockState();
        if (!EnhancedSlabBlockEntityRenderLimits.shouldSubmit(state.blockPos, cameraPos)) {
            clearSlabs(state.slabs);
            return;
        }
        state.axis = state.ownerState != null && state.ownerState.getBlock() instanceof QuadStepBlock
                ? state.ownerState.getValue(QuadStepBlock.AXIS)
                : Direction.Axis.X;
        if (EnhancedSlabTerrainRenderHelper.shouldSkipBlockEntityRender(state.level, state.blockPos, state.ownerState)) {
            clearSlabs(state.slabs);
            return;
        }

        BlockState[] slabs = blockEntity.getAllSlabs();
        System.arraycopy(slabs, 0, state.slabs, 0, Math.min(slabs.length, state.slabs.length));
        for (int i = slabs.length; i < state.slabs.length; i++) {
            state.slabs[i] = null;
        }
        state.lightCoords = VirtualRenderWorldLightBridge.capturePackedLight(state.level, state.lightCoords);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraRenderState) {
        if (state.blockPos == null || state.level == null) return;
        if (EnhancedSlabTerrainRenderHelper.shouldSkipBlockEntityRender(state.level, state.blockPos, state.ownerState)) return;
        if (!EnhancedSlabBlockEntityRenderLimits.shouldSubmit(state, cameraRenderState)) return;

        Direction.Axis axis = state.axis;
        GroupAccumulator groups = new GroupAccumulator(8);

        BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(state.slabs);
        if (fullBlockState != null) {
            net.minecraft.client.renderer.block.model.BlockModelPart part = StepBlockEntityRenderer.buildExactPrismPart(
                    state.level,
                    state.blockPos,
                    fullBlockState,
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Vector3f(16.0F, 16.0F, 16.0F),
                    EnhancedSlabRenderModelCompat.allDirectionMask()
            );
            if (part != null) {
                StepBlockEntityRenderer.submitQuadrantParts(
                        collector,
                        poseStack,
                        state.level,
                        state.blockPos,
                        fullBlockState,
                        EnhancedSlabRenderModelCompat.singletonPartList(part),
                        state.lightCoords
                );
            }
            return;
        }

        for (int i = 0; i < 4; i++) {
            BlockState slab = state.slabs[i];
            if (slab == null) continue;
            boolean positive = (i == 1 || i == 3);
            Direction facing = axis == Direction.Axis.Z
                    ? (positive ? Direction.SOUTH : Direction.NORTH)
                    : (positive ? Direction.EAST : Direction.WEST);
            boolean top = (i >= 2);
            int visibleMask = bitsandbalance$visibleMask(axis, i, state.slabs);
            BlockState renderSlab = bitsandbalance$stepRenderState(slab, top);
            bitsandbalance$addStepParts(groups, state.level, state.blockPos, renderSlab, facing, top, visibleMask);
        }

        for (int i = 0; i < groups.size; i++) {
            StepBlockEntityRenderer.submitQuadrantParts(
                    collector,
                    poseStack,
                    state.level,
                    state.blockPos,
                    groups.states[i],
                    EnhancedSlabRenderModelCompat.partList(groups.parts[i]),
                    state.lightCoords
            );
        }
    }

    private static void clearSlabs(BlockState[] slabs) {
        for (int i = 0; i < slabs.length; i++) {
            slabs[i] = null;
        }
    }

    private static void bitsandbalance$addStepParts(GroupAccumulator groups,
                                                    Level level,
                                                    net.minecraft.core.BlockPos pos,
                                                    BlockState slab,
                                                    Direction facing,
                                                    boolean top,
                                                    int visibleMask) {
        boolean joinedVertical = bitsandbalance$hasJoinedVerticalNeighbor(top, visibleMask);
        if (!joinedVertical || (visibleMask & HORIZONTAL_DIRECTIONS) == 0) {
            net.minecraft.client.renderer.block.model.BlockModelPart part = StepBlockEntityRenderer.buildQuadrantPart(slab, pos, facing, top);
            if (part != null) {
                groups.add(slab, EnhancedSlabRenderModelCompat.filterDirections(part, visibleMask));
            }
            return;
        }

        int bodyMask = visibleMask & ~HORIZONTAL_DIRECTIONS;
        net.minecraft.client.renderer.block.model.BlockModelPart bodyPart = StepBlockEntityRenderer.buildQuadrantPart(slab, pos, facing, top);
        if (bodyPart != null && bodyMask != 0) {
            groups.add(slab, EnhancedSlabRenderModelCompat.filterDirections(bodyPart, bodyMask));
        }

        BlockState sideState = bitsandbalance$fullBlockStateForSlab(slab);
        if (sideState == null) {
            sideState = slab;
        }
        float sourceHeight = StepBlockEntityRenderer.sourceModelHeight(level, pos, slab);
        float yMin = top ? 8.0F : 0.0F;
        float yMax = top ? Math.min(16.0F, 8.0F + sourceHeight) : 8.0F;
        Vector3f from = bitsandbalance$stepFrom(facing, yMin);
        Vector3f to = bitsandbalance$stepTo(facing, yMax);
        net.minecraft.client.renderer.block.model.BlockModelPart sidePart = StepBlockEntityRenderer.buildExactPrismPart(
                level,
                pos,
                sideState,
                from,
                to,
                visibleMask & HORIZONTAL_DIRECTIONS
        );
        if (sidePart != null) {
            groups.add(sideState, sidePart);
        }
    }

    private static int bitsandbalance$visibleMask(Direction.Axis axis, int index, BlockState[] slabs) {
        int mask = EnhancedSlabRenderModelCompat.allDirectionMask();
        boolean positive = index == 1 || index == 3;
        boolean top = index >= 2;
        int horizontalNeighbor = top ? (positive ? 2 : 3) : (positive ? 0 : 1);
        if (bitsandbalance$sameRenderMaterial(slabs[index], slabs[horizontalNeighbor])) {
            Direction internal = axis == Direction.Axis.Z
                    ? (positive ? Direction.NORTH : Direction.SOUTH)
                    : (positive ? Direction.WEST : Direction.EAST);
            mask = EnhancedSlabRenderModelCompat.without(mask, internal);
        }
        int verticalNeighbor = top ? index - 2 : index + 2;
        if (bitsandbalance$sameRenderMaterial(slabs[index], slabs[verticalNeighbor])) {
            mask = EnhancedSlabRenderModelCompat.without(mask, top ? Direction.DOWN : Direction.UP);
        }
        return mask;
    }

    private static boolean bitsandbalance$sameRenderMaterial(BlockState first, BlockState second) {
        return first != null && first.equals(second);
    }

    private static boolean bitsandbalance$hasJoinedVerticalNeighbor(boolean top, int visibleMask) {
        Direction hidden = top ? Direction.DOWN : Direction.UP;
        return (visibleMask & (1 << hidden.ordinal())) == 0;
    }

    private static BlockState bitsandbalance$stepRenderState(BlockState slabState, boolean top) {
        if (slabState != null && slabState.hasProperty(SlabBlock.TYPE)) {
            return slabState.setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
        }
        return slabState;
    }

    private static BlockState bitsandbalance$fullBlockStateForSlab(BlockState slabState) {
        return SlabFamilyModelSourceSelector.verticalPrismRenderState(slabState);
    }

    private static Vector3f bitsandbalance$stepFrom(Direction facing, float yMin) {
        return switch (facing) {
            case SOUTH -> new Vector3f(0.0F, yMin, 8.0F);
            case EAST -> new Vector3f(8.0F, yMin, 0.0F);
            default -> new Vector3f(0.0F, yMin, 0.0F);
        };
    }

    private static Vector3f bitsandbalance$stepTo(Direction facing, float yMax) {
        return switch (facing) {
            case SOUTH, EAST -> new Vector3f(16.0F, yMax, 16.0F);
            case NORTH -> new Vector3f(16.0F, yMax, 8.0F);
            default -> new Vector3f(8.0F, yMax, 16.0F);
        };
    }

    private static int bitsandbalance$directionMask(Direction... directions) {
        int mask = 0;
        for (Direction direction : directions) {
            if (direction != null) {
                mask |= 1 << direction.ordinal();
            }
        }
        return mask;
    }

    public static final class State extends BlockEntityRenderState {
        public BlockState[] slabs = new BlockState[4];
        public BlockState ownerState;
        public Level level;
        public Direction.Axis axis = Direction.Axis.X;
    }

    private static final class GroupAccumulator {
        private final BlockState[] states;
        private final ArrayList<net.minecraft.client.renderer.block.model.BlockModelPart>[] parts;
        private int size;

        @SuppressWarnings("unchecked")
        private GroupAccumulator(int capacity) {
            this.states = new BlockState[capacity];
            this.parts = (ArrayList<net.minecraft.client.renderer.block.model.BlockModelPart>[]) new ArrayList[capacity];
        }

        private void add(BlockState state, net.minecraft.client.renderer.block.model.BlockModelPart part) {
            if (state == null || part == null) {
                return;
            }
            for (int i = 0; i < size; i++) {
                if (states[i] == state || states[i].equals(state)) {
                    parts[i].add(part);
                    return;
                }
            }
            if (size >= states.length) {
                return;
            }
            states[size] = state;
            parts[size] = new ArrayList<>(states.length);
            parts[size].add(part);
            size++;
        }
    }
}
