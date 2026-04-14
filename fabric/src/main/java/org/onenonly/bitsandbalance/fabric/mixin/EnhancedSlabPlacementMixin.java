package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — Placement (Fabric)
 *
 * Reserved mixin for future slab placement enhancements.
 * See NeoForge {@code EnhancedSlabPlacementMixin} for rationale.
 */
@Mixin(SlabBlock.class)
public abstract class EnhancedSlabPlacementMixin {

    @Inject(method = "useShapeForLightOcclusion", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$useShapeForLightOcclusion(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        // Intentionally empty — reserved for future use.
    }
}
