package org.onenonly.bitsandbalance.common.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Injects reusable family tags into dynamically registered enhanced-slab variants after tags load.
 *
 * <p>These variants are created at runtime, so static JSON alone cannot keep block/item family tags
 * up to date. The tag manager rebinds holders from scratch on every reload, so this must run after
 * each tag load on both Fabric and NeoForge.</p>
 */
public final class DynamicVariantFamilyTagInjector {

    public static final TagKey<Block> VERTICAL_SLAB_BLOCKS =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("bitsandbalance", "vertical_slabs"));
    public static final TagKey<Block> STEP_BLOCKS =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("bitsandbalance", "steps"));
    public static final TagKey<Block> VERTICAL_STEP_BLOCKS =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("bitsandbalance", "vertical_steps"));

    public static final TagKey<Item> VERTICAL_SLAB_ITEMS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("bitsandbalance", "vertical_slabs"));
    public static final TagKey<Item> STEP_ITEMS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("bitsandbalance", "steps"));
    public static final TagKey<Item> VERTICAL_STEP_ITEMS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("bitsandbalance", "vertical_steps"));

    private static final Field TAGS_FIELD;

    static {
        Field found = null;
        for (Field field : Holder.Reference.class.getDeclaredFields()) {
            if (Set.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    found = field;
                } catch (Throwable ignored) {
                }
                break;
            }
        }
        TAGS_FIELD = found;
    }

    private DynamicVariantFamilyTagInjector() {
    }

    public static void injectFamilyTags() {
        if (TAGS_FIELD == null) return;

        for (Block verticalSlab : VerticalSlabDynamicRegistry.getAllVerticalBlocksSnapshot()) {
            bitsandbalance$mergeTag(verticalSlab.builtInRegistryHolder(), VERTICAL_SLAB_BLOCKS);
            bitsandbalance$mergeItemTagForId(BuiltInRegistries.BLOCK.getKey(verticalSlab), VERTICAL_SLAB_ITEMS);
        }

        for (Block step : StepDynamicRegistry.getAllStepBlocksSnapshot()) {
            Identifier stepId = BuiltInRegistries.BLOCK.getKey(step);
            bitsandbalance$mergeTag(step.builtInRegistryHolder(), STEP_BLOCKS);
            bitsandbalance$mergeItemTagForId(stepId, STEP_ITEMS);
            bitsandbalance$mergeItemTagForId(bitsandbalance$legacyMalformedStepItemId(stepId), STEP_ITEMS);
        }

        for (Block verticalStep : VerticalStepDynamicRegistry.getAllVerticalStepBlocksSnapshot()) {
            Identifier verticalStepId = BuiltInRegistries.BLOCK.getKey(verticalStep);
            bitsandbalance$mergeTag(verticalStep.builtInRegistryHolder(), VERTICAL_STEP_BLOCKS);
            bitsandbalance$mergeItemTagForId(verticalStepId, VERTICAL_STEP_ITEMS);
            bitsandbalance$mergeItemTagForId(bitsandbalance$legacyMalformedStepItemId(verticalStepId), VERTICAL_STEP_ITEMS);
        }
    }

    private static Identifier bitsandbalance$legacyMalformedStepItemId(Identifier stepId) {
        if (stepId == null) return null;
        String path = stepId.getPath();
        if (!path.matches(".*_step_\\d+$")) return null;
        return Identifier.fromNamespaceAndPath(stepId.getNamespace(), path + "_step");
    }

    private static void bitsandbalance$mergeItemTagForId(Identifier itemId, TagKey<Item> tag) {
        if (itemId == null || tag == null) return;
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) return;
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (item == null) return;
        bitsandbalance$mergeTag(item.builtInRegistryHolder(), tag);
    }

    private static <T> void bitsandbalance$mergeTag(Holder.Reference<T> holder, TagKey<T> tag) {
        if (holder == null || tag == null || TAGS_FIELD == null) return;

        try {
            @SuppressWarnings("unchecked")
            Set<TagKey<T>> existing = (Set<TagKey<T>>) TAGS_FIELD.get(holder);
            Set<TagKey<T>> merged = new HashSet<>(existing != null ? existing : Set.of());
            merged.add(tag);
            TAGS_FIELD.set(holder, Set.copyOf(merged));
        } catch (Throwable ignored) {
        }
    }
}