package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.onenonly.bitsandbalance.Config;
import net.minecraft.core.Direction;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepCrackProxyBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabCrackProxyBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepCrackProxyBlock;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCrumbleState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererEnhancedSlabCrumblingMixin {

    @Shadow @Final private Minecraft minecraft;

    @Redirect(
            method = "renderBlockDestroyAnimation(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/state/LevelRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"
            ),
            require = 0
    )
    private void bitsandbalance$renderHalfSlabCrumblingOverlay(BlockRenderDispatcher dispatcher,
                                                               BlockState state,
                                                               BlockPos pos,
                                                               BlockAndTintGetter level,
                                                               PoseStack poseStack,
                                                               VertexConsumer vertexConsumer) {
        if (!Config.enableEnhancedSlabs) {
            dispatcher.renderBreakingTexture(state, pos, level, poseStack, vertexConsumer);
            return;
        }

        Player player = this.minecraft.player;
        if (player == null) {
            dispatcher.renderBreakingTexture(state, pos, level, poseStack, vertexConsumer);
            return;
        }

        BlockState locked = EnhancedSlabCrumbleState.get(pos);
        if (locked != null) {
            dispatcher.renderBreakingTexture(locked, pos, level, poseStack, vertexConsumer);
            return;
        }

        // Mixed slabs: render cracks for the targeted half's real slab state.
        if (Config.enhancedSlabsMixedDoubleSlabs && state != null && state.getBlock() instanceof MixedSlabBlock) {
            if (this.minecraft.level != null) {
                BlockEntity be = this.minecraft.level.getBlockEntity(pos);
                if (be instanceof MixedSlabBlockEntity mixedBe) {
                    SlabType targetedHalf = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos);
                    BlockState halfState = (targetedHalf == SlabType.TOP) ? mixedBe.getTopSlab() : mixedBe.getBottomSlab();
                    if (halfState != null) {
                        dispatcher.renderBreakingTexture(halfState, pos, level, poseStack, vertexConsumer);
                        return;
                    }
                }
            }
            dispatcher.renderBreakingTexture(state, pos, level, poseStack, vertexConsumer);
            return;
        }

        // Vertical slabs: render cracks for the source slab's real texture.
        if (Config.enhancedSlabsVerticalSlabs && state != null && state.getBlock() instanceof VerticalSlabBlock) {
            if (state.getValue(VerticalSlabBlock.DOUBLE) && !player.isShiftKeyDown() && this.minecraft.level != null) {
                BlockEntity be = this.minecraft.level.getBlockEntity(pos);
                if (be instanceof org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity verticalSlab
                        && !VerticalSlabBlock.bitsandbalance$shouldTargetHalf(player, state, verticalSlab)) {
                    if (bitsandbalance$renderFullVerticalSlabCrumble(dispatcher, pos, level, poseStack, vertexConsumer, state)) {
                        return;
                    }
                    return;
                }
            }
            BlockState sourceState = bitsandbalance$verticalSlabCrumbleState(state, pos, player);
            if (sourceState != null) {
                dispatcher.renderBreakingTexture(sourceState, pos, level, poseStack, vertexConsumer);
                return;
            }
            dispatcher.renderBreakingTexture(state, pos, level, poseStack, vertexConsumer);
            return;
        }

        if (Config.enhancedSlabsSteps && state != null && (state.getBlock() instanceof StepBlock || state.getBlock() instanceof QuadStepBlock)) {
            if (state.getBlock() instanceof QuadStepBlock && !player.isShiftKeyDown()) {
                if (bitsandbalance$renderSelectedQuadStepCrumble(dispatcher, pos, level, poseStack, vertexConsumer, state, player)) {
                    return;
                }
                return;
            }
            BlockState sourceState = bitsandbalance$stepCrumbleState(state, pos, player);
            if (sourceState != null) {
                dispatcher.renderBreakingTexture(sourceState, pos, level, poseStack, vertexConsumer);
                return;
            }
            dispatcher.renderBreakingTexture(state, pos, level, poseStack, vertexConsumer);
            return;
        }

        if (Config.enhancedSlabsSteps && state != null && (state.getBlock() instanceof VerticalStepBlock || state.getBlock() instanceof QuadVerticalStepBlock)) {
            if (state.getBlock() instanceof QuadVerticalStepBlock && !player.isShiftKeyDown()) {
                if (bitsandbalance$renderSelectedQuadVerticalStepCrumble(dispatcher, pos, level, poseStack, vertexConsumer, state, player)) {
                    return;
                }
                return;
            }
            BlockState sourceState = bitsandbalance$verticalStepCrumbleState(state, pos, player);
            if (sourceState != null) {
                dispatcher.renderBreakingTexture(sourceState, pos, level, poseStack, vertexConsumer);
                return;
            }
            dispatcher.renderBreakingTexture(state, pos, level, poseStack, vertexConsumer);
            return;
        }

        // Vanilla double slabs: render cracks as if the targeted half were a standalone slab.
        if (!Config.enhancedSlabsKneeSlabMining) return;
        if (!player.isShiftKeyDown()) {
            dispatcher.renderBreakingTexture(state, pos, level, poseStack, vertexConsumer);
            return;
        }
        if (state == null || !(state.getBlock() instanceof SlabBlock)) {
            dispatcher.renderBreakingTexture(state, pos, level, poseStack, vertexConsumer);
            return;
        }
        if (!EnhancedSlabHelper.isDoubleSlab(state)) {
            dispatcher.renderBreakingTexture(state, pos, level, poseStack, vertexConsumer);
            return;
        }

        SlabType targetedHalf = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos);
        dispatcher.renderBreakingTexture(EnhancedSlabHelper.getHalfSlabState(state, targetedHalf), pos, level, poseStack, vertexConsumer);
    }

    private boolean bitsandbalance$renderFullVerticalSlabCrumble(BlockRenderDispatcher dispatcher,
                                                                 BlockPos pos,
                                                                 BlockAndTintGetter level,
                                                                 PoseStack poseStack,
                                                                 VertexConsumer vertexConsumer,
                                                                 BlockState state) {
        VerticalSlabCrackProxyBlock proxy = CommonBlocks.VERTICAL_SLAB_CRACK_PROXY;
        if (proxy == null) return false;

        Direction facing = state.getValue(VerticalSlabBlock.FACING);
        dispatcher.renderBreakingTexture(proxy.defaultBlockState().setValue(VerticalSlabCrackProxyBlock.FACING, facing), pos, level, poseStack, vertexConsumer);
        dispatcher.renderBreakingTexture(proxy.defaultBlockState().setValue(VerticalSlabCrackProxyBlock.FACING, facing.getOpposite()), pos, level, poseStack, vertexConsumer);
        return true;
    }

    private boolean bitsandbalance$renderSelectedQuadStepCrumble(BlockRenderDispatcher dispatcher,
                                                                 BlockPos pos,
                                                                 BlockAndTintGetter level,
                                                                 PoseStack poseStack,
                                                                 VertexConsumer vertexConsumer,
                                                                 BlockState state,
                                                                 Player player) {
        if (this.minecraft.level == null) return false;
        BlockEntity be = this.minecraft.level.getBlockEntity(pos);
        if (!(be instanceof QuadStepBlockEntity quad)) return false;

        StepCrackProxyBlock proxy = CommonBlocks.STEP_CRACK_PROXY;
        if (proxy == null) return false;

        Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
        int targetedIndex = QuadStepBlock.bitsandbalance$resolveOccupiedIndex(quad, QuadStepBlock.bitsandbalance$getTargetedIndex(player, pos, axis));
        if (targetedIndex < 0) return false;
        BlockState selected = quad.getSlabAt(targetedIndex);
        if (selected == null) return false;
        boolean rendered = false;
        for (int idx = 0; idx < 4; idx++) {
            BlockState slab = quad.getSlabAt(idx);
            if (slab == null || !selected.equals(slab)) continue;
            BlockState proxyState = proxy.defaultBlockState()
                    .setValue(StepCrackProxyBlock.FACING, QuadStepBlock.sideForIndex(axis, idx))
                    .setValue(StepCrackProxyBlock.TOP, QuadStepBlock.isTop(idx));
            dispatcher.renderBreakingTexture(proxyState, pos, level, poseStack, vertexConsumer);
            rendered = true;
        }
        return rendered;
    }

    private boolean bitsandbalance$renderSelectedQuadVerticalStepCrumble(BlockRenderDispatcher dispatcher,
                                                                         BlockPos pos,
                                                                         BlockAndTintGetter level,
                                                                         PoseStack poseStack,
                                                                         VertexConsumer vertexConsumer,
                                                                         BlockState state,
                                                                         Player player) {
        if (this.minecraft.level == null) return false;
        BlockEntity be = this.minecraft.level.getBlockEntity(pos);
        if (!(be instanceof QuadVerticalStepBlockEntity quad)) return false;

        VerticalStepCrackProxyBlock proxy = CommonBlocks.VERTICAL_STEP_CRACK_PROXY;
        if (proxy == null) return false;

        int targetedIndex = QuadVerticalStepBlock.bitsandbalance$resolveOccupiedIndex(quad, QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, pos));
        if (targetedIndex < 0) return false;
        BlockState selected = quad.getSlabAt(targetedIndex);
        if (selected == null) return false;
        boolean rendered = false;
        for (int idx = 0; idx < 4; idx++) {
            BlockState slab = quad.getSlabAt(idx);
            if (slab == null || !selected.equals(slab)) continue;
            BlockState proxyState = proxy.defaultBlockState()
                    .setValue(VerticalStepCrackProxyBlock.FACING, QuadVerticalStepBlock.facingForIndex(idx))
                    .setValue(VerticalStepCrackProxyBlock.SIDE, QuadVerticalStepBlock.sideForIndex(idx));
            dispatcher.renderBreakingTexture(proxyState, pos, level, poseStack, vertexConsumer);
            rendered = true;
        }
        return rendered;
    }

    @org.jetbrains.annotations.Nullable
    private static BlockState bitsandbalance$verticalSlabCrumbleState(BlockState vertState, BlockPos pos, Player player) {
        try {
            VerticalSlabCrackProxyBlock proxy = CommonBlocks.VERTICAL_SLAB_CRACK_PROXY;
            if (proxy == null) return null;
            Direction facing = vertState.getValue(VerticalSlabBlock.FACING);
            // For double slabs, crack the half the player is aiming at.
            if (vertState.getValue(VerticalSlabBlock.DOUBLE) && player != null) {
                facing = VerticalSlabBlock.bitsandbalance$getTargetedHalf(player, pos, facing);
            }
            // Return the proxy state: it has RenderShape.MODEL so renderBreakingTexture
            // will look up the vertical_slab_crack_proxy blockstates and render the
            // correct half-prism geometry models.
            return proxy.defaultBlockState().setValue(VerticalSlabCrackProxyBlock.FACING, facing);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @org.jetbrains.annotations.Nullable
    private static BlockState bitsandbalance$stepCrumbleState(BlockState stepState, BlockPos pos, Player player) {
        try {
            StepCrackProxyBlock proxy = CommonBlocks.STEP_CRACK_PROXY;
            if (proxy == null) return null;

            Direction facing;
            boolean top;
            if (stepState.getBlock() instanceof QuadStepBlock) {
                if (player == null) return null;
                Direction.Axis axis = stepState.getValue(QuadStepBlock.AXIS);
                int idx = QuadStepBlock.bitsandbalance$getTargetedIndex(player, pos, axis);
                facing = QuadStepBlock.sideForIndex(axis, idx);
                top = QuadStepBlock.isTop(idx);
            } else {
                facing = stepState.getValue(StepBlock.FACING);
                top = stepState.getValue(StepBlock.TOP);
            }

            return proxy.defaultBlockState()
                    .setValue(StepCrackProxyBlock.FACING, facing)
                    .setValue(StepCrackProxyBlock.TOP, top);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @org.jetbrains.annotations.Nullable
    private static BlockState bitsandbalance$verticalStepCrumbleState(BlockState stepState, BlockPos pos, Player player) {
        try {
            VerticalStepCrackProxyBlock proxy = CommonBlocks.VERTICAL_STEP_CRACK_PROXY;
            if (proxy == null) return null;

            Direction facing;
            Direction side;
            if (stepState.getBlock() instanceof QuadVerticalStepBlock) {
                if (player == null) return null;
                int idx = QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, pos);
                facing = QuadVerticalStepBlock.facingForIndex(idx);
                side = QuadVerticalStepBlock.sideForIndex(idx);
            } else {
                facing = stepState.getValue(VerticalStepBlock.FACING);
                side = stepState.getValue(VerticalStepBlock.SIDE);
            }

            return proxy.defaultBlockState()
                    .setValue(VerticalStepCrackProxyBlock.FACING, facing)
                    .setValue(VerticalStepCrackProxyBlock.SIDE, side);
        } catch (Throwable ignored) {
            return null;
        }
    }

}
