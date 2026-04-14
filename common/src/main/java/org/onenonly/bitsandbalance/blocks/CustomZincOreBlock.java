package org.onenonly.bitsandbalance.blocks;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

/**
 * Custom zinc ore block that dynamically handles Create mod integration.
 */
public class CustomZincOreBlock extends DropExperienceBlock {

    public CustomZincOreBlock(Properties properties) {
        super(UniformInt.of(0, 0), properties);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        Level level = builder.getLevel();
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

            ItemStack drop = createZincDrop();

            int fortuneLevel = 0;
            for (var holder : enchantments.keySet()) {
                if (holder.is(Enchantments.FORTUNE)) {
                    fortuneLevel = enchantments.getLevel(holder);
                    break;
                }
            }

            if (fortuneLevel > 0) {
                int bonusCount = level.getRandom().nextInt(fortuneLevel + 1);
                drop.setCount(drop.getCount() + bonusCount);
            }

            return List.of(drop);
        }

        return List.of(createZincDrop());
    }

    private ItemStack createZincDrop() {
        try {
            var zincHolderOpt = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                Identifier.parse("create:raw_zinc")
            );
            if (zincHolderOpt.isPresent()) {
                return new ItemStack(zincHolderOpt.get().value());
            }
        } catch (Exception e) {
            // Create mod not loaded or item doesn't exist
        }
        return new ItemStack(Items.RAW_IRON);
    }
}
