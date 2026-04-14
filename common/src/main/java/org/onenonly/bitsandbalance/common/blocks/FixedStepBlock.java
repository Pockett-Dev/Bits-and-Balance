package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.world.level.block.Block;

/**
 * A {@link StepBlock} that is permanently bound to a specific source slab.
 * One {@code FixedStepBlock} is generated per eligible slab via
 * {@link org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry}.
 */
public class FixedStepBlock extends StepBlock {

    private final Block sourceSlab;

    public FixedStepBlock(Block sourceSlab, Properties properties) {
        super(properties);
        this.sourceSlab = sourceSlab;
    }

    /** The source slab this step was generated from. */
    public Block getSourceSlab() {
        return sourceSlab;
    }
}
