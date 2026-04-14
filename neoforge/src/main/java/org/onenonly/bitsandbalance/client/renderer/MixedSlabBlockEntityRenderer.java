package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;

import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior — Mixed Double Slab Block Entity Renderer (NeoForge)
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
    }

    // ── Submit ──────────────────────────────────────────────────────────────

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraRenderState) {
        if (state.blockPos == null || state.level == null) return;

        // Render bottom slab half
        if (state.bottomSlabState != null) {
            submitSlabHalf(collector, poseStack, state.level, state.blockPos, state.bottomSlabState);
        }

        // Render top slab half
        if (state.topSlabState != null) {
            submitSlabHalf(collector, poseStack, state.level, state.blockPos, state.topSlabState);
        }
    }

    /**
     * Renders a single slab half at the block position using the vanilla block model
     * pipeline. The slab models already have correct geometry for their half (bottom
     * slab occupies Y 0-8, top slab occupies Y 8-16), so no translation is needed.
     */
    private static void submitSlabHalf(SubmitNodeCollector collector, PoseStack poseStack,
                                       Level level, net.minecraft.core.BlockPos blockPos,
                                       BlockState slabState) {
        RenderType renderType = switch (ItemBlockRenderTypes.getChunkRenderType(slabState)) {
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
            case TRIPWIRE -> RenderTypes.tripwireMovingBlock();
            default -> RenderTypes.solidMovingBlock();
        };

        BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(slabState);
        long seed = blockPos.asLong();
        List<BlockModelPart> parts = model.collectParts(RandomSource.create(seed));

        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            PoseStack tmp = new PoseStack();
            tmp.last().pose().set(pose.pose());
            tmp.last().normal().set(pose.normal());

            BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
            try {
                Minecraft.getInstance().getBlockRenderer()
                        .renderBatched(slabState, blockPos, level, tmp, consumer, false, parts);
            } finally {
                BlockColorRenderContext.clear();
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
