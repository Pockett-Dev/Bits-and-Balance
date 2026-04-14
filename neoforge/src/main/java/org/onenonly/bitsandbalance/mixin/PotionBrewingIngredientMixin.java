package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.onenonly.bitsandbalance.potions.CustomBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.onenonly.bitsandbalance.Config;
/**
 * Allows Wither Rose to be used as a brewing ingredient when the feature is enabled in config.
 * This ensures the brewing stand's ingredient slot accepts Wither Rose, complementing the data-driven recipe.
 */
@Mixin(PotionBrewing.class)
public abstract class PotionBrewingIngredientMixin {

    @Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true)
    private static void rebalance$allowWitherRose(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
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
            // Fail-safe: never break vanilla behavior if something goes wrong
        }
    }
}
