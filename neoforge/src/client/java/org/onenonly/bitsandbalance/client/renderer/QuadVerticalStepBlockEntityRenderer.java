package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;

public class QuadVerticalStepBlockEntityRenderer
        implements BlockEntityRenderer<QuadVerticalStepBlockEntity, QuadVerticalStepBlockEntityRenderer.State> {

    public QuadVerticalStepBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(QuadVerticalStepBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakOverlay);
        state.slabs = blockEntity.getAllSlabs();
        state.level = blockEntity.getLevel();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState) {
        bitsandbalance$submit(state, poseStack, collector);
    }

    private void bitsandbalance$submit(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.blockPos == null || state.level == null) return;

        for (int i = 0; i < 4; i++) {
            BlockState slab = state.slabs[i];
            if (slab == null) continue;
            Direction facing = (i == 2 || i == 3) ? Direction.SOUTH : Direction.NORTH;
            Direction side = (i == 1 || i == 3) ? Direction.EAST : Direction.WEST;
            VerticalStepBlockEntityRenderer.submitQuadrant(collector, poseStack, state.level, state.blockPos,
                    slab, facing, side, state.lightCoords);
        }
    }

    public static final class State extends BlockEntityRenderState {
        public BlockState[] slabs = new BlockState[4];
        public Level level;
    }
}