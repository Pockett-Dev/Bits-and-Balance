package org.onenonly.bitsandbalance.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * BlockItem for dynamically-registered per-slab vertical slabs.
 *
 * <p>These blocks/items are registered at runtime based on the slab blocks present,
 * so we intentionally avoid per-item lang entries.
 */
public final class VerticalSlabBlockItem extends BlockItem {

    private final Block sourceSlab;

    public VerticalSlabBlockItem(Block verticalSlab, Block sourceSlab, Properties properties) {
        super(verticalSlab, properties);
        this.sourceSlab = sourceSlab;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (sourceSlab == null) {
            return super.getName(stack);
        }
        return EnhancedSlabItemSupport.displayName(stack, sourceSlab, "Vertical Slab");
    }
}
