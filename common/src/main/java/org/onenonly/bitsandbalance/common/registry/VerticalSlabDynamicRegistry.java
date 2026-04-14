package org.onenonly.bitsandbalance.common.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime registry mapping from a slab {@link Block} to its dynamically generated vertical slab {@link Block}.
 *
 * <p>Both Fabric and NeoForge populate this during block registration.
 */
public final class VerticalSlabDynamicRegistry {

    /**
     * Data-driven opt-out tag for slabs that should NOT get a dynamically generated vertical-slab variant.
     *
     * <p>This is primarily for compatibility: if another mod already provides vertical slabs for a given
     * slab block, pack authors can add those slabs to this tag to prevent duplicates.</p>
     */
    public static final TagKey<Block> NO_DYNAMIC_VERTICAL_SLABS =
            TagKey.create(Registries.BLOCK, Identifier.parse("bitsandbalance:no_dynamic_vertical_slabs"));

    private static final Map<Block, Block> SLAB_TO_VERTICAL = new IdentityHashMap<>();
    private static final Map<Block, Block> VERTICAL_TO_SLAB = new IdentityHashMap<>();
    private static final Map<Identifier, Identifier> VERTICAL_ID_TO_SLAB_ID = new HashMap<>();

    private VerticalSlabDynamicRegistry() {
    }

    public static void register(Block slabBlock, Block verticalSlabBlock) {
        if (slabBlock == null || verticalSlabBlock == null) return;
        SLAB_TO_VERTICAL.putIfAbsent(slabBlock, verticalSlabBlock);
        VERTICAL_TO_SLAB.putIfAbsent(verticalSlabBlock, slabBlock);
    }

    public static @Nullable Block getVerticalForSlab(Block slabBlock) {
        return SLAB_TO_VERTICAL.get(slabBlock);
    }

    public static @Nullable Block getSlabForVertical(Block verticalSlabBlock) {
        return VERTICAL_TO_SLAB.get(verticalSlabBlock);
    }

    public static Collection<Block> getAllVerticalBlocksSnapshot() {
        if (SLAB_TO_VERTICAL.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(SLAB_TO_VERTICAL.values()));
    }

    public static Identifier idForSlab(Identifier slabId, String modId) {
        String ns = slabId.getNamespace();
        String path = slabId.getPath().replace('/', '_');

        // Preferred naming:
        // - anymod:umbran_slab -> bitsandbalance:vertical_umbran_slab
        // If that collides with another slab, fall back to:
        // - anymod:umbran_slab -> bitsandbalance:vertical_anymod_umbran_slab

        Identifier preferred = Identifier.fromNamespaceAndPath(modId, "vertical_" + path);
        Identifier existing = VERTICAL_ID_TO_SLAB_ID.get(preferred);
        if (existing == null || existing.equals(slabId)) {
            VERTICAL_ID_TO_SLAB_ID.put(preferred, slabId);
            return preferred;
        }

        Identifier fallback = Identifier.fromNamespaceAndPath(modId, "vertical_" + ns + "_" + path);
        VERTICAL_ID_TO_SLAB_ID.putIfAbsent(fallback, slabId);
        return fallback;
    }
}
