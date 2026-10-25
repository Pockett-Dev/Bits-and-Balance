package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tweaks: Enhanced Slab Behavior ??? Quad Step Block Entity Renderer (Fabric)
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
        return EnhancedSlabBlockEntityRenderLimits.viewDistance();
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
        BlockState[] slabs = blockEntity.getAllSlabs();
        System.arraycopy(slabs, 0, state.slabs, 0, Math.min(slabs.length, state.slabs.length));
        for (int i = slabs.length; i < state.slabs.length; i++) {
            state.slabs[i] = null;
        }
        state.level = blockEntity.getLevel();
        state.lightCoords = VirtualRenderWorldLightBridge.capturePackedLight(state.level, state.lightCoords);

        BlockState bs = blockEntity.getBlockState();
        state.ownerState = bs;
        state.axis = bs != null && bs.getBlock() instanceof QuadStepBlock ? bs.getValue(QuadStepBlock.AXIS) : Direction.Axis.X;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraRenderState) {
        if (EnhancedSlabTerrainRenderHelper.shouldSkipBlockEntityRender(state.level, state.blockPos, state.ownerState)) return;
        if (!EnhancedSlabBlockEntityRenderLimits.shouldSubmit(state, cameraRenderState)) return;
        bitsandbalance$submit(state, poseStack, collector);
    }

    private void bitsandbalance$submit(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.blockPos == null || state.level == null) return;

        Direction.Axis axis = state.axis;
        Map<BlockState, List<BlockStateModelPart>> partsBySlab = new LinkedHashMap<>(8);

        BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(state.slabs);
        if (fullBlockState != null) {
            BlockStateModelPart part = bitsandbalance$buildFullBlockPart(fullBlockState, state.blockPos);
            if (part != null) {
                StepBlockEntityRenderer.submitQuadrantParts(collector, poseStack, state.level, state.blockPos,
                        fullBlockState, FabricRuntimeModelCompat.singletonPartList(part), state.lightCoords);
            }
            return;
        }

        // Quadrant index mapping (matches QuadStepBlock constants):
        //   0 = bottom-negative, 1 = bottom-positive, 2 = top-negative, 3 = top-positive
        // Axis=X: negative=WEST, positive=EAST
        // Axis=Z: negative=NORTH, positive=SOUTH
        for (int i = 0; i < 4; i++) {
            BlockState slab = state.slabs[i];
            if (slab == null) continue;
            boolean positive = (i == 1 || i == 3);
            Direction facing = axis == Direction.Axis.Z
                    ? (positive ? Direction.SOUTH : Direction.NORTH)
                    : (positive ? Direction.EAST : Direction.WEST);
            boolean top = (i >= 2);
            bitsandbalance$addStepParts(partsBySlab, state.blockPos, slab, facing, top, bitsandbalance$visibleMask(axis, i, state.slabs));
        }

        for (Map.Entry<BlockState, List<BlockStateModelPart>> entry : partsBySlab.entrySet()) {
            StepBlockEntityRenderer.submitQuadrantParts(collector, poseStack, state.level, state.blockPos,
                    entry.getKey(), FabricRuntimeModelCompat.partList(entry.getValue()), state.lightCoords);
        }
    }

    private static void bitsandbalance$addStepParts(Map<BlockState, List<BlockStateModelPart>> groups,
                                                    net.minecraft.core.BlockPos pos,
                                                    BlockState slabState,
                                                    Direction facing,
                                                    boolean top,
                                                    int visibleMask) {
        BlockState renderSlab = bitsandbalance$stepRenderState(slabState, top);
        if (!bitsandbalance$hasJoinedVerticalNeighbor(top, visibleMask) || (visibleMask & HORIZONTAL_DIRECTIONS) == 0) {
            BlockStateModelPart part = StepBlockEntityRenderer.buildQuadrantPart(renderSlab, pos, facing, top);
            if (part != null) {
                groups.computeIfAbsent(renderSlab, ignored -> new ArrayList<>(4)).add(FabricRuntimeModelCompat.filterDirections(part, visibleMask));
            }
            return;
        }

        int bodyMask = visibleMask & ~HORIZONTAL_DIRECTIONS;
        BlockStateModelPart bodyPart = StepBlockEntityRenderer.buildQuadrantPart(renderSlab, pos, facing, top);
        if (bodyPart != null && bodyMask != 0) {
            groups.computeIfAbsent(renderSlab, ignored -> new ArrayList<>(4)).add(FabricRuntimeModelCompat.filterDirections(bodyPart, bodyMask));
        }

        BlockState sideState = bitsandbalance$fullBlockStateForSlab(renderSlab);
        if (sideState == null) {
            sideState = renderSlab;
        }
        BlockStateModelPart sidePart = bitsandbalance$buildJoinedStepSidePart(sideState, renderSlab, pos, facing, top, visibleMask & HORIZONTAL_DIRECTIONS);
        if (sidePart != null) {
            groups.computeIfAbsent(sideState, ignored -> new ArrayList<>(4)).add(sidePart);
        }
    }

    private static BlockStateModelPart bitsandbalance$buildJoinedStepSidePart(BlockState sideState,
                                                                               BlockState sourceState,
                                                                               net.minecraft.core.BlockPos pos,
                                                                               Direction facing,
                                                                               boolean top,
                                                                               int visibleMask) {
        if (visibleMask == 0) {
            return null;
        }
        float yMin = top ? 8.0f : 0.0f;
        float yMax = top ? Math.min(16.0f, 8.0f + FabricRuntimeModelCompat.sourceModelHeight(sourceState, pos)) : 8.0f;
        Object part = FabricRuntimeModelCompat.buildExactCuboidPart(
                sideState,
                pos,
                bitsandbalance$stepFrom(facing, yMin),
                bitsandbalance$stepTo(facing, yMax),
                visibleMask
        );
        return part instanceof BlockStateModelPart blockStateModelPart ? blockStateModelPart : null;
    }

    private static int bitsandbalance$visibleMask(Direction.Axis axis, int index, BlockState[] slabs) {
        int mask = FabricRuntimeModelCompat.allDirectionMask();
        boolean positive = index == 1 || index == 3;
        boolean top = index >= 2;
        int horizontalNeighbor = top ? (positive ? 2 : 3) : (positive ? 0 : 1);
        if (bitsandbalance$sameRenderMaterial(slabs[index], slabs[horizontalNeighbor])) {
            Direction internal = axis == Direction.Axis.Z
                    ? (positive ? Direction.NORTH : Direction.SOUTH)
                    : (positive ? Direction.WEST : Direction.EAST);
            mask = bitsandbalance$without(mask, internal);
        }
        int verticalNeighbor = top ? index - 2 : index + 2;
        if (bitsandbalance$sameRenderMaterial(slabs[index], slabs[verticalNeighbor])) {
            mask = bitsandbalance$without(mask, top ? Direction.DOWN : Direction.UP);
        }
        return mask;
    }

    private static boolean bitsandbalance$sameRenderMaterial(BlockState first, BlockState second) {
        return first != null && first.equals(second);
    }

    private static int bitsandbalance$without(int mask, Direction direction) {
        return direction == null ? mask : mask & ~(1 << direction.ordinal());
    }

    private static BlockState bitsandbalance$stepRenderState(BlockState slabState, boolean top) {
        if (slabState != null && slabState.hasProperty(SlabBlock.TYPE)) {
            return slabState.setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
        }
        return slabState;
    }

    private static boolean bitsandbalance$hasJoinedVerticalNeighbor(boolean top, int visibleMask) {
        Direction hidden = top ? Direction.DOWN : Direction.UP;
        return (visibleMask & (1 << hidden.ordinal())) == 0;
    }

    private static BlockState bitsandbalance$fullBlockStateForSlab(BlockState slabState) {
        return SlabFamilyModelSourceSelector.verticalPrismRenderState(slabState);
    }

    private static BlockStateModelPart bitsandbalance$buildFullBlockPart(BlockState fullBlockState, net.minecraft.core.BlockPos pos) {
        Object part = FabricRuntimeModelCompat.buildExactCuboidPart(
                fullBlockState,
                pos,
                new Vector3f(0.0F, 0.0F, 0.0F),
                new Vector3f(16.0F, 16.0F, 16.0F),
                FabricRuntimeModelCompat.allDirectionMask()
        );
        return part instanceof BlockStateModelPart blockStateModelPart ? blockStateModelPart : null;
    }

    private static Vector3fc bitsandbalance$stepFrom(Direction facing, float yMin) {
        return switch (facing) {
            case SOUTH -> new Vector3f(0.0F, yMin, 8.0F);
            case EAST -> new Vector3f(8.0F, yMin, 0.0F);
            default -> new Vector3f(0.0F, yMin, 0.0F);
        };
    }

    private static Vector3fc bitsandbalance$stepTo(Direction facing, float yMax) {
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
}
