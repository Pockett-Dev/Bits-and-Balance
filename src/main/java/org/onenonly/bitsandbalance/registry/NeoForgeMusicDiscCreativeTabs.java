package org.onenonly.bitsandbalance.registry;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.content.ModDogMusicDisc;

public final class NeoForgeMusicDiscCreativeTabs {

    private NeoForgeMusicDiscCreativeTabs() {
    }

    public static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!Config.enableDogMusicDisc) {
            return;
        }

        Item dogDisc = ModDogMusicDisc.musicDiscDog();
        if (dogDisc == Items.AIR) {
            return;
        }

        try {
            event.insertAfter(new ItemStack(Items.MUSIC_DISC_CAT), new ItemStack(dogDisc), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        } catch (IllegalArgumentException ignored) {
        }
    }
}