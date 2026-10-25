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
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior - Quad Vertical Step Block Entity Renderer (Fabric)
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
        return EnhancedSlabTerrainRenderHelper.quadVerticalStepBlockEntityViewDistance();
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
        state.level = blockEntity.getLevel();
        state.ownerState = blockEntity.getBlockState();
        if (!EnhancedSlabBlockEntityRenderLimits.shouldSubmit(state.blockPos, cameraPos)) {
            clearSlabs(state.slabs);
            return;
        }
        if (EnhancedSlabTerrainRenderHelper.shouldSkipBlockEntityRender(state.level, state.blockPos, state.ownerState)) {
            clearSlabs(state.slabs);
            return;
        }

        BlockState[] slabs = blockEntity.getAllSlabs();
        System.arraycopy(slabs, 0, state.slabs, 0, Math.min(slabs.length, state.slabs.length));
        for (int i = slabs.length; i < state.slabs.length; i++) {
            state.slabs[i] = null;
        }
        state.lightCoords = VirtualRenderWorldLightBridge.capturePackedLight(state.level, state.lightCoords);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraRenderState) {
        if (state.blockPos == null || state.level == null) return;
        if (EnhancedSlabTerrainRenderHelper.shouldSkipBlockEntityRender(state.level, state.blockPos, state.ownerState)) return;
        if (!EnhancedSlabBlockEntityRenderLimits.shouldSubmit(state, cameraRenderState)) return;

        GroupAccumulator groups = new GroupAccumulator(4);

        BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(state.slabs);
        if (fullBlockState != null) {
            net.minecraft.client.renderer.block.model.BlockModelPart part = StepBlockEntityRenderer.buildExactPrismPart(
                    state.level,
                    state.blockPos,
                    fullBlockState,
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Vector3f(16.0F, 16.0F, 16.0F),
                    EnhancedSlabRenderModelCompat.allDirectionMask()
            );
            if (part != null) {
                VerticalStepBlockEntityRenderer.submitQuadrantParts(
                        collector,
                        poseStack,
                        state.level,
                        state.blockPos,
                        fullBlockState,
                        EnhancedSlabRenderModelCompat.singletonPartList(part),
                        state.lightCoords
                );
            }
            return;
        }

        for (int i = 0; i < 4; i++) {
            BlockState slab = state.slabs[i];
            if (slab == null) continue;
            Direction facing = (i == 2 || i == 3) ? Direction.SOUTH : Direction.NORTH;
            Direction side   = (i == 1 || i == 3) ? Direction.EAST  : Direction.WEST;
            int visibleMask = bitsandbalance$visibleMask(i, state.slabs);
            net.minecraft.client.renderer.block.model.BlockModelPart part = VerticalStepBlockEntityRenderer.buildQuadrantPart(
                    slab,
                    state.blockPos,
                    facing,
                    side
            );
            if (part == null) {
                continue;
            }
            groups.add(
                    slab,
                    EnhancedSlabRenderModelCompat.filterDirections(part, visibleMask)
            );
        }

        for (int i = 0; i < groups.size; i++) {
            VerticalStepBlockEntityRenderer.submitQuadrantParts(
                    collector,
                    poseStack,
                    state.level,
                    state.blockPos,
                    groups.states[i],
                    EnhancedSlabRenderModelCompat.partList(groups.parts[i]),
                    state.lightCoords
            );
        }
    }

    private static void clearSlabs(BlockState[] slabs) {
        for (int i = 0; i < slabs.length; i++) {
            slabs[i] = null;
        }
    }

    private static int bitsandbalance$visibleMask(int index, BlockState[] slabs) {
        int mask = EnhancedSlabRenderModelCompat.allDirectionMask();
        int eastWestNeighbor = (index == 0 || index == 2) ? index + 1 : index - 1;
        if (slabs[eastWestNeighbor] != null) {
            mask = EnhancedSlabRenderModelCompat.without(mask, (index == 0 || index == 2) ? Direction.EAST : Direction.WEST);
        }
        int northSouthNeighbor = (index == 0 || index == 1) ? index + 2 : index - 2;
        if (slabs[northSouthNeighbor] != null) {
            mask = EnhancedSlabRenderModelCompat.without(mask, (index == 0 || index == 1) ? Direction.SOUTH : Direction.NORTH);
        }
        return mask;
    }

    public static final class State extends BlockEntityRenderState {
        public BlockState[] slabs = new BlockState[4];
        public BlockState ownerState;
        public Level level;
    }

    private static final class GroupAccumulator {
        private final BlockState[] states;
        private final ArrayList<net.minecraft.client.renderer.block.model.BlockModelPart>[] parts;
        private int size;

        @SuppressWarnings("unchecked")
        private GroupAccumulator(int capacity) {
            this.states = new BlockState[capacity];
            this.parts = (ArrayList<net.minecraft.client.renderer.block.model.BlockModelPart>[]) new ArrayList[capacity];
        }

        private void add(BlockState state, net.minecraft.client.renderer.block.model.BlockModelPart part) {
            if (state == null || part == null) {
                return;
            }
            for (int i = 0; i < size; i++) {
                if (states[i] == state || states[i].equals(state)) {
                    parts[i].add(part);
                    return;
                }
            }
            if (size >= states.length) {
                return;
            }
            states[size] = state;
            parts[size] = new ArrayList<>(states.length);
            parts[size].add(part);
            size++;
        }
    }
}
