package org.onenonly.bitsandbalance.blocks;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.util.valueproviders.UniformInt;

import java.util.List;

/**
 * Custom ore block that handles drops, XP, and silk touch behavior.
 */
public class CustomOreBlock extends DropExperienceBlock {
    private final Item dropItem;
    private final ItemStack dropStack;
    private final int minDropCount;
    private final int maxDropCount;

    public CustomOreBlock(Properties properties, ItemStack dropItem, int minXP, int maxXP) {
        this(properties, dropItem, minXP, maxXP, 1, 1);
    }

    public CustomOreBlock(Properties properties, Item dropItem, int minXP, int maxXP) {
        this(properties, dropItem, minXP, maxXP, 1, 1);
    }

    public CustomOreBlock(Properties properties, ItemStack dropItem, int minXP, int maxXP, int minDropCount, int maxDropCount) {
        super(UniformInt.of(minXP, maxXP), properties);
        this.dropItem = null;
        this.dropStack = dropItem == null ? ItemStack.EMPTY : dropItem.copy();
        this.minDropCount = minDropCount;
        this.maxDropCount = maxDropCount;
    }

    public CustomOreBlock(Properties properties, Item dropItem, int minXP, int maxXP, int minDropCount, int maxDropCount) {
        super(UniformInt.of(minXP, maxXP), properties);
        this.dropItem = dropItem;
        this.dropStack = ItemStack.EMPTY;
        this.minDropCount = minDropCount;
        this.maxDropCount = maxDropCount;
    }

    public CustomOreBlock(Properties properties, ItemStack dropItem) {
        this(properties, dropItem, 0, 0, 1, 1);
    }

    public CustomOreBlock(Properties properties, Item dropItem) {
        this(properties, dropItem, 0, 0, 1, 1);
    }

    private ItemStack createDropStack() {
        if (!dropStack.isEmpty()) {
            return dropStack.copy();
        }
        return dropItem == null ? ItemStack.EMPTY : new ItemStack(dropItem);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var tool = builder.getParameter(LootContextParams.TOOL);

        if (tool != null) {
            ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

            boolean hasSilkTouch = false;
            for (var holder : enchantments.keySet()) {
                if (holder.is(Enchantments.SILK_TOUCH)) {
                    hasSilkTouch = true;
                    break;
                }
            }

            if (hasSilkTouch) {
                return List.of(new ItemStack(this));
            }

            ItemStack drop = createDropStack();
            if (drop.isEmpty()) {
                return super.getDrops(state, builder);
            }

            var random = builder.getLevel().getRandom();
            int baseCount = OreDropCounts.randomBetween(random, minDropCount, maxDropCount);
            drop.setCount(baseCount);

            int fortuneLevel = 0;
            for (var holder : enchantments.keySet()) {
                if (holder.is(Enchantments.FORTUNE)) {
                    fortuneLevel = enchantments.getLevel(holder);
                    break;
                }
            }

            drop.setCount(OreDropCounts.applyVanillaOreFortune(random, drop.getCount(), fortuneLevel));

            return List.of(drop);
        }

        ItemStack drop = createDropStack();
        if (drop.isEmpty()) {
            return super.getDrops(state, builder);
        }

        int baseCount = OreDropCounts.randomBetween(builder.getLevel().getRandom(), minDropCount, maxDropCount);
        drop.setCount(baseCount);
        return List.of(drop);
    }
}
