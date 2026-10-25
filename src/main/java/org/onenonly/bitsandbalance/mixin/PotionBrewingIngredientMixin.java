package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
//? if mc == 1.21.1
import org.onenonly.bitsandbalance.Config;
//? endif
//? if mc == 1.21.11
import org.onenonly.bitsandbalance.common.potions.PotionFeatureFlags;
//? endif
import org.onenonly.bitsandbalance.potions.CustomBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows custom brewing reagents to be treated as valid ingredients.
 */
@Mixin(PotionBrewing.class)
public abstract class PotionBrewingIngredientMixin {

    @Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true)
    private static void rebalance$allowWitherRose(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        try {
            //? if mc == 1.21.1
            if (!Config.enableWitheringPotion) return;
            //? else
            //? if mc == 1.21.11
            if (!PotionFeatureFlags.isCustomBrewingEnabled()) return;
            //? else
            if (CustomBrewing.rules().isEmpty()) return;
            //? endif
            //? endif
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
