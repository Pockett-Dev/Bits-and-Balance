package org.onenonly.bitsandbalance.fabric.client;

/**
 * 1.21.11 fallback: keep mixed slabs on the block-entity-renderer path.
 *
 * The newer Fabric baked-model wrapper is not safe under all resource reload implementations,
 * including ModernFix dynamic resources.
 */
public final class FabricMixedSlabModelPlugin {

    private FabricMixedSlabModelPlugin() {
    }

    public static void init() {
    }
}