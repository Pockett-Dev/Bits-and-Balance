package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.onenonly.bitsandbalance.fabric.recipe.FabricRecipeManagerMirrorRecipes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMirrorRecipesMixin {
    @Shadow
    @Final
    private HolderLookup.Provider registries;

    @Inject(method = "prepare", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$appendMirrorRecipes(ResourceManager resourceManager,
                                                    ProfilerFiller profiler,
                                                    CallbackInfoReturnable<RecipeMap> cir) {
        cir.setReturnValue(FabricRecipeManagerMirrorRecipes.appendMirrorRecipes(cir.getReturnValue(), registries));
    }
}