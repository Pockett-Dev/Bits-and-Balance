package org.onenonly.bitsandbalance.common.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Generates dynamic stonecutting recipes for steps and vertical steps.
 *
 * <p>Rules:
 * <ul>
 *   <li>Any existing stonecutter recipe that outputs 2 slabs gets a matching 4-step recipe.</li>
 *   <li>The same source ingredient also gets a matching 4-vertical-step recipe if a vertical-step variant exists.</li>
 *   <li>Wood planks/log/wood/stem inputs are supplemented directly because many of those slab recipes are also dynamic.</li>
 *   <li>No step → vertical-step or vertical-step → step stonecutter conversions are generated.</li>
 * </ul>
 */
public final class StepStonecuttingRecipeGenerator {

    private StepStonecuttingRecipeGenerator() {
    }

    public static Map<String, String> generate(MinecraftServer server) {
        return generate(server.getRecipeManager(), server.registryAccess());
    }

    public static Map<String, String> generate(RecipeManager recipeManager, RegistryAccess registryAccess) {
        return generate(recipeManager, registryAccess, true);
    }

    public static Map<String, String> generate(RecipeManager recipeManager,
                                               RegistryAccess registryAccess,
                                               boolean includeVerticalSlabDependentRecipes) {
        Map<String, String> recipes = new LinkedHashMap<>();
        recipes.putAll(generateMirroredStonecuttingRecipes(recipeManager, registryAccess, includeVerticalSlabDependentRecipes));
        recipes.putAll(generateDirectStonecuttingRecipes(includeVerticalSlabDependentRecipes));
        return recipes;
    }

    public static Map<String, String> generateMirroredStonecuttingRecipes(RecipeManager recipeManager, RegistryAccess registryAccess) {
        return generateMirroredStonecuttingRecipes(recipeManager, registryAccess, true);
    }

    public static Map<String, String> generateMirroredStonecuttingRecipes(RecipeManager recipeManager,
                                                                          RegistryAccess registryAccess,
                                                                          boolean includeVerticalSlabDependentRecipes) {
        Map<String, String> recipes = new LinkedHashMap<>();
        Set<String> seenSimpleConversions = new HashSet<>();
        addRecipesFromExistingStonecutting(recipeManager, registryAccess, recipes, seenSimpleConversions, includeVerticalSlabDependentRecipes);
        return recipes;
    }

    public static Map<String, String> generateDirectStonecuttingRecipes() {
        return generateDirectStonecuttingRecipes(true);
    }

    public static Map<String, String> generateDirectStonecuttingRecipes(boolean includeVerticalSlabDependentRecipes) {
        Map<String, String> recipes = new LinkedHashMap<>();
        Set<String> seenSimpleConversions = new HashSet<>();
        addWoodBaseRecipes(recipes, seenSimpleConversions, includeVerticalSlabDependentRecipes);
        return recipes;
    }

    private static void addRecipesFromExistingStonecutting(RecipeManager recipeManager,
                                                           RegistryAccess registryAccess,
                                                           Map<String, String> out,
                                                           Set<String> seenSimpleConversions,
                                                           boolean includeVerticalSlabDependentRecipes) {
        try {
            for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                if (!(holder.value() instanceof StonecutterRecipe stonecutting)) continue;

                ItemStack output = stonecutting.assemble(new SingleRecipeInput(ItemStack.EMPTY), registryAccess);
                if (output == null || output.isEmpty()) continue;

                Item outputItem = output.getItem();
                Block outputBlock = (outputItem instanceof BlockItem bi) ? bi.getBlock() : null;
                if (outputBlock == null) continue;

                Block stepBlock = StepDynamicRegistry.getStepForSlab(outputBlock);
                if (stepBlock == null) continue;

                String ingredientJson = buildIngredientJson(stonecutting);
                if (ingredientJson == null) continue;
                Identifier singleIngredientId = singleIngredientId(stonecutting);

                Identifier recipeId = holder.id().identifier();
                if (recipeId == null) continue;

                addFromRecipe(out, recipeId, ingredientJson, singleIngredientId, output, stepBlock.asItem(), output.getCount() * 2, "step", seenSimpleConversions);

                if (!includeVerticalSlabDependentRecipes) {
                    continue;
                }

                Block verticalSlab = VerticalSlabDynamicRegistry.getVerticalForSlab(outputBlock);
                if (verticalSlab == null) continue;
                Block verticalStep = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlab);
                if (verticalStep == null) continue;

                addFromRecipe(out, recipeId, ingredientJson, singleIngredientId, output, verticalStep.asItem(), output.getCount() * 2, "vertical_step", seenSimpleConversions);
            }
        } catch (Exception ignored) {
        }
    }

    private static void addWoodBaseRecipes(Map<String, String> out,
                                           Set<String> seenSimpleConversions,
                                           boolean includeVerticalSlabDependentRecipes) {
        for (Item inputItem : BuiltInRegistries.ITEM) {
            if (!(inputItem instanceof BlockItem)) continue;

            Identifier inputId = BuiltInRegistries.ITEM.getKey(inputItem);
            if (inputId == null) continue;

            String ns = inputId.getNamespace();
            String path = inputId.getPath();

            if (path.endsWith("_planks")) {
                String base = path.substring(0, path.length() - "_planks".length());
                addWoodBaseRecipe(out, inputId, ns, base + "_slab", 4, "wood_planks_step", includeVerticalSlabDependentRecipes, seenSimpleConversions);
            } else if (path.endsWith("_log")) {
                addWoodTypeStepRecipes(out, inputId, ns, path, "_log", seenSimpleConversions, includeVerticalSlabDependentRecipes);
            } else if (path.endsWith("_wood")) {
                addWoodTypeStepRecipes(out, inputId, ns, path, "_wood", seenSimpleConversions, includeVerticalSlabDependentRecipes);
            } else if (path.endsWith("_stem")) {
                addWoodTypeStepRecipes(out, inputId, ns, path, "_stem", seenSimpleConversions, includeVerticalSlabDependentRecipes);
            } else if (path.endsWith("_hyphae")) {
                addWoodTypeStepRecipes(out, inputId, ns, path, "_hyphae", seenSimpleConversions, includeVerticalSlabDependentRecipes);
            }
        }
    }

    private static void addWoodTypeStepRecipes(Map<String, String> out,
                                               Identifier inputId,
                                               String namespace,
                                               String path,
                                               String suffix,
                                               Set<String> seenSimpleConversions,
                                               boolean includeVerticalSlabDependentRecipes) {
        String familyBase;
        String sortKeyPrefix;

        if (path.startsWith("stripped_")) {
            familyBase = path.substring("stripped_".length(), path.length() - suffix.length());
            sortKeyPrefix = "wood_stripped" + suffix + "_step";
        } else {
            familyBase = path.substring(0, path.length() - suffix.length());
            sortKeyPrefix = "wood" + suffix + "_step";
        }

        addWoodBaseRecipe(out, inputId, namespace, familyBase + "_slab", 16, sortKeyPrefix, includeVerticalSlabDependentRecipes, seenSimpleConversions);
    }

    private static void addWoodBaseRecipe(Map<String, String> out, Identifier inputId, String namespace, String slabPath, int count, String sortKey, boolean includeVerticalStep, Set<String> seenSimpleConversions) {
        Block slabBlock = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath(namespace, slabPath)).orElse(null);
        if (slabBlock == null) return;

        Block stepBlock = StepDynamicRegistry.getStepForSlab(slabBlock);
        if (stepBlock == null) return;

        addDirect(out, inputId, stepBlock.asItem(), count, sortKey + "_step", seenSimpleConversions);

        if (!includeVerticalStep) return;

        Block verticalSlab = VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock);
        if (verticalSlab == null) return;
        Block verticalStep = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlab);
        if (verticalStep == null) return;

        addDirect(out, inputId, verticalStep.asItem(), count, sortKey + "_vertical_step", seenSimpleConversions);
    }

    private static void addFromRecipe(Map<String, String> out, Identifier recipeId, String ingredientJson, Identifier singleIngredientId, ItemStack sourceOutput, Item output, int count, String suffix, Set<String> seenSimpleConversions) {
        Identifier outputId = BuiltInRegistries.ITEM.getKey(output);
        if (outputId == null) return;

        if (singleIngredientId != null) {
            String key = simpleConversionKey(singleIngredientId, outputId, count);
            if (!seenSimpleConversions.add(key)) return;
        }

        String safeName = StonecutterOutputSortHelper.buildDerivedRecipeName("step_sc_" + suffix, recipeId, output);
        String recipeType = "minecraft:stonecutting";

        String json = "{\n"
                + "  \"type\": \"" + recipeType + "\",\n"
                + "  \"ingredient\": " + ingredientJson + ",\n"
                + "  \"result\": {\n"
                + "    \"id\": \"" + outputId + "\",\n"
                + "    \"count\": " + count + "\n"
                + "  }\n"
                + "}";

        out.putIfAbsent(safeName, json);
    }

    private static void addDirect(Map<String, String> out, Identifier inputId, Item output, int count, String suffix, Set<String> seenSimpleConversions) {
        if (output == null) return;

        Identifier outputId = BuiltInRegistries.ITEM.getKey(output);
        if (outputId == null) return;

        String key = simpleConversionKey(inputId, outputId, count);
        if (!seenSimpleConversions.add(key)) return;

        String safeName = StonecutterOutputSortHelper.buildDirectRecipeName("step_sc_" + suffix, inputId, output);

        String recipeType = "minecraft:stonecutting";

        String json = "{\n"
                + "  \"type\": \"" + recipeType + "\",\n"
            + "  \"ingredient\": " + itemIngredient(inputId) + ",\n"
                + "  \"result\": {\n"
                + "    \"id\": \"" + outputId + "\",\n"
                + "    \"count\": " + count + "\n"
                + "  }\n"
                + "}";

        out.putIfAbsent(safeName, json);
    }

    private static Identifier singleIngredientId(StonecutterRecipe recipe) {
        try {
            var items = new ArrayList<>(recipe.input().items().toList());
            if (items.size() != 1) return null;
            return BuiltInRegistries.ITEM.getKey(items.get(0).value());
        } catch (Exception e) {
            return null;
        }
    }

    private static String simpleConversionKey(Identifier inputId, Identifier outputId, int count) {
        return inputId + "->" + outputId + "@" + count;
    }

    private static String buildIngredientJson(StonecutterRecipe recipe) {
        try {
            var ingredient = recipe.input();
            var items = new ArrayList<>(ingredient.items().toList());
            if (items.isEmpty()) return null;

            if (items.size() == 1) {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(items.get(0).value());
                return itemId != null ? itemIngredient(itemId) : null;
            }

            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (var h : items) {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(h.value());
                if (itemId != null) {
                    if (!first) sb.append(", ");
                    sb.append(itemIngredient(itemId));
                    first = false;
                }
            }
            sb.append("]");
            return first ? null : sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String itemIngredient(Identifier itemId) {
        return "\"" + itemId + "\"";
    }
}
