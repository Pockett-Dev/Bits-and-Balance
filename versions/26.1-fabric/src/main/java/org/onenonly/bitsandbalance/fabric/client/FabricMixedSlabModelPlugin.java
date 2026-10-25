package org.onenonly.bitsandbalance.fabric.client;

/**
 * 26.1 fallback: keep mixed slabs on the block-entity-renderer path.
 *
 * This runtime already registers the mixed-slab BER, so there is no need to keep a
 * reload-time post-bake wrapper in front of ModernFix-style dynamic resource reloads.
 */
public final class FabricMixedSlabModelPlugin {

    private FabricMixedSlabModelPlugin() {
    }

    public static void init() {
    }
}