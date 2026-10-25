package org.onenonly.bitsandbalance.fabric.mixin;

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
import net.minecraft.core.Direction;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
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
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCrumbleState;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererEnhancedSlabCrumblingMixin {

    @Shadow @Final private Minecraft minecraft;

    @Redirect(
            method = "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
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
        if (!FabricTweaksConfig.enableEnhancedSlabs) {
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
        if (FabricTweaksConfig.enhancedSlabsMixedDoubleSlabs && state != null && state.getBlock() instanceof MixedSlabBlock) {
            if (this.minecraft.level != null) {
                BlockState[] stored = EnhancedSlabMaterialSavedData.getClient(this.minecraft.level, pos.asLong());
                SlabType targetedHalf = bitsandbalance$targetedHalf(pos, player);
                BlockState halfState = targetedHalf == SlabType.TOP
                        ? bitsandbalance$slot(stored, 1)
                        : bitsandbalance$slot(stored, 0);
                if (halfState != null) {
                    dispatcher.renderBreakingTexture(halfState.setValue(SlabBlock.TYPE, targetedHalf), pos, level, poseStack, vertexConsumer);
                    return;
                }
            }
            dispatcher.renderBreakingTexture(state, pos, level, poseStack, vertexConsumer);
            return;
        }

        // Vertical slabs: render cracks for the source slab's real texture.
        if (FabricTweaksConfig.enhancedSlabsVerticalSlabs && state != null && state.getBlock() instanceof VerticalSlabBlock) {
            if (state.getValue(VerticalSlabBlock.DOUBLE) && !player.isShiftKeyDown() && this.minecraft.level != null) {
                BlockEntity be = this.minecraft.level.getBlockEntity(pos);
                org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity verticalSlab =
                        be instanceof org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity vbe ? vbe : null;
                BlockState[] stored = verticalSlab == null ? EnhancedSlabMaterialSavedData.getClient(this.minecraft.level, pos.asLong()) : null;
                if (!VerticalSlabBlock.bitsandbalance$shouldTargetHalf(player, state, verticalSlab, stored)) {
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

        if (FabricTweaksConfig.enhancedSlabsSteps && state != null && (state.getBlock() instanceof StepBlock || state.getBlock() instanceof QuadStepBlock)) {
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

        if (FabricTweaksConfig.enhancedSlabsSteps && state != null && (state.getBlock() instanceof VerticalStepBlock || state.getBlock() instanceof QuadVerticalStepBlock)) {
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
        if (!FabricTweaksConfig.enhancedSlabsKneeSlabMining) {
            dispatcher.renderBreakingTexture(state, pos, level, poseStack, vertexConsumer);
            return;
        }
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

        SlabType targetedHalf = bitsandbalance$targetedHalf(pos, player);
        dispatcher.renderBreakingTexture(EnhancedSlabHelper.getHalfSlabState(state, targetedHalf), pos, level, poseStack, vertexConsumer);
    }

    private SlabType bitsandbalance$targetedHalf(BlockPos pos, Player player) {
        if (this.minecraft.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                && pos.equals(hit.getBlockPos())) {
            return EnhancedSlabHelper.getTargetedHalf(pos, hit.getLocation());
        }

        return EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos);
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
        QuadStepBlockEntity quad = be instanceof QuadStepBlockEntity q ? q : null;
        BlockState[] stored = quad == null ? EnhancedSlabMaterialSavedData.getClient(this.minecraft.level, pos.asLong()) : null;
        if (quad == null && stored == null) return false;

        StepCrackProxyBlock proxy = CommonBlocks.STEP_CRACK_PROXY;
        if (proxy == null) return false;

        Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
        int preferredIndex = QuadStepBlock.bitsandbalance$getTargetedIndex(player, pos, axis);
        int targetedIndex = quad != null
                ? QuadStepBlock.bitsandbalance$resolveOccupiedIndex(quad, preferredIndex)
                : bitsandbalance$resolveOccupiedIndex(stored, preferredIndex);
        if (targetedIndex < 0) return false;
        BlockState selected = quad != null ? quad.getSlabAt(targetedIndex) : bitsandbalance$slot(stored, targetedIndex);
        if (selected == null) return false;
        boolean rendered = false;
        for (int idx = 0; idx < 4; idx++) {
            BlockState slab = quad != null ? quad.getSlabAt(idx) : bitsandbalance$slot(stored, idx);
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
        QuadVerticalStepBlockEntity quad = be instanceof QuadVerticalStepBlockEntity q ? q : null;
        BlockState[] stored = quad == null ? EnhancedSlabMaterialSavedData.getClient(this.minecraft.level, pos.asLong()) : null;
        if (quad == null && stored == null) return false;

        VerticalStepCrackProxyBlock proxy = CommonBlocks.VERTICAL_STEP_CRACK_PROXY;
        if (proxy == null) return false;

        int preferredIndex = QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, pos);
        int targetedIndex = quad != null
                ? QuadVerticalStepBlock.bitsandbalance$resolveOccupiedIndex(quad, preferredIndex)
                : bitsandbalance$resolveOccupiedIndex(stored, preferredIndex);
        if (targetedIndex < 0) return false;
        BlockState selected = quad != null ? quad.getSlabAt(targetedIndex) : bitsandbalance$slot(stored, targetedIndex);
        if (selected == null) return false;
        boolean rendered = false;
        for (int idx = 0; idx < 4; idx++) {
            BlockState slab = quad != null ? quad.getSlabAt(idx) : bitsandbalance$slot(stored, idx);
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

    @org.jetbrains.annotations.Nullable
    private static BlockState bitsandbalance$slot(@org.jetbrains.annotations.Nullable BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    private static int bitsandbalance$resolveOccupiedIndex(@org.jetbrains.annotations.Nullable BlockState[] slots, int preferredIndex) {
        if (bitsandbalance$slot(slots, preferredIndex) != null) {
            return preferredIndex;
        }
        if (slots == null) {
            return -1;
        }
        for (int i = 0; i < slots.length && i < 4; i++) {
            if (slots[i] != null) {
                return i;
            }
        }
        return -1;
    }

}
