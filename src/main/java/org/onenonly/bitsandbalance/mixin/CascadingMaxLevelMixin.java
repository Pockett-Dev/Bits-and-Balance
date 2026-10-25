package org.onenonly.bitsandbalance.mixin;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.enchantment.Enchantment;
import org.onenonly.bitsandbalance.common.mechanics.CascadingOreMining;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep Cascading at level I by default while allowing a config-selected cap of III. */
@Mixin(Enchantment.class)
public abstract class CascadingMaxLevelMixin {
    @Inject(method = "getMaxLevel", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$cascadingMaxLevel(CallbackInfoReturnable<Integer> cir) {
        Enchantment self = (Enchantment) (Object) this;
        if (self.description().getContents() instanceof TranslatableContents contents
                && "enchantment.bitsandbalance.cascading".equals(contents.getKey())) {
            cir.setReturnValue(CascadingOreMining.configuredMaxLevel);
        }
    }
}
