package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.onenonly.bitsandbalance.Config;
/**
 * Allow players to eat food unconditionally when enableFoodAlwaysEdible is true.
 * This overrides the vanilla canEat method to always return true when the config is enabled.
 */
@Mixin(Player.class)
public abstract class FoodAlwaysEdibleMixin {

    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void rebalance$foodAlwaysEdible(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        if (Config.enableFoodAlwaysEdible) {
            cir.setReturnValue(true);
        }
    }
}
