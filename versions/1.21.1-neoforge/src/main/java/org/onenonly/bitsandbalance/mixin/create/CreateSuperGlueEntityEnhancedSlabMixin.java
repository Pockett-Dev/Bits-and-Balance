package org.onenonly.bitsandbalance.mixin.create;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.glue.SuperGlueEntity", remap = false)
public abstract class CreateSuperGlueEntityEnhancedSlabMixin {
    @Inject(method = "isValidFace", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bitsandbalance$allowEnhancedSlabGlueFaces(Level level,
                                                                  BlockPos pos,
                                                                  Direction direction,
                                                                  CallbackInfoReturnable<Boolean> cir) {
        BlockState state = level.getBlockState(pos);
        if (EnhancedSlabCreateCompat.supportsMovement(state)) {
            cir.setReturnValue(true);
        }
    }
}
