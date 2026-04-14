package org.onenonly.bitsandbalance.common.registry;

import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepCrackProxyBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabCrackProxyBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepCrackProxyBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;

/**
 * Cross-loader holder for block instances that are registered by loader modules.
 *
 * NeoForge assigns these during DeferredRegister setup.
 * Fabric assigns these during direct registry registration.
 */
public final class CommonBlocks {
    private CommonBlocks() {
    }

    /** The mixed double slab block instance (two different slab types in one position). */
    public static MixedSlabBlock MIXED_SLAB;

    /** The vertical slab block instance (stores one or two slab halves with horizontal orientation). */
    public static VerticalSlabBlock VERTICAL_SLAB;

    /**
     * Lightweight proxy block used only for the crack-overlay animation.
     * Has RenderShape.MODEL and the correct half-prism geometry so that
     * renderBreakingTexture can render the breaking animation on vertical slabs.
     */
    public static VerticalSlabCrackProxyBlock VERTICAL_SLAB_CRACK_PROXY;

    /** Proxy block used only for step crack-overlay geometry. */
    public static StepCrackProxyBlock STEP_CRACK_PROXY;

    /** The step block instance (single ¼-block step, enhanced slab behavior). */
    public static StepBlock STEP;

    /** The quad step container block instance (2–4 different steps in one position). */
    public static QuadStepBlock QUAD_STEP;

    /** The vertical step block instance (single vertical ¼-block step). */
    public static VerticalStepBlock VERTICAL_STEP;

    /** Proxy block used only for vertical-step crack-overlay geometry. */
    public static VerticalStepCrackProxyBlock VERTICAL_STEP_CRACK_PROXY;

    /** The quad vertical step container block instance (2–4 different vertical steps in one position). */
    public static QuadVerticalStepBlock QUAD_VERTICAL_STEP;
}
