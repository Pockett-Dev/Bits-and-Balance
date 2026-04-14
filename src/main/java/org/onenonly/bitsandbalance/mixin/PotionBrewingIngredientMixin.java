package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.onenonly.bitsandbalance.potions.CustomBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ensures custom brewing reagents are treated as valid brewing ingredients.
 */
@Mixin(PotionBrewing.class)
public abstract class PotionBrewingIngredientMixin {

    @Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true)
    private static void rebalance$allowWitherRose(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (CustomBrewing.rules().isEmpty()) return;
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
            // Fail-safe: never break vanilla behavior if something goes wrong
        }
    }
}
