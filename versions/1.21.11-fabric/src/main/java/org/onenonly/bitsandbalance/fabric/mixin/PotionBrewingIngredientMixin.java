package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.onenonly.bitsandbalance.common.potions.PotionFeatureFlags;
import org.onenonly.bitsandbalance.fabric.potions.FabricCustomBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ensures custom brewing reagents (e.g., Wither Rose) are treated as valid ingredients.
 */
@Mixin(PotionBrewing.class)
public abstract class PotionBrewingIngredientMixin {
    @Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true)
    private static void bitsandbalance$allowCustomReagents(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!PotionFeatureFlags.isCustomBrewingEnabled()) return;
            if (stack == null || stack.isEmpty()) return;
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null) return;
            for (var rule : FabricCustomBrewing.rules()) {
                if (rule.reagentItem().equals(id)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // Never break vanilla behavior.
        }
    }
}
