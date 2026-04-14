package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.Collections;
import java.util.List;

public class VerticalStepBlockEntityRenderer
        implements BlockEntityRenderer<VerticalStepBlockEntity, VerticalStepBlockEntityRenderer.State> {

    public VerticalStepBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
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
        if (bs.getBlock() instanceof VerticalStepBlock) {
            state.facing = bs.getValue(VerticalStepBlock.FACING);
            state.side = bs.getValue(VerticalStepBlock.SIDE);
        } else {
            state.facing = Direction.NORTH;
            state.side = Direction.WEST;
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
            net.minecraft.world.level.block.Block sourceVerticalSlab;
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
                       net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState) {
        bitsandbalance$submit(state, poseStack, collector);
    }

    private void bitsandbalance$submit(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.blockPos == null || state.level == null || state.slabState == null) return;
        submitQuadrant(collector, poseStack, state.level, state.blockPos,
            state.slabState, state.facing, state.side, state.lightCoords);
    }

    public static void submitQuadrant(SubmitNodeCollector collector, PoseStack poseStack,
                                      Object level, BlockPos blockPos,
                                      BlockState slabState, Direction facing, Direction side, int packedLight) {
        float xMin = side == Direction.EAST ? 8f : 0f;
        float xMax = side == Direction.EAST ? 16f : 8f;
        float zMin = facing == Direction.SOUTH ? 8f : 0f;
        float zMax = facing == Direction.SOUTH ? 16f : 8f;

        Vector3fc from = new Vector3f(xMin, 0f, zMin);
        Vector3fc to = new Vector3f(xMax, 16f, zMax);

        Object part = buildStepPrism(level, blockPos, slabState, from, to);
        if (part == null) return;
        BlockStateModelPart prismPart = (BlockStateModelPart) part;
        RenderType renderType = MovingBlockRenderTypeCompat.get(slabState, prismPart);
        List<BlockStateModelPart> parts = Collections.singletonList(prismPart);
        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            EnhancedSlabRenderOffsetContext.pushBlockEntitySubmit();
            BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
            try {
                LitBlockModelTesselationBridge.render(pose, consumer, level, blockPos, slabState, parts, packedLight);
            } finally {
                BlockColorRenderContext.clear();
                EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            }
        });
    }

    private static @Nullable Object buildStepPrism(Object level, BlockPos pos, BlockState slabState,
                                                   Vector3fc from, Vector3fc to) {
        Direction facing = from.z() >= 8.0f ? Direction.SOUTH : Direction.NORTH;
        Direction side = from.x() >= 8.0f ? Direction.EAST : Direction.WEST;
        return RuntimeModelCompat.buildVerticalStepPart(slabState, pos, facing, side);
    }

    public static final class State extends BlockEntityRenderState {
        public @Nullable BlockState slabState;
        public @Nullable Level level;
        public Direction facing = Direction.NORTH;
        public Direction side = Direction.WEST;
    }
}