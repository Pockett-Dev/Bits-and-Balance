package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixedSlabClientDestroyBlockMixin {

    @Shadow @Final private Minecraft minecraft;

    @Redirect(
            method = "destroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;onDestroyedByPlayer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/world/level/material/FluidState;)Z"
            ),
            require = 0
    )
    private boolean bitsandbalance$neoForgeAvoidEnhancedSlabAirFlash(
            BlockState state, Level level, BlockPos pos, Player player,
            ItemStack tool, boolean willHarvest, FluidState fluid) {
        if (!Config.enableEnhancedSlabs) {
            return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
        }

        if (!(level instanceof ClientLevel clientLevel)) {
            return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
        }

        BlockState current = clientLevel.getBlockState(pos);
        final int flags = 11;

        if (Config.enhancedSlabsSteps && minecraft.player != null) {
            try {
                if (current.getBlock() instanceof QuadStepBlock) {
                    BlockEntity blockEntity = clientLevel.getBlockEntity(pos);
                    if (blockEntity instanceof QuadStepBlockEntity quad
                            && (minecraft.player.isShiftKeyDown() || quad.bitsandbalance$requiresKneeslabBreaking())) {
                        int idx = QuadStepBlock.bitsandbalance$getTargetedIndex(
                                minecraft.player,
                                pos,
                                current.getValue(QuadStepBlock.AXIS)
                        );
                        idx = QuadStepBlock.bitsandbalance$resolveOccupiedIndex(quad, idx);
                        if (idx >= 0 && idx < 4 && quad.getSlabAt(idx) != null) {
                            BlockState selected = quad.getSlabAt(idx);
                            for (int i = 0; i < 4; i++) {
                                BlockState slab = quad.getSlabAt(i);
                                if (slab == null) {
                                    continue;
                                }
                                if (minecraft.player.isShiftKeyDown()) {
                                    if (i == idx) {
                                        quad.bitsandbalance$setSlabAtSilent(i, null);
                                    }
                                } else if (selected.equals(slab)) {
                                    quad.bitsandbalance$setSlabAtSilent(i, null);
                                }
                            }
                            if (quad.getCount() > 1) {
                                clientLevel.sendBlockUpdated(pos, current, current, flags);
                                return true;
                            }
                            if (quad.getCount() == 1) {
                                QuadStepBlock.CachedBreak cached = new QuadStepBlock.CachedBreak();
                                cached.posLong = pos.asLong();
                                cached.remainingSlabs = quad.getAllSlabs();
                                cached.waterlogged = current.getValue(QuadStepBlock.WATERLOGGED);
                                cached.axis = current.getValue(QuadStepBlock.AXIS);
                                cached.targetedIndex = idx;
                                QuadStepBlock.bitsandbalance$restoreRemaining(clientLevel, pos, cached);
                                return true;
                            }
                        }
                    }
                } else if (current.getBlock() instanceof QuadVerticalStepBlock) {
                    BlockEntity blockEntity = clientLevel.getBlockEntity(pos);
                    if (blockEntity instanceof QuadVerticalStepBlockEntity quad
                            && (minecraft.player.isShiftKeyDown() || quad.bitsandbalance$requiresKneeslabBreaking())) {
                        int idx = QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(minecraft.player, pos);
                        idx = QuadVerticalStepBlock.bitsandbalance$resolveOccupiedIndex(quad, idx);
                        if (idx >= 0 && idx < 4 && quad.getSlabAt(idx) != null) {
                            BlockState selected = quad.getSlabAt(idx);
                            for (int i = 0; i < 4; i++) {
                                BlockState slab = quad.getSlabAt(i);
                                if (slab == null) {
                                    continue;
                                }
                                if (minecraft.player.isShiftKeyDown()) {
                                    if (i == idx) {
                                        quad.bitsandbalance$setSlabAtSilent(i, null);
                                    }
                                } else if (selected.equals(slab)) {
                                    quad.bitsandbalance$setSlabAtSilent(i, null);
                                }
                            }
                            if (quad.getCount() > 1) {
                                clientLevel.sendBlockUpdated(pos, current, current, flags);
                                return true;
                            }
                            if (quad.getCount() == 1) {
                                QuadVerticalStepBlock.CachedBreak cached = new QuadVerticalStepBlock.CachedBreak();
                                cached.posLong = pos.asLong();
                                cached.remainingSlabs = quad.getAllSlabs();
                                cached.waterlogged = current.getValue(QuadVerticalStepBlock.WATERLOGGED);
                                cached.targetedIndex = idx;
                                QuadVerticalStepBlock.bitsandbalance$restoreRemaining(clientLevel, pos, cached);
                                return true;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        SlabType targetedHalf = null;
        if (minecraft.hitResult instanceof BlockHitResult blockHitResult && pos.equals(blockHitResult.getBlockPos())) {
            targetedHalf = EnhancedSlabHelper.getTargetedHalf(pos, blockHitResult.getLocation());
        } else if (minecraft.player != null) {
            targetedHalf = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(minecraft.player, pos);
        }

        if (targetedHalf == null) {
            return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
        }

        if (Config.enhancedSlabsMixedDoubleSlabs && current.getBlock() instanceof MixedSlabBlock) {
            BlockEntity blockEntity = clientLevel.getBlockEntity(pos);
            if (!(blockEntity instanceof MixedSlabBlockEntity mixedSlab)) {
                return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
            }

            BlockState remaining = targetedHalf == SlabType.TOP ? mixedSlab.getBottomSlab() : mixedSlab.getTopSlab();
            if (remaining == null) {
                return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
            }

            if (current.hasProperty(MixedSlabBlock.WATERLOGGED)
                    && current.getValue(MixedSlabBlock.WATERLOGGED)
                    && remaining.hasProperty(SlabBlock.WATERLOGGED)) {
                remaining = remaining.setValue(SlabBlock.WATERLOGGED, true);
            }

            return level.setBlock(pos, remaining, flags);
        }

        if (Config.enhancedSlabsVerticalSlabs && current.getBlock() instanceof VerticalSlabBlock) {
            if (!current.getValue(VerticalSlabBlock.DOUBLE)) {
                return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
            }

            BlockEntity blockEntity = clientLevel.getBlockEntity(pos);
            if (!(blockEntity instanceof VerticalSlabBlockEntity verticalSlab)) {
                return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
            }

            if (!VerticalSlabBlock.bitsandbalance$shouldTargetHalf(minecraft.player, current, verticalSlab)) {
                return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
            }

            var facing = current.getValue(VerticalSlabBlock.FACING);
            net.minecraft.core.Direction targetedDir;
            if (minecraft.hitResult instanceof BlockHitResult blockHitResult && pos.equals(blockHitResult.getBlockPos())) {
                targetedDir = VerticalSlabBlock.bitsandbalance$getTargetedHalf(pos, blockHitResult.getLocation(), facing);
            } else if (minecraft.player != null) {
                targetedDir = VerticalSlabBlock.bitsandbalance$getTargetedHalf(minecraft.player, pos, facing);
            } else {
                return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
            }

            BlockState remainingSlab = (targetedDir == facing) ? verticalSlab.getOppositeSlab() : verticalSlab.getFacingSlab();
            if (remainingSlab == null) {
                return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
            }

            net.minecraft.core.Direction remainingFacing = (targetedDir == facing) ? facing.getOpposite() : facing;
            BlockState remainingVertical = ((VerticalSlabBlock) current.getBlock()).defaultBlockState()
                    .setValue(VerticalSlabBlock.WATERLOGGED, current.getValue(VerticalSlabBlock.WATERLOGGED))
                    .setValue(VerticalSlabBlock.FACING, remainingFacing)
                    .setValue(VerticalSlabBlock.DOUBLE, false);

            boolean ok = level.setBlock(pos, remainingVertical, flags);
            BlockEntity newBlockEntity = clientLevel.getBlockEntity(pos);
            if (newBlockEntity instanceof VerticalSlabBlockEntity newVerticalSlab) {
                newVerticalSlab.setSingleFacingSlab(remainingSlab);
            }
            return ok;
        }

        if (Config.enhancedSlabsSteps
                && (current.getBlock() instanceof StepBlock || current.getBlock() instanceof VerticalStepBlock)) {
            return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
        }

        if (Config.enhancedSlabsKneeSlabMining
                && current.getBlock() instanceof SlabBlock
                && EnhancedSlabHelper.isDoubleSlab(current)) {
            BlockState remaining = EnhancedSlabHelper.getRemainingHalfState(current, targetedHalf);
            return level.setBlock(pos, remaining, flags);
        }

        return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
    }
}