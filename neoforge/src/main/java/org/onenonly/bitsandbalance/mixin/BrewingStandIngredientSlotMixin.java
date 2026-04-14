package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.potions.CustomBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/**
 * Ensures custom brewing reagents are accepted in the strict ingredient-slot path.
 */
@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$IngredientSlot")
public abstract class BrewingStandIngredientSlotMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void rebalance$allowWitherRose(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (CustomBrewing.rules().isEmpty() || stack == null || stack.isEmpty()) {
                return;
            }
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null) {
                return;
            }
            for (var rule : CustomBrewing.rules()) {
                if (rule.reagentItem().equals(id)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // Never break vanilla if something goes wrong
        }
    }
}
