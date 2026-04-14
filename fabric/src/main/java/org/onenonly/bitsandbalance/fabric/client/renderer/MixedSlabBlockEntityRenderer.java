package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;

import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior — Mixed Double Slab Block Entity Renderer (Fabric)
 *
 * Renders both slab halves stored in the {@link MixedSlabBlockEntity} using the
 * vanilla block-model pipeline.  Each half is rendered using the actual slab model
 * of its block type (e.g. oak_slab for the bottom, stone_slab for the top).
 */
public class MixedSlabBlockEntityRenderer
        implements BlockEntityRenderer<MixedSlabBlockEntity, MixedSlabBlockEntityRenderer.State> {

    public MixedSlabBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    // ── Render state ────────────────────────────────────────────────────────

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

    // ── Submit ──────────────────────────────────────────────────────────────

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraRenderState) {
        bitsandbalance$submit(state, poseStack, collector);
    }

    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState) {
        bitsandbalance$submit(state, poseStack, collector);
    }

    public void submit(BlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState) {
        bitsandbalance$submit((State) state, poseStack, collector);
    }

    private void bitsandbalance$submit(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.blockPos == null || state.level == null) return;

        // Render bottom slab half
        if (state.bottomSlabState != null) {
            submitSlabHalf(collector, poseStack, state.level, state.blockPos, state.bottomSlabState, state.lightCoords);
        }

        // Render top slab half
        if (state.topSlabState != null) {
            submitSlabHalf(collector, poseStack, state.level, state.blockPos, state.topSlabState, state.lightCoords);
        }
    }

    /**
     * Renders a single slab half at the block position using the vanilla block model
     * pipeline. The slab models already have correct geometry for their half (bottom
     * slab occupies Y 0-8, top slab occupies Y 8-16), so no translation is needed.
     */
    private static void submitSlabHalf(SubmitNodeCollector collector, PoseStack poseStack,
                                       Object level, net.minecraft.core.BlockPos blockPos,
                                       BlockState slabState, int packedLight) {
        List<?> parts = FabricRuntimeModelCompat.collectRuntimeModelParts(slabState, blockPos);
        if (parts == null || parts.isEmpty()) {
            return;
        }
        RenderType renderType = MovingBlockRenderTypeCompat.get(slabState, parts);

        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            EnhancedSlabRenderOffsetContext.pushBlockEntitySubmit();
            BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
            try {
                ReflectiveLitBlockModelTesselationBridge.render(
                        pose,
                        consumer,
                        level,
                        blockPos,
                        slabState,
                        parts,
                        packedLight
                );
            } finally {
                BlockColorRenderContext.clear();
                EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            }
        });
    }

    // ── State class ─────────────────────────────────────────────────────────

    public static final class State extends BlockEntityRenderState {
        public BlockState bottomSlabState;
        public BlockState topSlabState;
        public Level level;
    }
}
