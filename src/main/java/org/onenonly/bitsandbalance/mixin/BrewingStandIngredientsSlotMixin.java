package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
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
 * Fallback mixin for environments where the inner class is named IngredientsSlot (plural).
 * Ensures custom brewing reagents are accepted in the brewing ingredient slot.
 */
@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$IngredientsSlot")
public abstract class BrewingStandIngredientsSlotMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void rebalance$acceptRuleReagents(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
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
            // Never break vanilla if something goes wrong
        }
    }
}
