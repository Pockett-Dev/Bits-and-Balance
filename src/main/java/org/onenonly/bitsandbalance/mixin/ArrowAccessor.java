package org.onenonly.bitsandbalance.mixin;

//? if mc == 1.21.1
import net.minecraft.world.entity.projectile.AbstractArrow;
//? endif
//? if mc != 1.21.1
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
//? endif
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
