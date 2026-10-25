package org.onenonly.bitsandbalance.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.onenonly.bitsandbalance.fabric.recipe.FabricRecipeManagerMirrorRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMirrorRecipesMixin {
    @ModifyExpressionValue(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/RecipeMap;create(Lnet/minecraft/core/HolderLookup;)Lnet/minecraft/world/item/crafting/RecipeMap;"
            )
    )
    private RecipeMap bitsandbalance$appendMirrorRecipes(RecipeMap original, HolderLookup.Provider registries) {
        return FabricRecipeManagerMirrorRecipes.appendMirrorRecipes(original, registries);
    }
}
