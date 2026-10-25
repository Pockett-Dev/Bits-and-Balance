package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabBlockEntityRenderLimits;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabRenderDiagnostics;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabRenderModelCompat;
import org.onenonly.bitsandbalance.fabric.client.renderer.PistonMovingBlockEntityRenderHelper;
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

        if (!EnhancedSlabBlockEntityRenderLimits.shouldSubmit(renderPos, cameraRenderState)) {
            EnhancedSlabRenderDiagnostics.pistonSkippedDistance();
            return;
        }

        if (movedState.getBlock() instanceof MixedSlabBlock) {
            MixedSlabPistonRenderCache.Entry entry = bitsandbalance$resolveMixedSlabRenderEntry(renderState, destLong, sourceLong);
            if (entry == null) return;

            if (entry.bottomSlab() != null) {
                int visibleMask = entry.topSlab() != null
                        ? EnhancedSlabRenderModelCompat.without(EnhancedSlabRenderModelCompat.allDirectionMask(), Direction.UP)
                        : EnhancedSlabRenderModelCompat.allDirectionMask();
                submitSlabHalf(collector, poseStack, levelView, seedPos, renderPos, entry.bottomSlab(), renderState.lightCoords, visibleMask);
            }
            if (entry.topSlab() != null) {
                int visibleMask = entry.bottomSlab() != null
                        ? EnhancedSlabRenderModelCompat.without(EnhancedSlabRenderModelCompat.allDirectionMask(), Direction.DOWN)
                        : EnhancedSlabRenderModelCompat.allDirectionMask();
                submitSlabHalf(collector, poseStack, levelView, seedPos, renderPos, entry.topSlab(), renderState.lightCoords, visibleMask);
            }

            PistonMovingBlockEntityRenderHelper.submitMoving(renderPos, renderState.blockPos, movedState, poseStack, collector, cameraRenderState);
            return;
        }

        if (EnhancedSlabPistonHooks.supports(movedState)) {
            EnhancedSlabPistonRenderCache.Entry enhancedEntry = bitsandbalance$resolveEnhancedSlabRenderEntry(renderState, movedState, destLong, sourceLong);
            if (enhancedEntry != null && enhancedEntry.data() != null) {
                EnhancedSlabPistonData data = enhancedEntry.data();
                switch (data.kind()) {
                    case VERTICAL_SLAB -> {
                        if (!(movedState.getBlock() instanceof VerticalSlabBlock)) return;

                        BlockState facingSlab = data.slab(0);
                        BlockState oppositeSlab = data.slab(1);
                        if (facingSlab != null) {
                            int visibleMask = oppositeSlab != null
                                    ? EnhancedSlabRenderModelCompat.without(EnhancedSlabRenderModelCompat.allDirectionMask(), movedState.getValue(VerticalSlabBlock.FACING).getOpposite())
                                    : EnhancedSlabRenderModelCompat.allDirectionMask();
                            VerticalSlabBlockEntityRenderer.submitVerticalHalf(
                                    collector, poseStack, levelView, renderPos, facingSlab,
                                    movedState.getValue(VerticalSlabBlock.FACING), renderState.lightCoords, visibleMask);
                        }
                        if (movedState.getValue(VerticalSlabBlock.DOUBLE) && oppositeSlab != null) {
                            int visibleMask = facingSlab != null
                                    ? EnhancedSlabRenderModelCompat.without(EnhancedSlabRenderModelCompat.allDirectionMask(), movedState.getValue(VerticalSlabBlock.FACING))
                                    : EnhancedSlabRenderModelCompat.allDirectionMask();
                            VerticalSlabBlockEntityRenderer.submitVerticalHalf(
                                    collector, poseStack, levelView, renderPos, oppositeSlab,
                                    movedState.getValue(VerticalSlabBlock.FACING).getOpposite(), renderState.lightCoords, visibleMask);
                        }
                    }
                    case STEP -> {
                        if (!(movedState.getBlock() instanceof StepBlock)) return;

                        BlockState slab = data.slab(0);
                        if (slab != null) {
                            StepBlockEntityRenderer.submitQuadrant(
                                    collector, poseStack, levelView, renderPos, slab,
                                    movedState.getValue(StepBlock.FACING),
                                    movedState.getValue(StepBlock.TOP), renderState.lightCoords,
                                    EnhancedSlabRenderModelCompat.allDirectionMask());
                        }
                    }
                    case VERTICAL_STEP -> {
                        if (!(movedState.getBlock() instanceof VerticalStepBlock)) return;

                        BlockState slab = data.slab(0);
                        if (slab != null) {
                            VerticalStepBlockEntityRenderer.submitQuadrant(
                                    collector, poseStack, levelView, renderPos, slab,
                                    movedState.getValue(VerticalStepBlock.FACING),
                                    movedState.getValue(VerticalStepBlock.SIDE), renderState.lightCoords,
                                    EnhancedSlabRenderModelCompat.allDirectionMask());
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
                            int visibleMask = bitsandbalance$quadStepVisibleMask(axis, i, data);
                            StepBlockEntityRenderer.submitQuadrant(
                                    collector, poseStack, levelView, renderPos, slab, facing, top, renderState.lightCoords, visibleMask);
                        }
                    }
                    case QUAD_VERTICAL_STEP -> {
                        for (int i = 0; i < 4; i++) {
                            BlockState slab = data.slab(i);
                            if (slab == null) continue;

                            Direction facing = (i == 2 || i == 3) ? Direction.SOUTH : Direction.NORTH;
                            Direction side = (i == 1 || i == 3) ? Direction.EAST : Direction.WEST;
                            int visibleMask = bitsandbalance$quadVerticalVisibleMask(i, data);
                            VerticalStepBlockEntityRenderer.submitQuadrant(
                                    collector, poseStack, levelView, renderPos, slab, facing, side, renderState.lightCoords, visibleMask);
                        }
                    }
                }
            }
        }

        PistonMovingBlockEntityRenderHelper.submitMoving(renderPos, renderState.blockPos, movedState, poseStack, collector, cameraRenderState);
    }

    private static MixedSlabPistonRenderCache.@org.jetbrains.annotations.Nullable Entry bitsandbalance$resolveMixedSlabRenderEntry(PistonHeadRenderState renderState,
                                                                                                                                     long destLong,
                                                                                                                                     long sourceLong) {
        MixedSlabPistonRenderCache.Entry entry = MixedSlabPistonRenderCache.peekForMove(destLong, sourceLong);
        if (entry != null) {
            return entry;
        }

        BlockState[] slots = bitsandbalance$resolveSavedSlots(destLong, sourceLong);
        BlockState bottom = bitsandbalance$slot(slots, 0);
        BlockState top = bitsandbalance$slot(slots, 1);
        return bottom != null || top != null
                ? new MixedSlabPistonRenderCache.Entry(sourceLong, destLong, bottom, top)
                : null;
    }

    private static EnhancedSlabPistonRenderCache.@org.jetbrains.annotations.Nullable Entry bitsandbalance$resolveEnhancedSlabRenderEntry(PistonHeadRenderState renderState,
                                                                                                                                           BlockState movedState,
                                                                                                                                           long destLong,
                                                                                                                                           long sourceLong) {
        EnhancedSlabPistonRenderCache.Entry entry = EnhancedSlabPistonRenderCache.peekForMove(destLong, sourceLong);
        if (entry != null) {
            return entry;
        }

        EnhancedSlabPistonData data = bitsandbalance$fromSavedSlots(movedState, bitsandbalance$resolveSavedSlots(destLong, sourceLong));
        return data != null ? new EnhancedSlabPistonRenderCache.Entry(sourceLong, destLong, data) : null;
    }

    private static BlockState @org.jetbrains.annotations.Nullable [] bitsandbalance$resolveSavedSlots(long destLong, long sourceLong) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }

        BlockState[] slots = EnhancedSlabMaterialSavedData.getClient(minecraft.level, sourceLong);
        if (slots != null) {
            return slots;
        }

        slots = EnhancedSlabMaterialSavedData.getClient(minecraft.level, destLong);
        if (slots != null) {
            return slots;
        }

        BlockPos sourcePos = BlockPos.of(sourceLong);
        BlockPos destPos = BlockPos.of(destLong);
        for (Direction direction : Direction.values()) {
            slots = EnhancedSlabMaterialSavedData.getClient(minecraft.level, sourcePos.relative(direction).asLong());
            if (slots != null) {
                return slots;
            }
            slots = EnhancedSlabMaterialSavedData.getClient(minecraft.level, destPos.relative(direction).asLong());
            if (slots != null) {
                return slots;
            }
        }
        return null;
    }

    private static @org.jetbrains.annotations.Nullable EnhancedSlabPistonData bitsandbalance$fromSavedSlots(BlockState movedState,
                                                                                                            BlockState @org.jetbrains.annotations.Nullable [] slots) {
        if (movedState == null || slots == null) {
            return null;
        }
        if (movedState.getBlock() instanceof VerticalSlabBlock) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.VERTICAL_SLAB, slots);
        }
        if (movedState.getBlock() instanceof StepBlock) {
            BlockState slab = bitsandbalance$slot(slots, 0);
            return slab == null ? null : new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.STEP, new BlockState[]{slab});
        }
        if (movedState.getBlock() instanceof VerticalStepBlock) {
            BlockState slab = bitsandbalance$slot(slots, 0);
            return slab == null ? null : new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.VERTICAL_STEP, new BlockState[]{slab});
        }
        if (movedState.getBlock() instanceof QuadStepBlock) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.QUAD_STEP, slots);
        }
        if (movedState.getBlock() instanceof org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.QUAD_VERTICAL_STEP, slots);
        }
        return null;
    }

    private static @org.jetbrains.annotations.Nullable BlockState bitsandbalance$slot(BlockState @org.jetbrains.annotations.Nullable [] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    private static void submitSlabHalf(SubmitNodeCollector collector,
                                       PoseStack poseStack,
                                       BlockAndTintGetter level,
                                       net.minecraft.core.BlockPos seedPos,
                                       net.minecraft.core.BlockPos renderPos,
                                       BlockState slabState,
                                       int packedLight,
                                       int visibleMask) {
        RenderType renderType = switch (ItemBlockRenderTypes.getChunkRenderType(slabState)) {
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
            case TRIPWIRE -> RenderTypes.tripwireMovingBlock();
            default -> RenderTypes.solidMovingBlock();
        };

        List<BlockModelPart> rawParts = EnhancedSlabRenderModelCompat.collectModelParts(slabState, seedPos);
        if (rawParts.isEmpty()) {
            return;
        }
        List<BlockModelPart> parts = EnhancedSlabRenderModelCompat.filterPartList(rawParts, visibleMask);
        if (parts.isEmpty()) {
            return;
        }

        EnhancedSlabRenderDiagnostics.pistonCustom();

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

    private static int bitsandbalance$quadStepVisibleMask(Direction.Axis axis, int index, EnhancedSlabPistonData data) {
        int mask = EnhancedSlabRenderModelCompat.allDirectionMask();
        boolean positive = index == 1 || index == 3;
        boolean top = index >= 2;
        int horizontalNeighbor = top ? (positive ? 2 : 3) : (positive ? 0 : 1);
        if (data.slab(horizontalNeighbor) != null) {
            Direction internal = axis == Direction.Axis.Z
                    ? (positive ? Direction.NORTH : Direction.SOUTH)
                    : (positive ? Direction.WEST : Direction.EAST);
            mask = EnhancedSlabRenderModelCompat.without(mask, internal);
        }
        int verticalNeighbor = top ? index - 2 : index + 2;
        if (data.slab(verticalNeighbor) != null) {
            mask = EnhancedSlabRenderModelCompat.without(mask, top ? Direction.DOWN : Direction.UP);
        }
        return mask;
    }

    private static int bitsandbalance$quadVerticalVisibleMask(int index, EnhancedSlabPistonData data) {
        int mask = EnhancedSlabRenderModelCompat.allDirectionMask();
        int eastWestNeighbor = (index == 0 || index == 2) ? index + 1 : index - 1;
        if (data.slab(eastWestNeighbor) != null) {
            mask = EnhancedSlabRenderModelCompat.without(mask, (index == 0 || index == 2) ? Direction.EAST : Direction.WEST);
        }
        int northSouthNeighbor = (index == 0 || index == 1) ? index + 2 : index - 2;
        if (data.slab(northSouthNeighbor) != null) {
            mask = EnhancedSlabRenderModelCompat.without(mask, (index == 0 || index == 1) ? Direction.SOUTH : Direction.NORTH);
        }
        return mask;
    }
}
