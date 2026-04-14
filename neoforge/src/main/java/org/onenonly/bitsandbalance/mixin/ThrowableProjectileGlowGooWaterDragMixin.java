package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.onenonly.bitsandbalance.common.entity.GlowGooProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileGlowGooWaterDragMixin {
    private static final float REDUCED_WATER_INERTIA = 0.98F;

    @ModifyConstant(method = "applyInertia", constant = @Constant(floatValue = 0.8F))
    private float bitsandbalance$reduceGlowGooWaterDrag(float original) {
        if ((Object) this instanceof GlowGooProjectile) {
            return REDUCED_WATER_INERTIA;
        }
        return original;
    }
}