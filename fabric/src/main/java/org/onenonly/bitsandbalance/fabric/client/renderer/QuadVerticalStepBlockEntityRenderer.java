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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;

/**
 * Tweaks: Enhanced Slab Behavior — Quad Vertical Step Block Entity Renderer (Fabric)
 *
 * Renders up to 4 quadrant vertical steps stored in a {@link QuadVerticalStepBlockEntity},
 * delegating per-quadrant geometry to {@link VerticalStepBlockEntityRenderer#submitQuadrant}.
 */
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
        state.lightCoords = VirtualRenderWorldLightBridge.capturePackedLight(state.level, state.lightCoords);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraRenderState) {
        if (state.blockPos == null || state.level == null) return;

        // Quadrant index mapping (matches QuadVerticalStepBlock constants):
        //   0 = NORTH_WEST: FACING=NORTH, SIDE=WEST  (X 0-8,  Z 0-8)
        //   1 = NORTH_EAST: FACING=NORTH, SIDE=EAST  (X 8-16, Z 0-8)
        //   2 = SOUTH_WEST: FACING=SOUTH, SIDE=WEST  (X 0-8,  Z 8-16)
        //   3 = SOUTH_EAST: FACING=SOUTH, SIDE=EAST  (X 8-16, Z 8-16)
        for (int i = 0; i < 4; i++) {
            BlockState slab = state.slabs[i];
            if (slab == null) continue;
            Direction facing = (i == 2 || i == 3) ? Direction.SOUTH : Direction.NORTH;
            Direction side   = (i == 1 || i == 3) ? Direction.EAST  : Direction.WEST;
            VerticalStepBlockEntityRenderer.submitQuadrant(collector, poseStack, state.level, state.blockPos,
                    slab, facing, side, state.lightCoords);
        }
    }

    public static final class State extends BlockEntityRenderState {
        public BlockState[] slabs = new BlockState[4];
        public Level level;
    }
}
