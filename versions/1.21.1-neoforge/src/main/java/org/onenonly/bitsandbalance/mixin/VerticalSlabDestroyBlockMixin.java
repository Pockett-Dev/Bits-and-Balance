package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.compat.SableCompat;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabStorageOptimizer;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes vertical slab half-breaking flicker by avoiding the intermediate AIR state.
 */
@Mixin(net.minecraft.server.level.ServerPlayerGameMode.class)
public abstract class VerticalSlabDestroyBlockMixin {

    @Shadow @Final protected ServerPlayer player;

    @Shadow protected ServerLevel level;

    @Inject(
        method = "destroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"
        ),
        cancellable = true,
        require = 0
    )
    private void bitsandbalance$destroyVerticalSlabNoFlash(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (bitsandbalance$tryDestroyVerticalSlabNoFlash(pos, true)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "removeBlock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bitsandbalance$neoForgeDestroyVerticalSlabNoFlash(BlockPos pos,
                                                                   BlockState adjustedState,
                                                                   boolean canHarvest,
                                                                   CallbackInfoReturnable<Boolean> cir) {
        if (bitsandbalance$tryDestroyVerticalSlabNoFlash(pos, false)) {
            cir.setReturnValue(false);
        }
    }

    private boolean bitsandbalance$tryDestroyVerticalSlabNoFlash(BlockPos pos, boolean applyToolDamage) {
        if (!Config.enableEnhancedSlabs) return false;
        if (!Config.enhancedSlabsVerticalSlabs) return false;

        BlockState verticalState = level.getBlockState(pos);
        if (!(verticalState.getBlock() instanceof VerticalSlabBlock)) return false;
        if (!verticalState.getValue(VerticalSlabBlock.DOUBLE)) return false;

        BlockState mined = VerticalSlabBlock.bitsandbalance$peekCachedMinedSlab(pos);
        BlockState remainingSlab = VerticalSlabBlock.bitsandbalance$peekCachedRemainingSlab(pos);
        BlockState remainingState = VerticalSlabBlock.bitsandbalance$peekCachedRemainingState(pos);

        if (mined == null || remainingSlab == null || remainingState == null) {
            BlockState[] stored = EnhancedSlabMaterialSavedData.get(level, pos.asLong());
            if (stored != null && VerticalSlabBlock.bitsandbalance$shouldTargetHalf(player, verticalState, null, stored)) {
                Direction facing = verticalState.getValue(VerticalSlabBlock.FACING);
                Direction targeted = VerticalSlabBlock.bitsandbalance$getTargetedHalf(player, pos, facing);
                mined = targeted == facing ? bitsandbalance$verticalSlot(stored, 0) : bitsandbalance$verticalSlot(stored, 1);
                remainingSlab = targeted == facing ? bitsandbalance$verticalSlot(stored, 1) : bitsandbalance$verticalSlot(stored, 0);
                Direction remainingFacing = targeted == facing ? facing.getOpposite() : facing;
                remainingState = bitsandbalance$remainingState(verticalState, remainingSlab, remainingFacing);
            }
        }

        if (mined == null || remainingSlab == null || remainingState == null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VerticalSlabBlockEntity vbe
                    && VerticalSlabBlock.bitsandbalance$shouldTargetHalf(player, verticalState, vbe)) {
                Direction facing = verticalState.getValue(VerticalSlabBlock.FACING);
                Direction targeted = VerticalSlabBlock.bitsandbalance$getTargetedHalf(player, pos, facing);
                mined = targeted == facing ? vbe.getFacingSlab() : vbe.getOppositeSlab();
                remainingSlab = targeted == facing ? vbe.getOppositeSlab() : vbe.getFacingSlab();
                Direction remainingFacing = targeted == facing ? facing.getOpposite() : facing;
                remainingState = bitsandbalance$remainingState(verticalState, remainingSlab, remainingFacing);
            }
        }

        if (mined == null || remainingSlab == null || remainingState == null) return false;

        level.setBlock(pos, remainingState, Block.UPDATE_ALL);

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VerticalSlabBlockEntity vbe) {
            vbe.setSingleFacingSlab(remainingSlab);
        }
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), remainingSlab);
        EnhancedSlabStorageOptimizer.optimizeAt(level, pos);
        SableCompat.notifyEnhancedSlabPartialRemoval(level, pos);
        level.destroyBlockProgress(player.getId(), pos, -1);

        if (!player.isCreative()) {
            ItemStack tool = player.getMainHandItem();
            if (applyToolDamage) {
                try {
                    tool.mineBlock(level, mined, pos, player);
                } catch (Throwable ignored) {
                }
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

        VerticalSlabBlock.bitsandbalance$clearCachedBreak();
        return true;
    }

    private static BlockState bitsandbalance$verticalSlot(BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    private static BlockState bitsandbalance$remainingState(BlockState originalState, BlockState remainingSlab, Direction remainingFacing) {
        return bitsandbalance$ownerBlockForSingle(originalState, remainingSlab).defaultBlockState()
                .setValue(VerticalSlabBlock.WATERLOGGED, originalState.getValue(VerticalSlabBlock.WATERLOGGED))
                .setValue(VerticalSlabBlock.FACING, remainingFacing)
                .setValue(VerticalSlabBlock.DOUBLE, false);
    }

    private static VerticalSlabBlock bitsandbalance$ownerBlockForSingle(BlockState originalState, BlockState slabState) {
        if (slabState != null) {
            Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(slabState.getBlock());
            if (verticalBlock instanceof VerticalSlabBlock verticalSlab) {
                return verticalSlab;
            }
        }
        return originalState.getBlock() instanceof VerticalSlabBlock verticalSlab ? verticalSlab : CommonBlocks.VERTICAL_SLAB;
    }
}
