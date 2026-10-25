package org.onenonly.bitsandbalance.fabric.compat.jei;

import com.mojang.logging.LogUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.onenonly.bitsandbalance.common.compat.jei.JeiExtraIngredients;
import org.onenonly.bitsandbalance.common.compat.jei.JeiKilnSupport;
import org.onenonly.bitsandbalance.common.compat.jei.JeiSyntheticCraftingRecipes;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.potions.FabricCustomBrewing;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JeiPlugin
public final class BitsAndBalanceJeiPlugin implements IModPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
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
        JeiKilnSupport.registerCategory(registration);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registerBrewingRecipes(registration);
        JeiKilnSupport.registerRecipes(registration);
        List<RecipeHolder<CraftingRecipe>> syntheticRecipes =
            JeiSyntheticCraftingRecipes.buildAll(
                FabricTweaksConfig.isVerticalSlabRuntimeEnabled(),
                FabricTweaksConfig.isStepRuntimeEnabled()
            );
        registration.addRecipes(RecipeTypes.CRAFTING, syntheticRecipes);
        LOGGER.info("[bitsandbalance] Registered {} synthetic JEI crafting recipes on 26.3 Fabric", syntheticRecipes.size());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        JeiKilnSupport.registerCatalyst(registration);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        JeiKilnSupport.registerTransferHandler(registration);
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        if (!FabricTweaksConfig.isGeneratedEnhancedSlabContentEnabled()) {
            return;
        }

        List<ItemStack> extraStacks = JeiExtraIngredients.buildEnhancedSlabItems(
                FabricTweaksConfig.isVerticalSlabRuntimeEnabled(),
                FabricTweaksConfig.isStepRuntimeEnabled()
        );
        if (!extraStacks.isEmpty()) {
            registration.addExtraItemStacks(extraStacks);
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JeiKilnSupport.registerRuntimeRecipes(jeiRuntime);
        List<RecipeHolder<CraftingRecipe>> allRecipes = jeiRuntime.getRecipeManager()
            .createRecipeLookup(RecipeTypes.CRAFTING)
            .includeHidden()
            .get()
            .toList();
        Set<String> loadedRecipePaths = new HashSet<>();
        for (RecipeHolder<CraftingRecipe> recipe : allRecipes) {
            Identifier recipeId = recipe.id().identifier();
            if (BITS_AND_BALANCE_NAMESPACE.equals(recipeId.getNamespace()) && !recipeId.getPath().startsWith("jei/")) {
                loadedRecipePaths.add(recipeId.getPath());
            }
        }

        List<RecipeHolder<CraftingRecipe>> hiddenRecipes = allRecipes.stream()
            .filter(recipe -> shouldHideDynamicCraftingRecipe(recipe) || shouldHideDuplicateSyntheticRecipe(recipe, loadedRecipePaths))
            .toList();
        if (!hiddenRecipes.isEmpty()) {
            jeiRuntime.getRecipeManager().hideRecipes(RecipeTypes.CRAFTING, hiddenRecipes);
        }
    }

    private static void registerBrewingRecipes(IRecipeRegistration registration) {
        List<FabricCustomBrewing.Rule> rules = FabricCustomBrewing.rules();
        if (rules.isEmpty()) {
            return;
        }

        var factory = registration.getVanillaRecipeFactory();
        List<IJeiBrewingRecipe> jeiRecipes = new ArrayList<>();

        for (FabricCustomBrewing.Rule rule : rules) {
            var inputPotionHolder = BuiltInRegistries.POTION.get(rule.inputPotion()).orElse(null);
            var reagentItemHolder = BuiltInRegistries.ITEM.get(rule.reagentItem()).orElse(null);
            var resultPotionHolder = BuiltInRegistries.POTION.get(rule.resultPotion()).orElse(null);

            if (inputPotionHolder == null || reagentItemHolder == null || resultPotionHolder == null) {
                continue;
            }

            ItemStack ingredient = new ItemStack(reagentItemHolder.value());

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
            FabricCustomBrewing.Rule rule,
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

    private static boolean shouldHideDuplicateSyntheticRecipe(RecipeHolder<CraftingRecipe> recipe, Set<String> loadedRecipePaths) {
        Identifier recipeId = recipe.id().identifier();
        if (!BITS_AND_BALANCE_NAMESPACE.equals(recipeId.getNamespace())) {
            return false;
        }
        String path = recipeId.getPath();
        if (!path.startsWith("jei/")) {
            return false;
        }

        String generatedPath = generatedPathForSyntheticRecipe(path.substring("jei/".length()));
        return generatedPath != null && loadedRecipePaths.contains(generatedPath);
    }

    private static String generatedPathForSyntheticRecipe(String path) {
        String[] parts = path.split("/");
        if (parts.length != 7 || !"to".equals(parts[4])) {
            return null;
        }

        String family = parts[0];
        String action = parts[1];
        String inputNamespace = parts[2];
        String inputPath = parts[3];
        String outputNamespace = parts[5];
        String outputPath = parts[6];

        if ("vertical_slabs".equals(family) && "from_input".equals(action)) {
            return namedGeneratedPath("vertical_slab_from_planks", inputNamespace, inputPath, outputNamespace, outputPath);
        }
        if ("vertical_slabs".equals(family) && "from_slab".equals(action)) {
            return canonicalGeneratedPath("vertical_slabs", "from_slab", outputNamespace, outputPath);
        }
        if ("slabs".equals(family) && "from_vertical_slab".equals(action)) {
            return canonicalGeneratedPath("slabs", "from_vertical_slab", outputNamespace, outputPath);
        }
        if ("steps".equals(family) && "from_slab".equals(action)) {
            return canonicalGeneratedPath("steps", "from_slab", outputNamespace, outputPath);
        }
        if ("slabs".equals(family) && "from_step".equals(action)) {
            return namedGeneratedPath("slab_from_step", inputNamespace, inputPath, outputNamespace, outputPath);
        }
        if ("vertical_steps".equals(family) && "from_vertical_slab".equals(action)) {
            return canonicalGeneratedPath("vertical_steps", "from_vertical_slab", outputNamespace, outputPath);
        }
        if ("vertical_slabs".equals(family) && "from_vertical_step".equals(action)) {
            return canonicalGeneratedPath("vertical_slabs", "from_vertical_step", outputNamespace, outputPath);
        }
        if ("vertical_steps".equals(family) && "from_step".equals(action)) {
            return canonicalGeneratedPath("vertical_steps", "from_step", outputNamespace, outputPath);
        }
        if ("steps".equals(family) && "from_vertical_step".equals(action)) {
            return canonicalGeneratedPath("steps", "from_vertical_step", outputNamespace, outputPath);
        }
        return null;
    }

    private static String canonicalGeneratedPath(String family, String action, String outputNamespace, String outputPath) {
        return "crafting/" + family + "/" + action + "/" + outputNamespace + "/" + outputPath;
    }

    private static String namedGeneratedPath(String prefix, String inputNamespace, String inputPath, String outputNamespace, String outputPath) {
        return prefix + "__" + sanitizeRecipeId(inputNamespace, inputPath) + "__to__" + sanitizeRecipeId(outputNamespace, outputPath);
    }

    private static String sanitizeRecipeId(String namespace, String path) {
        return namespace + "__" + path.replace('/', '_');
    }

}
