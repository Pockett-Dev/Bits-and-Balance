package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import org.onenonly.bitsandbalance.common.blockentity.TemporaryCloudBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.CloudBlock;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.common.client.CloudModelAlphaRenderContext;
import org.onenonly.bitsandbalance.common.client.TemporaryCloudFadeRenderer;
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
        state.lightCoords = VirtualRenderWorldLightBridge.capturePackedLight(state.level, state.lightCoords);
        state.containerState = blockEntity.getBlockState();
        if (state.level != null && state.blockPos != null) {
            state.biome = state.level.getBiome(state.blockPos);
        } else {
            state.biome = null;
        }

        BlockState containerState = state.containerState;
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
        if (state.blockPos == null || state.containerState == null || state.level == null) return;

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

            submitCandleSlotExactVanillaModel(collector, poseStack, state.level, state.blockPos, candleState, state.facing, i, rotSteps, state.candleCount, state.lightCoords);
        }
    }

    private static void submitCandleSlotExactVanillaModel(SubmitNodeCollector collector, PoseStack poseStack, Level level, net.minecraft.core.BlockPos blockPos, BlockState multiCandleStateForSlot, Direction facing, int slotIndex, int rotSteps, int candleCount, int packedLight) {
        List<?> parts = FabricRuntimeModelCompat.collectRuntimeModelParts(multiCandleStateForSlot, blockPos);
        if (parts == null || parts.isEmpty()) {
            return;
        }

        List<BlockStateModelPart> filtered = new ArrayList<>(parts.size());
        for (Object part : parts) {
            if (part instanceof BlockStateModelPart blockStateModelPart) {
                filtered.add(new TintFilteredBlockModelPart(blockStateModelPart, slotIndex, rotSteps, candleCount));
            }
        }
        if (filtered.isEmpty()) {
            return;
        }

        RenderType renderType = MovingBlockRenderTypeCompat.get(multiCandleStateForSlot, filtered);

        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            PoseStack adjusted = new PoseStack();
            adjusted.last().pose().set(pose.pose());
            adjusted.last().normal().set(pose.normal());
            bitsandbalance$applyFacingNudge(adjusted, facing);

            // Enhanced Slabs can apply an additional render offset in BlockRenderDispatcher.renderBatched.
            // Since this geometry is executed later (outside BlockEntityRenderDispatcher.submit), we must
            // re-enter the "block entity submit" context to avoid double-applying the slab offset.
            EnhancedSlabRenderOffsetContext.pushBlockEntitySubmit();
            BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
            try {
                ReflectiveLitBlockModelTesselationBridge.render(
                        adjusted.last(),
                        consumer,
                        level,
                        blockPos,
                        multiCandleStateForSlot,
                        filtered,
                        packedLight
                );
            } finally {
                BlockColorRenderContext.clear();
                EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            }
        });
    }

    private static void bitsandbalance$applyFacingNudge(PoseStack poseStack, Direction facing) {
        if (facing == null || facing.getAxis() != Direction.Axis.X) {
            return;
        }
        poseStack.translate(facing.getStepX() * (1.0F / 16.0F), 0.0F, 0.0F);
    }

    private static final class TintFilteredBlockModelPart implements BlockStateModelPart {
        private final BlockStateModelPart delegate;
        private final int slotIndex;
        private final int rotSteps;
        private final int candleCount;

        private TintFilteredBlockModelPart(BlockStateModelPart delegate, int slotIndex, int rotSteps, int candleCount) {
            this.delegate = delegate;
            this.slotIndex = slotIndex;
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

            if (candleCount <= 1) {
                if (slotIndex != 0) {
                    return List.of();
                }
                if (rotSteps == 0) {
                    return quads;
                }
                ArrayList<BakedQuad> rotated = new ArrayList<>(quads.size());
                for (BakedQuad quad : quads) {
                    rotated.add(rotateQuadY(quad, rotSteps));
                }
                return rotated;
            }

            ArrayList<BakedQuad> filtered = null;
            for (BakedQuad quad : quads) {
                int slot = classifyVanillaCandleSlot(quad, candleCount);
                if (slot != slotIndex) continue;
                if (filtered == null) filtered = new ArrayList<>();
                filtered.add(rotSteps != 0 ? rotateQuadY(quad, rotSteps) : quad);
            }

            return filtered != null ? filtered : List.of();
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
        public Material.Baked particleMaterial() {
            return delegate.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return delegate.materialFlags();
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
                    quad.packedUV(0), quad.packedUV(1),
                    quad.packedUV(2), quad.packedUV(3),
                    rotateDirCW(quad.direction(), steps),
                    quad.materialInfo()
            );
        }
    }

    public static final class State extends BlockEntityRenderState {
        public final BlockState[] candleStates = new BlockState[] { null, null, null, null };
        public int candleCount = 1;
        public BlockState containerState;
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

    public static void renderTemporaryClouds(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        net.minecraft.world.phys.Vec3 cameraPos = minecraft.getCameraEntity() != null ? minecraft.getCameraEntity().position() : net.minecraft.world.phys.Vec3.ZERO;
        net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        RenderType renderType = RenderTypes.translucentMovingBlock();
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        PoseStack poseStack = new PoseStack();

        for (BlockPos pos : TemporaryCloudBlockEntity.getTrackedPositions(minecraft.level)) {
            if (!(minecraft.level.getBlockEntity(pos) instanceof TemporaryCloudBlockEntity blockEntity)) {
                continue;
            }

            int alpha = blockEntity.getRenderAlpha(partialTick);
            if (alpha <= 0) {
                continue;
            }

            BlockState renderState = blockEntity.getBlockState();
            if (!renderState.hasProperty(CloudBlock.TEMPORARY) || !renderState.getValue(CloudBlock.TEMPORARY)) {
                continue;
            }
            renderState = renderState.setValue(CloudBlock.TEMPORARY, Boolean.FALSE);

            List<?> parts = FabricRuntimeModelCompat.collectRuntimeModelParts(renderState, pos);
            if (parts == null || parts.isEmpty()) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

            CloudModelAlphaRenderContext.push(alpha);
            try {
                VirtualRenderWorldLightBridge.renderBatched(
                        renderState,
                        pos,
                        minecraft.level,
                        poseStack,
                        consumer,
                        true,
                        parts,
                        0
                );
            } finally {
                CloudModelAlphaRenderContext.pop();
            }

            poseStack.popPose();
        }

        bufferSource.endBatch(renderType);
    }

    public static final class TemporaryCloudRenderer implements BlockEntityRenderer<TemporaryCloudBlockEntity, TemporaryCloudRenderer.State> {
        public TemporaryCloudRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        public void extractRenderState(TemporaryCloudBlockEntity blockEntity, State state, float partialTick, net.minecraft.world.phys.Vec3 cameraPos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakOverlay) {
            BlockEntityRenderState.extractBase(blockEntity, state, breakOverlay);
            state.level = blockEntity.getLevel();
            state.alpha = blockEntity.getRenderAlpha(partialTick);
            state.sampledLight = state.lightCoords;

            BlockState blockState = blockEntity.getBlockState();
            if (blockState != null && blockState.hasProperty(CloudBlock.TEMPORARY) && blockState.getValue(CloudBlock.TEMPORARY)) {
                state.renderState = blockState.setValue(CloudBlock.TEMPORARY, Boolean.FALSE);
                if (state.level != null) {
                    state.sampledLight = bitsandbalance$samplePackedLight(state.level, state.renderState, blockEntity.getBlockPos(), state.sampledLight);
                }
            } else {
                state.renderState = null;
            }
        }

        @Override
        public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
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
            if (state.alpha <= 0 || state.blockPos == null || state.level == null || state.renderState == null) {
                return;
            }

            TemporaryCloudFadeRenderer.queue(poseStack.last(), state.blockPos, state.alpha, state.sampledLight);
        }

        public static final class State extends BlockEntityRenderState {
            public Level level;
            public BlockState renderState;
            public int alpha;
            public int sampledLight;
        }

        private static int bitsandbalance$samplePackedLight(Object level, BlockState state, BlockPos pos, int fallbackPackedLight) {
            try {
                Class<?> levelRendererClass = Class.forName("net.minecraft.client.renderer.LevelRenderer");
                Class<?> brightnessGetterClass = null;
                for (Class<?> nested : levelRendererClass.getDeclaredClasses()) {
                    if (nested.getSimpleName().equals("BrightnessGetter")) {
                        brightnessGetterClass = nested;
                        break;
                    }
                }
                if (brightnessGetterClass == null) {
                    return fallbackPackedLight;
                }

                Object brightnessGetter = brightnessGetterClass.getField("DEFAULT").get(null);
                java.lang.reflect.Method method = levelRendererClass.getMethod("getLightColor", brightnessGetterClass, level.getClass().getInterfaces().length > 0 ? level.getClass().getInterfaces()[0] : level.getClass(), BlockState.class, BlockPos.class);
                Object result = method.invoke(null, brightnessGetter, level, state, pos);
                return result instanceof Integer light ? light : fallbackPackedLight;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return fallbackPackedLight;
            }
        }

        private static final class AlphaMultiplierVertexConsumer implements VertexConsumer {
            private final VertexConsumer delegate;
            private final float alphaMultiplier;

            private AlphaMultiplierVertexConsumer(VertexConsumer delegate, int alpha) {
                this.delegate = delegate;
                this.alphaMultiplier = Math.max(0.0F, Math.min(1.0F, alpha / 255.0F));
            }

            @Override
            public VertexConsumer addVertex(float x, float y, float z) {
                delegate.addVertex(x, y, z);
                return this;
            }

            @Override
            public VertexConsumer setColor(int red, int green, int blue, int alpha) {
                delegate.setColor(red, green, blue, scaleAlpha(alpha));
                return this;
            }

            @Override
            public VertexConsumer setColor(int argb) {
                int alpha = (argb >>> 24) & 0xFF;
                int rgb = argb & 0x00FFFFFF;
                delegate.setColor((scaleAlpha(alpha) << 24) | rgb);
                return this;
            }

            @Override
            public VertexConsumer setUv(float u, float v) {
                delegate.setUv(u, v);
                return this;
            }

            @Override
            public VertexConsumer setUv1(int u, int v) {
                delegate.setUv1(u, v);
                return this;
            }

            @Override
            public VertexConsumer setUv2(int u, int v) {
                delegate.setUv2(u, v);
                return this;
            }

            @Override
            public VertexConsumer setNormal(float x, float y, float z) {
                delegate.setNormal(x, y, z);
                return this;
            }

            @Override
            public VertexConsumer setLineWidth(float width) {
                delegate.setLineWidth(width);
                return this;
            }

            private int scaleAlpha(int alpha) {
                return Math.max(0, Math.min(255, Math.round(alpha * alphaMultiplier)));
            }
        }
    }

}
