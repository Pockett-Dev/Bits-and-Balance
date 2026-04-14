package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.onenonly.bitsandbalance.Config;
/**
 * Harden acceptance of Wither Rose as a brewing ingredient by also allowing it at the slot level.
 * This complements the PotionBrewing.isIngredient mixin and guarantees the UI accepts the item.
 */
@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$IngredientSlot")
public abstract class BrewingStandIngredientSlotMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void rebalance$allowWitherRose(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (Config.enableWitheringPotion && stack != null && stack.is(Items.WITHER_ROSE)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable ignored) {
            // Never break vanilla if something goes wrong
        }
    }
}
