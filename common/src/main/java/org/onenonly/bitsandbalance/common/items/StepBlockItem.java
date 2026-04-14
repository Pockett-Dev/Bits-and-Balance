package org.onenonly.bitsandbalance.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * BlockItem for dynamically-registered per-slab step blocks.
 *
 * <p>These blocks/items are registered at runtime based on the slab blocks present,
 * so we intentionally avoid per-item lang entries.</p>
 */
public final class StepBlockItem extends BlockItem {

    private final Block sourceSlab;

    public StepBlockItem(Block stepBlock, Block sourceSlab, Properties properties) {
        super(stepBlock, properties);
        this.sourceSlab = sourceSlab;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (sourceSlab == null) {
            return super.getName(stack);
        }
        return EnhancedSlabItemSupport.displayName(stack, sourceSlab, "Step");
    }
}
