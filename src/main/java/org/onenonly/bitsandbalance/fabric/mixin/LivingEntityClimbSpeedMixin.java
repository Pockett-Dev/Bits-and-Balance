package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Preserve vanilla onClimbable behavior to maintain ladder friction/stickiness.
 * Fast downward speed is applied elsewhere (see LivingEntityTravelTailMixin).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityClimbSpeedMixin {

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$preserveVanillaClimbable(CallbackInfoReturnable<Boolean> cir) {
        // Intentionally no-op: keep vanilla onClimbable.
    }

    private static boolean isLadder(BlockState state) {
        return state.getBlock() instanceof LadderBlock;
    }

    @SuppressWarnings("unused")
    private static boolean isNearLadder(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockPos[] positions = new BlockPos[]{
                pos, above,
                pos.north(), above.north(),
                pos.south(), above.south(),
                pos.east(), above.east(),
                pos.west(), above.west()
        };
        for (BlockPos p : positions) {
            if (isLadder(level.getBlockState(p))) return true;
        }
        return false;
    }
}
