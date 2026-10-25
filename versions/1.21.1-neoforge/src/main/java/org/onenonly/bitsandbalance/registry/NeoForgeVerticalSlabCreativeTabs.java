package org.onenonly.bitsandbalance.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.registry.EnhancedSlabCreativeTabFamilies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

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
        ResourceLocation tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(event.getTab());
        if (tabId == null) return;
        if (BitsAndBalanceCommon.MOD_ID.equals(tabId.getNamespace())) return;

        for (EnhancedSlabCreativeTabFamilies.Family family : EnhancedSlabCreativeTabFamilies.snapshot(
                Config.enhancedSlabsVerticalSlabs,
                Config.enhancedSlabsSteps)) {
            bitsandbalance$insertFamily(event, family);
        }
    }

    public static void injectEnhancedSlabFamilyItems(CreativeModeTab tab,
                                                     Collection<ItemStack> displayItems,
                                                     Collection<ItemStack> searchTabItems) {
        if (displayItems == null || searchTabItems == null) {
            return;
        }
        if (!bitsandbalance$shouldInjectEnhancedSlabItems(tab)) {
            return;
        }

        bitsandbalance$injectEnhancedSlabFamilies(displayItems, searchTabItems);
    }

    private static boolean bitsandbalance$shouldInjectEnhancedSlabItems(CreativeModeTab tab) {
        if (tab == null) {
            return false;
        }

        ResourceLocation tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (tabId == null) {
            return false;
        }

        return !BitsAndBalanceCommon.MOD_ID.equals(tabId.getNamespace());
    }

    private static void bitsandbalance$injectEnhancedSlabFamilies(Collection<ItemStack> displayItems,
                                                                  Collection<ItemStack> searchTabItems) {
        if (!Config.enableEnhancedSlabs) {
            return;
        }
        if (!Config.enhancedSlabsVerticalSlabs && !Config.enhancedSlabsSteps) {
            return;
        }

        List<ItemStack> displayStacks = new ArrayList<>(displayItems);
        if (displayStacks.isEmpty()) {
            return;
        }

        List<ItemStack> searchStacks = new ArrayList<>(searchTabItems);
        Set<Item> presentItems = bitsandbalance$presentItems(displayStacks);
        Set<Item> presentSearchItems = bitsandbalance$presentItems(searchStacks);

        for (EnhancedSlabCreativeTabFamilies.Family family : EnhancedSlabCreativeTabFamilies.snapshot(
                Config.enhancedSlabsVerticalSlabs,
                Config.enhancedSlabsSteps)) {
            bitsandbalance$insertFamily(displayStacks, presentItems, family);
            bitsandbalance$insertFamily(searchStacks, presentSearchItems, family);
        }

        bitsandbalance$replaceStacks(displayItems, displayStacks);
        bitsandbalance$replaceStacks(searchTabItems, searchStacks);
    }

    private static Set<Item> bitsandbalance$presentItems(List<ItemStack> stacks) {
        Set<Item> presentItems = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ItemStack stack : stacks) {
            Item item = stack.getItem();
            if (item != Items.AIR) {
                presentItems.add(item);
            }
        }
        return presentItems;
    }

    private static void bitsandbalance$insertFamily(List<ItemStack> entries,
                                                    Set<Item> presentItems,
                                                    EnhancedSlabCreativeTabFamilies.Family family) {
        if (entries == null || presentItems == null || family == null) {
            return;
        }

        Item anchor = family.sourceSlab().asItem();
        if (anchor == Items.AIR || !presentItems.contains(anchor)) {
            return;
        }

        Item insertedVerticalSlab = bitsandbalance$insertAfterAnchor(entries, presentItems, anchor, family.verticalSlab());
        if (insertedVerticalSlab != Items.AIR) {
            anchor = insertedVerticalSlab;
        }

        Item insertedStep = bitsandbalance$insertAfterAnchor(entries, presentItems, anchor, family.step());
        if (insertedStep != Items.AIR) {
            anchor = insertedStep;
        }

        bitsandbalance$insertAfterAnchor(entries, presentItems, anchor, family.verticalStep());
    }

    private static Item bitsandbalance$insertAfterAnchor(List<ItemStack> entries,
                                                         Set<Item> presentItems,
                                                         Item anchor,
                                                         Block outputBlock) {
        if (anchor == null || anchor == Items.AIR || outputBlock == null) {
            return Items.AIR;
        }

        Item outputItem = outputBlock.asItem();
        if (outputItem == Items.AIR || presentItems.contains(outputItem)) {
            return Items.AIR;
        }

        int insertionIndex = -1;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).getItem() == anchor) {
                insertionIndex = i;
                break;
            }
        }

        if (insertionIndex < 0) {
            return Items.AIR;
        }

        entries.add(insertionIndex + 1, new ItemStack(outputItem));
        presentItems.add(outputItem);
        return outputItem;
    }

    private static void bitsandbalance$replaceStacks(Collection<ItemStack> target, List<ItemStack> orderedStacks) {
        try {
            Set<ItemStack> rebuilt = ItemStackLinkedSet.createTypeAndComponentsSet();
            rebuilt.addAll(orderedStacks);
            target.clear();
            target.addAll(rebuilt);
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private static void bitsandbalance$insertFamily(BuildCreativeModeTabContentsEvent event,
                                                    EnhancedSlabCreativeTabFamilies.Family family) {
        if (event == null || family == null) return;

        Item sourceItem = family.sourceSlab().asItem();
        if (sourceItem == Items.AIR) return;

        TabAnchors anchors = new TabAnchors(
                bitsandbalance$findEntry(event.getParentEntries(), sourceItem),
                bitsandbalance$findEntry(event.getSearchEntries(), sourceItem)
        );
        if (!anchors.hasAny()) return;

        TabAnchors insertedVerticalSlab = bitsandbalance$tryInsertAfter(event, anchors, family.verticalSlab());
        if (insertedVerticalSlab.hasAny()) {
            anchors = insertedVerticalSlab.withFallback(anchors);
        }

        TabAnchors insertedStep = bitsandbalance$tryInsertAfter(event, anchors, family.step());
        if (insertedStep.hasAny()) {
            anchors = insertedStep.withFallback(anchors);
        }

        bitsandbalance$tryInsertAfter(event, anchors, family.verticalStep());
    }

    private static TabAnchors bitsandbalance$tryInsertAfter(BuildCreativeModeTabContentsEvent event,
                                                            TabAnchors anchors,
                                                            net.minecraft.world.level.block.Block outputBlock) {
        if (event == null || anchors == null || !anchors.hasAny() || outputBlock == null) return TabAnchors.EMPTY;

        Item outputItem = outputBlock.asItem();
        if (outputItem == Items.AIR) return TabAnchors.EMPTY;

        ItemStack parentInserted = null;
        ItemStack searchInserted = null;

        if (anchors.parent() != null) {
            ItemStack outputStack = new ItemStack(outputItem);
            if (bitsandbalance$tryInsertAfter(event, anchors.parent(), outputStack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY)) {
                parentInserted = outputStack;
            } else {
                parentInserted = bitsandbalance$findEntry(event.getParentEntries(), outputItem);
            }
        }

        if (anchors.search() != null) {
            ItemStack outputStack = new ItemStack(outputItem);
            if (bitsandbalance$tryInsertAfter(event, anchors.search(), outputStack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY)) {
                searchInserted = outputStack;
            } else {
                searchInserted = bitsandbalance$findEntry(event.getSearchEntries(), outputItem);
            }
        }

        return new TabAnchors(parentInserted, searchInserted);
    }

    private static boolean bitsandbalance$tryInsertAfter(BuildCreativeModeTabContentsEvent event,
                                                         ItemStack anchor,
                                                         ItemStack outputStack,
                                                         CreativeModeTab.TabVisibility visibility) {
        if (anchor == null || anchor.isEmpty() || outputStack == null || outputStack.isEmpty()) {
            return false;
        }
        try {
            event.insertAfter(anchor, outputStack, visibility);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ItemStack bitsandbalance$findEntry(Iterable<ItemStack> entries, Item item) {
        if (entries == null || item == null || item == Items.AIR) {
            return null;
        }

        for (ItemStack entry : entries) {
            if (entry != null && !entry.isEmpty() && entry.getItem() == item) {
                return entry;
            }
        }
        return null;
    }

    private record TabAnchors(ItemStack parent, ItemStack search) {
        private static final TabAnchors EMPTY = new TabAnchors(null, null);

        private boolean hasAny() {
            return this.parent != null || this.search != null;
        }

        private TabAnchors withFallback(TabAnchors fallback) {
            if (fallback == null) {
                return this;
            }
            return new TabAnchors(
                    this.parent != null ? this.parent : fallback.parent,
                    this.search != null ? this.search : fallback.search
            );
        }
    }
}
