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
 * Runtime registry mapping from a slab {@link Block} to its dynamically generated step {@link Block}.
 *
 * <p>Both Fabric and NeoForge populate this during block registration.
 */
public final class StepDynamicRegistry {

    /**
     * Data-driven opt-out tag for slabs that should NOT get a dynamically generated step variant.
     */
    public static final TagKey<Block> NO_DYNAMIC_STEPS =
            TagKey.create(Registries.BLOCK, Identifier.parse("bitsandbalance:no_dynamic_steps"));

    private static final Map<Block, Block> SLAB_TO_STEP = new IdentityHashMap<>();
    private static final Map<Block, Block> STEP_TO_SLAB = new IdentityHashMap<>();
    private static final Map<Identifier, Identifier> STEP_ID_TO_SLAB_ID = new HashMap<>();

    private StepDynamicRegistry() {
    }

    public static void register(Block slabBlock, Block stepBlock) {
        if (slabBlock == null || stepBlock == null) return;
        SLAB_TO_STEP.putIfAbsent(slabBlock, stepBlock);
        STEP_TO_SLAB.putIfAbsent(stepBlock, slabBlock);
    }

    public static @Nullable Block getStepForSlab(Block slabBlock) {
        return SLAB_TO_STEP.get(slabBlock);
    }

    public static @Nullable Block getSlabForStep(Block stepBlock) {
        return STEP_TO_SLAB.get(stepBlock);
    }

    public static Collection<Block> getAllStepBlocksSnapshot() {
        if (SLAB_TO_STEP.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(SLAB_TO_STEP.values()));
    }

    /**
     * Generates a unique {@link Identifier} for the step block corresponding to a slab.
     *
     * <p>Preferred: {@code bitsandbalance:<path>_step}<br>
     * Fallback (collision): {@code bitsandbalance:<namespace>_<path>_step}</p>
     */
    public static Identifier idForSlab(Identifier slabId, String modId) {
        String ns   = slabId.getNamespace();
        String path = bitsandbalance$stepPathForSlabPath(slabId.getPath());

        Identifier preferred = Identifier.fromNamespaceAndPath(modId, path);
        Identifier existing  = STEP_ID_TO_SLAB_ID.get(preferred);
        if (existing == null || existing.equals(slabId)) {
            STEP_ID_TO_SLAB_ID.put(preferred, slabId);
            return preferred;
        }

        Identifier fallback = Identifier.fromNamespaceAndPath(modId, ns + "_" + path);
        STEP_ID_TO_SLAB_ID.putIfAbsent(fallback, slabId);
        return fallback;
    }

    private static String bitsandbalance$stepPathForSlabPath(String slabPath) {
        String path = slabPath.replace('/', '_');
        if (path.matches(".*_slab_\\d+$")) {
            return path.replaceFirst("_slab_(\\d+)$", "_step_$1");
        }
        if (path.endsWith("_slab")) {
            return path.substring(0, path.length() - "_slab".length()) + "_step";
        }
        return path + "_step";
    }
}
