package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Responsive Shields
 * Allows configuring shield raise time by controlling when an item counts as blocking.
 */
@Mixin(LivingEntity.class)
public abstract class ResponsiveShieldsMixin {

    @Inject(method = "getItemBlockingWith", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$getItemBlockingWith(CallbackInfoReturnable<ItemStack> cir) {
        if (!FabricTweaksConfig.enableResponsiveShields) {
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

        int configuredDelay = Math.max(0, FabricTweaksConfig.shieldRaiseTime);
        int ticksUsing = useItem.getItem().getUseDuration(useItem, self) - self.getUseItemRemainingTicks();
        if (ticksUsing >= configuredDelay) {
            cir.setReturnValue(useItem);
        } else {
            cir.setReturnValue(null);
        }
    }
}
