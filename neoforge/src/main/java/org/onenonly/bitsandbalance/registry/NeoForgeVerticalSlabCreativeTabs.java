package org.onenonly.bitsandbalance.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.registry.EnhancedSlabCreativeTabFamilies;

/**
 * Injects dynamically registered enhanced slab-family items into creative tabs.
 *
 * <p>Each vertical slab, step, or vertical step is inserted immediately after its
 * corresponding source slab in any creative tab that already contains that source slab.
 * This includes source-mod tabs for modded slab families in addition to vanilla tabs.
 * Registered on the mod event bus via {@code BitsAndBalance} constructor.</p>
 */
public final class NeoForgeVerticalSlabCreativeTabs {

    private NeoForgeVerticalSlabCreativeTabs() {
    }

    public static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!Config.enableEnhancedSlabs) return;

        // Skip the Bits and Balance tab; it already adds enhanced slab-family items directly.
        Identifier tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(event.getTab());
        if (tabId == null) return;
        if (BitsAndBalanceCommon.MOD_ID.equals(tabId.getNamespace())) return;

        for (EnhancedSlabCreativeTabFamilies.Family family : EnhancedSlabCreativeTabFamilies.snapshot(
                Config.enhancedSlabsVerticalSlabs,
                Config.enhancedSlabsSteps)) {
            bitsandbalance$insertFamily(event, family);
        }
    }

    private static void bitsandbalance$insertFamily(BuildCreativeModeTabContentsEvent event,
                                                    EnhancedSlabCreativeTabFamilies.Family family) {
        if (event == null || family == null) return;

        Item sourceItem = family.sourceSlab().asItem();
        if (sourceItem == Items.AIR) return;

        ItemStack anchor = new ItemStack(sourceItem);
        ItemStack insertedVerticalSlab = bitsandbalance$tryInsertAfter(event, anchor, family.verticalSlab());
        if (insertedVerticalSlab != null) {
            anchor = insertedVerticalSlab;
        }

        ItemStack insertedStep = bitsandbalance$tryInsertAfter(event, anchor, family.step());
        if (insertedStep != null) {
            anchor = insertedStep;
        }

        bitsandbalance$tryInsertAfter(event, anchor, family.verticalStep());
    }

    private static ItemStack bitsandbalance$tryInsertAfter(BuildCreativeModeTabContentsEvent event,
                                                           ItemStack anchor,
                                                           net.minecraft.world.level.block.Block outputBlock) {
        if (event == null || anchor == null || anchor.isEmpty() || outputBlock == null) return null;

        Item outputItem = outputBlock.asItem();
        if (outputItem == Items.AIR) return null;

        ItemStack outputStack = new ItemStack(outputItem);
        try {
            event.insertAfter(anchor, outputStack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            return outputStack;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
