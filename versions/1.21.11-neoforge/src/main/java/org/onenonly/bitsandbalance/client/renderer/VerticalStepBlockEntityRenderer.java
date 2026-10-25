package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior - Vertical Step Block Entity Renderer (NeoForge)
 *
 * Renders the slab model of a single-quadrant {@link VerticalStepBlock} using the source
 * vertical-slab's textures, clipped to the vertical 1/4-block prism shape (8 x 16 x 8).
 */
public class VerticalStepBlockEntityRenderer
        implements BlockEntityRenderer<VerticalStepBlockEntity, VerticalStepBlockEntityRenderer.State> {

    public VerticalStepBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 1024;
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
            state.side   = bs.getValue(VerticalStepBlock.SIDE);
        } else {
            state.facing = Direction.NORTH;
            state.side   = Direction.WEST;
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
            net.minecraft.world.level.block.Block sourceVerticalSlab = null;
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
                       CameraRenderState cameraRenderState) {
        if (EnhancedSlabTerrainRenderHelper.isEnabled()) return;
        if (state.blockPos == null || state.level == null || state.slabState == null) return;
        submitQuadrant(collector, poseStack, state.level, state.blockPos,
                state.slabState, state.facing, state.side);
    }

    // Shared helper used by QuadVerticalStepBlockEntityRenderer

    /**
     * Submits a single vertical 1/4-block quadrant for rendering using the source slab's textures.
     *
     * @param facing Direction.NORTH (z 0-8) or Direction.SOUTH (z 8-16)
     * @param side   Direction.WEST  (x 0-8) or Direction.EAST  (x 8-16)
     */
    public static void submitQuadrant(SubmitNodeCollector collector, PoseStack poseStack,
                                      BlockAndTintGetter level, BlockPos blockPos,
                                      BlockState slabState, Direction facing, Direction side) {
        float xMin = side == Direction.EAST ? 8f : 0f;
        float xMax = side == Direction.EAST ? 16f : 8f;
        float zMin = facing == Direction.SOUTH ? 8f : 0f;
        float zMax = facing == Direction.SOUTH ? 16f : 8f;

        Vector3fc from = new Vector3f(xMin, 0f, zMin);
        Vector3fc to   = new Vector3f(xMax, 16f, zMax);

        RenderType renderType = switch (ItemBlockRenderTypes.getChunkRenderType(slabState)) {
            case CUTOUT      -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
            case TRIPWIRE    -> RenderTypes.tripwireMovingBlock();
            default          -> RenderTypes.solidMovingBlock();
        };

        BlockModelPart part = buildStepPrism(level, blockPos, slabState, from, to);
        if (part == null) return;
        List<BlockModelPart> parts = Collections.singletonList(part);

        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            PoseStack tmp = new PoseStack();
            tmp.last().pose().set(pose.pose());
            tmp.last().normal().set(pose.normal());

            EnhancedSlabRenderOffsetContext.pushBlockEntitySubmit();
            BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
            try {
                Minecraft.getInstance().getBlockRenderer()
                        .renderBatched(slabState, blockPos, level, tmp, consumer, false, parts);
            } finally {
                BlockColorRenderContext.clear();
                EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            }
        });
    }

    public static void renderTerrainQuadrant(BlockRenderDispatcher dispatcher,
                                             BlockAndTintGetter level,
                                             BlockPos blockPos,
                                             BlockState slabState,
                                             Direction facing,
                                             Direction side,
                                             PoseStack poseStack,
                                             VertexConsumer consumer) {
        float xMin = side == Direction.EAST ? 8f : 0f;
        float xMax = side == Direction.EAST ? 16f : 8f;
        float zMin = facing == Direction.SOUTH ? 8f : 0f;
        float zMax = facing == Direction.SOUTH ? 16f : 8f;

        Vector3fc from = new Vector3f(xMin, 0f, zMin);
        Vector3fc to = new Vector3f(xMax, 16f, zMax);

        BlockModelPart part = buildStepPrism(level, blockPos, slabState, from, to);
        if (part == null) return;

        BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
        try {
            EnhancedSlabTerrainRenderHelper.renderNested(dispatcher, slabState, blockPos, level, poseStack, consumer, Collections.singletonList(part));
        } finally {
            BlockColorRenderContext.clear();
        }
    }

    // Prism geometry

    private static @Nullable BlockModelPart buildStepPrism(BlockAndTintGetter level, BlockPos pos, BlockState slabState,
                                                           Vector3fc from, Vector3fc to) {
        slabState = StepBlockEntityRenderer.verticalPrismRenderState(slabState);
        StepBlockEntityRenderer.SourceGeometry geometry = StepBlockEntityRenderer.extractSourceGeometry(level, pos, slabState);
        if (geometry == null || geometry.fallback() == null) return null;

        EnumMap<Direction, List<BakedQuad>> byDir = new EnumMap<>(Direction.class);
        ArrayList<BakedQuad> all = new ArrayList<>(12);

        for (Direction dir : Direction.values()) {
            List<StepBlockEntityRenderer.FaceData> templates = geometry.faceData().get(dir);
            if (templates == null || templates.isEmpty()) {
                templates = Collections.singletonList(geometry.fallback());
            }
            for (StepBlockEntityRenderer.FaceData template : templates) {
                BakedQuad q = StepBlockEntityRenderer.bakeFaceQuadForPrism(dir, from, to, template, false);
                if (q == null) continue;
                all.add(q);
                byDir.computeIfAbsent(dir, k -> new ArrayList<>()).add(q);
            }
        }

        TextureAtlasSprite particle = geometry.fallback().sprite();

        return new BlockModelPart() {
            @Override
            public List<BakedQuad> getQuads(Direction direction) {
                if (direction == null) return all;
                List<BakedQuad> l = byDir.get(direction);
                return l != null ? l : Collections.emptyList();
            }

            @Override
            public boolean useAmbientOcclusion() {
                return true;
            }

            @Override
            public TextureAtlasSprite particleIcon() {
                return particle;
            }
        };
    }

    // Render state

    public static final class State extends BlockEntityRenderState {
        public @Nullable BlockState slabState;
        public @Nullable Level level;
        public Direction facing = Direction.NORTH;
        public Direction side   = Direction.WEST;
    }
}
