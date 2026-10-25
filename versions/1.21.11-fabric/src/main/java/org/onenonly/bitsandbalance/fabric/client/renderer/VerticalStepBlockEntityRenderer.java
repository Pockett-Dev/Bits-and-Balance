package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior - Vertical Step Block Entity Renderer (Fabric)
 *
 * Renders the slab model of a single-quadrant {@link VerticalStepBlock} using the source
 * vertical-slab's textures, clipped to the vertical 1/4-block prism shape (8 x 16 x 8).
 */
public class VerticalStepBlockEntityRenderer
        implements BlockEntityRenderer<VerticalStepBlockEntity, VerticalStepBlockEntityRenderer.State> {

    public VerticalStepBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return EnhancedSlabTerrainRenderHelper.verticalStepBlockEntityViewDistance();
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
        if (!EnhancedSlabBlockEntityRenderLimits.shouldSubmit(state.blockPos, cameraPos)) {
            state.slabState = null;
            return;
        }
        if (bs.getBlock() instanceof VerticalStepBlock) {
            state.facing = bs.getValue(VerticalStepBlock.FACING);
            state.side   = bs.getValue(VerticalStepBlock.SIDE);
        } else {
            state.facing = Direction.NORTH;
            state.side   = Direction.WEST;
        }
        if (EnhancedSlabTerrainRenderHelper.shouldSkipBlockEntityRender(state.level, state.blockPos, bs)) {
            state.slabState = null;
            return;
        }

        state.lightCoords = VirtualRenderWorldLightBridge.capturePackedLight(state.level, state.lightCoords);
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
        if (state.blockPos == null || state.level == null || state.slabState == null) return;
        if (EnhancedSlabTerrainRenderHelper.shouldSkipBlockEntityRender(state.level, state.blockPos, state.ownerState)) return;
        if (!EnhancedSlabBlockEntityRenderLimits.shouldSubmit(state, cameraRenderState)) return;
        EnhancedSlabRenderDiagnostics.verticalStepFallback();
        submitQuadrant(collector, poseStack, state.level, state.blockPos,
            state.slabState, state.facing, state.side, state.lightCoords);
    }

    // Shared helper used by QuadVerticalStepBlockEntityRenderer

    /**
     * Submits a single vertical 1/4-block quadrant for rendering.
     *
     * @param facing Direction.NORTH (z 0-8) or Direction.SOUTH (z 8-16)
     * @param side   Direction.WEST  (x 0-8) or Direction.EAST  (x 8-16)
     */
    public static void submitQuadrant(SubmitNodeCollector collector, PoseStack poseStack,
                                      BlockAndTintGetter level, BlockPos blockPos,
                                      BlockState slabState, Direction facing, Direction side, int packedLight) {
        submitQuadrant(collector, poseStack, level, blockPos, slabState, facing, side, packedLight, EnhancedSlabRenderModelCompat.allDirectionMask());
    }

    public static void submitQuadrant(SubmitNodeCollector collector, PoseStack poseStack,
                                      BlockAndTintGetter level, BlockPos blockPos,
                                      BlockState slabState, Direction facing, Direction side, int packedLight, int visibleMask) {
        float xMin = side == Direction.EAST ? 8f : 0f;
        float xMax = side == Direction.EAST ? 16f : 8f;
        float zMin = facing == Direction.SOUTH ? 8f : 0f;
        float zMax = facing == Direction.SOUTH ? 16f : 8f;

        Vector3fc from = new Vector3f(xMin, 0f, zMin);
        Vector3fc to   = new Vector3f(xMax, 16f, zMax);

        BlockModelPart part = buildStepPrism(level, blockPos, slabState, from, to);
        if (part == null) return;
        submitQuadrantParts(
                collector,
                poseStack,
                level,
                blockPos,
                slabState,
                EnhancedSlabRenderModelCompat.singletonPartList(EnhancedSlabRenderModelCompat.filterDirections(part, visibleMask)),
                packedLight
        );
    }

    public static void submitQuadrantParts(SubmitNodeCollector collector, PoseStack poseStack,
                                           BlockAndTintGetter level, BlockPos blockPos,
                                           BlockState slabState, List<BlockModelPart> parts, int packedLight) {
        if (parts == null || parts.isEmpty()) {
            return;
        }

        RenderType renderType = switch (ItemBlockRenderTypes.getChunkRenderType(slabState)) {
            case CUTOUT      -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
            case TRIPWIRE    -> RenderTypes.tripwireMovingBlock();
            default          -> RenderTypes.solidMovingBlock();
        };

        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            PoseStack tmp = new PoseStack();
            tmp.last().pose().set(pose.pose());
            tmp.last().normal().set(pose.normal());

            EnhancedSlabRenderOffsetContext.pushBlockEntitySubmit();
            BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
            try {
                VirtualRenderWorldLightBridge.renderBatched(
                        slabState,
                        blockPos,
                        level,
                        tmp,
                        consumer,
                        false,
                        parts,
                        packedLight
                );
            } finally {
                BlockColorRenderContext.clear();
                EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            }
        });
    }

    public static boolean renderTerrainQuadrant(BlockRenderDispatcher dispatcher,
                                                BlockAndTintGetter level,
                                                BlockPos blockPos,
                                                BlockState slabState,
                                                Direction facing,
                                                Direction side,
                                                PoseStack poseStack,
                                                VertexConsumer consumer) {
        return renderTerrainQuadrant(dispatcher, level, blockPos, slabState, facing, side, poseStack, consumer, EnhancedSlabRenderModelCompat.allDirectionMask());
    }

    public static boolean renderTerrainQuadrant(BlockRenderDispatcher dispatcher,
                                                BlockAndTintGetter level,
                                                BlockPos blockPos,
                                                BlockState slabState,
                                                Direction facing,
                                                Direction side,
                                                PoseStack poseStack,
                                                VertexConsumer consumer,
                                                int visibleMask) {
        float xMin = side == Direction.EAST ? 8f : 0f;
        float xMax = side == Direction.EAST ? 16f : 8f;
        float zMin = facing == Direction.SOUTH ? 8f : 0f;
        float zMax = facing == Direction.SOUTH ? 16f : 8f;

        Vector3fc from = new Vector3f(xMin, 0f, zMin);
        Vector3fc to = new Vector3f(xMax, 16f, zMax);

        BlockModelPart part = buildStepPrism(level, blockPos, slabState, from, to);
        if (part == null) return false;

        BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
        try {
            EnhancedSlabTerrainRenderHelper.renderNested(
                    dispatcher,
                    slabState,
                    blockPos,
                    level,
                    poseStack,
                    consumer,
                    EnhancedSlabRenderModelCompat.singletonPartList(EnhancedSlabRenderModelCompat.filterDirections(part, visibleMask))
            );
        } finally {
            BlockColorRenderContext.clear();
        }
        return true;
    }

    public static @Nullable BlockModelPart buildQuadrantPart(BlockState slabState, BlockPos blockPos, Direction facing, Direction side) {
        float xMin = side == Direction.EAST ? 8f : 0f;
        float xMax = side == Direction.EAST ? 16f : 8f;
        float zMin = facing == Direction.SOUTH ? 8f : 0f;
        float zMax = facing == Direction.SOUTH ? 16f : 8f;
        return buildStepPrism(
                null,
                blockPos,
                slabState,
                new Vector3f(xMin, 0f, zMin),
                new Vector3f(xMax, 16f, zMax)
        );
    }

    // Prism geometry

    private static @Nullable BlockModelPart buildStepPrism(BlockAndTintGetter level, BlockPos pos, BlockState slabState,
                                                           Vector3fc from, Vector3fc to) {
        return StepBlockEntityRenderer.buildExactPrismPart(level, pos, StepBlockEntityRenderer.verticalPrismRenderState(slabState), from, to, EnhancedSlabRenderModelCompat.allDirectionMask());
    }

    // Render state

    public static final class State extends BlockEntityRenderState {
        public @Nullable BlockState slabState;
        public @Nullable Level level;
        public @Nullable BlockState ownerState;
        public Direction facing = Direction.NORTH;
        public Direction side   = Direction.WEST;
    }
}
