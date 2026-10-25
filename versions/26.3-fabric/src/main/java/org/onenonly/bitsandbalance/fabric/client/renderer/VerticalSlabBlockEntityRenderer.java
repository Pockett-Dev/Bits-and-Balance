package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.joml.Vector3fc;
import org.joml.Vector3f;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;

import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior ??? Vertical Slab Block Entity Renderer (Fabric)
 */
public class VerticalSlabBlockEntityRenderer
        implements BlockEntityRenderer<VerticalSlabBlockEntity, VerticalSlabBlockEntityRenderer.State> {
    private static final int ALL_DIRECTIONS = FabricRuntimeModelCompat.allDirectionMask();

    public VerticalSlabBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
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
    public void extractRenderState(VerticalSlabBlockEntity blockEntity, State state, float partialTick,
                                   net.minecraft.world.phys.Vec3 cameraPos,
                                   net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakOverlay);
        state.level = blockEntity.getLevel();

        BlockState beState = blockEntity.getBlockState();
        state.ownerState = beState;
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
                       CameraRenderState cameraRenderState) {
        if (EnhancedSlabTerrainRenderHelper.shouldSkipFixedBlockEntityRender(state.ownerState)) return;
        if (EnhancedSlabTerrainRenderHelper.shouldSkipBlockEntityRender(state.level, state.blockPos, state.ownerState)) return;
        if (!EnhancedSlabBlockEntityRenderLimits.shouldSubmit(state, cameraRenderState)) return;
        bitsandbalance$submit(state, poseStack, collector);
    }

    private void bitsandbalance$submit(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.blockPos == null || state.level == null) return;
        EnhancedSlabRenderDiagnostics.verticalSlabFallback();
        boolean hasFacing = state.facingSlabState != null;
        boolean hasOpposite = state.isDouble && state.oppositeSlabState != null;

        if (hasFacing && hasOpposite) {
            BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(
                    state.facingSlabState,
                    state.oppositeSlabState
            );
            if (fullBlockState != null) {
                submitFullBlock(collector, poseStack, state.level, state.blockPos, fullBlockState, state.lightCoords);
                return;
            }
        }

        if (hasFacing) {
            int visibleMask = hasOpposite ? bitsandbalance$without(ALL_DIRECTIONS, state.facing.getOpposite()) : ALL_DIRECTIONS;
            submitVerticalHalf(collector, poseStack, state.level, state.blockPos,
                    state.facingSlabState, state.facing, state.lightCoords, visibleMask);
        }

        if (hasOpposite) {
            int visibleMask = hasFacing ? bitsandbalance$without(ALL_DIRECTIONS, state.facing) : ALL_DIRECTIONS;
            submitVerticalHalf(collector, poseStack, state.level, state.blockPos,
                    state.oppositeSlabState, state.facing.getOpposite(), state.lightCoords, visibleMask);
        }
    }

    private static void submitFullBlock(SubmitNodeCollector collector, PoseStack poseStack,
                                        Object level, net.minecraft.core.BlockPos blockPos,
                                        BlockState fullBlockState, int packedLight) {
        Object prismPart = FabricRuntimeModelCompat.buildExactCuboidPart(
                fullBlockState,
                blockPos,
                new Vector3f(0.0F, 0.0F, 0.0F),
                new Vector3f(16.0F, 16.0F, 16.0F),
                ALL_DIRECTIONS
        );
        if (!(prismPart instanceof BlockStateModelPart part)) return;
        RenderType renderType = MovingBlockRenderTypeCompat.get(fullBlockState, part);
        List<BlockStateModelPart> parts = FabricRuntimeModelCompat.singletonPartList(part);

        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            EnhancedSlabRenderOffsetContext.pushBlockEntitySubmit();
            BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
            try {
                ReflectiveLitBlockModelTesselationBridge.render(pose, consumer, level, blockPos, fullBlockState, parts, packedLight);
            } finally {
                BlockColorRenderContext.clear();
                EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            }
        });
    }

    public static void submitVerticalHalf(SubmitNodeCollector collector, PoseStack poseStack,
                                          Object level, net.minecraft.core.BlockPos blockPos,
                                          BlockState slabState, Direction half, int packedLight) {
        submitVerticalHalf(collector, poseStack, level, blockPos, slabState, half, packedLight, ALL_DIRECTIONS);
    }

    private static void submitVerticalHalf(SubmitNodeCollector collector, PoseStack poseStack,
                                           Object level, net.minecraft.core.BlockPos blockPos,
                                           BlockState slabState, Direction half, int packedLight, int visibleMask) {
        // Build a vertical-slab prism using the slab model's sprites per face.
        // Rotating the slab model's baked quads moves TOP onto a side, which looks wrong for many textures.
        // This mirrors the item-model fix in ItemModelResolverVerticalSlabItemMixin.
        Object prismPart = buildVerticalSlabPrismPart(level, blockPos, slabState, half);
        if (prismPart == null) return;
        BlockStateModelPart part = FabricRuntimeModelCompat.filterDirections((BlockStateModelPart) prismPart, visibleMask);
        RenderType renderType = MovingBlockRenderTypeCompat.get(slabState, part);
        List<BlockStateModelPart> parts = FabricRuntimeModelCompat.singletonPartList(part);

        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            // This geometry runs later (outside BlockEntityRenderDispatcher.submit), so we must
            // re-enter the block-entity submit context to avoid double-applying slab render offsets.
            EnhancedSlabRenderOffsetContext.pushBlockEntitySubmit();
            BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
            try {
                ReflectiveLitBlockModelTesselationBridge.render(pose, consumer, level, blockPos, slabState, parts, packedLight);
            } finally {
                BlockColorRenderContext.clear();
                EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            }
        });
    }

    public static void renderTerrainHalf(Object dispatcher,
                                         BlockAndTintGetter level,
                                         net.minecraft.core.BlockPos blockPos,
                                         BlockState slabState,
                                         Direction half,
                                         PoseStack poseStack,
                                         VertexConsumer consumer) {
        Object prismPart = buildVerticalSlabPrismPart(level, blockPos, slabState, half);
        if (!(prismPart instanceof BlockStateModelPart part)) return;

        BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
        try {
            EnhancedSlabTerrainRenderHelper.renderNested(dispatcher, slabState, blockPos, level, poseStack, consumer, java.util.Collections.singletonList(part));
        } finally {
            BlockColorRenderContext.clear();
        }
    }

    public static BlockStateModelPart buildTerrainPart(BlockState slabState, net.minecraft.core.BlockPos blockPos, Direction half) {
        Object prismPart = buildVerticalSlabPrismPart(null, blockPos, slabState, half);
        return prismPart instanceof BlockStateModelPart part ? part : null;
    }

    private static Object buildVerticalSlabPrismPart(Object level, net.minecraft.core.BlockPos pos, BlockState slabState, Direction half) {
        if (half == null || half.getAxis() == Direction.Axis.Y) return null;
        return FabricRuntimeModelCompat.buildVerticalSlabPart(slabState, pos, half);
    }

    private static int bitsandbalance$without(int mask, Direction direction) {
        return mask & ~(1 << direction.ordinal());
    }

    public static final class State extends BlockEntityRenderState {
        public BlockState facingSlabState;
        public BlockState oppositeSlabState;
        public BlockState ownerState;
        public Level level;
        public Direction facing;
        public boolean isDouble;
    }
}
