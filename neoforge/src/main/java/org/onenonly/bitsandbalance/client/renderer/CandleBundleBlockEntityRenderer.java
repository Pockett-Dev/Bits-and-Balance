package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.onenonly.bitsandbalance.common.blockentity.CandleBundleBlockEntity;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;

public class CandleBundleBlockEntityRenderer implements BlockEntityRenderer<CandleBundleBlockEntity, CandleBundleBlockEntityRenderer.State> {
    public CandleBundleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CandleBundleBlockEntity blockEntity, State state, float partialTick, net.minecraft.world.phys.Vec3 cameraPos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakOverlay);

        state.facing = blockEntity.getFacing();

        state.level = blockEntity.getLevel();
        if (state.level != null && state.blockPos != null) {
            state.biome = state.level.getBiome(state.blockPos);
        } else {
            state.biome = null;
        }

        BlockState containerState = state.blockState;
        int candles = 1;
        if (containerState != null && containerState.hasProperty(CandleBlock.CANDLES)) {
            candles = containerState.getValue(CandleBlock.CANDLES);
        }
        state.candleCount = Math.min(Math.max(candles, 1), 4);

        boolean lit = containerState != null && containerState.hasProperty(AbstractCandleBlock.LIT) && containerState.getValue(AbstractCandleBlock.LIT);
        boolean waterlogged = containerState != null && containerState.hasProperty(CandleBlock.WATERLOGGED) && containerState.getValue(CandleBlock.WATERLOGGED);

        for (int i = 0; i < state.candleStates.length; i++) {
            state.candleStates[i] = Blocks.CANDLE.defaultBlockState();
        }

        for (int i = 0; i < state.candleCount; i++) {
            ItemStack stack = blockEntity.getCandle(i);
            if (stack == null || stack.isEmpty()) {
                stack = org.onenonly.bitsandbalance.common.candle.CandleBundleContentsClientCache.get(blockEntity.getBlockPos().asLong(), i);
            }
            Block candleBlock = candleBlockFromStack(stack);
            BlockState candleState = candleBlock.defaultBlockState();

            if (candleState.hasProperty(CandleBlock.CANDLES)) {
                candleState = candleState.setValue(CandleBlock.CANDLES, state.candleCount);
            }
            if (candleState.hasProperty(AbstractCandleBlock.LIT)) {
                candleState = candleState.setValue(AbstractCandleBlock.LIT, lit);
            }
            if (candleState.hasProperty(CandleBlock.WATERLOGGED)) {
                candleState = candleState.setValue(CandleBlock.WATERLOGGED, waterlogged);
            }

            state.candleStates[i] = candleState;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        if (state.blockPos == null || state.blockState == null || state.level == null) return;

        // Rotate via quad-level vertex/normal transform instead of PoseStack so
        // each quad's face Direction is also rotated.  This makes the block
        // renderer apply correct directional face shading (shadows).
        int rotSteps = switch (state.facing) {
            case WEST  -> 1; // 90° CW from above
            case NORTH -> 2; // 180°
            case EAST  -> 3; // 270° CW
            default    -> 0; // SOUTH = model default, no rotation
        };

        for (int i = 0; i < state.candleCount; i++) {
            BlockState candleState = state.candleStates[i];
            if (candleState == null) continue;

            submitCandleSlotExactVanillaModel(collector, poseStack, state.level, state.blockPos, candleState, i, rotSteps, state.candleCount);
        }
    }

    private static void submitCandleSlotExactVanillaModel(SubmitNodeCollector collector, PoseStack poseStack, Level level, net.minecraft.core.BlockPos blockPos, BlockState multiCandleStateForSlot, int slotIndex, int rotSteps, int candleCount) {
        RenderType renderType = switch (ItemBlockRenderTypes.getChunkRenderType(multiCandleStateForSlot)) {
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
            case TRIPWIRE -> RenderTypes.tripwireMovingBlock();
            default -> RenderTypes.solidMovingBlock();
        };

        BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(multiCandleStateForSlot);
        long seed = blockPos.asLong();
        List<BlockModelPart> parts = model.collectParts(RandomSource.create(seed));
        List<BlockModelPart> filtered = new ArrayList<>(parts.size());
        for (BlockModelPart part : parts) {
            filtered.add(new TintFilteredBlockModelPart(part, slotIndex, rotSteps, candleCount));
        }

        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            PoseStack tmp = new PoseStack();
            tmp.last().pose().set(pose.pose());
            tmp.last().normal().set(pose.normal());

            // Enhanced Slabs can apply an additional render offset in BlockRenderDispatcher.renderBatched.
            // Since this geometry is executed later (outside BlockEntityRenderDispatcher.submit), we must
            // re-enter the "block entity submit" context to avoid double-applying the slab offset.
            EnhancedSlabRenderOffsetContext.pushBlockEntitySubmit();
            try {
                Minecraft.getInstance().getBlockRenderer().renderBatched(multiCandleStateForSlot, blockPos, level, tmp, consumer, true, filtered);
            } finally {
                EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            }
        });
    }

    private static final class TintFilteredBlockModelPart implements BlockModelPart {
        private final BlockModelPart delegate;
        private final int tintIndex;
        private final int rotSteps;
        private final int candleCount;

        private TintFilteredBlockModelPart(BlockModelPart delegate, int tintIndex, int rotSteps, int candleCount) {
            this.delegate = delegate;
            this.tintIndex = tintIndex;
            this.rotSteps = rotSteps & 3;
            this.candleCount = Math.min(Math.max(candleCount, 1), 4);
        }

        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            // Un-rotate the requested face so we pull quads from the correct
            // original face bucket, then rotate the quads themselves.
            Direction sourceDir = (direction != null && rotSteps != 0)
                    ? rotateDirCW(direction, 4 - rotSteps)
                    : direction;

            List<BakedQuad> quads = delegate.getQuads(sourceDir);
            if (quads.isEmpty()) return quads;

            ArrayList<BakedQuad> filtered = null;
            ArrayList<BakedQuad> fallback = null;
            boolean sawTintedQuad = false;
            for (BakedQuad quad : quads) {
                int quadTintIndex = quad.tintIndex();
                if (quadTintIndex >= 0) {
                    sawTintedQuad = true;
                }

                if (quadTintIndex == tintIndex) {
                    if (filtered == null) filtered = new ArrayList<>();
                    filtered.add(rotSteps != 0 ? rotateQuadY(quad, rotSteps) : quad);
                } else if (quadTintIndex < 0 && tintIndex == 0) {
                    // Vanilla candle models often use non-tinted quads only.
                    // Render those once via slot 0 so bundles don't disappear.
                    if (fallback == null) fallback = new ArrayList<>();
                    fallback.add(rotSteps != 0 ? rotateQuadY(quad, rotSteps) : quad);
                }
            }

            if (filtered != null) return filtered;

            // Vanilla candle models typically have *no* tinted quads at all. We still need to render
            // mixed vanilla candle types within one bundle *and* preserve vanilla per-candle heights.
            // If the model is untinted, split quads into candle slots by their XZ centroid.
            if (!sawTintedQuad) {
                if (tintIndex < 0 || tintIndex >= candleCount) return List.of();
                ArrayList<BakedQuad> bySlot = null;
                for (BakedQuad quad : quads) {
                    int slot = classifyVanillaCandleSlot(quad, candleCount);
                    if (slot != tintIndex) continue;
                    if (bySlot == null) bySlot = new ArrayList<>();
                    bySlot.add(rotSteps != 0 ? rotateQuadY(quad, rotSteps) : quad);
                }
                return bySlot != null ? bySlot : List.of();
            }

            if (fallback != null) return fallback;
            return List.of();
        }

        // SOUTH-facing candle centres (normalized 0..1), derived from vanilla candle geometry.
        // Indices match the element ordering in vanilla multi-candle models.
        private static final float[][][] VANILLA_CENTERS_XZ = new float[][][] {
                { { 0.5f, 0.5f } },
                { { 0.375f, 0.5f }, { 0.625f, 0.4375f } },
                { { 0.5f, 0.625f }, { 0.375f, 0.5f }, { 0.5625f, 0.4375f } },
                { { 0.4375f, 0.5625f }, { 0.625f, 0.5625f }, { 0.375f, 0.375f }, { 0.5625f, 0.375f } }
        };

        private static int classifyVanillaCandleSlot(BakedQuad quad, int candleCount) {
            candleCount = Math.min(Math.max(candleCount, 1), 4);
            float cx = 0f;
            float cz = 0f;
            for (int v = 0; v < 4; v++) {
                Vector3fc p = quad.position(v);
                cx += p.x();
                cz += p.z();
            }
            cx *= 0.25f;
            cz *= 0.25f;

            float[][] centers = VANILLA_CENTERS_XZ[candleCount - 1];
            int best = 0;
            float bestDist = Float.MAX_VALUE;
            for (int i = 0; i < centers.length; i++) {
                float dx = cx - centers[i][0];
                float dz = cz - centers[i][1];
                float d2 = dx * dx + dz * dz;
                if (d2 < bestDist) {
                    bestDist = d2;
                    best = i;
                }
            }
            return best;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return delegate.useAmbientOcclusion();
        }

        @Override
        public net.minecraft.client.renderer.texture.TextureAtlasSprite particleIcon() {
            return delegate.particleIcon();
        }

        /** Rotates a horizontal direction CW (from above) by {@code steps} × 90°. */
        private static Direction rotateDirCW(Direction dir, int steps) {
            if (!dir.getAxis().isHorizontal()) return dir;
            steps = steps & 3;
            for (int i = 0; i < steps; i++) dir = dir.getClockWise();
            return dir;
        }

        /**
         * Rotates a {@link BakedQuad} around the Y-axis at block center
         * (0.5, 0.5) by {@code steps} × 90° CW (from above).
         *
         * <p>Transforms vertex positions and the quad's {@link Direction}
         * so the block renderer computes correct directional face shading.</p>
         */
        private static BakedQuad rotateQuadY(BakedQuad quad, int steps) {
            steps = steps & 3;
            if (steps == 0) return quad;

            Vector3fc[] rp = new Vector3fc[4];
            for (int v = 0; v < 4; v++) {
                Vector3fc pos = quad.position(v);
                float x = pos.x(), z = pos.z();
                float rx, rz;
                switch (steps) {
                    case 1  -> { rx = 1.0f - z; rz = x;         }
                    case 2  -> { rx = 1.0f - x; rz = 1.0f - z;  }
                    case 3  -> { rx = z;         rz = 1.0f - x;  }
                    default -> { rx = x;         rz = z;          }
                }
                rp[v] = new Vector3f(rx, pos.y(), rz);
            }

            return new BakedQuad(
                    rp[0], rp[1], rp[2], rp[3],
                    quad.packedUV0(), quad.packedUV1(),
                    quad.packedUV2(), quad.packedUV3(),
                    quad.tintIndex(),
                    rotateDirCW(quad.direction(), steps),
                    quad.sprite(), quad.shade(),
                    quad.lightEmission()
            );
        }
    }

    public static final class State extends BlockEntityRenderState {
        public final BlockState[] candleStates = new BlockState[] { null, null, null, null };
        public int candleCount = 1;
        public Level level;
        public net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome;
        public Direction facing = Direction.SOUTH;
    }

    private static Block candleBlockFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Blocks.CANDLE;
        }
        Item item = stack.getItem();
        if (item instanceof BlockItem bi) {
            return bi.getBlock();
        }
        return Blocks.CANDLE;
    }

}
