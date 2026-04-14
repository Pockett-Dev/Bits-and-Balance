package org.onenonly.bitsandbalance.common.mechanics;

/**
 * Internal render-context flags for Enhanced Slabs.
 *
 * <p>Used to prevent double-applying visual offsets when a block entity renderer
 * internally calls block-model rendering (e.g. via {@code BlockRenderDispatcher.renderBatched}).
 */
public final class EnhancedSlabRenderOffsetContext {

    private EnhancedSlabRenderOffsetContext() {
    }

    /**
     * Depth counter for block-entity submission calls.
     * When > 0, block-model render offset mixins should skip applying their own offset.
     */
    public static final ThreadLocal<Integer> BLOCK_ENTITY_SUBMIT_DEPTH =
            ThreadLocal.withInitial(() -> 0);

    public static void pushBlockEntitySubmit() {
        BLOCK_ENTITY_SUBMIT_DEPTH.set(BLOCK_ENTITY_SUBMIT_DEPTH.get() + 1);
    }

    public static void popBlockEntitySubmit() {
        int next = BLOCK_ENTITY_SUBMIT_DEPTH.get() - 1;
        BLOCK_ENTITY_SUBMIT_DEPTH.set(Math.max(next, 0));
    }

    public static boolean isInBlockEntitySubmit() {
        return BLOCK_ENTITY_SUBMIT_DEPTH.get() > 0;
    }
}
