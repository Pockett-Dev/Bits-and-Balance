package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.onenonly.bitsandbalance.Config;
/**
 * Tweaks: Responsive Shields
 * Removes or reduces shield blocking delay by allowing configuration of shield raise time.
 * 
 * This mixin tracks shield usage timing and prevents blocking until the configured
 * raise time has elapsed.
 */
@Mixin(LivingEntity.class)
public abstract class ResponsiveShieldsMixin {

    @Inject(method = "getItemBlockingWith", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$getItemBlockingWith(CallbackInfoReturnable<ItemStack> cir) {
        if (!Config.enableResponsiveShields) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.isUsingItem()) {
            return;
        }

        ItemStack useItem = self.getUseItem();
        if (useItem.isEmpty()) {
            return;
        }

        BlocksAttacks blocksAttacks = useItem.get(DataComponents.BLOCKS_ATTACKS);
        if (blocksAttacks == null) {
            return;
        }

        int configuredDelay = Math.max(0, Config.shieldRaiseTime);
        int ticksUsing = useItem.getItem().getUseDuration(useItem, self) - self.getUseItemRemainingTicks();
        if (ticksUsing >= configuredDelay) {
            cir.setReturnValue(useItem);
        } else {
            cir.setReturnValue(null);
        }
    }
}
