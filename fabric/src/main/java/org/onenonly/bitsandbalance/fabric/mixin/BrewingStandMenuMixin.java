package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;
import org.onenonly.bitsandbalance.fabric.potions.FabricCustomBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ensures custom brewing reagents are treated as valid ingredients by the brewing menu checks.
 */
@Mixin(PotionBrewing.class)
public abstract class BrewingStandMenuMixin {

    @Inject(method = "isIngredient(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$allowCustomReagents(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!FabricPotionConfig.enableCustomBrewing) return;
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
