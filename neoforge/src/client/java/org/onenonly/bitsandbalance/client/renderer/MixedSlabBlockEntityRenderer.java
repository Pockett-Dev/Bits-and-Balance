package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;

import java.util.List;

public class MixedSlabBlockEntityRenderer
        implements BlockEntityRenderer<MixedSlabBlockEntity, MixedSlabBlockEntityRenderer.State> {

    public MixedSlabBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MixedSlabBlockEntity blockEntity, State state, float partialTick,
                                   net.minecraft.world.phys.Vec3 cameraPos,
                                   net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakOverlay);

        state.bottomSlabState = blockEntity.getBottomSlab();
        state.topSlabState = blockEntity.getTopSlab();
        state.level = blockEntity.getLevel();
        state.lightCoords = VirtualRenderWorldLightBridge.capturePackedLight(state.level, state.lightCoords);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState) {
        if (state.blockPos == null || state.level == null) {
            return;
        }

        if (state.bottomSlabState != null) {
            submitSlabHalf(collector, poseStack, state.level, state.blockPos, state.bottomSlabState, state.lightCoords);
        }
        if (state.topSlabState != null) {
            submitSlabHalf(collector, poseStack, state.level, state.blockPos, state.topSlabState, state.lightCoords);
        }
    }

    public static void submitSlabHalf(SubmitNodeCollector collector, PoseStack poseStack,
                                      Object level, net.minecraft.core.BlockPos blockPos,
                                      BlockState slabState, int packedLight) {
        List<BlockStateModelPart> parts = RuntimeModelCompat.collectRuntimeModelParts(slabState, blockPos);
        if (parts == null || parts.isEmpty()) {
            return;
        }
        var renderType = MovingBlockRenderTypeCompat.get(slabState, parts);

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

    public static final class State extends BlockEntityRenderState {
        public BlockState bottomSlabState;
        public BlockState topSlabState;
        public Level level;
    }
}