package org.onenonly.bitsandbalance.common.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Runtime registry mapping from a vertical-slab {@link Block} to its dynamically generated
 * vertical-step {@link Block}.
 *
 * <p>Both Fabric and NeoForge populate this during block registration.
 */
public final class VerticalStepDynamicRegistry {

    /**
     * Data-driven opt-out tag for vertical slabs that should NOT get a dynamically generated
     * vertical-step variant.
     */
    public static final TagKey<Block> NO_DYNAMIC_VERTICAL_STEPS =
            TagKey.create(Registries.BLOCK, Identifier.parse("bitsandbalance:no_dynamic_vertical_steps"));

    private static final Map<Block, Block> VERTICAL_SLAB_TO_VERTICAL_STEP = new IdentityHashMap<>();
    private static final Map<Block, Block> VERTICAL_STEP_TO_VERTICAL_SLAB = new IdentityHashMap<>();
    private static final Map<Identifier, Identifier> VERTICAL_STEP_ID_TO_SLAB_ID = new HashMap<>();

    private VerticalStepDynamicRegistry() {
    }

    public static void register(Block verticalSlabBlock, Block verticalStepBlock) {
        if (verticalSlabBlock == null || verticalStepBlock == null) return;
        VERTICAL_SLAB_TO_VERTICAL_STEP.putIfAbsent(verticalSlabBlock, verticalStepBlock);
        VERTICAL_STEP_TO_VERTICAL_SLAB.putIfAbsent(verticalStepBlock, verticalSlabBlock);
    }

    public static @Nullable Block getVerticalStepForVerticalSlab(Block verticalSlabBlock) {
        return VERTICAL_SLAB_TO_VERTICAL_STEP.get(verticalSlabBlock);
    }

    public static @Nullable Block getVerticalSlabForVerticalStep(Block verticalStepBlock) {
        return VERTICAL_STEP_TO_VERTICAL_SLAB.get(verticalStepBlock);
    }

    public static Collection<Block> getAllVerticalStepBlocksSnapshot() {
        if (VERTICAL_SLAB_TO_VERTICAL_STEP.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(VERTICAL_SLAB_TO_VERTICAL_STEP.values()));
    }

    /**
     * Generates a unique {@link Identifier} for the vertical-step block corresponding to a vertical slab.
     *
     * <p>Preferred: {@code bitsandbalance:vertical_<path>_step}<br>
     * Fallback (collision): {@code bitsandbalance:vertical_<namespace>_<path>_step}</p>
     */
    public static Identifier idForVerticalSlab(Identifier verticalSlabId, String modId) {
        String ns   = verticalSlabId.getNamespace();
        String path = bitsandbalance$verticalStepPathForVerticalSlabPath(verticalSlabId.getPath());

        Identifier preferred = Identifier.fromNamespaceAndPath(modId, path);
        Identifier existing  = VERTICAL_STEP_ID_TO_SLAB_ID.get(preferred);
        if (existing == null || existing.equals(verticalSlabId)) {
            VERTICAL_STEP_ID_TO_SLAB_ID.put(preferred, verticalSlabId);
            return preferred;
        }

        Identifier fallback = Identifier.fromNamespaceAndPath(modId, ns + "_" + path);
        VERTICAL_STEP_ID_TO_SLAB_ID.putIfAbsent(fallback, verticalSlabId);
        return fallback;
    }

    private static String bitsandbalance$verticalStepPathForVerticalSlabPath(String verticalSlabPath) {
        String path = verticalSlabPath.replace('/', '_');
        if (path.startsWith("vertical_")) {
            path = path.substring("vertical_".length());
        }
        if (path.matches(".*_slab_\\d+$")) {
            return "vertical_" + path.replaceFirst("_slab_(\\d+)$", "_step_$1");
        }
        if (path.endsWith("_slab")) {
            return "vertical_" + path.substring(0, path.length() - "_slab".length()) + "_step";
        }
        return "vertical_" + path + "_step";
    }
}
