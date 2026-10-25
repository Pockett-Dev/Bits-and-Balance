package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sugarcane grows 25% faster when planted on sand instead of grass/dirt.
 */
@Mixin(SugarCaneBlock.class)
public class SugarCaneBlockMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!FabricTweaksConfig.enableSugarcaneSand) return;

        // Find base of sugarcane
        BlockPos basePos = pos;
        while (level.getBlockState(basePos.below()).getBlock() instanceof SugarCaneBlock) {
            basePos = basePos.below();
        }

        Block belowBlock = level.getBlockState(basePos.below()).getBlock();
        if (belowBlock != Blocks.SAND && belowBlock != Blocks.RED_SAND) return;

        if (!level.isEmptyBlock(pos.above())) return;

        // 1/12 instead of 1/15 (25% faster)
        if (random.nextInt(12) != 0) return;

        int height = 0;
        BlockPos currentPos = basePos;
        while (level.getBlockState(currentPos).getBlock() instanceof SugarCaneBlock) {
            height++;
            currentPos = currentPos.above();
        }

        if (height < 3) {
            level.setBlockAndUpdate(pos.above(), state);
            ci.cancel();
        }
    }
}
