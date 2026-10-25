package org.onenonly.bitsandbalance.mixin.sable;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps Sable's state-only solidity cache from rejecting storage-backed enhanced slabs.
 *
 * <p>Sable memoizes this probe by {@link BlockState} and evaluates collision at
 * {@code BlockPos.ZERO}. Quad Step occupancy is stored by world position, so the probe can see an
 * empty shape and cache that result for every matching Step state. Intercepting the position-aware
 * outer method lets valid occupied containers participate in mass tracking without treating a
 * genuinely empty or not-yet-restored container as solid.</p>
 */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState", remap = false)
public abstract class SableVoxelNeighborhoodEnhancedSlabSolidityMixin {
    @Inject(
            method = "isSolid(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void bitsandbalance$usePositionAwareEnhancedSlabSolidity(BlockGetter level,
                                                              BlockPos pos,
                                                              BlockState state,
                                                              CallbackInfoReturnable<Boolean> cir) {
        if (!EnhancedSlabCreateCompat.supports(state) || level == null || pos == null) {
            return;
        }

        VoxelShape shape = EnhancedSlabCreateCompat.getContraptionCollisionShape(level, level, pos, state);
        if (shape.isEmpty()) {
            try {
                shape = state.getCollisionShape(level, pos, CollisionContext.empty());
            } catch (RuntimeException ignored) {
                shape = Shapes.empty();
            }
        }
        cir.setReturnValue(!shape.isEmpty());
    }
}
