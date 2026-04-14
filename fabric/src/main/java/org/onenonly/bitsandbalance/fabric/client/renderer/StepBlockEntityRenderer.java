package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quadrant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior — Step Block Entity Renderer (Fabric)
 *
 * Renders the slab model of a single-quadrant {@link StepBlock} using the source slab's
 * textures, clipped to the ¼-block prism shape (8 × 8 × 16).
 */
public class StepBlockEntityRenderer
        implements BlockEntityRenderer<StepBlockEntity, StepBlockEntityRenderer.State> {

    private static final ModelState NO_MODEL_TRANSFORM = BlockModelRotation.IDENTITY;
    private static final ModelBaker.PartCache PART_CACHE = new ModelBaker.PartCache() {
        @Override
        public Vector3fc vector(Vector3fc in) {
            return new Vector3f(in);
        }
    };

    public StepBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StepBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakOverlay);
        state.level = blockEntity.getLevel();
        state.lightCoords = VirtualRenderWorldLightBridge.capturePackedLight(state.level, state.lightCoords);

        BlockState bs = blockEntity.getBlockState();
        if (bs.getBlock() instanceof StepBlock) {
            state.facing = bs.getValue(StepBlock.FACING);
            state.top = bs.getValue(StepBlock.TOP);
        } else {
            state.facing = Direction.EAST;
            state.top = false;
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
            if (ownerState.getBlock() instanceof FixedStepBlock fixed) {
                inferred = fixed.getSourceSlab().defaultBlockState();
            } else {
                var mapped = StepDynamicRegistry.getSlabForStep(ownerState.getBlock());
                if (mapped != null) {
                    inferred = mapped.defaultBlockState();
                }
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
        if (state.blockPos == null || state.level == null || state.slabState == null) return;
        submitQuadrant(collector, poseStack, state.level, state.blockPos,
            state.slabState, state.facing, state.top, state.lightCoords);
    }

    // ── Shared helper used by QuadStepBlockEntityRenderer ────────────────────

    /**
     * Submits a single ¼-block quadrant for rendering using the source slab's textures.
     *
     * @param facing Direction.EAST/WEST/NORTH/SOUTH
     * @param top    {@code true} = top half (y 8–16), {@code false} = bottom (y 0–8)
     */
    public static void submitQuadrant(SubmitNodeCollector collector, PoseStack poseStack,
                                      BlockAndTintGetter level, BlockPos blockPos,
                                      BlockState slabState, Direction facing, boolean top, int packedLight) {
        float xMin;
        float xMax;
        float zMin;
        float zMax;

        switch (facing) {
            case EAST -> {
                xMin = 8f;
                xMax = 16f;
                zMin = 0f;
                zMax = 16f;
            }
            case WEST -> {
                xMin = 0f;
                xMax = 8f;
                zMin = 0f;
                zMax = 16f;
            }
            case SOUTH -> {
                xMin = 0f;
                xMax = 16f;
                zMin = 8f;
                zMax = 16f;
            }
            case NORTH -> {
                xMin = 0f;
                xMax = 16f;
                zMin = 0f;
                zMax = 8f;
            }
            default -> {
                xMin = 8f;
                xMax = 16f;
                zMin = 0f;
                zMax = 16f;
            }
        }
        float yMin = top ? 8f : 0f;
        float yMax = top ? 16f : 8f;

        Vector3fc from = new Vector3f(xMin, yMin, zMin);
        Vector3fc to   = new Vector3f(xMax, yMax, zMax);

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
                VirtualRenderWorldLightBridge.renderBatched(
                        slabState,
                        blockPos,
                        level,
                        tmp,
                        consumer,
                        false,
                        parts,
                        packedLight
                );
            } finally {
                BlockColorRenderContext.clear();
                EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            }
        });
    }

    // ── Prism geometry ────────────────────────────────────────────────────────

    private static @Nullable BlockModelPart buildStepPrism(BlockAndTintGetter level, BlockPos pos, BlockState slabState,
                                                           Vector3fc from, Vector3fc to) {
        FaceData[] faces = extractFaceData(level, pos, slabState);
        if (faces == null) return null;

        EnumMap<Direction, List<BakedQuad>> byDir = new EnumMap<>(Direction.class);
        ArrayList<BakedQuad> all = new ArrayList<>(6);

        for (Direction dir : Direction.values()) {
            BakedQuad q = bakeFaceQuadForPrism(dir, from, to, faces[dir.ordinal()]);
            if (q == null) continue;
            all.add(q);
            byDir.computeIfAbsent(dir, k -> new ArrayList<>()).add(q);
        }

        TextureAtlasSprite particle = faces[Direction.NORTH.ordinal()].sprite();

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

    // ── Face data extraction ──────────────────────────────────────────────────

    record FaceData(TextureAtlasSprite sprite, int tintIndex, boolean shade, int lightEmission) {
    }

    static @Nullable FaceData[] extractFaceData(BlockAndTintGetter level, BlockPos pos, BlockState slabState) {
        try {
            long seed = pos.asLong();
            BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(slabState);
            List<BlockModelPart> parts = model.collectParts(RandomSource.create(seed));
            if (parts == null || parts.isEmpty()) return null;

            EnumMap<Direction, FaceData> byDir = new EnumMap<>(Direction.class);
            FaceData any = null;

            for (BlockModelPart part : parts) {
                if (part == null) continue;

                try {
                    List<BakedQuad> allQuads = part.getQuads(null);
                    if (allQuads != null && !allQuads.isEmpty()) {
                        for (BakedQuad q : allQuads) {
                            if (q == null) continue;
                            Direction d = q.direction();
                            if (d == null || byDir.containsKey(d)) continue;
                            TextureAtlasSprite sprite = q.sprite();
                            if (sprite == null) continue;
                            FaceData fd = new FaceData(sprite, q.tintIndex(), q.shade(), q.lightEmission());
                            byDir.put(d, fd);
                            if (any == null) any = fd;
                        }
                    }
                } catch (Throwable ignored) {
                }

                for (Direction d : Direction.values()) {
                    if (byDir.containsKey(d)) continue;
                    List<BakedQuad> quads = part.getQuads(d);
                    if (quads == null || quads.isEmpty()) continue;
                    BakedQuad q = quads.getFirst();
                    if (q == null) continue;
                    TextureAtlasSprite sprite = q.sprite();
                    if (sprite == null) continue;
                    FaceData fd = new FaceData(sprite, q.tintIndex(), q.shade(), q.lightEmission());
                    byDir.put(d, fd);
                    if (any == null) any = fd;
                }
            }

            if (any == null) return null;

            FaceData finalAny = any;
            FaceData[] out = new FaceData[Direction.values().length];
            for (Direction d : Direction.values()) {
                out[d.ordinal()] = byDir.getOrDefault(d, finalAny);
            }
            return out;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static float[] defaultUV(Direction dir, Vector3fc from, Vector3fc to) {
        float fx = from.x(), fy = from.y(), fz = from.z();
        float tx = to.x(),   ty = to.y(),   tz = to.z();
        return switch (dir) {
            case NORTH -> new float[]{ 16 - tx, 16 - ty, 16 - fx, 16 - fy };
            case SOUTH -> new float[]{ fx,      16 - ty, tx,      16 - fy };
            case WEST  -> new float[]{ fz,      16 - ty, tz,      16 - fy };
            case EAST  -> new float[]{ 16 - tz, 16 - ty, 16 - fz, 16 - fy };
            case UP    -> new float[]{ fx, fz, tx, tz };
            case DOWN  -> new float[]{ fx, 16 - tz, tx, 16 - fz };
        };
    }

    static @Nullable BakedQuad bakeFaceQuadForPrism(Direction direction, Vector3fc from, Vector3fc to,
                                                     @Nullable FaceData face) {
        if (face == null || face.sprite() == null) return null;
        float[] uv = defaultUV(direction, from, to);
        BlockElementFace.UVs uvs = new BlockElementFace.UVs(uv[0], uv[1], uv[2], uv[3]);
        BlockElementFace elementFace = new BlockElementFace(null, face.tintIndex(), "#texture", uvs, Quadrant.R0);
        return FaceBakery.bakeQuad(
                PART_CACHE,
                from,
                to,
                elementFace,
                face.sprite(),
                direction,
                NO_MODEL_TRANSFORM,
                null,
                face.shade(),
                face.lightEmission()
        );
    }

    // ── Render state ──────────────────────────────────────────────────────────

    public static final class State extends BlockEntityRenderState {
        public @Nullable BlockState slabState;
        public @Nullable Level level;
        public Direction facing = Direction.EAST;
        public boolean top = false;
    }
}
