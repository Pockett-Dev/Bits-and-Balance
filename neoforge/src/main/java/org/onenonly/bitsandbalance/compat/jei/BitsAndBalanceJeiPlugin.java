package org.onenonly.bitsandbalance.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe;
import org.onenonly.bitsandbalance.common.compat.jei.JeiExtraIngredients;
import org.onenonly.bitsandbalance.common.compat.jei.JeiSyntheticCraftingRecipes;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.potions.CustomBrewing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@JeiPlugin
public final class BitsAndBalanceJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath("bitsandbalance", "jei_plugin");
    private static final String BITS_AND_BALANCE_NAMESPACE = "bitsandbalance";
    private static final Set<Identifier> HIDDEN_CRAFTING_SERIALIZER_IDS = Set.of(
        Identifier.fromNamespaceAndPath(BITS_AND_BALANCE_NAMESPACE, "base_block_recovery"),
        Identifier.fromNamespaceAndPath(BITS_AND_BALANCE_NAMESPACE, "step_from_slab"),
        Identifier.fromNamespaceAndPath(BITS_AND_BALANCE_NAMESPACE, "step_convert"),
        Identifier.fromNamespaceAndPath(BITS_AND_BALANCE_NAMESPACE, "vertical_slab_convert"),
        Identifier.fromNamespaceAndPath(BITS_AND_BALANCE_NAMESPACE, "vertical_slab_from_planks"),
        Identifier.fromNamespaceAndPath(BITS_AND_BALANCE_NAMESPACE, "vertical_step_from_vertical_slab")
    );
    private static final Set<String> HIDDEN_CRAFTING_IDS = Set.of(
        "base_block_recovery",
        "step_from_slab",
        "step_convert",
        "vertical_step_to_step",
        "vertical_slab_convert",
        "vertical_slab_from_planks",
        "vertical_step_from_vertical_slab",
        "vertical_slab_from_vertical_step"
    );

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registerBrewingRecipes(registration);
        registration.addRecipes(
            RecipeTypes.CRAFTING,
            JeiSyntheticCraftingRecipes.buildAll(
                Config.enableEnhancedSlabs && Config.enhancedSlabsVerticalSlabs,
                Config.enableEnhancedSlabs && Config.enhancedSlabsSteps
            )
        );
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        if (!Config.enableEnhancedSlabs) {
            return;
        }

        List<ItemStack> extraStacks = JeiExtraIngredients.buildEnhancedSlabItems(
                Config.enhancedSlabsVerticalSlabs,
                Config.enhancedSlabsSteps
        );
        if (!extraStacks.isEmpty()) {
            registration.addExtraItemStacks(extraStacks);
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        List<RecipeHolder<CraftingRecipe>> hiddenRecipes = jeiRuntime.getRecipeManager()
            .createRecipeLookup(RecipeTypes.CRAFTING)
            .includeHidden()
            .get()
            .filter(BitsAndBalanceJeiPlugin::shouldHideDynamicCraftingRecipe)
            .toList();
        if (!hiddenRecipes.isEmpty()) {
            jeiRuntime.getRecipeManager().hideRecipes(RecipeTypes.CRAFTING, hiddenRecipes);
        }
    }

    private static void registerBrewingRecipes(IRecipeRegistration registration) {
        List<CustomBrewing.Rule> rules = CustomBrewing.rules();
        if (rules.isEmpty()) {
            return;
        }

        var factory = registration.getVanillaRecipeFactory();
        List<IJeiBrewingRecipe> jeiRecipes = new ArrayList<>();

        for (CustomBrewing.Rule rule : rules) {
            var inputPotionHolder = BuiltInRegistries.POTION.get(rule.inputPotion()).orElse(null);
            var reagentItemHolder = BuiltInRegistries.ITEM.get(rule.reagentItem()).orElse(null);
            var resultPotionHolder = BuiltInRegistries.POTION.get(rule.resultPotion()).orElse(null);

            if (inputPotionHolder == null || reagentItemHolder == null || resultPotionHolder == null) {
                continue;
            }

            ItemStack ingredient = new ItemStack(reagentItemHolder.value());

            // Show the recipe for all potion container types (drinkable/splash/lingering)
            addBrewingRecipe(factory, jeiRecipes, rule, ingredient, Items.POTION, inputPotionHolder, resultPotionHolder);
            addBrewingRecipe(factory, jeiRecipes, rule, ingredient, Items.SPLASH_POTION, inputPotionHolder, resultPotionHolder);
            addBrewingRecipe(factory, jeiRecipes, rule, ingredient, Items.LINGERING_POTION, inputPotionHolder, resultPotionHolder);
        }

        if (!jeiRecipes.isEmpty()) {
            registration.addRecipes(RecipeTypes.BREWING, jeiRecipes);
        }
    }

    private static void addBrewingRecipe(
            mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory factory,
            List<IJeiBrewingRecipe> out,
            CustomBrewing.Rule rule,
            ItemStack ingredient,
            Item potionContainer,
            net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> inputPotion,
            net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> resultPotion
    ) {
        ItemStack inStack = PotionContents.createItemStack(potionContainer, inputPotion);
        ItemStack outStack = PotionContents.createItemStack(potionContainer, resultPotion);

        String bottle;
        if (potionContainer == Items.SPLASH_POTION) {
            bottle = "splash";
        } else if (potionContainer == Items.LINGERING_POTION) {
            bottle = "lingering";
        } else {
            bottle = "potion";
        }

        String base = rule.inputPotion().getPath() + "_with_" + rule.reagentItem().getPath() + "_to_" + rule.resultPotion().getPath();
        Identifier uid = Identifier.fromNamespaceAndPath("bitsandbalance", "jei_brewing/" + base + "/" + bottle);

        // JEI factory signature order is (ingredients, potionInputs, potionOutput, uid)
        out.add(factory.createBrewingRecipe(List.of(ingredient), List.of(inStack), outStack, uid));
    }

    private static boolean shouldHideDynamicCraftingRecipe(RecipeHolder<CraftingRecipe> recipe) {
        Identifier recipeId = recipe.id().identifier();
        if (!BITS_AND_BALANCE_NAMESPACE.equals(recipeId.getNamespace())) {
            return false;
        }
        String path = recipeId.getPath();
        if (path.startsWith("jei/")) {
            return false;
        }

        Identifier serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.value().getSerializer());
        if (serializerId != null && HIDDEN_CRAFTING_SERIALIZER_IDS.contains(serializerId)) {
            return true;
        }

        return HIDDEN_CRAFTING_IDS.contains(path);
    }

}
