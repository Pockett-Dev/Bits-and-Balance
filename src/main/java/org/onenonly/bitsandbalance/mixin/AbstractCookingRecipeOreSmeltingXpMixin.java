package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.onenonly.bitsandbalance.common.mechanics.OreSmeltingXpRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(AbstractCookingRecipe.class)
public abstract class AbstractCookingRecipeOreSmeltingXpMixin {
    //? if mc == 1.21.1
    @Inject(method = "getExperience", at = @At("RETURN"), cancellable = true)
    //? else
    @Inject(method = "experience", at = @At("RETURN"), cancellable = true)
    //? endif
    private void bitsandbalance$useConfiguredOreExperience(CallbackInfoReturnable<Float> cir) {
        AbstractCookingRecipe recipe = (AbstractCookingRecipe) (Object) this;
        if (!(recipe instanceof SmeltingRecipe) && !(recipe instanceof BlastingRecipe)) {
            return;
        }

        Item input = bitsandbalance$singleInputItem(recipe);
        if (input == null) {
            return;
        }

        float configuredExperience = OreSmeltingXpRuntime.configuredExperience(input);
        if (!Float.isNaN(configuredExperience)) {
            cir.setReturnValue(configuredExperience);
        }
    }

    private static Item bitsandbalance$singleInputItem(AbstractCookingRecipe recipe) {
        try {
            //? if mc == 1.21.1
            if (recipe.getIngredients().isEmpty()) {
                return null;
            }
            var items = new ArrayList<>(java.util.Arrays.asList(recipe.getIngredients().getFirst().getItems()));
            if (items.size() != 1) {
                return null;
            }
            return items.getFirst().getItem();
            //? else
            var items = new ArrayList<>(recipe.input().items().toList());
            if (items.size() != 1) {
                return null;
            }
            return items.getFirst().value();
            //? endif
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
