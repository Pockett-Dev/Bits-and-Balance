package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tweaks: Enhanced Slab Behavior ??? Quad Vertical Step Block Entity Renderer (Fabric)
 *
 * Renders up to 4 quadrant vertical steps stored in a {@link QuadVerticalStepBlockEntity},
 * delegating per-quadrant geometry to {@link VerticalStepBlockEntityRenderer#submitQuadrant}.
 */
public class QuadVerticalStepBlockEntityRenderer
        implements BlockEntityRenderer<QuadVerticalStepBlockEntity, QuadVerticalStepBlockEntityRenderer.State> {

    public QuadVerticalStepBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
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
    public void extractRenderState(QuadVerticalStepBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakOverlay);
        BlockState[] slabs = blockEntity.getAllSlabs();
        System.arraycopy(slabs, 0, state.slabs, 0, Math.min(slabs.length, state.slabs.length));
        for (int i = slabs.length; i < state.slabs.length; i++) {
            state.slabs[i] = null;
        }
        state.level = blockEntity.getLevel();
        state.ownerState = blockEntity.getBlockState();
        state.lightCoords = VirtualRenderWorldLightBridge.capturePackedLight(state.level, state.lightCoords);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraRenderState) {
        if (EnhancedSlabTerrainRenderHelper.shouldSkipBlockEntityRender(state.level, state.blockPos, state.ownerState)) return;
        if (!EnhancedSlabBlockEntityRenderLimits.shouldSubmit(state, cameraRenderState)) return;
        bitsandbalance$submit(state, poseStack, collector);
    }

    private void bitsandbalance$submit(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.blockPos == null || state.level == null) return;
        Map<BlockState, List<BlockStateModelPart>> partsBySlab = new LinkedHashMap<>(4);

        BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(state.slabs);
        if (fullBlockState != null) {
            BlockStateModelPart part = bitsandbalance$buildFullBlockPart(fullBlockState, state.blockPos);
            if (part != null) {
                VerticalStepBlockEntityRenderer.submitQuadrantParts(collector, poseStack, state.level, state.blockPos,
                        fullBlockState, FabricRuntimeModelCompat.singletonPartList(part), state.lightCoords);
            }
            return;
        }

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
            BlockStateModelPart part = VerticalStepBlockEntityRenderer.buildQuadrantPart(slab, state.blockPos, facing, side);
            if (part == null) continue;
            partsBySlab.computeIfAbsent(slab, ignored -> new ArrayList<>(4)).add(part);
        }

        for (Map.Entry<BlockState, List<BlockStateModelPart>> entry : partsBySlab.entrySet()) {
            VerticalStepBlockEntityRenderer.submitQuadrantParts(collector, poseStack, state.level, state.blockPos,
                    entry.getKey(), FabricRuntimeModelCompat.partList(entry.getValue()), state.lightCoords);
        }
    }

    private static BlockStateModelPart bitsandbalance$buildFullBlockPart(BlockState fullBlockState, net.minecraft.core.BlockPos pos) {
        Object part = FabricRuntimeModelCompat.buildExactCuboidPart(
                fullBlockState,
                pos,
                new Vector3f(0.0F, 0.0F, 0.0F),
                new Vector3f(16.0F, 16.0F, 16.0F),
                FabricRuntimeModelCompat.allDirectionMask()
        );
        return part instanceof BlockStateModelPart blockStateModelPart ? blockStateModelPart : null;
    }

    public static final class State extends BlockEntityRenderState {
        public BlockState[] slabs = new BlockState[4];
        public BlockState ownerState;
        public Level level;
    }
}
