package org.onenonly.bitsandbalance.common.compat.jei;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Collects dynamically generated enhanced-slab item stacks that JEI may miss when it snapshots
 * creative tab contents before late-generated compat content has stabilized.
 */
public final class JeiExtraIngredients {

    private JeiExtraIngredients() {
    }

    public static List<ItemStack> buildEnhancedSlabItems(boolean includeVerticalSlabs, boolean includeSteps) {
        List<ItemStack> out = new ArrayList<>();
        Set<Item> seen = new LinkedHashSet<>();

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            if (!(slabBlock instanceof SlabBlock)) {
                continue;
            }

            if (includeVerticalSlabs) {
                addFromSource(out, seen, slabBlock, VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock));
            }

            if (includeSteps) {
                addFromSource(out, seen, slabBlock, StepDynamicRegistry.getStepForSlab(slabBlock));

                Block verticalSlab = VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock);
                if (verticalSlab != null) {
                    addFromSource(out, seen, slabBlock, VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlab));
                }
            }
        }

        return out;
    }

    private static void addFromSource(List<ItemStack> out, Set<Item> seen, Block sourceBlock, Block outputBlock) {
        if (sourceBlock == null || outputBlock == null) {
            return;
        }
        Item outputItem = outputBlock.asItem();
        if (outputItem == null || outputItem == Items.AIR || !seen.add(outputItem)) {
            return;
        }

        out.add(new ItemStack(outputItem));
    }
}