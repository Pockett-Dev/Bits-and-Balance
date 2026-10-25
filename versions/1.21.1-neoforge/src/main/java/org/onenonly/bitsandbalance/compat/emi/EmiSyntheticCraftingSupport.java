package org.onenonly.bitsandbalance.compat.emi;

import com.mojang.logging.LogUtils;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.fml.ModList;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.compat.jei.JeiSyntheticCraftingRecipes;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Displays code-backed slab and step conversions that have no datapack recipe entry. */
final class EmiSyntheticCraftingSupport {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> GENERIC_RECIPE_PATHS = Set.of(
            "base_block_recovery", "step_from_slab", "step_convert",
            "vertical_step_to_step", "vertical_slab_convert", "vertical_slab_from_planks",
            "vertical_step_from_vertical_slab", "vertical_slab_from_vertical_step"
    );

    private EmiSyntheticCraftingSupport() {
    }

    static void register(EmiRegistry registry) {
        // JEMI imports JEI's synthetic crafting displays when both viewers are installed.
        if (ModList.get().isLoaded("jei")) {
            return;
        }

        List<RecipeHolder<CraftingRecipe>> synthetic = JeiSyntheticCraftingRecipes.buildAll(
                Config.enableEnhancedSlabs && Config.enhancedSlabsVerticalSlabs,
                Config.enableEnhancedSlabs && Config.enhancedSlabsSteps
        );
        if (synthetic.isEmpty() || Minecraft.getInstance().level == null) {
            return;
        }

        int registered = 0;
        int alreadyLoaded = 0;
        for (RecipeHolder<CraftingRecipe> holder : synthetic) {
            String path = holder.id().getPath().substring("jei/".length());
            String generatedPath = generatedPathFor(path);
            if (generatedPath != null && registry.getRecipeManager().byKey(
                    ResourceLocation.fromNamespaceAndPath("bitsandbalance", generatedPath)).isPresent()) {
                alreadyLoaded++;
                continue;
            }

            CraftingRecipe recipe = holder.value();
            List<EmiIngredient> inputs = inputs(recipe);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("bitsandbalance", "/emi_crafting/" + path);
            registry.addRecipe(new EmiCraftingRecipe(
                    inputs,
                    EmiStack.of(recipe.getResultItem(Minecraft.getInstance().level.registryAccess())),
                    id,
                    !(recipe instanceof ShapedRecipe)
            ));
            registered++;
        }

        registry.removeRecipes(recipe -> {
            ResourceLocation id = recipe.getId();
            return id != null && "bitsandbalance".equals(id.getNamespace())
                    && GENERIC_RECIPE_PATHS.contains(id.getPath());
        });
        LOGGER.info("[bitsandbalance] Registered {} synthetic EMI crafting recipes ({} already loaded)",
                registered, alreadyLoaded);
    }

    private static List<EmiIngredient> inputs(CraftingRecipe recipe) {
        List<Ingredient> ingredients = recipe.getIngredients();
        if (recipe instanceof ShapedRecipe shaped && shaped.getHeight() > 1) {
            List<EmiIngredient> grid = new ArrayList<>(9);
            for (int row = 0; row < shaped.getHeight(); row++) {
                for (int column = 0; column < 3; column++) {
                    grid.add(column < shaped.getWidth()
                            ? EmiIngredient.of(ingredients.get(row * shaped.getWidth() + column))
                            : EmiStack.EMPTY);
                }
            }
            return grid;
        }
        return ingredients.stream().map(EmiIngredient::of).toList();
    }

    private static String generatedPathFor(String path) {
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
            return namedPath("vertical_slab_from_planks", inputNamespace, inputPath, outputNamespace, outputPath);
        }
        if ("vertical_slabs".equals(family) && "from_slab".equals(action)) {
            return canonicalPath("vertical_slabs", "from_slab", outputNamespace, outputPath);
        }
        if ("slabs".equals(family) && "from_vertical_slab".equals(action)) {
            return canonicalPath("slabs", "from_vertical_slab", outputNamespace, outputPath);
        }
        if ("steps".equals(family) && "from_slab".equals(action)) {
            return canonicalPath("steps", "from_slab", outputNamespace, outputPath);
        }
        if ("slabs".equals(family) && "from_step".equals(action)) {
            return namedPath("slab_from_step", inputNamespace, inputPath, outputNamespace, outputPath);
        }
        if ("vertical_steps".equals(family) && "from_vertical_slab".equals(action)) {
            return canonicalPath("vertical_steps", "from_vertical_slab", outputNamespace, outputPath);
        }
        if ("vertical_slabs".equals(family) && "from_vertical_step".equals(action)) {
            return canonicalPath("vertical_slabs", "from_vertical_step", outputNamespace, outputPath);
        }
        if ("vertical_steps".equals(family) && "from_step".equals(action)) {
            return canonicalPath("vertical_steps", "from_step", outputNamespace, outputPath);
        }
        if ("steps".equals(family) && "from_vertical_step".equals(action)) {
            return canonicalPath("steps", "from_vertical_step", outputNamespace, outputPath);
        }
        return null;
    }

    private static String canonicalPath(String family, String action, String namespace, String path) {
        return "crafting/" + family + "/" + action + "/" + namespace + "/" + path;
    }

    private static String namedPath(String prefix, String inputNamespace, String inputPath,
                                    String outputNamespace, String outputPath) {
        return prefix + "__" + sanitize(inputNamespace, inputPath)
                + "__to__" + sanitize(outputNamespace, outputPath);
    }

    private static String sanitize(String namespace, String path) {
        return namespace + "__" + path.replace('/', '_');
    }
}
