package org.onenonly.bitsandbalance.common.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EnhancedSlabCraftingRecipeGenerator {
    private EnhancedSlabCraftingRecipeGenerator() {
    }

    public static Map<String, String> generateVerticalSlabRecipes() {
        Map<String, String> recipes = new LinkedHashMap<>();

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            if (!(slabBlock instanceof SlabBlock)) {
                continue;
            }

            Block verticalBlock = EnhancedSlabRecipeLookup.verticalForSlab(slabBlock);
            if (verticalBlock == null) {
                continue;
            }

            Item inputItem = VerticalSlabFromPlanksRecipe.bitsandbalance$displayInputForSlab(slabBlock);
            Item outputItem = verticalBlock.asItem();
            if (!isRealItem(inputItem) || !isRealItem(outputItem)) {
                continue;
            }

            recipes.putIfAbsent(
                    name("vertical_slab_from_planks", inputItem, outputItem),
                    shaped(new String[]{"A", "A", "A"}, ingredient(inputItem), result(outputItem, 6))
            );
                recipes.putIfAbsent(
                    canonicalPath("vertical_slabs", "from_slab", outputItem),
                    shapeless(ingredient(slabBlock.asItem()), result(outputItem, 1))
                );
                recipes.putIfAbsent(
                    canonicalPath("slabs", "from_vertical_slab", slabBlock.asItem()),
                    shapeless(ingredient(outputItem), result(slabBlock.asItem(), 1))
                );
        }

        recipes.putIfAbsent("vertical_slab_from_planks", special("bitsandbalance:vertical_slab_from_planks"));
        recipes.putIfAbsent("vertical_slab_convert", special("bitsandbalance:vertical_slab_convert"));

        return recipes;
    }

    public static Map<String, String> generateStepAndRecoveryRecipes() {
        return generateStepAndRecoveryRecipes(true);
    }

    public static Map<String, String> generateStepAndRecoveryRecipes(boolean includeVerticalSlabDependentRecipes) {
        Map<String, String> recipes = new LinkedHashMap<>();

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            if (!(slabBlock instanceof SlabBlock)) {
                continue;
            }

            Item slabItem = slabBlock.asItem();
            if (!isRealItem(slabItem)) {
                continue;
            }

            Block stepBlock = EnhancedSlabRecipeLookup.stepForSlab(slabBlock);
            if (stepBlock != null && isRealItem(stepBlock.asItem())) {
                Item stepItem = stepBlock.asItem();
                recipes.putIfAbsent(
                    canonicalPath("steps", "from_slab", stepItem),
                    shaped(new String[]{"AA"}, ingredient(slabItem), result(stepItem, 4))
                );
                recipes.putIfAbsent(
                        name("slab_from_step", stepItem, slabItem),
                        shaped(new String[]{"AA"}, ingredient(stepItem), result(slabItem, 1))
                );
            }

            Block baseBlock = SlabFamilyBlockLookup.getBaseBlockForSlab(slabBlock);
            if (baseBlock != null && isRealItem(baseBlock.asItem())) {
                recipes.putIfAbsent(
                        name("base_block_from_slab", slabItem, baseBlock.asItem()),
                        shaped(new String[]{"A", "A"}, ingredient(slabItem), result(baseBlock.asItem(), 1))
                );
            }

            if (!includeVerticalSlabDependentRecipes) {
                continue;
            }

            Block verticalBlock = EnhancedSlabRecipeLookup.verticalForSlab(slabBlock);
            if (verticalBlock == null || !isRealItem(verticalBlock.asItem())) {
                continue;
            }

            Block verticalStepBlock = EnhancedSlabRecipeLookup.verticalStepForVerticalSlab(verticalBlock);
            if (verticalStepBlock != null && isRealItem(verticalStepBlock.asItem())) {
                Item verticalStepItem = verticalStepBlock.asItem();
                recipes.putIfAbsent(
                        canonicalPath("vertical_steps", "from_vertical_slab", verticalStepItem),
                        shaped(new String[]{"AA"}, ingredient(verticalBlock.asItem()), result(verticalStepItem, 4))
                );
                if (stepBlock != null && isRealItem(stepBlock.asItem())) {
                    Item stepItem = stepBlock.asItem();
                    recipes.putIfAbsent(
                            canonicalPath("vertical_steps", "from_step", verticalStepItem),
                            shapeless(ingredient(stepItem), result(verticalStepItem, 1))
                    );
                    recipes.putIfAbsent(
                        canonicalPath("vertical_slabs", "from_vertical_step", verticalBlock.asItem()),
                            shaped(new String[]{"AA"}, ingredient(verticalStepItem), result(verticalBlock.asItem(), 1))
                    );
                    recipes.putIfAbsent(
                            canonicalPath("steps", "from_vertical_step", stepItem),
                            shapeless(ingredient(verticalStepItem), result(stepItem, 1))
                    );
                }
            }
        }

        recipes.putIfAbsent("step_from_slab", special("bitsandbalance:step_from_slab"));
        recipes.putIfAbsent("step_convert", directedSpecial("bitsandbalance:step_convert", "step_to_vertical_step"));
        recipes.putIfAbsent("vertical_step_to_step", directedSpecial("bitsandbalance:step_convert", "vertical_step_to_step"));
        recipes.putIfAbsent("base_block_recovery", special("bitsandbalance:base_block_recovery"));

        return recipes;
    }

    private static boolean isRealItem(Item item) {
        return item != null && item != Items.AIR;
    }

    private static String ingredient(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return "\"" + id + "\"";
    }

    private static String result(Item item, int count) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return "{\n"
                + "    \"id\": \"" + id + "\",\n"
                + "    \"count\": " + count + "\n"
                + "  }";
    }

    private static String shaped(String[] pattern, String ingredient, String result) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"type\": \"minecraft:crafting_shaped\",\n");
        builder.append("  \"category\": \"building\",\n");
        builder.append("  \"pattern\": [\n");
        for (int i = 0; i < pattern.length; i++) {
            if (i > 0) {
                builder.append(",\n");
            }
            builder.append("    \"").append(pattern[i]).append("\"");
        }
        builder.append("\n  ],\n");
        builder.append("  \"key\": {\n");
        builder.append("    \"A\": ").append(ingredient).append("\n");
        builder.append("  },\n");
        builder.append("  \"result\": ").append(result).append("\n");
        builder.append("}");
        return builder.toString();
    }

    private static String shapeless(String ingredient, String result) {
        return "{\n"
                + "  \"type\": \"minecraft:crafting_shapeless\",\n"
                + "  \"category\": \"building\",\n"
                + "  \"ingredients\": [\n"
                + "    " + ingredient + "\n"
                + "  ],\n"
                + "  \"result\": " + result + "\n"
                + "}";
    }

    private static String name(String prefix, Item input, Item output) {
        Identifier inputId = BuiltInRegistries.ITEM.getKey(input);
        Identifier outputId = BuiltInRegistries.ITEM.getKey(output);
        return prefix + "__" + sanitize(inputId) + "__to__" + sanitize(outputId);
    }

    private static String canonicalPath(String family, String action, Item output) {
        Identifier outputId = BuiltInRegistries.ITEM.getKey(output);
        return "crafting/" + family + "/" + action + "/" + outputId.getNamespace() + "/" + outputId.getPath();
    }

    private static String sanitize(Identifier id) {
        return id.getNamespace() + "__" + id.getPath().replace('/', '_');
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