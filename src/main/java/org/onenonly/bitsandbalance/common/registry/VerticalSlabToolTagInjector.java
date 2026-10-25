package org.onenonly.bitsandbalance.common.registry;

import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Injects tool-related block tags into dynamically-registered {@link FixedVerticalSlabBlock} instances
 * after tags are loaded, so that systems like Jade can correctly display the required tool for the block.
 *
 * <p>Must be called from both the NeoForge {@code TagsUpdatedEvent} handler and the Fabric
 * {@code CommonLifecycleEvents.TAGS_LOADED} callback, as tags are rebound on every reload.</p>
 */
public final class VerticalSlabToolTagInjector {

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

    /**
     * The private {@code Set<TagKey<T>> tags} field on {@link Holder.Reference}.
     * Located by type at class-load time to be mapping-independent (works on both Fabric/Yarn
     * and NeoForge/mojmap where the runtime field name may differ).
     */
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

    private VerticalSlabToolTagInjector() {
    }

    /**
     * Copies tool-related tags from each source slab's {@link Holder.Reference} into the
     * corresponding vertical slab block's {@link Holder.Reference}.
     *
     * <p>This is idempotent — calling it multiple times (e.g. on successive reloads) is safe
     * because the merged set is reconstructed from scratch on each call.</p>
     */
    public static void injectToolTags() {
        if (TAGS_FIELD == null) return;

        for (Block vertBlock : VerticalSlabDynamicRegistry.getAllVerticalBlocksSnapshot()) {
            if (!(vertBlock instanceof FixedVerticalSlabBlock fixed)) continue;
            Block sourceSlab = fixed.getSourceSlab();
            if (sourceSlab == null) continue;

            try {
                Holder.Reference<Block> sourceHolder = sourceSlab.builtInRegistryHolder();
                Holder.Reference<Block> vertHolder = vertBlock.builtInRegistryHolder();

                // Determine which tool tags the source slab has
                Set<TagKey<Block>> toAdd = new HashSet<>();
                for (TagKey<Block> tag : TOOL_TAGS) {
                    if (sourceHolder.is(tag)) {
                        toAdd.add(tag);
                    }
                }
                if (toAdd.isEmpty()) continue;

                // Read the vertical block's current tag set and merge
                @SuppressWarnings("unchecked")
                Set<TagKey<Block>> existing = (Set<TagKey<Block>>) TAGS_FIELD.get(vertHolder);
                Set<TagKey<Block>> merged = new HashSet<>(existing != null ? existing : Set.of());
                merged.addAll(toAdd);

                // Replace the field with an immutable copy (matching vanilla's convention)
                TAGS_FIELD.set(vertHolder, Set.copyOf(merged));

            } catch (Throwable ignored) {
                // Best-effort — never crash the game over a display enhancement
            }
        }
    }
}
