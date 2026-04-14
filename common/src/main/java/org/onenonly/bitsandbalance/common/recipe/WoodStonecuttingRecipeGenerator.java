package org.onenonly.bitsandbalance.common.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates stonecutter recipe JSON entries for wood-based items discovered through registry scanning.
 *
 * <h3>Plank rules (1 plank input)</h3>
 * <ul>
 *   <li>→ 1 Stair</li>
 *   <li>→ 2 Slabs</li>
 *   <li>→ 4 Pressure Plates</li>
 *   <li>→ 4 Buttons</li>
 * </ul>
 *
 * <h3>Log / Wood / Stem rules — 4× the plank outputs (1 log input)</h3>
 * <ul>
 *   <li>→ 1 Stripped Log/Wood/Stem</li>
 *   <li>→ 4 Planks</li>
 *   <li>→ 4 Stairs</li>
 *   <li>→ 8 Slabs</li>
 *   <li>→ 16 Pressure Plates</li>
 *   <li>→ 16 Buttons</li>
 * </ul>
 *
 * <p>Works with any mod that follows standard {@code <namespace>:<base>_planks},
 * {@code <namespace>:<base>_log}, {@code <namespace>:<base>_wood}, or
 * {@code <namespace>:<base>_stem} naming conventions.</p>
 */
public final class WoodStonecuttingRecipeGenerator {

    private WoodStonecuttingRecipeGenerator() {}

    /**
     * Scans {@link BuiltInRegistries#ITEM} and produces a filename → JSON map for all
     * discoverable wood stonecutting recipes.  Call this after all mods have registered their items.
     *
     * @return mutable map of {@code safeName → recipeJson} ready to be written to a data pack
     */
    public static Map<String, String> generate() {
        return generate(true, false);
    }

    public static Map<String, String> generate(boolean includeVerticalSlabs, boolean includeSteps) {
        Map<String, String> recipes = new LinkedHashMap<>();

        for (Item inputItem : BuiltInRegistries.ITEM) {
            // Only BlockItems represent physical blocks that can be placed in a stonecutter.
            if (!(inputItem instanceof BlockItem)) continue;

            Identifier inputId = BuiltInRegistries.ITEM.getKey(inputItem);
            if (inputId == null) continue;

            String ns   = inputId.getNamespace();
            String path = inputId.getPath();

            if (path.endsWith("_planks")) {
                String base = path.substring(0, path.length() - "_planks".length());
                addPlankRecipes(recipes, inputId, ns, base, includeVerticalSlabs, includeSteps);

            } else if (path.endsWith("_log")) {
                addWoodTypeRecipes(recipes, inputId, ns, path, "_log", includeVerticalSlabs, includeSteps);

            } else if (path.endsWith("_wood")) {
                addWoodTypeRecipes(recipes, inputId, ns, path, "_wood", includeVerticalSlabs, includeSteps);

            } else if (path.endsWith("_stem")) {
                addWoodTypeRecipes(recipes, inputId, ns, path, "_stem", includeVerticalSlabs, includeSteps);

            } else if (path.endsWith("_hyphae")) {
                addWoodTypeRecipes(recipes, inputId, ns, path, "_hyphae", includeVerticalSlabs, includeSteps);
            }
        }

        return recipes;
    }

    // ── Plank output recipes ─────────────────────────────────────────────────

    private static void addPlankRecipes(Map<String, String> out,
                                         Identifier plankId, String ns, String base,
                                         boolean includeVerticalSlabs, boolean includeSteps) {
        addIfExists(out, plankId, ns, base + "_stairs",         1, includeVerticalSlabs, includeSteps);
        addIfExists(out, plankId, ns, base + "_slab",           2, includeVerticalSlabs, includeSteps);
        addIfExists(out, plankId, ns, base + "_pressure_plate", 4);
        addIfExists(out, plankId, ns, base + "_button",         4);
    }

    // ── Log / Wood / Stem output recipes ─────────────────────────────────────

    private static void addWoodTypeRecipes(Map<String, String> out,
                                           Identifier inputId, String ns, String path, String suffix,
                                           boolean includeVerticalSlabs, boolean includeSteps) {
        if (path.startsWith("stripped_")) {
            String base = path.substring("stripped_".length(), path.length() - suffix.length());
            addProcessedWoodTypeRecipes(out, inputId, ns, base, suffix, includeVerticalSlabs, includeSteps);
            return;
        }

        String base = path.substring(0, path.length() - suffix.length());
        addLogRecipes(out, inputId, ns, base, suffix, includeVerticalSlabs, includeSteps);
    }

    private static void addLogRecipes(Map<String, String> out,
                                       Identifier logId, String ns, String base, String suffix,
                                       boolean includeVerticalSlabs, boolean includeSteps) {
        addIfExists(out, logId, ns, "stripped_" + base + suffix, 1, includeVerticalSlabs, includeSteps);
        addIfExists(out, logId, ns, base + "_planks",         4, includeVerticalSlabs, includeSteps);
        addIfExists(out, logId, ns, base + "_stairs",         4, includeVerticalSlabs, includeSteps);
        addIfExists(out, logId, ns, base + "_slab",           8, includeVerticalSlabs, includeSteps);
        addIfExists(out, logId, ns, base + "_pressure_plate", 16);
        addIfExists(out, logId, ns, base + "_button",         16);
    }

    private static void addProcessedWoodTypeRecipes(Map<String, String> out,
                                                    Identifier inputId, String ns, String base, String suffix,
                                                    boolean includeVerticalSlabs, boolean includeSteps) {
        addIfExists(out, inputId, ns, base + "_planks",         4, includeVerticalSlabs, includeSteps);
        addIfExists(out, inputId, ns, base + "_stairs",         4, includeVerticalSlabs, includeSteps);
        addIfExists(out, inputId, ns, base + "_slab",           8, includeVerticalSlabs, includeSteps);
        addIfExists(out, inputId, ns, base + "_pressure_plate", 16);
        addIfExists(out, inputId, ns, base + "_button",         16);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Adds one stonecutting recipe entry if {@code outputPath} is registered in the item registry.
     *
     * <p>The safe filename is deterministic and unique per (input, output) pair so that
     * e.g. both {@code oak_log} and {@code oak_wood} can independently stonecut into planks
     * without colliding.</p>
     */
    private static void addIfExists(Map<String, String> out,
                                     Identifier inputId, String ns, String outputPath, int count) {
        addIfExists(out, inputId, ns, outputPath, count, true, true);
    }

    private static void addIfExists(Map<String, String> out,
                                     Identifier inputId, String ns, String outputPath, int count,
                                     boolean includeVerticalSlabs, boolean includeSteps) {
        Identifier outputId = Identifier.fromNamespaceAndPath(ns, outputPath);
        if (!BuiltInRegistries.ITEM.containsKey(outputId)) return;
        Item outputItem = BuiltInRegistries.ITEM.get(outputId).map(h -> h.value()).orElse(null);
        if (outputItem == null) return;

        String safeName = StonecutterOutputSortHelper.buildDirectRecipeName("wood_sc", inputId, outputItem);
        String ingredientJson = itemIngredient(inputId);

        String json = "{\n"
                + "  \"type\": \"minecraft:stonecutting\",\n"
            + "  \"ingredient\": " + ingredientJson + ",\n"
                + "  \"result\": {\n"
                + "    \"id\": \"" + outputId + "\",\n"
                + "    \"count\": " + count + "\n"
                + "  }\n"
                + "}";

        out.put(safeName, json);

        if (StonecutterOutputSortHelper.sortKeyForOutput(outputItem).startsWith("30_slab")) {
            Block slabBlock = (outputItem instanceof BlockItem bi) ? bi.getBlock() : null;
            if (slabBlock != null) {
                if (includeVerticalSlabs) {
                    Block vertBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock);
                    if (vertBlock != null) {
                        Item vertItem = vertBlock.asItem();
                        Identifier vertId = BuiltInRegistries.ITEM.getKey(vertItem);
                        if (vertId != null) {
                            String verticalSafeName = StonecutterOutputSortHelper.buildDirectRecipeName("wood_sc", inputId, vertItem);
                            String vJson = "{\n"
                                    + "  \"type\": \"minecraft:stonecutting\",\n"
                                    + "  \"ingredient\": " + ingredientJson + ",\n"
                                    + "  \"result\": {\n"
                                    + "    \"id\": \"" + vertId + "\",\n"
                                    + "    \"count\": " + count + "\n"
                                    + "  }\n"
                                    + "}";
                            out.put(verticalSafeName, vJson);
                        }
                    }
                }

                if (includeSteps) {
                    Block stepBlock = StepDynamicRegistry.getStepForSlab(slabBlock);
                    if (stepBlock != null) {
                        addDerivedFamilyStonecutting(out, inputId, stepBlock.asItem(), count * 2);
                    }

                    Block verticalSlabBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock);
                    if (verticalSlabBlock != null) {
                        Block verticalStepBlock = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlabBlock);
                        if (verticalStepBlock != null) {
                            addDerivedFamilyStonecutting(out, inputId, verticalStepBlock.asItem(), count * 2);
                        }
                    }
                }
            }
        }
    }

    private static void addDerivedFamilyStonecutting(Map<String, String> out, Identifier inputId, Item outputItem, int count) {
        Identifier outputId = BuiltInRegistries.ITEM.getKey(outputItem);
        if (outputId == null) {
            return;
        }

        String recipeType = "minecraft:stonecutting";

        String safeName = StonecutterOutputSortHelper.buildDirectRecipeName("wood_sc", inputId, outputItem);
        String json = "{\n"
                + "  \"type\": \"" + recipeType + "\",\n"
                + "  \"ingredient\": " + itemIngredient(inputId) + ",\n"
                + "  \"result\": {\n"
                + "    \"id\": \"" + outputId + "\",\n"
                + "    \"count\": " + count + "\n"
                + "  }\n"
                + "}";

        out.put(safeName, json);
    }

    private static String itemIngredient(Identifier itemId) {
        return "\"" + itemId + "\"";
    }
}
