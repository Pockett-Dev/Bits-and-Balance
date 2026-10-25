package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.compat.SableCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes MixedSlab half-breaking flicker by avoiding the intermediate AIR state.
 *
 * Vanilla destroyBlock does: playerWillDestroy -> removeBlock(AIR) -> destroy(...)
 * For mixed slabs, we want to replace directly with the remaining slab half.
 */
@Mixin(net.minecraft.server.level.ServerPlayerGameMode.class)
public abstract class MixedSlabDestroyBlockMixin {

    @Shadow @Final protected ServerPlayer player;

    @Shadow protected ServerLevel level;

    @Inject(
        method = "destroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"
        ),
        cancellable = true
    )
    private void bitsandbalance$destroyMixedSlabNoFlash(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableEnhancedSlabs) return;
        if (!Config.enhancedSlabsMixedDoubleSlabs) return;

        BlockState mixedState = level.getBlockState(pos);
        if (!(mixedState.getBlock() instanceof MixedSlabBlock)) return;

        BlockState mined = MixedSlabBlock.bitsandbalance$peekCachedMinedSlab(pos);
        BlockState remaining = MixedSlabBlock.bitsandbalance$peekCachedRemainingSlab(pos);

        // Fallback if caching didn't happen for some reason.
        if (mined == null || remaining == null) {
            SlabType half = MixedSlabBlock.bitsandbalance$getTargetedHalf(player, pos);
            BlockState[] stored = EnhancedSlabMaterialSavedData.get(level, pos.asLong());
            mined = half == SlabType.TOP ? bitsandbalance$slot(stored, 1) : bitsandbalance$slot(stored, 0);
            remaining = half == SlabType.TOP ? bitsandbalance$slot(stored, 0) : bitsandbalance$slot(stored, 1);
        }

        if (mined == null || remaining == null) return;

        SlabType brokenHalf = MixedSlabBlock.bitsandbalance$getTargetedHalf(player, pos);
        SlabType remainingHalf = brokenHalf == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP;
        boolean waterlogged = mixedState.hasProperty(MixedSlabBlock.WATERLOGGED)
                && mixedState.getValue(MixedSlabBlock.WATERLOGGED);
        remaining = bitsandbalance$singleSlabHalf(remaining, remainingHalf, waterlogged);

        level.setBlock(pos, remaining, Block.UPDATE_ALL);
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong());
        SableCompat.notifyEnhancedSlabPartialRemoval(level, pos);

        if (!player.isCreative()) {
            ItemStack tool = player.getMainHandItem();

            // Tool durability should reflect the slab half mined.
            try {
                tool.mineBlock(level, mined, pos, player);
            } catch (Throwable ignored) {
            }

            boolean correctTool = !mined.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(mined);
            if (correctTool) {
                Block.dropResources(mined, level, pos, null, player, tool);
            }

            player.awardStat(Stats.BLOCK_MINED.get(mined.getBlock()));
            player.causeFoodExhaustion(0.005F);
        }

        MixedSlabBlock.bitsandbalance$clearCachedBreak();
        cir.setReturnValue(true);
    }

    private static BlockState bitsandbalance$slot(BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    private static BlockState bitsandbalance$singleSlabHalf(BlockState slab, SlabType half, boolean waterlogged) {
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
