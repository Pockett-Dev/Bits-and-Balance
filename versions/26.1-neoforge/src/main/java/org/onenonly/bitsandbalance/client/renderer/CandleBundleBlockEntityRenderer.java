package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.CandleBundleBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.TemporaryCloudBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.CloudBlock;
import org.onenonly.bitsandbalance.common.client.TemporaryCloudFadeRenderer;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;

import java.util.List;

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
        state.blockState = blockEntity.getBlockState();

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
        if (state.blockPos == null || state.level == null) {
            return;
        }

        for (int i = 0; i < state.candleCount; i++) {
            BlockState candleState = state.candleStates[i];
            if (candleState != null) {
                submitCandleModel(collector, poseStack, state.level, state.blockPos, candleState, state.facing, state.lightCoords);
            }
        }
    }

    private static void submitCandleModel(SubmitNodeCollector collector, PoseStack poseStack, Level level, BlockPos blockPos, BlockState candleState, Direction facing, int packedLight) {
        List<BlockStateModelPart> parts = RuntimeModelCompat.collectRuntimeModelParts(candleState, blockPos);
        if (parts == null || parts.isEmpty()) {
            return;
        }

        var renderType = MovingBlockRenderTypeCompat.get(candleState, parts);
        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            PoseStack tmp = new PoseStack();
            tmp.last().pose().set(pose.pose());
            tmp.last().normal().set(pose.normal());
            applyFacingNudge(tmp, facing);

            EnhancedSlabRenderOffsetContext.pushBlockEntitySubmit();
            try {
                LitBlockModelTesselationBridge.render(tmp.last(), consumer, level, blockPos, candleState, parts, packedLight);
            } finally {
                EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            }
        });
    }

    private static void applyFacingNudge(PoseStack poseStack, Direction facing) {
        if (facing != null && facing.getAxis() == Direction.Axis.X) {
            poseStack.translate(facing.getStepX() * (1.0F / 16.0F), 0.0F, 0.0F);
        }
    }

    public static final class State extends BlockEntityRenderState {
        public final BlockState[] candleStates = new BlockState[] { null, null, null, null };
        public int candleCount = 1;
        public Level level;
        public BlockState blockState;
        public Direction facing = Direction.SOUTH;
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

    public static void renderTemporaryClouds(float partialTick) {
        TemporaryCloudFadeRenderer.renderTrackedClouds(partialTick);
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
                state.renderState = blockState.setValue(CloudBlock.TEMPORARY, Boolean.TRUE);
            } else {
                state.renderState = null;
            }
        }

        @Override
        public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
            // 26.1 block-entity render states can retain stale alpha snapshots.
            // Temporary clouds are drawn from the live tracked-position pass instead.
        }

        public static final class State extends BlockEntityRenderState {
            public Level level;
            public BlockState renderState;
            public int alpha;
            public int sampledLight;
        }
    }
}
