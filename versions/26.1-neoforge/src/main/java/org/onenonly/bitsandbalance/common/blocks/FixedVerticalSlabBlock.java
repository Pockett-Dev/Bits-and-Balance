package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.world.level.block.Block;

/**
 * A per-slab vertical slab block (unique registry ID per slab {@link Block}).
 *
 * <p>Behavior is identical to {@link VerticalSlabBlock}, but the block itself encodes which slab block it represents.
 */
public class FixedVerticalSlabBlock extends VerticalSlabBlock {

    private final Block sourceSlab;

    public FixedVerticalSlabBlock(Properties properties, Block sourceSlab) {
        super(properties);
        this.sourceSlab = sourceSlab;
    }

    public Block getSourceSlab() {
        return sourceSlab;
    }
}
