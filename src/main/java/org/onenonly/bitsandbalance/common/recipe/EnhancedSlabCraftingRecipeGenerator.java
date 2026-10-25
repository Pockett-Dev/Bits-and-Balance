package org.onenonly.bitsandbalance.common.recipe;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EnhancedSlabCraftingRecipeGenerator {
    private EnhancedSlabCraftingRecipeGenerator() {
    }

    public static Map<String, String> generateVerticalSlabRecipes() {
        Map<String, String> recipes = new LinkedHashMap<>();

        recipes.putIfAbsent("vertical_slab_from_planks", special("bitsandbalance:vertical_slab_from_planks"));
        recipes.putIfAbsent("vertical_slab_convert", special("bitsandbalance:vertical_slab_convert"));

        return recipes;
    }

    public static Map<String, String> generateStepAndRecoveryRecipes() {
        return generateStepAndRecoveryRecipes(true);
    }

    public static Map<String, String> generateStepAndRecoveryRecipes(boolean includeVerticalSlabDependentRecipes) {
        Map<String, String> recipes = new LinkedHashMap<>();

        recipes.putIfAbsent("step_from_slab", special("bitsandbalance:step_from_slab"));
        recipes.putIfAbsent("step_convert", directedSpecial("bitsandbalance:step_convert", "step_to_vertical_step"));
        recipes.putIfAbsent("vertical_step_to_step", directedSpecial("bitsandbalance:step_convert", "vertical_step_to_step"));
        if (includeVerticalSlabDependentRecipes) {
            recipes.putIfAbsent("vertical_step_from_vertical_slab", directedSpecial("bitsandbalance:vertical_step_from_vertical_slab", "vertical_slab_to_vertical_step"));
            recipes.putIfAbsent("vertical_slab_from_vertical_step", directedSpecial("bitsandbalance:vertical_step_from_vertical_slab", "vertical_step_to_vertical_slab"));
        }
        return recipes;
    }

    private static String special(String type) {
        return "{\n"
                + "  \"type\": \"" + type + "\",\n"
                + "  \"category\": \"building\"\n"
                + "}";
    }

    private static String directedSpecial(String type, String direction) {
        return "{\n"
                + "  \"type\": \"" + type + "\",\n"
                + "  \"category\": \"building\",\n"
                + "  \"direction\": \"" + direction + "\"\n"
                + "}";
    }

}
