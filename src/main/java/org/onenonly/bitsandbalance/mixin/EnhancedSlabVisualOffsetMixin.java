package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — Outline/Selection Shape Offset
 *
 * Shifts the outline (selection wireframe) and interaction ray-trace shape of
 * blocks that sit on a bottom slab or hang below a top slab, so that the
 * wireframe and click target match the visually-offset model position.
 *
 * <p>Collision shapes are intentionally <b>NOT</b> offset — doing so causes
 * players to clip into adjacent full blocks when walking onto them from the
 * slab surface (step-up exceeds MC's 0.6 step height).
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class EnhancedSlabVisualOffsetMixin {

    @Inject(
        method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void bitsandbalance$offsetOutlineShape(
            BlockGetter level, BlockPos pos, CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir) {

        if (!Config.enableEnhancedSlabs) return;

        BlockState state = (BlockState) (Object) this;
        double yOff = EnhancedSlabHelper.getVisualYOffset(level, pos, state);
        if (yOff != 0.0) {
            cir.setReturnValue(cir.getReturnValue().move(0.0, yOff, 0.0));
        }
    }
}
