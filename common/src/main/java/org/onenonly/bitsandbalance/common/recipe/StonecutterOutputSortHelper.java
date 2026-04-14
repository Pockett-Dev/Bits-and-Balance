package org.onenonly.bitsandbalance.common.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

/**
 * Shared filename ordering helper for generated stonecutter recipes.
 *
 * <p>Recipe file names are prefixed with a stable size/family bucket so stonecutter outputs
 * appear in a more intuitive order: full blocks, stairs, slabs, vertical slabs, walls,
 * steps, vertical steps, pressure plates, buttons.</p>
 */
public final class StonecutterOutputSortHelper {

    private StonecutterOutputSortHelper() {
    }

    public static String buildDirectRecipeName(String familyPrefix, Identifier inputId, Item outputItem) {
        Identifier outputId = BuiltInRegistries.ITEM.getKey(outputItem);
        String sortKey = sortKeyForOutput(outputItem);
        return sortKey + "_" + familyPrefix + "_"
                + safe(inputId.getNamespace()) + "_"
                + safe(inputId.getPath()) + "__"
                + safe(outputId.getNamespace()) + "_"
                + safe(outputId.getPath());
    }

    public static String buildDerivedRecipeName(String familyPrefix, Identifier recipeId, Item outputItem) {
        Identifier outputId = BuiltInRegistries.ITEM.getKey(outputItem);
        String sortKey = sortKeyForOutput(outputItem);
        return sortKey + "_" + familyPrefix + "_"
                + safe(recipeId.getNamespace()) + "_"
                + safe(recipeId.getPath()) + "__"
                + safe(outputId.getNamespace()) + "_"
                + safe(outputId.getPath());
    }

    public static String sortKeyForOutput(Item outputItem) {
        Identifier outputId = BuiltInRegistries.ITEM.getKey(outputItem);
        String path = outputId != null ? outputId.getPath() : "";

        if (outputItem instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block != null) {
                if (VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(block) != null) {
                    return "60_vertical_step";
                }
                if (StepDynamicRegistry.getSlabForStep(block) != null) {
                    return "50_step";
                }
                if (VerticalSlabDynamicRegistry.getSlabForVertical(block) != null) {
                    return "40_vertical_slab";
                }
                if (block instanceof WallBlock) {
                    return "45_wall";
                }
            }
        }

        if (path.endsWith("_stairs")) {
            return "20_stairs";
        }
        if (path.endsWith("_slab")) {
            return "30_slab";
        }
        if (path.endsWith("_wall")) {
            return "45_wall";
        }
        if (path.endsWith("_pressure_plate")) {
            return "70_pressure_plate";
        }
        if (path.endsWith("_button")) {
            return "80_button";
        }
        if (path.startsWith("stripped_")) {
            return "10a_full_block";
        }
        if (path.endsWith("_planks")) {
            return "10b_full_block";
        }
        return "10c_full_block";
    }

    private static String safe(String value) {
        return value.replace('/', '_');
    }
}
