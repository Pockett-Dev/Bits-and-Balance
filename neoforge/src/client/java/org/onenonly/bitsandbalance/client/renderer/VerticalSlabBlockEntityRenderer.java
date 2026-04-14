package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;

import java.util.Collections;
import java.util.List;

public class VerticalSlabBlockEntityRenderer
        implements BlockEntityRenderer<VerticalSlabBlockEntity, VerticalSlabBlockEntityRenderer.State> {

    public VerticalSlabBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VerticalSlabBlockEntity blockEntity, State state, float partialTick,
                                   net.minecraft.world.phys.Vec3 cameraPos,
                                   net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakOverlay);
        state.level = blockEntity.getLevel();

        BlockState beState = blockEntity.getBlockState();
        if (beState != null && beState.getBlock() instanceof VerticalSlabBlock) {
            state.facing = beState.getValue(VerticalSlabBlock.FACING);
            state.isDouble = beState.getValue(VerticalSlabBlock.DOUBLE);
        } else {
            state.facing = Direction.NORTH;
            state.isDouble = false;
        }

        state.facingSlabState = bitsandbalance$resolveRenderSlabState(
                blockEntity.getFacingSlab(), beState, state.level, state.blockPos);
        state.oppositeSlabState = bitsandbalance$resolveRenderSlabState(
                blockEntity.getOppositeSlab(), beState, state.level, state.blockPos);
    }

    private static BlockState bitsandbalance$resolveRenderSlabState(@org.jetbrains.annotations.Nullable BlockState candidate,
                                                                    BlockState ownerState,
                                                                    Level level,
                                                                    BlockPos pos) {
        if (!(ownerState.getBlock() instanceof FixedVerticalSlabBlock)) {
            return candidate;
        }

        BlockState inferred = VerticalSlabBlockEntity.bitsandbalance$defaultSlabStateForOwner(ownerState);
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
        if (state.blockPos == null || state.level == null) return;
        if (state.facingSlabState != null) {
            submitVerticalHalf(collector, poseStack, state.level, state.blockPos,
                    state.facingSlabState, state.facing, state.lightCoords);
        }

        if (state.isDouble && state.oppositeSlabState != null) {
            submitVerticalHalf(collector, poseStack, state.level, state.blockPos,
                    state.oppositeSlabState, state.facing.getOpposite(), state.lightCoords);
        }
    }

    public static void submitVerticalHalf(SubmitNodeCollector collector, PoseStack poseStack,
                                          Object level, BlockPos blockPos,
                                          BlockState slabState, Direction half, int packedLight) {
        Object prismPart = buildVerticalSlabPrismPart(level, blockPos, slabState, half);
        if (prismPart == null) return;
        BlockStateModelPart part = (BlockStateModelPart) prismPart;
        RenderType renderType = MovingBlockRenderTypeCompat.get(slabState, part);
        List<BlockStateModelPart> parts = Collections.singletonList(part);
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

    private static Object buildVerticalSlabPrismPart(Object level, BlockPos pos, BlockState slabState, Direction half) {
        if (half == null || half.getAxis() == Direction.Axis.Y) return null;
        return RuntimeModelCompat.buildVerticalSlabPart(slabState, pos, half);
    }

    public static final class State extends BlockEntityRenderState {
        public BlockState facingSlabState;
        public BlockState oppositeSlabState;
        public Level level;
        public Direction facing;
        public boolean isDouble;
    }
}