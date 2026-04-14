package org.onenonly.bitsandbalance.mixin;

import org.onenonly.bitsandbalance.ClientConfig;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.stats.ServerRecipeBook.class)
public abstract class ServerRecipeBookMixin {

    @Redirect(
        method = "loadRecipes",
        at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V"
        )
    )
    private void bitsandbalance$suppressRemovedRecipeWarning(Logger logger, String message, Object recipeKey) {
        if (ClientConfig.suppressRemovedRecipeBookWarnings
                && "Tried to load unrecognized recipe: {} removed now.".equals(message)) {
            return;
        }
        logger.error(message, recipeKey);
    }
}