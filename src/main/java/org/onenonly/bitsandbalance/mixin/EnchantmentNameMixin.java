package org.onenonly.bitsandbalance.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;
import org.onenonly.bitsandbalance.tweaks.TreasureEnchantmentGoldColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to modify enchantment display names at the source.
 * This ensures treasure enchantments appear in gold everywhere they're displayed.
 */
@Mixin(Enchantment.class)
public class EnchantmentNameMixin {

    /**
     * Intercepts the getFullname method to apply gold color to treasure enchantments.
     * This affects all places where enchantment names are displayed.
     */
    @Inject(method = "getFullname", at = @At("RETURN"), cancellable = true)
    private static void bitsandbalance$applyTreasureGoldColor(Holder<Enchantment> enchantmentHolder, int level, CallbackInfoReturnable<Component> cir) {
        if (TreasureEnchantmentGoldColor.isTreasureEnchantment(enchantmentHolder)) {
            Component originalComponent = cir.getReturnValue();
            
            // Create a new component with gold formatting
            MutableComponent goldComponent;
            if (originalComponent instanceof MutableComponent mutable) {
                goldComponent = mutable.withStyle(ChatFormatting.GOLD);
            } else {
                goldComponent = Component.empty().append(originalComponent).withStyle(ChatFormatting.GOLD);
            }
            
            cir.setReturnValue(goldComponent);
        }
    }
}
