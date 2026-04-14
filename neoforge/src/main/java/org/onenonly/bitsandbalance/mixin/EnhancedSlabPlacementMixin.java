package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — Placement
 *
 * Reserved mixin for future slab placement enhancements.
 *
 * Mixed-type double slabs (e.g. oak + stone in one block) are NOT supported because
 * vanilla {@link SlabBlock} can only represent a single block type.  Implementing true
 * mixed double slabs would require a custom {@code BlockEntity} to store the second
 * slab's identity, plus a custom {@code BlockEntityRenderer} — essentially the scope
 * of a dedicated mod like Double Slabs.
 *
 * <p>Support-surface behaviour (placing items on top of bottom slabs, hanging items
 * below top slabs) is handled by {@link EnhancedSlabSupportSurfaceMixin}.</p>
 */
@Mixin(SlabBlock.class)
public abstract class EnhancedSlabPlacementMixin {

    /**
     * No-op hook point for future placement enhancements.
     * Support surfaces are handled by the separate
     * {@link EnhancedSlabSupportSurfaceMixin} on {@code BlockStateBase.isFaceSturdy}.
     */
    @Inject(method = "useShapeForLightOcclusion", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$useShapeForLightOcclusion(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        // Intentionally empty — reserved for future use.
    }
}
