package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.world.level.block.Block;

/**
 * A {@link VerticalStepBlock} that is permanently bound to a specific source vertical slab.
 * One {@code FixedVerticalStepBlock} is generated per eligible vertical slab via
 * {@link org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry}.
 */
public class FixedVerticalStepBlock extends VerticalStepBlock {

    private final Block sourceVerticalSlab;

    public FixedVerticalStepBlock(Block sourceVerticalSlab, Properties properties) {
        super(properties);
        this.sourceVerticalSlab = sourceVerticalSlab;
    }

    /** The source vertical slab this vertical step was generated from. */
    public Block getSourceVerticalSlab() {
        return sourceVerticalSlab;
    }
}
