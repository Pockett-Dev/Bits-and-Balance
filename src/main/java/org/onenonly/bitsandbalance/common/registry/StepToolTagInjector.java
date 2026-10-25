package org.onenonly.bitsandbalance.common.registry;

import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.recipe.SlabFamilyBlockLookup;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Injects tool-related block tags into dynamically-registered step/vertical-step blocks
 * after tags are loaded, so that systems like Jade can correctly display the required tool.
 *
 * <p>Must be called on every tag reload (e.g. /reload) because vanilla holder tag-binding
 * replaces the tag set from scratch.</p>
 */
public final class StepToolTagInjector {

    /** Tool-type and tier tags that Jade (and other systems) read to determine displayed tool. */
    private static final List<TagKey<Block>> TOOL_TAGS = List.of(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.MINEABLE_WITH_AXE,
            BlockTags.MINEABLE_WITH_SHOVEL,
            BlockTags.MINEABLE_WITH_HOE,
            BlockTags.NEEDS_STONE_TOOL,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.NEEDS_DIAMOND_TOOL
    );

    /** See {@link VerticalSlabToolTagInjector} for rationale. */
    private static final Field TAGS_FIELD;

    static {
        Field found = null;
        for (Field f : Holder.Reference.class.getDeclaredFields()) {
            if (Set.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    found = f;
                } catch (Throwable ignored) {
                }
                break;
            }
        }
        TAGS_FIELD = found;
    }

    private StepToolTagInjector() {
    }

    public static void injectToolTags() {
        if (TAGS_FIELD == null) return;

        // Steps: copy tool/tier tags from each source slab into its generated step block.
        for (Block stepBlock : StepDynamicRegistry.getAllStepBlocksSnapshot()) {
            Block sourceSlab = stepBlock instanceof FixedStepBlock fixed ? fixed.getSourceSlab() : null;
            if (sourceSlab == null) sourceSlab = StepDynamicRegistry.getSlabForStep(stepBlock);

            Set<TagKey<Block>> toAdd = bitsandbalance$collectToolTags(
                    sourceSlab,
                    SlabFamilyBlockLookup.getBaseBlockForSlab(sourceSlab)
            );
            bitsandbalance$mergeToolTagsIntoBlock(stepBlock, toAdd);
        }

        // Vertical steps: copy from each source vertical slab into its generated vertical-step block.
        for (Block verticalStepBlock : VerticalStepDynamicRegistry.getAllVerticalStepBlocksSnapshot()) {
            Block sourceVerticalSlab = verticalStepBlock instanceof FixedVerticalStepBlock fixed
                    ? fixed.getSourceVerticalSlab()
                    : null;
            if (sourceVerticalSlab == null) {
                sourceVerticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(verticalStepBlock);
            }

            // Vertical-slab blocks are our own, and may rely on VerticalSlabToolTagInjector.
            // Also include the underlying source slab (and its base full block) as a robust fallback.
            Block underlyingSlab = sourceVerticalSlab instanceof FixedVerticalSlabBlock fvs ? fvs.getSourceSlab() : null;

            Set<TagKey<Block>> toAdd = bitsandbalance$collectToolTags(
                    sourceVerticalSlab,
                    underlyingSlab,
                    SlabFamilyBlockLookup.getBaseBlockForSlab(underlyingSlab)
            );
            bitsandbalance$mergeToolTagsIntoBlock(verticalStepBlock, toAdd);
        }
    }

    private static Set<TagKey<Block>> bitsandbalance$collectToolTags(@Nullable Block... sources) {
        if (sources == null || sources.length == 0) return Set.of();

        try {
            Set<TagKey<Block>> toAdd = new HashSet<>();
            for (Block source : sources) {
                if (source == null) continue;
                Holder.Reference<Block> holder = source.builtInRegistryHolder();
                for (TagKey<Block> tag : TOOL_TAGS) {
                    if (holder.is(tag)) {
                        toAdd.add(tag);
                    }
                }
            }
            return toAdd.isEmpty() ? Set.of() : Set.copyOf(toAdd);
        } catch (Throwable ignored) {
            return Set.of();
        }
    }

    private static void bitsandbalance$mergeToolTagsIntoBlock(Block target, Set<TagKey<Block>> toAdd) {
        if (TAGS_FIELD == null) return;
        if (target == null || toAdd == null || toAdd.isEmpty()) return;

        try {
            Holder.Reference<Block> holder = target.builtInRegistryHolder();
            @SuppressWarnings("unchecked")
            Set<TagKey<Block>> existing = (Set<TagKey<Block>>) TAGS_FIELD.get(holder);
            Set<TagKey<Block>> merged = new HashSet<>(existing != null ? existing : Set.of());
            merged.addAll(toAdd);
            TAGS_FIELD.set(holder, Set.copyOf(merged));
        } catch (Throwable ignored) {
        }
    }
}
