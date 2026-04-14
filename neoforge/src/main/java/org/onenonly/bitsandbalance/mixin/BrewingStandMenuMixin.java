package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.potions.CustomBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.onenonly.bitsandbalance.Config;
/**
 * Ensure Wither Rose is treated as a valid brewing ingredient by the brewing menu checks as well.
 */
@Mixin(BrewingStandMenu.class)
public abstract class BrewingStandMenuMixin {

    @Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true)
    private static void rebalance$acceptRuleReagents(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!Config.enableWitheringPotion) return;
            if (stack == null || stack.isEmpty()) return;
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null) return;
            for (var rule : CustomBrewing.rules()) {
                if (rule.reagentItem().equals(id)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // fail-safe
        }
    }
}
