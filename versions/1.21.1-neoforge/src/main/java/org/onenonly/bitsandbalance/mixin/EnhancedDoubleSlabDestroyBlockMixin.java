package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabMiningTarget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — KneeSlab-like Mining (vanilla double slabs)
 *
 * Runs in {@code ServerPlayerGameMode.destroyBlock} before vanilla removes the block,
 * so we can replace a DOUBLE slab with the remaining half without an intermediate AIR
 * state (no flicker). Also works in creative (no drops).
 */
@Mixin(net.minecraft.server.level.ServerPlayerGameMode.class)
public abstract class EnhancedDoubleSlabDestroyBlockMixin {

    @Shadow @Final protected ServerPlayer player;

    @Shadow protected ServerLevel level;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void bitsandbalance$lockEnhancedSlabMiningTarget(BlockPos pos,
                                                             ServerboundPlayerActionPacket.Action action,
                                                             Direction direction,
                                                             int maxBuildHeight,
                                                             int sequence,
                                                             org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
            EnhancedSlabMiningTarget.clear(player);
            return;
        }

        if (action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            return;
        }

        if (!Config.enableEnhancedSlabs) {
            EnhancedSlabMiningTarget.clear(player);
            return;
        }

        BlockState state = level.getBlockState(pos);
        boolean vanillaDoubleSlab = Config.enhancedSlabsKneeSlabMining
                && state.getBlock() instanceof SlabBlock
                && EnhancedSlabHelper.isDoubleSlab(state);
        boolean mixedSlab = Config.enhancedSlabsMixedDoubleSlabs
                && state.getBlock() instanceof MixedSlabBlock;

        if ((vanillaDoubleSlab || mixedSlab) && (player.isShiftKeyDown() || player.isCreative())) {
            EnhancedSlabMiningTarget.lock(player, pos, EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos));
        } else {
            EnhancedSlabMiningTarget.clear(player);
        }
    }

    @Inject(
        method = "destroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"
        ),
        cancellable = true
    )
    private void bitsandbalance$destroyVanillaDoubleSlabHalf(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = level.getBlockState(pos);
        if (!bitsandbalance$tryDestroyVanillaDoubleSlabHalf(pos, state)) return;
        cir.setReturnValue(true);
    }

    @Redirect(
        method = "destroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;removeBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Z"
        ),
        require = 1
    )
    private boolean bitsandbalance$neoForgeDestroyVanillaDoubleSlabHalf(
            net.minecraft.server.level.ServerPlayerGameMode gameMode,
            BlockPos pos,
            BlockState adjustedState,
            boolean canHarvest) {
        if (bitsandbalance$tryDestroyVerticalSlabHalfNoFlash(pos, level.getBlockState(pos))) {
            return true;
        }

        if (bitsandbalance$tryDestroyMixedSlabNoFlash(pos, level.getBlockState(pos))) {
            return true;
        }

        if (bitsandbalance$tryDestroyVanillaDoubleSlabHalf(pos, level.getBlockState(pos))) {
            return true;
        }

        boolean removed = adjustedState.onDestroyedByPlayer(level, pos, player, canHarvest, level.getFluidState(pos));
        if (removed) {
            adjustedState.getBlock().destroy(level, pos, adjustedState);
        }
        return removed;
    }

    private boolean bitsandbalance$tryDestroyVanillaDoubleSlabHalf(BlockPos pos, BlockState state) {
        if (!Config.enableEnhancedSlabs) return false;
        if (!Config.enhancedSlabsKneeSlabMining) return false;
        if (!player.isCreative() && !player.isShiftKeyDown()) return false;
        if (!(state.getBlock() instanceof SlabBlock)) return false;
        if (!EnhancedSlabHelper.isDoubleSlab(state)) return false;

        SlabType targetedHalf = EnhancedSlabMiningTarget.get(player, pos);
        if (targetedHalf == null) {
            targetedHalf = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos);
        }
        BlockState minedHalfState = EnhancedSlabHelper.getHalfSlabState(state, targetedHalf);
        BlockState remainingState = EnhancedSlabHelper.getRemainingHalfState(state, targetedHalf);

        level.setBlock(pos, remainingState, Block.UPDATE_ALL);
        level.destroyBlockProgress(player.getId(), pos, -1);

        if (!player.isCreative()) {
            ItemStack tool = player.getMainHandItem();

            try {
                tool.mineBlock(level, minedHalfState, pos, player);
            } catch (Throwable ignored) {
            }

            boolean correctTool = !minedHalfState.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(minedHalfState);
            if (correctTool) {
                Block.dropResources(minedHalfState, level, pos, null, player, tool);
            }

            player.awardStat(Stats.BLOCK_MINED.get(minedHalfState.getBlock()));
            player.causeFoodExhaustion(0.005F);
        }

        EnhancedSlabMiningTarget.clear(player);
        return true;
    }

    private boolean bitsandbalance$tryDestroyVerticalSlabHalfNoFlash(BlockPos pos, BlockState state) {
        if (!Config.enableEnhancedSlabs) return false;
        if (!Config.enhancedSlabsVerticalSlabs) return false;
        if (!(state.getBlock() instanceof VerticalSlabBlock)) return false;
        if (!state.getValue(VerticalSlabBlock.DOUBLE)) return false;

        BlockEntity be = level.getBlockEntity(pos);
        VerticalSlabBlockEntity verticalBe = be instanceof VerticalSlabBlockEntity verticalSlab ? verticalSlab : null;
        BlockState[] stored = verticalBe != null
                ? new BlockState[]{verticalBe.getFacingSlab(), verticalBe.getOppositeSlab()}
                : EnhancedSlabMaterialSavedData.get(level, pos.asLong());
        if (!VerticalSlabBlock.bitsandbalance$shouldTargetHalf(player, state, verticalBe, stored)) {
            return false;
        }

        Direction facing = state.getValue(VerticalSlabBlock.FACING);
        Direction targeted = VerticalSlabBlock.bitsandbalance$getTargetedHalf(player, pos, facing);
        boolean targetedFacingSide = targeted == facing;
        BlockState mined = bitsandbalance$mixedSlot(stored, targetedFacingSide ? 0 : 1);
        BlockState remaining = bitsandbalance$mixedSlot(stored, targetedFacingSide ? 1 : 0);
        if (mined == null || remaining == null) {
            return false;
        }

        Direction remainingDirection = targetedFacingSide ? facing.getOpposite() : facing;
        boolean waterlogged = state.hasProperty(VerticalSlabBlock.WATERLOGGED)
                && state.getValue(VerticalSlabBlock.WATERLOGGED);
        BlockState remainingState = state
                .setValue(VerticalSlabBlock.DOUBLE, false)
                .setValue(VerticalSlabBlock.FACING, remainingDirection)
                .setValue(VerticalSlabBlock.WATERLOGGED, waterlogged);

        level.setBlock(pos, remainingState, Block.UPDATE_ALL);
        BlockEntity updatedBe = level.getBlockEntity(pos);
        if (updatedBe instanceof VerticalSlabBlockEntity updatedVerticalSlab) {
            updatedVerticalSlab.setSingleFacingSlab(remaining);
        }
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), remaining);
        level.destroyBlockProgress(player.getId(), pos, -1);

        if (!player.isCreative()) {
            ItemStack tool = player.getMainHandItem();
            try {
                tool.mineBlock(level, mined, pos, player);
            } catch (Throwable ignored) {
            }

            boolean correctTool = !mined.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(mined);
            if (correctTool) {
                ItemStack drop = VerticalSlabBlock.bitsandbalance$makeHalfDropItem(mined);
                if (!drop.isEmpty()) {
                    Block.popResource(level, pos, drop);
                }
            }

            player.awardStat(Stats.BLOCK_MINED.get(mined.getBlock()));
            player.causeFoodExhaustion(0.005F);
        }

        EnhancedSlabMiningTarget.clear(player);
        return true;
    }

    private boolean bitsandbalance$tryDestroyMixedSlabNoFlash(BlockPos pos, BlockState mixedState) {
        if (!Config.enableEnhancedSlabs) return false;
        if (!Config.enhancedSlabsMixedDoubleSlabs) return false;
        if (!(mixedState.getBlock() instanceof MixedSlabBlock)) return false;

        BlockState mined = MixedSlabBlock.bitsandbalance$peekCachedMinedSlab(pos);
        BlockState remaining = MixedSlabBlock.bitsandbalance$peekCachedRemainingSlab(pos);

        SlabType lockedHalf = EnhancedSlabMiningTarget.get(player, pos);
        if (lockedHalf != null) {
            if (mined == null || remaining == null) {
                BlockState[] stored = EnhancedSlabMaterialSavedData.get(level, pos.asLong());
                mined = lockedHalf == SlabType.TOP ? bitsandbalance$mixedSlot(stored, 1) : bitsandbalance$mixedSlot(stored, 0);
                remaining = lockedHalf == SlabType.TOP ? bitsandbalance$mixedSlot(stored, 0) : bitsandbalance$mixedSlot(stored, 1);
            }
        }

        if (mined == null || remaining == null) {
            {
                SlabType half = MixedSlabBlock.bitsandbalance$getTargetedHalf(player, pos);
                BlockState[] stored = EnhancedSlabMaterialSavedData.get(level, pos.asLong());
                mined = half == SlabType.TOP ? bitsandbalance$mixedSlot(stored, 1) : bitsandbalance$mixedSlot(stored, 0);
                remaining = half == SlabType.TOP ? bitsandbalance$mixedSlot(stored, 0) : bitsandbalance$mixedSlot(stored, 1);
                lockedHalf = half;
            }
        }

        if (mined == null || remaining == null) return false;

        SlabType brokenHalf = lockedHalf != null
                ? lockedHalf
                : MixedSlabBlock.bitsandbalance$getTargetedHalf(player, pos);
        SlabType remainingHalf = brokenHalf == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP;
        boolean waterlogged = mixedState.hasProperty(MixedSlabBlock.WATERLOGGED)
                && mixedState.getValue(MixedSlabBlock.WATERLOGGED);
        BlockState remainingHalfState = bitsandbalance$mixedSingleSlabHalf(remaining, remainingHalf, waterlogged);

        level.setBlock(pos, remainingHalfState, Block.UPDATE_ALL);
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong());
        level.destroyBlockProgress(player.getId(), pos, -1);

        if (!player.isCreative()) {
            ItemStack tool = player.getMainHandItem();
            boolean correctTool = !mined.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(mined);
            if (correctTool) {
                Block.dropResources(mined, level, pos, null, player, tool);
            }

            player.awardStat(Stats.BLOCK_MINED.get(mined.getBlock()));
            player.causeFoodExhaustion(0.005F);
        }

        MixedSlabBlock.bitsandbalance$clearCachedBreak();
        EnhancedSlabMiningTarget.clear(player);
        return true;
    }

    private static BlockState bitsandbalance$mixedSlot(BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    private static BlockState bitsandbalance$mixedSingleSlabHalf(BlockState slab, SlabType half, boolean waterlogged) {
        BlockState result = slab;
        if (result.hasProperty(SlabBlock.TYPE)) {
            result = result.setValue(SlabBlock.TYPE, half);
        }
        if (result.hasProperty(SlabBlock.WATERLOGGED)) {
            result = result.setValue(SlabBlock.WATERLOGGED, waterlogged);
        }
        return result;
    }

}
