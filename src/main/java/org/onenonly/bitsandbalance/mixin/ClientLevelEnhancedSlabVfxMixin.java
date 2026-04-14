package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabDestroyParticleHelper;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelEnhancedSlabVfxMixin {

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$spawnEnhancedDestroyParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!Config.enableEnhancedSlabs || state == null) return;

        ClientLevel level = (ClientLevel) (Object) this;
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;

        if (!bitsandbalance$isEnhancedInvisibleBreakState(state)) return;
        if (EnhancedSlabDestroyParticleHelper.spawnForState(level, player, pos, state)) {
            ci.cancel();
        }
    }

    @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$spawnEnhancedHitParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
        if (!Config.enableEnhancedSlabs) return;

        ClientLevel level = (ClientLevel) (Object) this;
        BlockState state = level.getBlockState(pos);
        if (state == null) return;

        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;

        if (!bitsandbalance$isEnhancedInvisibleBreakState(state)) return;
        if (EnhancedSlabDestroyParticleHelper.spawnHitForState(level, player, pos, direction, state)) {
            ci.cancel();
        }
    }

    @ModifyVariable(
            method = "addDestroyBlockEffect",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0,
            index = 2
    )
    private BlockState bitsandbalance$useHalfStateForDestroyParticles(BlockState state, BlockPos pos) {
        if (!Config.enableEnhancedSlabs) return state;

        ClientLevel level = (ClientLevel) (Object) this;
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return state;

        return getHalfStateForVfx(level, player, pos, state);
    }

    @ModifyVariable(
            method = "addBreakingBlockEffect",
            at = @At(
                value = "INVOKE_ASSIGN",
                target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            require = 0
    )
    private BlockState bitsandbalance$useHalfStateForHitParticles(BlockState state, BlockPos pos, Direction direction) {
        if (!Config.enableEnhancedSlabs) return state;

        ClientLevel level = (ClientLevel) (Object) this;
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return state;

        return getHalfStateForVfx(level, player, pos, state);
    }

    private static BlockState getHalfStateForVfx(ClientLevel level, Player player, BlockPos pos, BlockState state) {
        try {
            if (state != null && state.getBlock() instanceof MixedSlabBlock) {
                if (!Config.enhancedSlabsMixedDoubleSlabs) return state;

                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof MixedSlabBlockEntity mixedBe) {
                    SlabType targetedHalf;
                    if (net.minecraft.client.Minecraft.getInstance().hitResult instanceof BlockHitResult bhr
                            && pos.equals(bhr.getBlockPos())) {
                        targetedHalf = EnhancedSlabHelper.getTargetedHalf(pos, bhr.getLocation());
                    } else {
                        targetedHalf = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos);
                    }
                    BlockState halfState = (targetedHalf == SlabType.TOP) ? mixedBe.getTopSlab() : mixedBe.getBottomSlab();
                    return halfState != null ? halfState : state;
                }

                return state;
            }

            if (state != null && state.getBlock() instanceof VerticalSlabBlock) {
                if (!Config.enhancedSlabsVerticalSlabs) return state;
                VerticalSlabBlock genericBlock = CommonBlocks.VERTICAL_SLAB;
                if (genericBlock == null) return state;
                Direction facing = state.getValue(VerticalSlabBlock.FACING);
                if (state.getValue(VerticalSlabBlock.DOUBLE)) {
                    facing = VerticalSlabBlock.bitsandbalance$getTargetedHalf(player, pos, facing);
                }
                // Generic block state: getShape(level, pos) returns halfShape(facing)
                // so particles spawn within the correct half-prism of the block.
                return genericBlock.defaultBlockState()
                        .setValue(VerticalSlabBlock.FACING, facing)
                        .setValue(VerticalSlabBlock.DOUBLE, false);
            }

            if (state != null && state.getBlock() instanceof StepBlock) {
                if (!Config.enhancedSlabsSteps) return state;
                StepBlock genericBlock = CommonBlocks.STEP;
                if (genericBlock == null) return state;
                return genericBlock.defaultBlockState()
                        .setValue(StepBlock.FACING, state.getValue(StepBlock.FACING))
                        .setValue(StepBlock.TOP, state.getValue(StepBlock.TOP));
            }

            if (state != null && state.getBlock() instanceof QuadStepBlock) {
                if (!Config.enhancedSlabsSteps) return state;
                if (player == null) return state;
                StepBlock genericBlock = CommonBlocks.STEP;
                if (genericBlock == null) return state;
                Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
                int idx = QuadStepBlock.bitsandbalance$getTargetedIndex(player, pos, axis);
                return genericBlock.defaultBlockState()
                        .setValue(StepBlock.FACING, QuadStepBlock.sideForIndex(axis, idx))
                        .setValue(StepBlock.TOP, QuadStepBlock.isTop(idx));
            }

            if (state != null && state.getBlock() instanceof VerticalStepBlock) {
                if (!Config.enhancedSlabsSteps) return state;
                VerticalStepBlock genericBlock = CommonBlocks.VERTICAL_STEP;
                if (genericBlock == null) return state;
                return genericBlock.defaultBlockState()
                        .setValue(VerticalStepBlock.FACING, state.getValue(VerticalStepBlock.FACING))
                        .setValue(VerticalStepBlock.SIDE, state.getValue(VerticalStepBlock.SIDE));
            }

            if (state != null && state.getBlock() instanceof QuadVerticalStepBlock) {
                if (!Config.enhancedSlabsSteps) return state;
                if (player == null) return state;
                VerticalStepBlock genericBlock = CommonBlocks.VERTICAL_STEP;
                if (genericBlock == null) return state;
                int idx = QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, pos);
                return genericBlock.defaultBlockState()
                        .setValue(VerticalStepBlock.FACING, QuadVerticalStepBlock.facingForIndex(idx))
                        .setValue(VerticalStepBlock.SIDE, QuadVerticalStepBlock.sideForIndex(idx));
            }

            if (state != null && state.getBlock() instanceof SlabBlock) {
                if (!Config.enhancedSlabsKneeSlabMining) return state;
                if (!player.isShiftKeyDown()) return state; // Only trigger when crouching
                if (!EnhancedSlabHelper.isDoubleSlab(state)) return state;

                SlabType targetedHalf = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos);
                return EnhancedSlabHelper.getHalfSlabState(state, targetedHalf);
            }
        } catch (Throwable ignored) {
        }
        return state;
    }

    private static boolean bitsandbalance$isEnhancedInvisibleBreakState(BlockState state) {
        return state.getBlock() instanceof MixedSlabBlock
                || state.getBlock() instanceof VerticalSlabBlock
                || state.getBlock() instanceof StepBlock
                || state.getBlock() instanceof QuadStepBlock
                || state.getBlock() instanceof VerticalStepBlock
                || state.getBlock() instanceof QuadVerticalStepBlock;
    }
}
