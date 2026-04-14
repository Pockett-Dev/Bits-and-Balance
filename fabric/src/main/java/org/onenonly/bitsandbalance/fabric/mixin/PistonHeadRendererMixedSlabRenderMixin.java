package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.client.renderer.StepBlockEntityRenderer;
import org.onenonly.bitsandbalance.fabric.client.renderer.VirtualRenderWorldLightBridge;
import org.onenonly.bitsandbalance.fabric.client.renderer.VerticalSlabBlockEntityRenderer;
import org.onenonly.bitsandbalance.fabric.client.renderer.VerticalStepBlockEntityRenderer;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonData;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonHooks;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonRenderCache;
import org.onenonly.bitsandbalance.common.mechanics.MixedSlabPistonRenderCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(net.minecraft.client.renderer.blockentity.PistonHeadRenderer.class)
public class PistonHeadRendererMixedSlabRenderMixin {

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitMovingBlock(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/block/MovingBlockRenderState;)V",
            shift = At.Shift.AFTER
        ),
        require = 0
    )
    private void bitsandbalance$submitMovingMixedSlab(PistonHeadRenderState renderState,
                                                     PoseStack poseStack,
                                                     SubmitNodeCollector collector,
                                                     net.minecraft.client.renderer.state.CameraRenderState cameraRenderState,
                                                     CallbackInfo ci) {
        if (renderState == null || collector == null || poseStack == null) return;

        MovingBlockRenderState moving = renderState.block;
        if (moving == null) return;

        BlockState movedState = moving.blockState;
        if (movedState == null) return;

        if (renderState.blockPos == null || moving.blockPos == null) return;

        long destLong = renderState.blockPos.asLong();
        long sourceLong = moving.blockPos.asLong();

        BlockAndTintGetter levelView = moving;
        net.minecraft.core.BlockPos seedPos = moving.randomSeedPos != null ? moving.randomSeedPos : moving.blockPos;
        net.minecraft.core.BlockPos renderPos = moving.blockPos;

        if (movedState.getBlock() instanceof MixedSlabBlock) {
            MixedSlabPistonRenderCache.Entry entry = MixedSlabPistonRenderCache.peekForMove(destLong, sourceLong);
            if (entry == null) return;

            if (entry.bottomSlab() != null) {
                submitSlabHalf(collector, poseStack, levelView, seedPos, renderPos, entry.bottomSlab(), renderState.lightCoords);
            }
            if (entry.topSlab() != null) {
                submitSlabHalf(collector, poseStack, levelView, seedPos, renderPos, entry.topSlab(), renderState.lightCoords);
            }
            return;
        }

        if (!EnhancedSlabPistonHooks.supports(movedState)) return;

        EnhancedSlabPistonRenderCache.Entry enhancedEntry = EnhancedSlabPistonRenderCache.peekForMove(destLong, sourceLong);
        if (enhancedEntry == null || enhancedEntry.data() == null) return;

        EnhancedSlabPistonData data = enhancedEntry.data();
        switch (data.kind()) {
            case VERTICAL_SLAB -> {
                if (!(movedState.getBlock() instanceof VerticalSlabBlock)) return;

                BlockState facingSlab = data.slab(0);
                BlockState oppositeSlab = data.slab(1);
                if (facingSlab != null) {
                    VerticalSlabBlockEntityRenderer.submitVerticalHalf(
                            collector, poseStack, levelView, renderPos, facingSlab,
                            movedState.getValue(VerticalSlabBlock.FACING), renderState.lightCoords);
                }
                if (movedState.getValue(VerticalSlabBlock.DOUBLE) && oppositeSlab != null) {
                    VerticalSlabBlockEntityRenderer.submitVerticalHalf(
                            collector, poseStack, levelView, renderPos, oppositeSlab,
                            movedState.getValue(VerticalSlabBlock.FACING).getOpposite(), renderState.lightCoords);
                }
            }
            case STEP -> {
                if (!(movedState.getBlock() instanceof StepBlock)) return;

                BlockState slab = data.slab(0);
                if (slab != null) {
                    StepBlockEntityRenderer.submitQuadrant(
                            collector, poseStack, levelView, renderPos, slab,
                            movedState.getValue(StepBlock.FACING),
                            movedState.getValue(StepBlock.TOP), renderState.lightCoords);
                }
            }
            case VERTICAL_STEP -> {
                if (!(movedState.getBlock() instanceof VerticalStepBlock)) return;

                BlockState slab = data.slab(0);
                if (slab != null) {
                    VerticalStepBlockEntityRenderer.submitQuadrant(
                            collector, poseStack, levelView, renderPos, slab,
                            movedState.getValue(VerticalStepBlock.FACING),
                            movedState.getValue(VerticalStepBlock.SIDE), renderState.lightCoords);
                }
            }
            case QUAD_STEP -> {
                if (!(movedState.getBlock() instanceof QuadStepBlock)) return;

                Direction.Axis axis = movedState.getValue(QuadStepBlock.AXIS);
                for (int i = 0; i < 4; i++) {
                    BlockState slab = data.slab(i);
                    if (slab == null) continue;

                    boolean positive = i == 1 || i == 3;
                    Direction facing = axis == Direction.Axis.Z
                            ? (positive ? Direction.SOUTH : Direction.NORTH)
                            : (positive ? Direction.EAST : Direction.WEST);
                    boolean top = i >= 2;
                    StepBlockEntityRenderer.submitQuadrant(
                            collector, poseStack, levelView, renderPos, slab, facing, top, renderState.lightCoords);
                }
            }
            case QUAD_VERTICAL_STEP -> {
                for (int i = 0; i < 4; i++) {
                    BlockState slab = data.slab(i);
                    if (slab == null) continue;

                    Direction facing = (i == 2 || i == 3) ? Direction.SOUTH : Direction.NORTH;
                    Direction side = (i == 1 || i == 3) ? Direction.EAST : Direction.WEST;
                    VerticalStepBlockEntityRenderer.submitQuadrant(
                            collector, poseStack, levelView, renderPos, slab, facing, side, renderState.lightCoords);
                }
            }
        }
    }

    private static void submitSlabHalf(SubmitNodeCollector collector,
                                       PoseStack poseStack,
                                       BlockAndTintGetter level,
                                       net.minecraft.core.BlockPos seedPos,
                                       net.minecraft.core.BlockPos renderPos,
                                       BlockState slabState,
                                       int packedLight) {
        RenderType renderType = switch (ItemBlockRenderTypes.getChunkRenderType(slabState)) {
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
            case TRIPWIRE -> RenderTypes.tripwireMovingBlock();
            default -> RenderTypes.solidMovingBlock();
        };

        BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(slabState);
        long seed = seedPos.asLong();
        List<BlockModelPart> parts = model.collectParts(RandomSource.create(seed));

        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            PoseStack tmp = new PoseStack();
            tmp.last().pose().set(pose.pose());
            tmp.last().normal().set(pose.normal());

            BlockColorRenderContext.setForcedPosLong(renderPos.asLong());
            try {
                VirtualRenderWorldLightBridge.renderBatched(
                        slabState,
                        renderPos,
                        level,
                        tmp,
                        consumer,
                        false,
                        parts,
                        packedLight
                );
            } finally {
                BlockColorRenderContext.clear();
            }
        });
    }
}
