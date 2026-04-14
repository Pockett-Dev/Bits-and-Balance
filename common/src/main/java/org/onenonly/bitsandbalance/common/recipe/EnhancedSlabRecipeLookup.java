package org.onenonly.bitsandbalance.common.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Shared runtime lookup helpers for dynamic slab-family recipe resolution.
 *
 * <p>The crafting recipes should not rely exclusively on the transient runtime maps,
 * because if a loader populates them late or incompletely the recipe JSONs still load
 * but assemble to empty. These helpers first use the direct maps, then fall back to
 * deterministic id-based resolution against the live block registry.</p>
 */
public final class EnhancedSlabRecipeLookup {
    private static final Map<Block, Block> SLAB_TO_VERTICAL_CACHE = new IdentityHashMap<>();
    private static final Map<Block, Block> VERTICAL_TO_SLAB_CACHE = new IdentityHashMap<>();
    private static final Map<Block, Block> SLAB_TO_STEP_CACHE = new IdentityHashMap<>();
    private static final Map<Block, Block> STEP_TO_SLAB_CACHE = new IdentityHashMap<>();
    private static final Map<Block, Block> VERTICAL_SLAB_TO_STEP_CACHE = new IdentityHashMap<>();
    private static final Map<Block, Block> VERTICAL_STEP_TO_SLAB_CACHE = new IdentityHashMap<>();

    private EnhancedSlabRecipeLookup() {
    }

    public static @Nullable Block verticalForSlab(@Nullable Block slabBlock) {
        if (slabBlock == null) return null;

        Block resolved = VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock);
        if (resolved != null) return resolved;

        resolved = SLAB_TO_VERTICAL_CACHE.get(slabBlock);
        if (resolved != null) return resolved;

        Identifier slabId = BuiltInRegistries.BLOCK.getKey(slabBlock);
        if (slabId == null) return null;

        Identifier verticalId = VerticalSlabDynamicRegistry.idForSlab(slabId, BitsAndBalanceCommon.MOD_ID);
        resolved = BuiltInRegistries.BLOCK.getOptional(verticalId).orElse(null);
        if (resolved != null) {
            SLAB_TO_VERTICAL_CACHE.put(slabBlock, resolved);
            VERTICAL_TO_SLAB_CACHE.put(resolved, slabBlock);
        }
        return resolved;
    }

    public static @Nullable Block slabForVertical(@Nullable Block verticalSlabBlock) {
        if (verticalSlabBlock == null) return null;

        if (verticalSlabBlock instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            return fixedVerticalSlab.getSourceSlab();
        }

        Block resolved = VerticalSlabDynamicRegistry.getSlabForVertical(verticalSlabBlock);
        if (resolved != null) return resolved;

        resolved = VERTICAL_TO_SLAB_CACHE.get(verticalSlabBlock);
        if (resolved != null) return resolved;

        Identifier targetId = BuiltInRegistries.BLOCK.getKey(verticalSlabBlock);
        if (targetId == null) return null;

        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof SlabBlock)) continue;

            Identifier slabId = BuiltInRegistries.BLOCK.getKey(block);
            if (slabId == null) continue;

            Identifier expectedVerticalId = VerticalSlabDynamicRegistry.idForSlab(slabId, BitsAndBalanceCommon.MOD_ID);
            if (!targetId.equals(expectedVerticalId)) continue;

            VERTICAL_TO_SLAB_CACHE.put(verticalSlabBlock, block);
            SLAB_TO_VERTICAL_CACHE.put(block, verticalSlabBlock);
            return block;
        }

        return null;
    }

    public static @Nullable Block stepForSlab(@Nullable Block slabBlock) {
        if (slabBlock == null) return null;
        if (!(slabBlock instanceof SlabBlock)) return null;

        Block resolved = StepDynamicRegistry.getStepForSlab(slabBlock);
        if (resolved != null) return resolved;

        resolved = SLAB_TO_STEP_CACHE.get(slabBlock);
        if (resolved != null) return resolved;

        Identifier slabId = BuiltInRegistries.BLOCK.getKey(slabBlock);
        if (slabId == null) return null;

        Identifier stepId = StepDynamicRegistry.idForSlab(slabId, BitsAndBalanceCommon.MOD_ID);
        resolved = BuiltInRegistries.BLOCK.getOptional(stepId).orElse(null);
        if (resolved != null) {
            SLAB_TO_STEP_CACHE.put(slabBlock, resolved);
            STEP_TO_SLAB_CACHE.put(resolved, slabBlock);
        }
        return resolved;
    }

    public static @Nullable Block slabForStep(@Nullable Block stepBlock) {
        if (stepBlock == null) return null;

        if (stepBlock instanceof FixedStepBlock fixedStep) {
            return fixedStep.getSourceSlab();
        }

        Block resolved = StepDynamicRegistry.getSlabForStep(stepBlock);
        if (resolved != null) return resolved;

        resolved = STEP_TO_SLAB_CACHE.get(stepBlock);
        if (resolved != null) return resolved;

        Identifier targetId = BuiltInRegistries.BLOCK.getKey(stepBlock);
        if (targetId == null) return null;

        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof SlabBlock)) continue;

            Identifier slabId = BuiltInRegistries.BLOCK.getKey(block);
            if (slabId == null) continue;

            Identifier expectedStepId = StepDynamicRegistry.idForSlab(slabId, BitsAndBalanceCommon.MOD_ID);
            if (!targetId.equals(expectedStepId)) continue;

            STEP_TO_SLAB_CACHE.put(stepBlock, block);
            SLAB_TO_STEP_CACHE.put(block, stepBlock);
            return block;
        }

        return null;
    }

    public static @Nullable Block verticalStepForVerticalSlab(@Nullable Block verticalSlabBlock) {
        if (verticalSlabBlock == null) return null;

        Block resolved = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlabBlock);
        if (resolved != null) return resolved;

        resolved = VERTICAL_SLAB_TO_STEP_CACHE.get(verticalSlabBlock);
        if (resolved != null) return resolved;

        Identifier verticalSlabId = BuiltInRegistries.BLOCK.getKey(verticalSlabBlock);
        if (verticalSlabId == null) return null;

        Identifier stepId = VerticalStepDynamicRegistry.idForVerticalSlab(verticalSlabId, BitsAndBalanceCommon.MOD_ID);
        resolved = BuiltInRegistries.BLOCK.getOptional(stepId).orElse(null);
        if (resolved != null) {
            VERTICAL_SLAB_TO_STEP_CACHE.put(verticalSlabBlock, resolved);
            VERTICAL_STEP_TO_SLAB_CACHE.put(resolved, verticalSlabBlock);
        }
        return resolved;
    }

    public static @Nullable Block verticalSlabForVerticalStep(@Nullable Block verticalStepBlock) {
        if (verticalStepBlock == null) return null;

        if (verticalStepBlock instanceof FixedVerticalStepBlock fixedVerticalStep) {
            return fixedVerticalStep.getSourceVerticalSlab();
        }

        Block resolved = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(verticalStepBlock);
        if (resolved != null) return resolved;

        resolved = VERTICAL_STEP_TO_SLAB_CACHE.get(verticalStepBlock);
        if (resolved != null) return resolved;

        Identifier targetId = BuiltInRegistries.BLOCK.getKey(verticalStepBlock);
        if (targetId == null) return null;

        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof FixedVerticalSlabBlock)) continue;

            Identifier verticalSlabId = BuiltInRegistries.BLOCK.getKey(block);
            if (verticalSlabId == null) continue;

            Identifier expectedStepId = VerticalStepDynamicRegistry.idForVerticalSlab(verticalSlabId, BitsAndBalanceCommon.MOD_ID);
            if (!targetId.equals(expectedStepId)) continue;

            VERTICAL_STEP_TO_SLAB_CACHE.put(verticalStepBlock, block);
            VERTICAL_SLAB_TO_STEP_CACHE.put(block, verticalStepBlock);
            return block;
        }

        return null;
    }
}