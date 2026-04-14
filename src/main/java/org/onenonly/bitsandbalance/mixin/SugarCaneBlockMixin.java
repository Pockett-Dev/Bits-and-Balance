package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to modify sugarcane growth speed based on the substrate it's planted on.
 * Sugarcane grows 25% faster when planted on sand instead of grass/dirt.
 */
@Mixin(net.minecraft.world.level.block.SugarCaneBlock.class)
public class SugarCaneBlockMixin {
    
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void onRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!Config.enableSugarcaneSand) return;
        
        // Find the base of the sugarcane (the bottom block)
        BlockPos basePos = pos;
        while (level.getBlockState(basePos.below()).getBlock() instanceof SugarCaneBlock) {
            basePos = basePos.below();
        }
        
        // Check if the base block is planted on sand
        Block belowBlock = level.getBlockState(basePos.below()).getBlock();
        
        // If planted on sand, implement faster growth
        if (belowBlock == Blocks.SAND || belowBlock == Blocks.RED_SAND) {
            // Check vanilla growth conditions
            if (level.isEmptyBlock(pos.above())) {
                // Use faster growth rate: 1/12 instead of 1/15 (25% faster)
                if (random.nextInt(12) == 0) {
                    // Manually implement the growth logic
                    int height = 0;
                    BlockPos currentPos = basePos;
                    while (level.getBlockState(currentPos).getBlock() instanceof SugarCaneBlock) {
                        height++;
                        currentPos = currentPos.above();
                    }
                    
                    if (height < 3) {
                        level.setBlockAndUpdate(pos.above(), state);
                        ci.cancel(); // Cancel vanilla processing since we handled it
                    }
                }
            }
        }
    }
}