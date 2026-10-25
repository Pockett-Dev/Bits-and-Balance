package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
//? if mc != 1.21.1
import net.minecraft.world.flag.FeatureFlagSet;
//? endif
import net.minecraft.world.item.ItemStack;
//? if mc != 1.21.1
import net.minecraft.world.item.crafting.Ingredient;
//? endif
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
//? if mc != 1.21.1
import net.minecraft.world.item.crafting.SelectableRecipe;
//? endif
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.common.recipe.StonecutterOutputSortHelper;
import org.spongepowered.asm.mixin.Mixin;
//? if mc != 1.21.1
import org.spongepowered.asm.mixin.Shadow;
//? endif
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if mc != 1.21.1
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? endif
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerStonecuttingSortMixin {
    //? if mc != 1.21.1
    @Shadow
    private SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes;

    //? endif
    //? if mc == 1.21.1
    @Inject(method = "getRecipesFor", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void bitsandbalance$sortStonecuttingRecipes(
            RecipeType<T> recipeType,
            I input,
            Level level,
            CallbackInfoReturnable<List<RecipeHolder<T>>> cir
    ) {
        if (recipeType != RecipeType.STONECUTTING) {
            return;
        }

        Map<RecipeHolder<?>, String> sortKeys = new IdentityHashMap<>();
        List<RecipeHolder<T>> sortedRecipes = new ArrayList<>(cir.getReturnValue());
        sortedRecipes.sort(Comparator.comparing(holder ->
                sortKeys.computeIfAbsent(holder, key -> bitsandbalance$sortKey(key, level))));
        cir.setReturnValue(bitsandbalance$dedupeRecipesForInput(sortedRecipes, level));
    }
    //? else
    @Inject(method = "finalizeRecipeLoading", at = @At("TAIL"))
    private void bitsandbalance$sortStonecuttingRecipes(FeatureFlagSet enabledFeatures, CallbackInfo ci) {
        if (this.stonecutterRecipes == null || this.stonecutterRecipes.isEmpty()) {
            return;
        }

        Map<RecipeHolder<?>, String> sortKeys = new IdentityHashMap<>();
        List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> sortedEntries = new ArrayList<>(this.stonecutterRecipes.entries());
        sortedEntries.sort(Comparator.comparing(entry ->
                entry.recipe().recipe()
                        .map(holder -> sortKeys.computeIfAbsent(holder, key -> bitsandbalance$sortKey(key, null)))
                        .orElse("99_empty|")));
        this.stonecutterRecipes = new SelectableRecipe.SingleInputSet<>(bitsandbalance$dedupeEntries(sortedEntries));
    }
    //? endif

    //? if mc == 1.21.1
    private static <T extends Recipe<?>> List<RecipeHolder<T>> bitsandbalance$dedupeRecipesForInput(List<RecipeHolder<T>> recipes, Level level) {
        Set<String> seen = new HashSet<>();
        List<RecipeHolder<T>> deduped = new ArrayList<>(recipes.size());
        for (RecipeHolder<T> holder : recipes) {
            if (seen.add(bitsandbalance$resultKey(holder, level))) {
                deduped.add(holder);
            }
        }
        return deduped;
    }
    //? else
    private static List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> bitsandbalance$dedupeEntries(List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> entries) {
        Set<String> seen = new HashSet<>();
        List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> deduped = new ArrayList<>(entries.size());
        for (SelectableRecipe.SingleInputEntry<StonecutterRecipe> entry : entries) {
            String key = entry.recipe().recipe()
                    .map(holder -> bitsandbalance$ingredientKey(entry.input()) + "|" + bitsandbalance$resultKey(holder, null))
                    .orElse("empty|" + entry.hashCode());
            if (seen.add(key)) {
                deduped.add(entry);
            }
        }
        return deduped;
    }

    private static String bitsandbalance$ingredientKey(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return "";
        }

        List<String> itemIds = new ArrayList<>();
        ingredient.items().forEach(holder -> {
            Identifier id = BuiltInRegistries.ITEM.getKey(holder.value());
            if (id != null) {
                itemIds.add(id.toString());
            }
        });
        itemIds.sort(String::compareTo);
        return String.join(",", itemIds);
    }
    //? endif

    private static String bitsandbalance$sortKey(RecipeHolder<?> holder, Level level) {
        ItemStack result = bitsandbalance$result(holder, level);
        if (result.isEmpty()) {
            return "99_empty|" + bitsandbalance$recipeId(holder);
        }

        Identifier outputId = BuiltInRegistries.ITEM.getKey(result.getItem());
        String stableOutputId = outputId != null ? outputId.toString() : "";
        return StonecutterOutputSortHelper.sortKeyForOutput(result.getItem())
                + "|" + stableOutputId
                + "|" + bitsandbalance$recipeId(holder);
    }

    private static String bitsandbalance$resultKey(RecipeHolder<?> holder, Level level) {
        ItemStack result = bitsandbalance$result(holder, level);
        if (result.isEmpty()) {
            return "empty|" + bitsandbalance$recipeId(holder);
        }

        Identifier outputId = BuiltInRegistries.ITEM.getKey(result.getItem());
        return (outputId != null ? outputId.toString() : "")
                + "@" + result.getCount();
    }

    private static ItemStack bitsandbalance$result(RecipeHolder<?> holder, Level level) {
        if (!(holder.value() instanceof StonecutterRecipe stonecutting)) {
            return ItemStack.EMPTY;
        }

        try {
            return bitsandbalance$assemble(stonecutting, new SingleRecipeInput(ItemStack.EMPTY), level != null ? level.registryAccess() : null);
        } catch (RuntimeException exception) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack bitsandbalance$assemble(Object recipe, Object input, Object registries) {
        Method oneArgMethod = null;

        for (Method method : recipe.getClass().getMethods()) {
            if (!"assemble".equals(method.getName())) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 2
                    && parameterTypes[0].isInstance(input)
                    && registries != null
                    && parameterTypes[1].isInstance(registries)) {
                try {
                    return (ItemStack) method.invoke(recipe, input, registries);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Failed to invoke two-argument assemble() on " + recipe.getClass().getName(), exception);
                }
            }

            if (parameterTypes.length == 1 && parameterTypes[0].isInstance(input)) {
                oneArgMethod = method;
            }
        }

        if (oneArgMethod != null) {
            try {
                return (ItemStack) oneArgMethod.invoke(recipe, input);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to invoke one-argument assemble() on " + recipe.getClass().getName(), exception);
            }
        }

        throw new IllegalStateException("No compatible assemble() method found on " + recipe.getClass().getName());
    }

    private static String bitsandbalance$recipeId(RecipeHolder<?> holder) {
        return holder.id().toString();
    }
}
