package org.onenonly.bitsandbalance.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.recipe.SlabFamilyBlockLookup;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

public final class EnhancedSlabItemSupport {
    public enum ModelType {
        NONE,
        VERTICAL_SLAB,
        STEP,
        VERTICAL_STEP
    }

    private EnhancedSlabItemSupport() {
    }

    public static @Nullable Block sourceSlabForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return sourceSlabForItem(stack.getItem());
    }

    public static @Nullable Block sourceSlabForItem(Item item) {
        if (!(item instanceof BlockItem blockItem)) return null;
        return sourceSlabForBlock(blockItem.getBlock());
    }

    public static @Nullable Block modelSourceBlockForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return modelSourceBlockForItem(stack.getItem());
    }

    public static @Nullable Block modelSourceBlockForItem(Item item) {
        if (!(item instanceof BlockItem blockItem)) return null;
        return modelSourceBlockForBlock(blockItem.getBlock());
    }

    public static @Nullable Block modelSourceBlockForBlock(Block block) {
        Block sourceSlab = sourceSlabForBlock(block);
        if (sourceSlab == null) return null;

        ModelType modelType = modelTypeForBlock(block);
        if (modelType == ModelType.VERTICAL_SLAB || modelType == ModelType.VERTICAL_STEP) {
            Block baseBlock = SlabFamilyBlockLookup.getBaseBlockForSlab(sourceSlab);
            if (baseBlock != null && baseBlock.asItem() != Items.AIR) {
                return baseBlock;
            }
        }

        return sourceSlab;
    }

    public static ModelType modelTypeForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ModelType.NONE;
        return modelTypeForItem(stack.getItem());
    }

    public static ModelType modelTypeForItem(Item item) {
        if (!(item instanceof BlockItem blockItem)) return ModelType.NONE;
        return modelTypeForBlock(blockItem.getBlock());
    }

    public static ModelType modelTypeForBlock(Block block) {
        if (block == null) return ModelType.NONE;
        if (block instanceof FixedVerticalSlabBlock) return ModelType.VERTICAL_SLAB;
        if (block instanceof FixedStepBlock) return ModelType.STEP;
        if (block instanceof FixedVerticalStepBlock) return ModelType.VERTICAL_STEP;
        if (StepDynamicRegistry.getSlabForStep(block) != null) return ModelType.STEP;
        if (VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(block) != null) return ModelType.VERTICAL_STEP;
        if (VerticalSlabDynamicRegistry.getSlabForVertical(block) != null) return ModelType.VERTICAL_SLAB;
        return ModelType.NONE;
    }

    public static @Nullable Block sourceSlabForBlock(Block block) {
        if (block == null) return null;
        if (block instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            return fixedVerticalSlab.getSourceSlab();
        }
        if (block instanceof FixedStepBlock fixedStep) {
            return fixedStep.getSourceSlab();
        }
        if (block instanceof FixedVerticalStepBlock fixedVerticalStep) {
            Block sourceVerticalSlab = fixedVerticalStep.getSourceVerticalSlab();
            Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
            return sourceSlab != null ? sourceSlab : sourceVerticalSlab;
        }

        Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(block);
        if (sourceSlab != null) return sourceSlab;

        sourceSlab = StepDynamicRegistry.getSlabForStep(block);
        if (sourceSlab != null) return sourceSlab;

        Block sourceVerticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(block);
        if (sourceVerticalSlab == null) return null;

        sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
        return sourceSlab != null ? sourceSlab : sourceVerticalSlab;
    }

    public static Component displayName(ItemStack stack, @Nullable Block sourceSlab, String suffix) {
        if (sourceSlab == null) {
            return Component.literal(suffix);
        }

        String base = sourceSlabBaseName(sourceSlab);
        if (base.isBlank()) {
            return Component.literal(suffix);
        }

        return Component.literal(base + " " + suffix);
    }

    private static String sourceSlabBaseName(Block sourceSlab) {
        Item sourceItem = sourceSlab.asItem();
        String base = sourceItem != null && sourceItem != Items.AIR
                ? new ItemStack(sourceItem).getHoverName().getString()
                : Component.translatable(sourceSlab.getDescriptionId()).getString();
        if (base.endsWith(" Slab")) {
            base = base.substring(0, base.length() - " Slab".length());
        }
        return base.trim();
    }
}
