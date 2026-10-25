package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.CandleBundleBlockEntity;
import org.onenonly.bitsandbalance.common.candle.CandleBundleContentsClientCache;
import org.onenonly.bitsandbalance.common.candle.CandleBundleFacingClientCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public class CandleBundleBlockEntityRenderer implements BlockEntityRenderer<CandleBundleBlockEntity> {

    private final BlockRenderDispatcher blockRenderDispatcher;

    public CandleBundleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(CandleBundleBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }

        BlockState containerState = blockEntity.getBlockState();
        int candleCount = 1;
        if (containerState.hasProperty(CandleBlock.CANDLES)) {
            candleCount = Math.min(Math.max(containerState.getValue(CandleBlock.CANDLES), 1), 4);
        }

        boolean lit = containerState.hasProperty(AbstractCandleBlock.LIT) && containerState.getValue(AbstractCandleBlock.LIT);
        boolean waterlogged = containerState.hasProperty(CandleBlock.WATERLOGGED) && containerState.getValue(CandleBlock.WATERLOGGED);
        Direction facing = blockEntity.getFacing();
        if (facing == Direction.SOUTH && candleCount > 1) {
            facing = CandleBundleFacingClientCache.get(blockEntity.getBlockPos().asLong());
        }

        for (int index = 0; index < candleCount; index++) {
            ItemStack stack = blockEntity.getCandle(index);
            if (stack == null || stack.isEmpty()) {
                stack = CandleBundleContentsClientCache.get(blockEntity.getBlockPos().asLong(), index);
            }

            BlockState candleState = candleBlockFromStack(stack).defaultBlockState();
            if (candleState.hasProperty(CandleBlock.CANDLES)) {
                candleState = candleState.setValue(CandleBlock.CANDLES, candleCount);
            }
            if (candleState.hasProperty(AbstractCandleBlock.LIT)) {
                candleState = candleState.setValue(AbstractCandleBlock.LIT, lit);
            }
            if (candleState.hasProperty(CandleBlock.WATERLOGGED)) {
                candleState = candleState.setValue(CandleBlock.WATERLOGGED, waterlogged);
            }

            BakedModel slotModel = SlotFilteredBakedModel.create(this.blockRenderDispatcher.getBlockModel(candleState), candleState, index, candleCount);
            if (slotModel == null) {
                continue;
            }

            poseStack.pushPose();
            bitsandbalance$applyFacingRotation(poseStack, facing);
            this.blockRenderDispatcher.getModelRenderer().tesselateBlock(
                    blockEntity.getLevel(),
                    slotModel,
                    candleState,
                    blockEntity.getBlockPos(),
                    poseStack,
                    buffer.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(candleState)),
                    false,
                    bitsandbalance$seededRandom(),
                    candleState.getSeed(blockEntity.getBlockPos()),
                    packedOverlay
            );
            poseStack.popPose();
        }
    }

    private static void bitsandbalance$applyFacingRotation(PoseStack poseStack, Direction facing) {
        float degrees = switch (facing) {
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
        if (degrees == 0.0F) {
            return;
        }

        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
    }

    private static RandomSource bitsandbalance$seededRandom() {
        RandomSource random = RandomSource.create();
        random.setSeed(42L);
        return random;
    }

    private static Block candleBlockFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Blocks.CANDLE;
        }
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem) {
            return blockItem.getBlock();
        }
        return Blocks.CANDLE;
    }

    private static final class SlotFilteredBakedModel implements BakedModel {
        private static final float[][][] VANILLA_CENTERS_XZ = new float[][][] {
                { { 0.5f, 0.5f } },
                { { 0.375f, 0.5f }, { 0.625f, 0.4375f } },
                { { 0.5f, 0.625f }, { 0.375f, 0.5f }, { 0.5625f, 0.4375f } },
                { { 0.4375f, 0.5625f }, { 0.625f, 0.5625f }, { 0.375f, 0.375f }, { 0.5625f, 0.375f } }
        };

        private final BakedModel sourceModel;
        private final List<BakedQuad> allQuads;
        private final EnumMap<Direction, List<BakedQuad>> byDirection;
        private final TextureAtlasSprite particleIcon;

        private SlotFilteredBakedModel(BakedModel sourceModel, List<BakedQuad> allQuads, EnumMap<Direction, List<BakedQuad>> byDirection) {
            this.sourceModel = sourceModel;
            this.allQuads = allQuads;
            this.byDirection = byDirection;
            this.particleIcon = sourceModel.getParticleIcon();
        }

        private static BakedModel create(BakedModel sourceModel, BlockState state, int slotIndex, int candleCount) {
            candleCount = Math.min(Math.max(candleCount, 1), 4);
            if (slotIndex < 0 || slotIndex >= candleCount) {
                return null;
            }

            EnumMap<Direction, List<BakedQuad>> byDirection = new EnumMap<>(Direction.class);
            List<BakedQuad> allQuads = filterSlot(sourceModel.getQuads(state, null, bitsandbalance$seededRandom()), slotIndex, candleCount);

            for (Direction direction : Direction.values()) {
                List<BakedQuad> filtered = filterSlot(sourceModel.getQuads(state, direction, bitsandbalance$seededRandom()), slotIndex, candleCount);
                if (!filtered.isEmpty()) {
                    byDirection.put(direction, filtered);
                }
            }

            if (allQuads.isEmpty() && byDirection.isEmpty()) {
                return null;
            }

            return new SlotFilteredBakedModel(sourceModel, allQuads, byDirection);
        }

        private static List<BakedQuad> filterSlot(List<BakedQuad> quads, int slotIndex, int candleCount) {
            if (quads == null || quads.isEmpty()) {
                return List.of();
            }
            if (candleCount <= 1) {
                return slotIndex == 0 ? quads : List.of();
            }

            ArrayList<BakedQuad> filtered = null;
            for (BakedQuad quad : quads) {
                if (classifyVanillaCandleSlot(quad, candleCount) != slotIndex) {
                    continue;
                }
                if (filtered == null) {
                    filtered = new ArrayList<>();
                }
                filtered.add(quad);
            }
            return filtered != null ? List.copyOf(filtered) : List.of();
        }

        private static int classifyVanillaCandleSlot(BakedQuad quad, int candleCount) {
            int[] vertices = quad.getVertices();
            if (vertices == null || vertices.length < 12) {
                return 0;
            }

            int stride = vertices.length / 4;
            float centerX = 0.0F;
            float centerZ = 0.0F;
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * stride;
                centerX += Float.intBitsToFloat(vertices[base]);
                centerZ += Float.intBitsToFloat(vertices[base + 2]);
            }
            centerX *= 0.25F;
            centerZ *= 0.25F;

            float[][] centers = VANILLA_CENTERS_XZ[candleCount - 1];
            int best = 0;
            float bestDistance = Float.MAX_VALUE;
            for (int index = 0; index < centers.length; index++) {
                float dx = centerX - centers[index][0];
                float dz = centerZ - centers[index][1];
                float distance = dx * dx + dz * dz;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = index;
                }
            }
            return best;
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            if (side == null) {
                return this.allQuads;
            }
            return this.byDirection.getOrDefault(side, Collections.emptyList());
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.sourceModel.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return this.sourceModel.isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return this.sourceModel.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer() {
            return this.sourceModel.isCustomRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return this.particleIcon;
        }

        @Override
        public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() {
            return this.sourceModel.getTransforms();
        }

        @Override
        public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() {
            return this.sourceModel.getOverrides();
        }
    }
}
