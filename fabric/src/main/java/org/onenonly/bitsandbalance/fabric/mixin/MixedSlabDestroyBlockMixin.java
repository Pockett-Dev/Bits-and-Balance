package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes MixedSlab half-breaking flicker by avoiding the intermediate AIR state.
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
        if (!FabricTweaksConfig.enableEnhancedSlabs) return;
        if (!FabricTweaksConfig.enhancedSlabsMixedDoubleSlabs) return;

        BlockState mixedState = level.getBlockState(pos);
        if (!(mixedState.getBlock() instanceof MixedSlabBlock)) return;

        BlockState mined = MixedSlabBlock.bitsandbalance$peekCachedMinedSlab(pos);
        BlockState remaining = MixedSlabBlock.bitsandbalance$peekCachedRemainingSlab(pos);

        if (mined == null || remaining == null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MixedSlabBlockEntity mixedBe) {
                SlabType half = MixedSlabBlock.bitsandbalance$getTargetedHalf(player, pos);
                mined = half == SlabType.TOP ? mixedBe.getTopSlab() : mixedBe.getBottomSlab();
                remaining = half == SlabType.TOP ? mixedBe.getBottomSlab() : mixedBe.getTopSlab();
            }
        }

        if (mined == null || remaining == null) return;

        level.setBlock(pos, remaining, Block.UPDATE_ALL);

        if (!player.isCreative()) {
            ItemStack tool = player.getMainHandItem();
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
}
