package org.onenonly.bitsandbalance.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

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

        if (Config.enhancedSlabsVerticalSlabs) {
            for (Block verticalBlock : VerticalSlabDynamicRegistry.getAllVerticalBlocksSnapshot()) {
                try {
                    Block slabBlock = VerticalSlabDynamicRegistry.getSlabForVertical(verticalBlock);
                    bitsandbalance$insertAfterSource(event, slabBlock, verticalBlock);
                } catch (Throwable ignored) {
                }
            }
        }

        if (Config.enhancedSlabsSteps) {
            for (Block stepBlock : StepDynamicRegistry.getAllStepBlocksSnapshot()) {
                try {
                    Block slabBlock = StepDynamicRegistry.getSlabForStep(stepBlock);
                    bitsandbalance$insertAfterSource(event, slabBlock, stepBlock);
                } catch (Throwable ignored) {
                }
            }

            for (Block verticalStepBlock : VerticalStepDynamicRegistry.getAllVerticalStepBlocksSnapshot()) {
                try {
                    Block sourceVerticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(verticalStepBlock);
                    Block sourceSlab = sourceVerticalSlab == null
                            ? null
                            : VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
                    if (sourceSlab == null) {
                        sourceSlab = sourceVerticalSlab;
                    }
                    bitsandbalance$insertAfterSource(event, sourceSlab, verticalStepBlock);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void bitsandbalance$insertAfterSource(BuildCreativeModeTabContentsEvent event,
                                                          Block sourceBlock,
                                                          Block outputBlock) {
        if (sourceBlock == null || outputBlock == null) return;
        Item sourceItem = sourceBlock.asItem();
        if (sourceItem == Items.AIR) return;

        Item outputItem = outputBlock.asItem();
        if (outputItem == Items.AIR) return;

        ItemStack sourceStack = new ItemStack(sourceItem);
        ItemStack outputStack = new ItemStack(outputItem);

        // insertAfter throws if the reference stack is not present in this tab; catch and skip.
        event.insertAfter(sourceStack, outputStack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}
