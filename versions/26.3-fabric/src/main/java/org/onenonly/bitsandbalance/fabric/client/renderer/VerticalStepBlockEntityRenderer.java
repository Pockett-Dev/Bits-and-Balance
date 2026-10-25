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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior ??? Vertical Step Block Entity Renderer (Fabric)
 *
 * Renders the slab model of a single-quadrant {@link VerticalStepBlock} using the source
 * vertical-slab's textures, clipped to the vertical ??-block prism shape (8 ?? 16 ?? 8).
 */
public class VerticalStepBlockEntityRenderer
        implements BlockEntityRenderer<VerticalStepBlockEntity, VerticalStepBlockEntityRenderer.State> {

    public VerticalStepBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
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
    public void extractRenderState(VerticalStepBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakOverlay);
        state.level = blockEntity.getLevel();

        BlockState bs = blockEntity.getBlockState();
        state.ownerState = bs;
        if (bs.getBlock() instanceof VerticalStepBlock) {
            state.facing = bs.getValue(VerticalStepBlock.FACING);
            state.side   = bs.getValue(VerticalStepBlock.SIDE);
        } else {
            state.facing = Direction.NORTH;
            state.side   = Direction.WEST;
        }

        state.slabState = bitsandbalance$resolveRenderSlabState(
                blockEntity.getSlabState(), bs, state.level, state.blockPos);
    }

    private static BlockState bitsandbalance$resolveRenderSlabState(@Nullable BlockState candidate,
                                                                    BlockState ownerState,
                                                                    @Nullable Level level,
                                                                    @Nullable BlockPos pos) {
        BlockState inferred = null;
        if (ownerState != null) {
            net.minecraft.world.level.block.Block sourceVerticalSlab = null;
            if (ownerState.getBlock() instanceof FixedVerticalStepBlock fixed) {
                sourceVerticalSlab = fixed.getSourceVerticalSlab();
            } else {
                sourceVerticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(ownerState.getBlock());
            }
            if (sourceVerticalSlab != null) {
                var sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
                inferred = (sourceSlab != null ? sourceSlab : sourceVerticalSlab).defaultBlockState();
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
        EnhancedSlabRenderDiagnostics.verticalStepFallback();
        submitQuadrant(collector, poseStack, state.level, state.blockPos,
            state.slabState, state.facing, state.side, state.lightCoords);
    }

    // Shared helper used by QuadVerticalStepBlockEntityRenderer

    /**
     * Submits a single vertical ??-block quadrant for rendering.
     *
     * @param facing Direction.NORTH (z 0???8) or Direction.SOUTH (z 8???16)
     * @param side   Direction.WEST  (x 0???8) or Direction.EAST  (x 8???16)
     */
    public static void submitQuadrant(SubmitNodeCollector collector, PoseStack poseStack,
                                      Object level, BlockPos blockPos,
                                      BlockState slabState, Direction facing, Direction side, int packedLight) {
        BlockStateModelPart prismPart = buildQuadrantPart(slabState, blockPos, facing, side);
        if (prismPart == null) return;
        submitQuadrantParts(collector, poseStack, level, blockPos, slabState, FabricRuntimeModelCompat.singletonPartList(prismPart), packedLight);
    }

    public static @Nullable BlockStateModelPart buildQuadrantPart(BlockState slabState, BlockPos blockPos, Direction facing, Direction side) {
        Object part = FabricRuntimeModelCompat.buildVerticalStepPart(slabState, blockPos, facing, side);
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
                                             Direction side,
                                             PoseStack poseStack,
                                             VertexConsumer consumer) {
        float xMin = side == Direction.EAST ? 8f : 0f;
        float xMax = side == Direction.EAST ? 16f : 8f;
        float zMin = facing == Direction.SOUTH ? 8f : 0f;
        float zMax = facing == Direction.SOUTH ? 16f : 8f;

        Object part = FabricRuntimeModelCompat.buildVerticalStepPart(slabState, blockPos, facing, side);
        if (!(part instanceof BlockStateModelPart prismPart)) return;

        BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
        try {
            EnhancedSlabTerrainRenderHelper.renderNested(dispatcher, slabState, blockPos, level, poseStack, consumer, FabricRuntimeModelCompat.singletonPartList(prismPart));
        } finally {
            BlockColorRenderContext.clear();
        }
    }

    // Render state

    public static final class State extends BlockEntityRenderState {
        public @Nullable BlockState slabState;
        public @Nullable BlockState ownerState;
        public @Nullable Level level;
        public Direction facing = Direction.NORTH;
        public Direction side   = Direction.WEST;
    }
}
