package org.onenonly.bitsandbalance.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;

/**
 * BlockItem for dynamically-registered per-vertical-slab vertical step blocks.
 *
 * <p>These blocks/items are registered at runtime based on the vertical-slab blocks present,
 * so we intentionally avoid per-item lang entries.</p>
 */
public final class VerticalStepBlockItem extends BlockItem {

    /** The source {@link org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock} for this step. */
    private final Block sourceVerticalSlab;

    public VerticalStepBlockItem(Block verticalStepBlock, Block sourceVerticalSlab, Properties properties) {
        super(verticalStepBlock, properties);
        this.sourceVerticalSlab = sourceVerticalSlab;
    }

    public Block getSourceVerticalSlab() {
        return sourceVerticalSlab;
    }

    @Override
    public Component getName(ItemStack stack) {
        Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
        if (sourceSlab == null) {
            sourceSlab = sourceVerticalSlab;
        }
        return EnhancedSlabItemSupport.displayName(stack, sourceSlab, "Vertical Step");
    }
}
