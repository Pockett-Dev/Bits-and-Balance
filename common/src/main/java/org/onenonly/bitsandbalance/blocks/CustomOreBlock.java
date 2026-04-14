package org.onenonly.bitsandbalance.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.valueproviders.UniformInt;

import java.util.List;

/**
 * Custom ore block that handles drops, XP, and silk touch behavior.
 */
public class CustomOreBlock extends DropExperienceBlock {
    private final ItemStack dropItem;
    private final int minDropCount;
    private final int maxDropCount;

    public CustomOreBlock(Properties properties, ItemStack dropItem, int minXP, int maxXP) {
        this(properties, dropItem, minXP, maxXP, 1, 1);
    }

    public CustomOreBlock(Properties properties, ItemStack dropItem, int minXP, int maxXP, int minDropCount, int maxDropCount) {
        super(UniformInt.of(minXP, maxXP), properties);
        this.dropItem = dropItem;
        this.minDropCount = minDropCount;
        this.maxDropCount = maxDropCount;
    }

    public CustomOreBlock(Properties properties, ItemStack dropItem) {
        this(properties, dropItem, 0, 0, 1, 1);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        Level level = builder.getLevel();
        Vec3 origin = builder.getParameter(LootContextParams.ORIGIN);
        BlockPos pos = new BlockPos((int) origin.x, (int) origin.y, (int) origin.z);
        ItemStack tool = builder.getParameter(LootContextParams.TOOL);

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

            if (dropItem == null || dropItem.isEmpty()) {
                return super.getDrops(state, builder);
            }

            ItemStack drop = dropItem.copy();
            int baseCount = level.getRandom().nextInt(maxDropCount - minDropCount + 1) + minDropCount;
            drop.setCount(baseCount);

            int fortuneLevel = 0;
            for (var holder : enchantments.keySet()) {
                if (holder.is(Enchantments.FORTUNE)) {
                    fortuneLevel = enchantments.getLevel(holder);
                    break;
                }
            }

            if (fortuneLevel > 0) {
                int bonusCount = level.getRandom().nextInt(fortuneLevel + 2) - 1;
                if (bonusCount < 0) bonusCount = 0;
                drop.setCount(drop.getCount() + bonusCount);
            }

            return List.of(drop);
        }

        if (dropItem == null || dropItem.isEmpty()) {
            return super.getDrops(state, builder);
        }

        ItemStack drop = dropItem.copy();
        int baseCount = level.getRandom().nextInt(maxDropCount - minDropCount + 1) + minDropCount;
        drop.setCount(baseCount);
        return List.of(drop);
    }
}
