//? if mc != 1.21.1
package org.onenonly.bitsandbalance.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.common.mechanics.GoatHornPurposes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native cooldown packets and inventory overlays use a separate key for each instrument. */
@Mixin(ItemCooldowns.class)
public abstract class ItemCooldownsGoatHornGroupMixin {
    @Inject(method = "getCooldownGroup", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$separateGoatHorns(ItemStack stack, CallbackInfoReturnable<Identifier> cir) {
        String horn = GoatHornPurposes.horn(stack);
        boolean dreamCooldown = "dream".equals(horn) && GoatHornPurposes.isEnabled("dream");
        if (!GoatHornPurposes.separateCooldowns && !dreamCooldown) {
            return;
        }
        if (!horn.isEmpty()) {
            cir.setReturnValue(Identifier.fromNamespaceAndPath("bitsandbalance", "goat_horn/" + horn));
        }
    }
}
//? endif
