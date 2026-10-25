package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;

import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior ??? Step Block Entity Renderer (Fabric)
 *
 * Renders the slab model of a single-quadrant {@link StepBlock} using the source slab's
 * textures, clipped to the ??-block prism shape (8 ?? 8 ?? 16).
 */
public class StepBlockEntityRenderer
        implements BlockEntityRenderer<StepBlockEntity, StepBlockEntityRenderer.State> {

    public StepBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
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
    public void extractRenderState(StepBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakOverlay);
        state.level = blockEntity.getLevel();

        BlockState bs = blockEntity.getBlockState();
        state.ownerState = bs;
        if (bs.getBlock() instanceof StepBlock) {
            state.facing = bs.getValue(StepBlock.FACING);
            state.top = bs.getValue(StepBlock.TOP);
        } else {
            state.facing = Direction.EAST;
            state.top = false;
        }

        state.slabState = bitsandbalance$stepRenderState(
                bitsandbalance$resolveRenderSlabState(blockEntity.getSlabState(), bs, state.level, state.blockPos),
                state.top);
    }

    private static BlockState bitsandbalance$resolveRenderSlabState(@Nullable BlockState candidate,
                                                                    BlockState ownerState,
                                                                    @Nullable Level level,
                                                                    @Nullable BlockPos pos) {
        BlockState inferred = null;
        if (ownerState != null) {
            if (ownerState.getBlock() instanceof FixedStepBlock fixed) {
                inferred = fixed.getSourceSlab().defaultBlockState();
            } else {
                var mapped = StepDynamicRegistry.getSlabForStep(ownerState.getBlock());
                if (mapped != null) {
                    inferred = mapped.defaultBlockState();
                }
            }
        }
        if (inferred == null) {
            return candidate;
        }
        if (candidate == null) {
            return inferred;
        }
        if (candidate.getBlock() != inferred.getBlock()) {
            return inferred;
        }
        return candidate;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraRenderState) {
        if (EnhancedSlabTerrainRenderHelper.shouldSkipFixedBlockEntityRender(state.ownerState)) return;
        if (EnhancedSlabTerrainRenderHelper.shouldSkipBlockEntityRender(state.level, state.blockPos, state.ownerState)) return;
        if (!EnhancedSlabBlockEntityRenderLimits.shouldSubmit(state, cameraRenderState)) return;
        bitsandbalance$submit(state, poseStack, collector);
    }

    private void bitsandbalance$submit(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.blockPos == null || state.level == null || state.slabState == null) return;
        EnhancedSlabRenderDiagnostics.stepFallback();
        submitQuadrant(collector, poseStack, state.level, state.blockPos,
            state.slabState, state.facing, state.top, state.lightCoords);
    }

    // Shared helper used by QuadStepBlockEntityRenderer

    /**
     * Submits a single ??-block quadrant for rendering using the source slab's textures.
     *
     * @param facing Direction.EAST/WEST/NORTH/SOUTH
     * @param top    {@code true} = top half (y 8???16), {@code false} = bottom (y 0???8)
     */
    public static void submitQuadrant(SubmitNodeCollector collector, PoseStack poseStack,
                                      Object level, BlockPos blockPos,
                                      BlockState slabState, Direction facing, boolean top, int packedLight) {
        BlockState renderSlab = bitsandbalance$stepRenderState(slabState, top);
        BlockStateModelPart prismPart = buildQuadrantPart(renderSlab, blockPos, facing, top);
        if (prismPart == null) return;
        submitQuadrantParts(collector, poseStack, level, blockPos, renderSlab, FabricRuntimeModelCompat.singletonPartList(prismPart), packedLight);
    }

    public static @Nullable BlockStateModelPart buildQuadrantPart(BlockState slabState, BlockPos blockPos, Direction facing, boolean top) {
        Object part = FabricRuntimeModelCompat.buildStepPart(bitsandbalance$stepRenderState(slabState, top), blockPos, facing, top);
        return part instanceof BlockStateModelPart prismPart ? prismPart : null;
    }

    public static void submitQuadrantParts(SubmitNodeCollector collector, PoseStack poseStack,
                                           Object level, BlockPos blockPos,
                                           BlockState slabState, List<BlockStateModelPart> parts, int packedLight) {
        if (parts == null || parts.isEmpty()) return;
        RenderType renderType = MovingBlockRenderTypeCompat.get(slabState, parts);

        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            EnhancedSlabRenderOffsetContext.pushBlockEntitySubmit();
            BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
            try {
                ReflectiveLitBlockModelTesselationBridge.render(pose, consumer, level, blockPos, slabState, parts, packedLight);
            } finally {
                BlockColorRenderContext.clear();
                EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            }
        });
    }

    public static void renderTerrainQuadrant(Object dispatcher,
                                             BlockAndTintGetter level,
                                             BlockPos blockPos,
                                             BlockState slabState,
                                             Direction facing,
                                             boolean top,
                                             PoseStack poseStack,
                                             VertexConsumer consumer) {
        float xMin;
        float xMax;
        float zMin;
        float zMax;

        switch (facing) {
            case EAST -> {
                xMin = 8f;
                xMax = 16f;
                zMin = 0f;
                zMax = 16f;
            }
            case WEST -> {
                xMin = 0f;
                xMax = 8f;
                zMin = 0f;
                zMax = 16f;
            }
            case SOUTH -> {
                xMin = 0f;
                xMax = 16f;
                zMin = 8f;
                zMax = 16f;
            }
            case NORTH -> {
                xMin = 0f;
                xMax = 16f;
                zMin = 0f;
                zMax = 8f;
            }
            default -> {
                xMin = 8f;
                xMax = 16f;
                zMin = 0f;
                zMax = 16f;
            }
        }
        float yMin = top ? 8f : 0f;
        float yMax = top ? 16f : 8f;

        BlockState renderSlab = bitsandbalance$stepRenderState(slabState, top);
        Object part = FabricRuntimeModelCompat.buildStepPart(renderSlab, blockPos, facing, top);
        if (!(part instanceof BlockStateModelPart prismPart)) return;

        BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
        try {
            EnhancedSlabTerrainRenderHelper.renderNested(dispatcher, renderSlab, blockPos, level, poseStack, consumer, FabricRuntimeModelCompat.singletonPartList(prismPart));
        } finally {
            BlockColorRenderContext.clear();
        }
    }

    private static BlockState bitsandbalance$stepRenderState(BlockState slabState, boolean top) {
        if (slabState != null && slabState.hasProperty(SlabBlock.TYPE)) {
            slabState = slabState.setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
        }
        if (slabState != null && slabState.hasProperty(SlabBlock.WATERLOGGED)) {
            slabState = slabState.setValue(SlabBlock.WATERLOGGED, false);
        }
        return slabState;
    }

    // Render state

    public static final class State extends BlockEntityRenderState {
        public @Nullable BlockState slabState;
        public @Nullable BlockState ownerState;
        public @Nullable Level level;
        public Direction facing = Direction.EAST;
        public boolean top = false;
    }
}
