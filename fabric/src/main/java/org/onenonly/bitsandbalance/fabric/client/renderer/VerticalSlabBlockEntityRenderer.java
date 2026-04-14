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
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior — Vertical Slab Block Entity Renderer (Fabric)
 */
public class VerticalSlabBlockEntityRenderer
        implements BlockEntityRenderer<VerticalSlabBlockEntity, VerticalSlabBlockEntityRenderer.State> {

    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final ModelState NO_MODEL_TRANSFORM = BlockModelRotation.IDENTITY;
    private static final ModelBaker.PartCache PART_CACHE = new ModelBaker.PartCache() {
        @Override
        public Vector3fc vector(Vector3fc in) {
            return new Vector3f(in);
        }
    };

    public VerticalSlabBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
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
        state.lightCoords = VirtualRenderWorldLightBridge.capturePackedLight(state.level, state.lightCoords);

        BlockState beState = blockEntity.getBlockState();
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
        if (state.blockPos == null || state.level == null) return;

        if (state.facingSlabState != null) {
            submitVerticalHalf(collector, poseStack, state.level, state.blockPos,
                    state.facingSlabState, state.facing, state.lightCoords);
        }

        if (state.isDouble && state.oppositeSlabState != null) {
            submitVerticalHalf(collector, poseStack, state.level, state.blockPos,
                    state.oppositeSlabState, state.facing.getOpposite(), state.lightCoords);
        }
    }

    public static void submitVerticalHalf(SubmitNodeCollector collector, PoseStack poseStack,
                                          BlockAndTintGetter level, net.minecraft.core.BlockPos blockPos,
                                          BlockState slabState, Direction half, int packedLight) {
        RenderType renderType = switch (ItemBlockRenderTypes.getChunkRenderType(slabState)) {
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
            case TRIPWIRE -> RenderTypes.tripwireMovingBlock();
            default -> RenderTypes.solidMovingBlock();
        };

        // Build a vertical-slab prism using the slab model's sprites per face.
        // Rotating the slab model's baked quads moves TOP onto a side, which looks wrong for many textures.
        // This mirrors the item-model fix in ItemModelResolverVerticalSlabItemMixin.
        BlockModelPart prismPart = buildVerticalSlabPrismPart(level, blockPos, slabState, half);
        if (prismPart == null) return;
        List<BlockModelPart> parts = java.util.Collections.singletonList(prismPart);

        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            PoseStack tmp = new PoseStack();
            tmp.last().pose().set(pose.pose());
            tmp.last().normal().set(pose.normal());

            // This geometry runs later (outside BlockEntityRenderDispatcher.submit), so we must
            // re-enter the block-entity submit context to avoid double-applying slab render offsets.
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

    private record FaceData(TextureAtlasSprite sprite, int tintIndex, boolean shade, int lightEmission) {
    }

    private static BlockModelPart buildVerticalSlabPrismPart(BlockAndTintGetter level, net.minecraft.core.BlockPos pos, BlockState slabState, Direction half) {
        if (half == null || half.getAxis() == Direction.Axis.Y) return null;

        FaceData[] faces = extractFaceData(level, pos, slabState);
        if (faces == null) return null;

        // Per-facing geometry bounds in element-space (0-16).
        // Build the half-block prism flush with the appropriate side - no rotation required.
        Vector3fc from = switch (half) {
            case SOUTH -> new Vector3f(0, 0, 8);
            case NORTH -> new Vector3f(0, 0, 0);
            case EAST  -> new Vector3f(8, 0, 0);
            default    -> new Vector3f(0, 0, 0); // WEST
        };
        Vector3fc to = switch (half) {
            case SOUTH -> new Vector3f(16, 16, 16);
            case NORTH -> new Vector3f(16, 16, 8);
            case EAST  -> new Vector3f(16, 16, 16);
            default    -> new Vector3f(8, 16, 16); // WEST
        };

        EnumMap<Direction, List<BakedQuad>> byDir = new EnumMap<>(Direction.class);
        ArrayList<BakedQuad> all = new ArrayList<>(6);

        for (Direction faceDir : Direction.values()) {
            BakedQuad q = bakeFaceQuadForPrism(faceDir, from, to, faces[faceDir.ordinal()]);
            if (q == null) continue;
            all.add(q);
            byDir.computeIfAbsent(faceDir, k -> new ArrayList<>()).add(q);
        }

        TextureAtlasSprite particle = faces[Direction.NORTH.ordinal()].sprite;

        return new BlockModelPart() {
            @Override
            public List<BakedQuad> getQuads(Direction direction) {
                if (direction == null) return all;
                List<BakedQuad> l = byDir.get(direction);
                return l != null ? l : java.util.Collections.emptyList();
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

    private static FaceData[] extractFaceData(BlockAndTintGetter level, net.minecraft.core.BlockPos pos, BlockState slabState) {
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
                    if (d == null || byDir.containsKey(d)) continue;
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

            FaceData[] out = new FaceData[Direction.values().length];
            for (Direction d : Direction.values()) {
                out[d.ordinal()] = byDir.getOrDefault(d, any);
            }
            return out;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Computes default (auto) UV values for a face quad given the element-space box [from, to].
     * Mirrors Minecraft's built-in auto-UV derivation so textures align correctly per facing.
     */
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

    private static BakedQuad bakeFaceQuadForPrism(Direction direction, Vector3fc from, Vector3fc to, FaceData face) {
        if (face == null || face.sprite == null) return null;
        float[] uv = defaultUV(direction, from, to);
        BlockElementFace.UVs uvs = new BlockElementFace.UVs(uv[0], uv[1], uv[2], uv[3]);
        BlockElementFace elementFace = new BlockElementFace(null, face.tintIndex, "#texture", uvs, Quadrant.R0);
        return FaceBakery.bakeQuad(
                PART_CACHE,
                from,
                to,
                elementFace,
                face.sprite,
                direction,
                NO_MODEL_TRANSFORM,
                null,
                face.shade,
                face.lightEmission
        );
    }

    public static final class State extends BlockEntityRenderState {
        public BlockState facingSlabState;
        public BlockState oppositeSlabState;
        public Level level;
        public Direction facing;
        public boolean isDouble;
    }
}
