package org.onenonly.bitsandbalance.common.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.ArrayList;

public final class DynamicRecipeInputFingerprints {
    private DynamicRecipeInputFingerprints() {
    }

    public static String buildWoodStonecuttingFingerprint() {
        return buildWoodStonecuttingFingerprint(true, false);
    }

    public static String buildWoodStonecuttingFingerprint(boolean includeVerticalSlabs, boolean includeSteps) {
        DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
        builder.add("type", "wood_stonecutting");
        builder.add("schema_version", 4);
        builder.add("vertical_slabs", includeVerticalSlabs);
        builder.add("steps", includeSteps);

        for (Item inputItem : BuiltInRegistries.ITEM) {
            if (!(inputItem instanceof BlockItem)) {
                continue;
            }

            Identifier inputId = BuiltInRegistries.ITEM.getKey(inputItem);
            if (inputId == null) {
                continue;
            }

            String path = inputId.getPath();
            if (!isWoodRecipeRelevantPath(path)) {
                continue;
            }

            builder.add("item", inputId.toString());
        }

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock);
            Identifier slabId = BuiltInRegistries.BLOCK.getKey(slabBlock);

            if (includeVerticalSlabs && verticalBlock != null) {
                Identifier verticalId = BuiltInRegistries.BLOCK.getKey(verticalBlock);
                if (slabId != null && verticalId != null) {
                    builder.add("vertical_pair", slabId + "->" + verticalId);
                }
            }

            if (includeSteps) {
                Block stepBlock = StepDynamicRegistry.getStepForSlab(slabBlock);
                if (slabId != null && stepBlock != null) {
                    Identifier stepId = BuiltInRegistries.BLOCK.getKey(stepBlock);
                    if (stepId != null) {
                        builder.add("step_pair", slabId + "->" + stepId);
                    }
                }

                if (verticalBlock != null) {
                    Block verticalStepBlock = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalBlock);
                    if (slabId != null && verticalStepBlock != null) {
                        Identifier verticalStepId = BuiltInRegistries.BLOCK.getKey(verticalStepBlock);
                        if (verticalStepId != null) {
                            builder.add("vertical_step_pair", slabId + "->" + verticalStepId);
                        }
                    }
                }
            }
        }

        return builder.build();
    }

    public static String buildVerticalSlabFingerprint(RecipeManager recipeManager, RegistryAccess registryAccess) {
        DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
        builder.add("type", "vertical_slab");
        builder.add("schema_version", 9);

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            Block verticalBlock = EnhancedSlabRecipeLookup.verticalForSlab(slabBlock);
            if (verticalBlock == null) {
                continue;
            }

            Identifier slabId = BuiltInRegistries.BLOCK.getKey(slabBlock);
            Identifier verticalId = BuiltInRegistries.BLOCK.getKey(verticalBlock);
            if (slabId != null && verticalId != null) {
                builder.add("dynamic_pair", slabId + "->" + verticalId);
            }

            Item displayInput = VerticalSlabFromPlanksRecipe.bitsandbalance$displayInputForSlab(slabBlock);
            if (isRealItem(displayInput)) {
                builder.add("display_input", BuiltInRegistries.ITEM.getKey(displayInput).toString());
            }
        }

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (!(holder.value() instanceof StonecutterRecipe stonecutting)) {
                continue;
            }

            ItemStack output = stonecutting.assemble(new SingleRecipeInput(ItemStack.EMPTY), registryAccess);
            if (output == null || output.isEmpty()) {
                continue;
            }

            Item outputItem = output.getItem();
            Block outputBlock = outputItem instanceof BlockItem blockItem ? blockItem.getBlock() : null;
            if (outputBlock == null) {
                continue;
            }

            Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(outputBlock);
            if (verticalBlock == null) {
                continue;
            }

            Identifier recipeId = holder.id().identifier();
            if (recipeId == null || recipeId.getPath().contains("_wood_sc_")) {
                continue;
            }

            builder.add("stonecutter_recipe", recipeId.toString());
            builder.add("stonecutter_output", safeId(BuiltInRegistries.ITEM.getKey(outputItem)) + "@" + output.getCount());
            builder.add("stonecutter_ingredient", buildIngredientSignature(stonecutting));
        }

        return builder.build();
    }

    public static String buildVerticalSlabStaticFingerprint() {
        DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
        builder.add("type", "vertical_slab_static");
        builder.add("schema_version", 1);

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            Block verticalBlock = EnhancedSlabRecipeLookup.verticalForSlab(slabBlock);
            if (verticalBlock == null) {
                continue;
            }

            Identifier slabId = BuiltInRegistries.BLOCK.getKey(slabBlock);
            Identifier verticalId = BuiltInRegistries.BLOCK.getKey(verticalBlock);
            if (slabId != null && verticalId != null) {
                builder.add("dynamic_pair", slabId + "->" + verticalId);
            }

            Item displayInput = VerticalSlabFromPlanksRecipe.bitsandbalance$displayInputForSlab(slabBlock);
            if (isRealItem(displayInput)) {
                builder.add("display_input", BuiltInRegistries.ITEM.getKey(displayInput).toString());
            }
        }

        return builder.build();
    }

    public static String buildVerticalSlabMirrorFingerprint(RecipeManager recipeManager, RegistryAccess registryAccess) {
        DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
        builder.add("type", "vertical_slab_mirror");
        builder.add("schema_version", 1);

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            Block verticalBlock = EnhancedSlabRecipeLookup.verticalForSlab(slabBlock);
            if (verticalBlock == null) {
                continue;
            }

            Identifier slabId = BuiltInRegistries.BLOCK.getKey(slabBlock);
            Identifier verticalId = BuiltInRegistries.BLOCK.getKey(verticalBlock);
            if (slabId != null && verticalId != null) {
                builder.add("dynamic_pair", slabId + "->" + verticalId);
            }
        }

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (!(holder.value() instanceof StonecutterRecipe stonecutting)) {
                continue;
            }

            ItemStack output = stonecutting.assemble(new SingleRecipeInput(ItemStack.EMPTY), registryAccess);
            if (output == null || output.isEmpty()) {
                continue;
            }

            Item outputItem = output.getItem();
            Block outputBlock = outputItem instanceof BlockItem blockItem ? blockItem.getBlock() : null;
            if (outputBlock == null) {
                continue;
            }

            Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(outputBlock);
            if (verticalBlock == null) {
                continue;
            }

            Identifier recipeId = holder.id().identifier();
            if (recipeId == null || recipeId.getPath().contains("_wood_sc_")) {
                continue;
            }

            builder.add("stonecutter_recipe", recipeId.toString());
            builder.add("stonecutter_output", safeId(BuiltInRegistries.ITEM.getKey(outputItem)) + "@" + output.getCount());
            builder.add("stonecutter_ingredient", buildIngredientSignature(stonecutting));
        }

        return builder.build();
    }

    public static String buildStepFingerprint(RecipeManager recipeManager, RegistryAccess registryAccess) {
        return buildStepFingerprint(recipeManager, registryAccess, true);
    }

    public static String buildStepFingerprint(RecipeManager recipeManager,
                                              RegistryAccess registryAccess,
                                              boolean includeVerticalSlabDependentRecipes) {
        DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
        builder.add("type", "step");
        builder.add("schema_version", 13);
        builder.add("include_vertical_slab_dependent_recipes", Boolean.toString(includeVerticalSlabDependentRecipes));

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            Identifier slabId = BuiltInRegistries.BLOCK.getKey(slabBlock);
            if (slabId == null) {
                continue;
            }

            Block stepBlock = StepDynamicRegistry.getStepForSlab(slabBlock);
            if (stepBlock != null) {
                builder.add("step_pair", slabId + "->" + safeId(BuiltInRegistries.BLOCK.getKey(stepBlock)));
            }

            Block baseBlock = SlabFamilyBlockLookup.getBaseBlockForSlab(slabBlock);
            if (baseBlock != null) {
                builder.add("base_pair", slabId + "->" + safeId(BuiltInRegistries.BLOCK.getKey(baseBlock)));
            }

            if (includeVerticalSlabDependentRecipes) {
                Block verticalSlabBlock = EnhancedSlabRecipeLookup.verticalForSlab(slabBlock);
                if (verticalSlabBlock != null) {
                    builder.add("vertical_slab_pair", slabId + "->" + safeId(BuiltInRegistries.BLOCK.getKey(verticalSlabBlock)));

                    Block verticalStepBlock = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlabBlock);
                    if (verticalStepBlock != null) {
                        builder.add("vertical_step_pair", safeId(BuiltInRegistries.BLOCK.getKey(verticalSlabBlock)) + "->" + safeId(BuiltInRegistries.BLOCK.getKey(verticalStepBlock)));
                    }
                }
            }
        }

        for (Item inputItem : BuiltInRegistries.ITEM) {
            if (!(inputItem instanceof BlockItem)) {
                continue;
            }

            Identifier inputId = BuiltInRegistries.ITEM.getKey(inputItem);
            if (inputId != null && isWoodStepRelevantPath(inputId.getPath())) {
                builder.add("wood_item", inputId.toString());
            }
        }

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (!(holder.value() instanceof StonecutterRecipe stonecutting)) {
                continue;
            }

            ItemStack output = stonecutting.assemble(new SingleRecipeInput(ItemStack.EMPTY), registryAccess);
            if (output == null || output.isEmpty()) {
                continue;
            }

            Item outputItem = output.getItem();
            Block outputBlock = outputItem instanceof BlockItem blockItem ? blockItem.getBlock() : null;
            if (outputBlock == null || StepDynamicRegistry.getStepForSlab(outputBlock) == null) {
                continue;
            }

            Identifier recipeId = holder.id().identifier();
            if (recipeId == null) {
                continue;
            }

            builder.add("stonecutter_recipe", recipeId.toString());
            builder.add("stonecutter_output", safeId(BuiltInRegistries.ITEM.getKey(outputItem)) + "@" + output.getCount());
            builder.add("stonecutter_ingredient", buildIngredientSignature(stonecutting));
        }

        return builder.build();
    }

    public static String buildStepStaticFingerprint() {
        DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
        builder.add("type", "step_static");
        builder.add("schema_version", 1);

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            Identifier slabId = BuiltInRegistries.BLOCK.getKey(slabBlock);
            if (slabId == null) {
                continue;
            }

            Block stepBlock = StepDynamicRegistry.getStepForSlab(slabBlock);
            if (stepBlock != null) {
                builder.add("step_pair", slabId + "->" + safeId(BuiltInRegistries.BLOCK.getKey(stepBlock)));
            }

            Block baseBlock = SlabFamilyBlockLookup.getBaseBlockForSlab(slabBlock);
            if (baseBlock != null) {
                builder.add("base_pair", slabId + "->" + safeId(BuiltInRegistries.BLOCK.getKey(baseBlock)));
            }

            Block verticalSlabBlock = EnhancedSlabRecipeLookup.verticalForSlab(slabBlock);
            if (verticalSlabBlock != null) {
                builder.add("vertical_slab_pair", slabId + "->" + safeId(BuiltInRegistries.BLOCK.getKey(verticalSlabBlock)));

                Block verticalStepBlock = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlabBlock);
                if (verticalStepBlock != null) {
                    builder.add("vertical_step_pair", safeId(BuiltInRegistries.BLOCK.getKey(verticalSlabBlock)) + "->" + safeId(BuiltInRegistries.BLOCK.getKey(verticalStepBlock)));
                }
            }
        }

        for (Item inputItem : BuiltInRegistries.ITEM) {
            if (!(inputItem instanceof BlockItem)) {
                continue;
            }

            Identifier inputId = BuiltInRegistries.ITEM.getKey(inputItem);
            if (inputId != null && isWoodStepRelevantPath(inputId.getPath())) {
                builder.add("wood_item", inputId.toString());
            }
        }

        return builder.build();
    }

    public static String buildStepMirrorFingerprint(RecipeManager recipeManager, RegistryAccess registryAccess) {
        DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
        builder.add("type", "step_mirror");
        builder.add("schema_version", 1);

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            Identifier slabId = BuiltInRegistries.BLOCK.getKey(slabBlock);
            if (slabId == null) {
                continue;
            }

            Block stepBlock = StepDynamicRegistry.getStepForSlab(slabBlock);
            if (stepBlock != null) {
                builder.add("step_pair", slabId + "->" + safeId(BuiltInRegistries.BLOCK.getKey(stepBlock)));
            }

            Block verticalSlabBlock = EnhancedSlabRecipeLookup.verticalForSlab(slabBlock);
            if (verticalSlabBlock != null) {
                builder.add("vertical_slab_pair", slabId + "->" + safeId(BuiltInRegistries.BLOCK.getKey(verticalSlabBlock)));

                Block verticalStepBlock = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlabBlock);
                if (verticalStepBlock != null) {
                    builder.add("vertical_step_pair", safeId(BuiltInRegistries.BLOCK.getKey(verticalSlabBlock)) + "->" + safeId(BuiltInRegistries.BLOCK.getKey(verticalStepBlock)));
                }
            }
        }

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (!(holder.value() instanceof StonecutterRecipe stonecutting)) {
                continue;
            }

            ItemStack output = stonecutting.assemble(new SingleRecipeInput(ItemStack.EMPTY), registryAccess);
            if (output == null || output.isEmpty()) {
                continue;
            }

            Item outputItem = output.getItem();
            Block outputBlock = outputItem instanceof BlockItem blockItem ? blockItem.getBlock() : null;
            if (outputBlock == null || StepDynamicRegistry.getStepForSlab(outputBlock) == null) {
                continue;
            }

            Identifier recipeId = holder.id().identifier();
            if (recipeId == null) {
                continue;
            }

            builder.add("stonecutter_recipe", recipeId.toString());
            builder.add("stonecutter_output", safeId(BuiltInRegistries.ITEM.getKey(outputItem)) + "@" + output.getCount());
            builder.add("stonecutter_ingredient", buildIngredientSignature(stonecutting));
        }

        return builder.build();
    }

    public static String buildStairRecipeFingerprint(RecipeManager recipeManager, RegistryAccess registryAccess) {
        DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
        builder.add("type", "stairs");

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (!(holder.value() instanceof ShapedRecipe shapedRecipe)) {
                continue;
            }

            ItemStack result = shapedRecipe.assemble(CraftingInput.EMPTY, registryAccess);
            if (result == null || result.isEmpty()) {
                continue;
            }

            Identifier resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (resultId == null || !resultId.getPath().contains("stairs")) {
                continue;
            }

            Identifier recipeId = holder.id().identifier();
            if (recipeId == null) {
                continue;
            }

            builder.add("recipe", recipeId.toString());
            builder.add("result", resultId + "@" + result.getCount());
            builder.add("shape", shapedRecipe.getWidth() + "x" + shapedRecipe.getHeight());

            int index = 0;
            for (var ingredientOptional : shapedRecipe.getIngredients()) {
                if (!ingredientOptional.isPresent()) {
                    builder.add("ingredient_" + index, "<empty>");
                    index++;
                    continue;
                }

                var ingredient = ingredientOptional.get();
                var accepted = new ArrayList<>(ingredient.items().toList());
                if (accepted.isEmpty()) {
                    builder.add("ingredient_" + index, "<empty>");
                    index++;
                    continue;
                }

                StringBuilder acceptedIds = new StringBuilder();
                for (var acceptedItem : accepted) {
                    Identifier acceptedId = BuiltInRegistries.ITEM.getKey(acceptedItem.value());
                    if (acceptedId == null) {
                        continue;
                    }

                    if (acceptedIds.length() > 0) {
                        acceptedIds.append(',');
                    }
                    acceptedIds.append(acceptedId);
                }
                builder.add("ingredient_" + index, acceptedIds.toString());
                index++;
            }
        }

        return builder.build();
    }

    private static boolean isRealItem(@Nullable Item item) {
        return item != null && item != Items.AIR;
    }

    private static boolean isWoodRecipeRelevantPath(String path) {
        return path.endsWith("_planks")
                || path.endsWith("_log")
                || path.endsWith("_wood")
                || path.endsWith("_stem")
                || path.endsWith("_hyphae")
                || path.endsWith("_stairs")
                || path.endsWith("_slab")
                || path.endsWith("_pressure_plate")
                || path.endsWith("_button");
    }

    private static boolean isWoodStepRelevantPath(String path) {
        return path.endsWith("_planks")
                || path.endsWith("_log")
                || path.endsWith("_wood")
                || path.endsWith("_stem")
                || path.endsWith("_hyphae")
                || path.endsWith("_slab");
    }

    private static String buildIngredientSignature(StonecutterRecipe recipe) {
        try {
            var items = new ArrayList<>(recipe.input().items().toList());
            if (items.isEmpty()) {
                return "<empty>";
            }

            StringBuilder builder = new StringBuilder();
            for (var itemHolder : items) {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(itemHolder.value());
                if (itemId == null) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(',');
                }
                builder.append(itemId);
            }
            return builder.toString();
        } catch (Exception e) {
            return "<error>";
        }
    }

    private static String safeId(@Nullable Identifier id) {
        return id == null ? "<null>" : id.toString();
    }
}