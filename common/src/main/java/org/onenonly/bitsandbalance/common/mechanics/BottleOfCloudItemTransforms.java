package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;

public final class BottleOfCloudItemTransforms {
    private BottleOfCloudItemTransforms() {
    }

    public static ItemStack fillCloudBottle(ItemStack emptyBottle, Player player) {
        player.awardStat(Stats.ITEM_USED.get(emptyBottle.getItem()));
        return ItemUtils.createFilledResult(emptyBottle, player, new ItemStack(ModBottleOfCloud.bottleOfCloud()));
    }

    public static ItemStack emptyCloudBottle(ItemStack bottleOfCloud, Player player) {
        player.awardStat(Stats.ITEM_USED.get(bottleOfCloud.getItem()));
        if (player.getAbilities().instabuild) {
            return bottleOfCloud.copy();
        }
        return ItemUtils.createFilledResult(bottleOfCloud, player, new ItemStack(Items.GLASS_BOTTLE));
    }
}