package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quadrant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior - Step Block Entity Renderer (NeoForge)
 *
 * Renders the slab model of a single-quadrant {@link StepBlock} using the source slab's
 * textures, clipped to the 1/4-block prism shape (8 x 8 x 16).
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
    public int getViewDistance() {
        return 1024;
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
        if (EnhancedSlabTerrainRenderHelper.isEnabled()) return;
        if (state.blockPos == null || state.level == null || state.slabState == null) return;
        submitQuadrant(collector, poseStack, state.level, state.blockPos,
                state.slabState, state.facing, state.top);
    }

    // Shared helper used by QuadStepBlockEntityRenderer

    /**
     * Submits a single 1/4-block quadrant for rendering using the source slab's textures.
     *
     * @param facing Direction.EAST/WEST/NORTH/SOUTH
     * @param top    {@code true} = top half (y 8-16), {@code false} = bottom (y 0-8)
     */
    public static void submitQuadrant(SubmitNodeCollector collector, PoseStack poseStack,
                                      BlockAndTintGetter level, BlockPos blockPos,
                                      BlockState slabState, Direction facing, boolean top) {
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
                                             boolean top,
                                             PoseStack poseStack,
                                             VertexConsumer consumer) {
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
        Vector3fc to = new Vector3f(xMax, yMax, zMax);

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
        SourceGeometry geometry = extractSourceGeometry(level, pos, slabState);
        if (geometry == null || geometry.fallback() == null) return null;

        Vector3fc adjustedFrom = adjustedFrom(from, to, geometry.bounds());
        Vector3fc adjustedTo = adjustedTo(from, to, geometry.bounds());

        EnumMap<Direction, List<BakedQuad>> byDir = new EnumMap<>(Direction.class);
        ArrayList<BakedQuad> all = new ArrayList<>(12);

        for (Direction dir : Direction.values()) {
            List<FaceData> templates = geometry.faceData().get(dir);
            if (templates == null || templates.isEmpty()) {
                templates = Collections.singletonList(geometry.fallback());
            }
            for (FaceData template : templates) {
                List<BakedQuad> quads = bakeFaceQuadsForPrism(dir, adjustedFrom, adjustedTo, template);
                if (quads.isEmpty()) continue;
                all.addAll(quads);
                byDir.computeIfAbsent(dir, k -> new ArrayList<>()).addAll(quads);
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

    // Face data extraction

    record FaceData(TextureAtlasSprite sprite, int tintIndex, boolean shade, int lightEmission, @Nullable float[] uv) {
    }

    static @Nullable SourceGeometry extractSourceGeometry(BlockAndTintGetter level, BlockPos pos, BlockState slabState) {
        try {
            long seed = pos.asLong();
            BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(slabState);
            List<BlockModelPart> parts = model.collectParts(RandomSource.create(seed));
            if (parts == null || parts.isEmpty()) return null;

            EnumMap<Direction, List<FaceData>> byDir = new EnumMap<>(Direction.class);
            IdentityHashMap<BakedQuad, Boolean> seenQuads = new IdentityHashMap<>();
            SourceBounds bounds = new SourceBounds();
            FaceData any = null;

            for (BlockModelPart part : parts) {
                if (part == null) continue;

                try {
                    List<BakedQuad> allQuads = part.getQuads(null);
                    if (allQuads != null && !allQuads.isEmpty()) {
                        for (BakedQuad q : allQuads) {
                            if (q == null) continue;
                            Direction d = q.direction();
                            TextureAtlasSprite sprite = q.sprite();
                            if (sprite == null) continue;
                            FaceData fd = new FaceData(sprite, q.tintIndex(), q.shade(), q.lightEmission(), extractFaceUv(q));
                            if (d != null) {
                                byDir.computeIfAbsent(d, ignored -> new ArrayList<>()).add(fd);
                            }
                            if (any == null) any = fd;
                            addBounds(bounds, seenQuads, q);
                        }
                    }
                } catch (Throwable ignored) {
                }

                for (Direction d : Direction.values()) {
                    List<BakedQuad> quads = part.getQuads(d);
                    if (quads == null || quads.isEmpty()) continue;
                    for (BakedQuad q : quads) {
                        if (q == null) continue;
                        TextureAtlasSprite sprite = q.sprite();
                        if (sprite == null) continue;
                        FaceData fd = new FaceData(sprite, q.tintIndex(), q.shade(), q.lightEmission(), extractFaceUv(q));
                        byDir.computeIfAbsent(d, ignored -> new ArrayList<>()).add(fd);
                        if (any == null) any = fd;
                        addBounds(bounds, seenQuads, q);
                    }
                }
            }

            if (any == null) return null;
            return new SourceGeometry(byDir, any, bounds);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void addBounds(SourceBounds bounds, IdentityHashMap<BakedQuad, Boolean> seenQuads, BakedQuad quad) {
        if (quad == null || seenQuads.put(quad, Boolean.TRUE) != null) {
            return;
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            Vector3fc position = quad.position(vertex);
            bounds.include(position.x() * 16.0F, position.y() * 16.0F, position.z() * 16.0F);
        }
    }

    private static float[] extractFaceUv(BakedQuad quad) {
        if (quad == null || quad.sprite() == null) {
            return null;
        }

        TextureAtlasSprite sprite = quad.sprite();
        float uScale = sprite.getU1() - sprite.getU0();
        float vScale = sprite.getV1() - sprite.getV0();
        if (Math.abs(uScale) < 0.000001F || Math.abs(vScale) < 0.000001F) {
            return null;
        }

        float minU = Float.POSITIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;
        long[] packed = new long[]{quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3()};
        for (long value : packed) {
            float atlasU = Float.intBitsToFloat((int) (value >>> 32));
            float atlasV = Float.intBitsToFloat((int) value);
            float localU = 16.0F * (atlasU - sprite.getU0()) / uScale;
            float localV = 16.0F * (atlasV - sprite.getV0()) / vScale;
            minU = Math.min(minU, localU);
            minV = Math.min(minV, localV);
            maxU = Math.max(maxU, localU);
            maxV = Math.max(maxV, localV);
        }

        if (!Float.isFinite(minU) || !Float.isFinite(minV) || !Float.isFinite(maxU) || !Float.isFinite(maxV)) {
            return null;
        }
        if (!saneUv(minU) || !saneUv(minV) || !saneUv(maxU) || !saneUv(maxV)) {
            return null;
        }
        return new float[]{minU, minV, maxU, maxV};
    }

    private static boolean saneUv(float value) {
        return value >= -0.01F && value <= 16.01F;
    }

    static BlockState verticalPrismRenderState(BlockState slabState) {
        return SlabFamilyModelSourceSelector.verticalPrismRenderState(slabState);
    }

    static Vector3fc adjustedFrom(Vector3fc from, Vector3fc to, SourceBounds bounds) {
        float thickness = bounds.height();
        if (thickness <= 0.0F || Math.abs(thickness - 8.0F) < 0.001F) {
            return new Vector3f(from);
        }

        Vector3f adjusted = new Vector3f(from);
        float xSize = to.x() - from.x();
        float ySize = to.y() - from.y();
        float zSize = to.z() - from.z();

        if (isHalfBlock(ySize) && !isHalfBlock(xSize) && !isHalfBlock(zSize)) {
            return adjusted;
        }
        if (isHalfBlock(xSize)) {
            adjusted.x = alignStart(from.x(), thickness);
        }
        if (isHalfBlock(zSize)) {
            adjusted.z = alignStart(from.z(), thickness);
        }
        return adjusted;
    }

    static Vector3fc adjustedTo(Vector3fc from, Vector3fc to, SourceBounds bounds) {
        float thickness = bounds.height();
        if (thickness <= 0.0F || Math.abs(thickness - 8.0F) < 0.001F) {
            return new Vector3f(to);
        }

        Vector3f adjusted = new Vector3f(to);
        float xSize = to.x() - from.x();
        float ySize = to.y() - from.y();
        float zSize = to.z() - from.z();

        if (isHalfBlock(ySize) && !isHalfBlock(xSize) && !isHalfBlock(zSize)) {
            adjusted.y = from.y() + thickness;
            return adjusted;
        }
        if (isHalfBlock(xSize)) {
            adjusted.x = alignEnd(from.x(), thickness);
        }
        if (isHalfBlock(zSize)) {
            adjusted.z = alignEnd(from.z(), thickness);
        }
        return adjusted;
    }

    private static boolean isHalfBlock(float size) {
        return Math.abs(size - 8.0F) < 0.001F;
    }

    private static float alignStart(float from, float thickness) {
        return from > 0.0F ? 16.0F - thickness : from;
    }

    private static float alignEnd(float from, float thickness) {
        return from > 0.0F ? 16.0F : from + thickness;
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
        return bakeFaceQuadForPrism(direction, from, to, face, true);
    }

    static @Nullable BakedQuad bakeFaceQuadForPrism(Direction direction, Vector3fc from, Vector3fc to,
                                                     @Nullable FaceData face, boolean preserveSourceSideUv) {
        if (face == null || face.sprite() == null) return null;
        float[] uv = preserveSourceSideUv ? mergedFaceUv(direction, from, to, face.uv()) : defaultUV(direction, from, to);
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

    static List<BakedQuad> bakeFaceQuadsForPrism(Direction direction, Vector3fc from, Vector3fc to,
                                                  @Nullable FaceData face) {
        if (shouldRepeatSourceSideUv(direction, from, to, face)) {
            float midY = from.y() + 8.0F;
            if (midY < to.y() - 0.001F) {
                ArrayList<BakedQuad> quads = new ArrayList<>(2);
                BakedQuad lower = bakeFaceQuadForPrism(direction, from, new Vector3f(to.x(), midY, to.z()), face);
                BakedQuad upper = bakeFaceQuadForPrism(direction, new Vector3f(from.x(), midY, from.z()), to, face);
                if (lower != null) {
                    quads.add(lower);
                }
                if (upper != null) {
                    quads.add(upper);
                }
                return quads.isEmpty() ? Collections.emptyList() : quads;
            }
        }

        BakedQuad quad = bakeFaceQuadForPrism(direction, from, to, face);
        return quad == null ? Collections.emptyList() : Collections.singletonList(quad);
    }

    private static float[] mergedFaceUv(Direction direction, Vector3fc from, Vector3fc to, @Nullable float[] sourceUv) {
        float[] uv = defaultUV(direction, from, to);
        if (sourceUv == null || direction == null) {
            return uv;
        }
        if (direction.getAxis().isHorizontal() && shouldPreserveSourceSideUv(from, to)) {
            uv[1] = sourceUv[1];
            uv[3] = sourceUv[3];
        }
        return uv;
    }

    private static boolean shouldPreserveSourceSideUv(Vector3fc from, Vector3fc to) {
        return !isHalfBlock(to.y() - from.y()) && (isHalfBlock(to.x() - from.x()) || isHalfBlock(to.z() - from.z()));
    }

    private static boolean shouldRepeatSourceSideUv(Direction direction, Vector3fc from, Vector3fc to, @Nullable FaceData face) {
        return direction != null
                && direction.getAxis().isHorizontal()
                && face != null
                && face.uv() != null
                && isHalfBlock(face.uv()[3] - face.uv()[1])
                && shouldPreserveSourceSideUv(from, to);
    }

    // Render state

    public static final class State extends BlockEntityRenderState {
        public @Nullable BlockState slabState;
        public @Nullable Level level;
        public Direction facing = Direction.EAST;
        public boolean top = false;
    }

    record SourceGeometry(EnumMap<Direction, List<FaceData>> faceData,
                          FaceData fallback,
                          SourceBounds bounds) {
    }

    static final class SourceBounds {
        private float minY = Float.POSITIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;

        void include(float x, float y, float z) {
            this.minY = Math.min(this.minY, y);
            this.maxY = Math.max(this.maxY, y);
        }

        float height() {
            if (!Float.isFinite(this.minY) || !Float.isFinite(this.maxY)) {
                return 8.0F;
            }
            return Math.max(0.0F, this.maxY - this.minY);
        }
    }
}
