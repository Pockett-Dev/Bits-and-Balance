package org.onenonly.bitsandbalance.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.client.renderer.MixedSlabBlockEntityRenderer;
import org.onenonly.bitsandbalance.client.renderer.StepBlockEntityRenderer;
import org.onenonly.bitsandbalance.client.renderer.VerticalSlabBlockEntityRenderer;
import org.onenonly.bitsandbalance.client.renderer.VerticalStepBlockEntityRenderer;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.MixedSlabPistonHeadRenderStateAccess;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonData;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonHooks;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonRenderCache;
import org.onenonly.bitsandbalance.common.mechanics.MixedSlabPistonRenderCache;
import org.onenonly.bitsandbalance.common.mixin.MixedSlabMovingPistonAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.renderer.blockentity.PistonHeadRenderer.class)
public class PistonHeadRendererMixedSlabRenderMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At("TAIL"),
        require = 0
    )
    private void bitsandbalance$captureMixedSlabRenderState(PistonMovingBlockEntity piston,
                                                            PistonHeadRenderState renderState,
                                                            float partialTick,
                                                            Vec3 cameraPos,
                                                            ModelFeatureRenderer.CrumblingOverlay overlay,
                                                            CallbackInfo ci) {
        if (!(renderState instanceof MixedSlabPistonHeadRenderStateAccess renderStateAccess)) {
            return;
        }

        BlockState bottom = null;
        BlockState top = null;
        if (piston instanceof MixedSlabMovingPistonAccess movingAccess) {
            bottom = movingAccess.bitsandbalance$getBottomSlab();
            top = movingAccess.bitsandbalance$getTopSlab();
        }
        renderStateAccess.bitsandbalance$setMixedSlabData(bottom, top);
    }

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
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
                                                     net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState,
                                                     CallbackInfo ci) {
        if (renderState == null || collector == null || poseStack == null) return;

        MovingBlockRenderState moving = renderState.block;
        if (moving == null) return;

        BlockState movedState = moving.blockState;
        if (movedState == null) return;

        if (renderState.blockPos == null || moving.blockPos == null) return;

        long destLong = renderState.blockPos.asLong();
        long sourceLong = moving.blockPos.asLong();

        Object levelView = moving;
        net.minecraft.core.BlockPos renderPos = moving.blockPos;

        if (movedState.getBlock() instanceof MixedSlabBlock) {
            MixedSlabPistonRenderCache.Entry entry = bitsandbalance$resolveMixedSlabRenderEntry(renderState, destLong, sourceLong, renderState.blockPos);
            if (entry == null) return;

            if (entry.bottomSlab() != null) {
                MixedSlabBlockEntityRenderer.submitSlabHalf(collector, poseStack, levelView, renderPos, entry.bottomSlab(), renderState.lightCoords);
            }
            if (entry.topSlab() != null) {
                MixedSlabBlockEntityRenderer.submitSlabHalf(collector, poseStack, levelView, renderPos, entry.topSlab(), renderState.lightCoords);
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

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("RETURN"),
        require = 0
    )
    private void bitsandbalance$submitMixedSlabOnEarlyReturn(PistonHeadRenderState renderState,
                                                             PoseStack poseStack,
                                                             SubmitNodeCollector collector,
                                                             net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState,
                                                             CallbackInfo ci) {
        if (renderState == null || renderState.block != null || renderState.blockPos == null || collector == null || poseStack == null) {
            return;
        }

        MixedSlabPistonRenderCache.Entry entry = bitsandbalance$resolveMixedSlabRenderEntry(renderState, renderState.blockPos.asLong(), 0L, renderState.blockPos);
        if (entry == null) {
            return;
        }

        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(renderState.xOffset, renderState.yOffset, renderState.zOffset);
        try {
            if (entry.bottomSlab() != null) {
                MixedSlabBlockEntityRenderer.submitSlabHalf(collector, poseStack, minecraft.level, renderState.blockPos, entry.bottomSlab(), renderState.lightCoords);
            }
            if (entry.topSlab() != null) {
                MixedSlabBlockEntityRenderer.submitSlabHalf(collector, poseStack, minecraft.level, renderState.blockPos, entry.topSlab(), renderState.lightCoords);
            }
        } finally {
            poseStack.popPose();
        }
    }

    private static MixedSlabPistonRenderCache.@org.jetbrains.annotations.Nullable Entry bitsandbalance$resolveMixedSlabRenderEntry(PistonHeadRenderState renderState,
                                                                                                                                    long destLong,
                                                                                                                                    long sourceLong,
                                                                                                                                    net.minecraft.core.BlockPos destPos) {
        if (renderState instanceof MixedSlabPistonHeadRenderStateAccess renderStateAccess) {
            BlockState bottom = renderStateAccess.bitsandbalance$getBottomSlab();
            BlockState top = renderStateAccess.bitsandbalance$getTopSlab();
            if (bottom != null || top != null) {
                return new MixedSlabPistonRenderCache.Entry(sourceLong, destLong, bottom, top);
            }
        }

        MixedSlabPistonRenderCache.Entry entry = MixedSlabPistonRenderCache.peekForMove(destLong, sourceLong);
        if (entry != null) {
            return entry;
        }

        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null || destPos == null) {
            return null;
        }

        if (minecraft.level.getBlockEntity(destPos) instanceof MixedSlabBlockEntity mixedSlabBlockEntity) {
            BlockState bottom = mixedSlabBlockEntity.getBottomSlab();
            BlockState top = mixedSlabBlockEntity.getTopSlab();
            if (bottom != null || top != null) {
                return new MixedSlabPistonRenderCache.Entry(sourceLong, destLong, bottom, top);
            }
        }

        return null;
    }
}