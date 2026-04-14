package org.onenonly.bitsandbalance.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.items.VerticalStepBlockItem;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

/**
 * Registers per-vertical-slab vertical step blocks on NeoForge.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class NeoForgeDynamicVerticalSteps {

    private NeoForgeDynamicVerticalSteps() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterBlocks(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK)) return;

        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            bitsandbalance$registerForVerticalSlabIfNeeded(event, blockId, block);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterItems(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.ITEM)) return;

        for (Block block : BuiltInRegistries.BLOCK) {
            try {
                if (!(block instanceof FixedVerticalSlabBlock)) continue;

                Block stepBlock = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(block);
                if (!(stepBlock instanceof FixedVerticalStepBlock)) continue;

                Identifier stepId = BuiltInRegistries.BLOCK.getKey(stepBlock);
                if (stepId == null) continue;
                if (BuiltInRegistries.ITEM.containsKey(stepId)) continue;

                Item.Properties props = new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, stepId));
                final Block sourceVerticalSlab = block;
                event.register(Registries.ITEM, stepId,
                        () -> new VerticalStepBlockItem(stepBlock, sourceVerticalSlab, props));

                Identifier legacyAliasId = bitsandbalance$legacyMalformedStepItemId(stepId);
                if (legacyAliasId != null && !BuiltInRegistries.ITEM.containsKey(legacyAliasId)) {
                    Item.Properties aliasProps = new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, legacyAliasId));
                    event.register(Registries.ITEM, legacyAliasId,
                        () -> new VerticalStepBlockItem(stepBlock, sourceVerticalSlab, aliasProps));
                }
            } catch (Throwable ignored) {
            }
        }
    }

            private static Identifier bitsandbalance$legacyMalformedStepItemId(Identifier stepId) {
            String path = stepId.getPath();
            if (!path.matches(".*_step_\\d+$")) return null;
            return Identifier.fromNamespaceAndPath(stepId.getNamespace(), path + "_step");
            }

    private static void bitsandbalance$registerForVerticalSlabIfNeeded(RegisterEvent event,
                                                                         Identifier verticalSlabId,
                                                                         Block verticalSlabBlock) {
        try {
            if (verticalSlabBlock == null || verticalSlabId == null) return;
            if (!(verticalSlabBlock instanceof FixedVerticalSlabBlock)) return;
            if (VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlabBlock) != null) return;

            if (bitsandbalance$shouldSkipDynamicGeneration(verticalSlabId, verticalSlabBlock)) return;

            Identifier stepId = VerticalStepDynamicRegistry.idForVerticalSlab(verticalSlabId, BitsAndBalance.MODID);
            if (BuiltInRegistries.BLOCK.containsKey(stepId)) {
                Block existing = BuiltInRegistries.BLOCK.getValue(stepId);
                if (existing != null) {
                    VerticalStepDynamicRegistry.register(verticalSlabBlock, existing);
                }
                return;
            }

            FixedVerticalStepBlock stepBlock = new FixedVerticalStepBlock(
                    verticalSlabBlock,
                    BlockBehaviour.Properties.ofFullCopy(verticalSlabBlock)
                        .noOcclusion()
                            .setId(ResourceKey.create(Registries.BLOCK, stepId))
            );

            event.register(Registries.BLOCK, stepId, () -> stepBlock);
            VerticalStepDynamicRegistry.register(verticalSlabBlock, stepBlock);
        } catch (Throwable ignored) {
        }
    }

    private static boolean bitsandbalance$shouldSkipDynamicGeneration(Identifier verticalSlabId,
                                                                        Block verticalSlabBlock) {
        try {
            if (verticalSlabId == null || verticalSlabBlock == null) return false;

            // Data-driven opt-out tag
            try {
                if (verticalSlabBlock.builtInRegistryHolder().is(VerticalStepDynamicRegistry.NO_DYNAMIC_VERTICAL_STEPS)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }

            // Heuristic opt-out: if the vertical slab's namespace already provides vertical_<path>_step, skip
            String ns   = verticalSlabId.getNamespace();
            String path = verticalSlabId.getPath();
            String base = path;
            if (base.startsWith("vertical_")) {
                base = base.substring("vertical_".length());
            }
            if (base.endsWith("_slab")) {
                base = base.substring(0, base.length() - "_slab".length());
            }

            Identifier candidate1 = Identifier.fromNamespaceAndPath(ns, "vertical_" + base + "_step");
            if (BuiltInRegistries.BLOCK.containsKey(candidate1)) return true;

            Identifier candidate2 = Identifier.fromNamespaceAndPath(ns, "vertical_" + base.replace('/', '_') + "_step");
            return BuiltInRegistries.BLOCK.containsKey(candidate2);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
