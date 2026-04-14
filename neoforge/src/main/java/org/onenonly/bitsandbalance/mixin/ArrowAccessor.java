package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Mixin accessor to access protected methods in AbstractArrow.
 */
@Mixin(AbstractArrow.class)
public interface ArrowAccessor {
    
    @Invoker("getPickupItem")
    ItemStack invokeGetPickupItem();
}
