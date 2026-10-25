package org.onenonly.bitsandbalance.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.items.VerticalStepBlockItem;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Registers per-vertical-slab vertical step blocks on NeoForge.
 */
public final class NeoForgeDynamicVerticalSteps {

    private NeoForgeDynamicVerticalSteps() {
    }

    public static void onRegisterBlocks(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK)) return;

        for (Block block : bitsandbalance$sortedRegistryBlocks()) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            bitsandbalance$registerForVerticalSlabIfNeeded(event, blockId, block);
        }
    }

    public static void onRegisterItems(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.ITEM)) return;

        for (Block block : bitsandbalance$sortedRegistryBlocks()) {
            try {
                if (!(block instanceof FixedVerticalSlabBlock)) continue;

                Block stepBlock = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(block);
                if (!(stepBlock instanceof FixedVerticalStepBlock)) continue;

                ResourceLocation stepId = BuiltInRegistries.BLOCK.getKey(stepBlock);
                if (stepId == null) continue;
                if (BuiltInRegistries.ITEM.containsKey(stepId)) continue;

                Item.Properties props = new Item.Properties()
                        ;
                final Block sourceVerticalSlab = block;
                event.register(Registries.ITEM, stepId,
                        () -> new VerticalStepBlockItem(stepBlock, sourceVerticalSlab, props));

                ResourceLocation legacyAliasId = bitsandbalance$legacyMalformedStepItemId(stepId);
                if (legacyAliasId != null && !BuiltInRegistries.ITEM.containsKey(legacyAliasId)) {
                    Item.Properties aliasProps = new Item.Properties()
                        ;
                    event.register(Registries.ITEM, legacyAliasId,
                        () -> new VerticalStepBlockItem(stepBlock, sourceVerticalSlab, aliasProps));
                }
            } catch (Throwable ignored) {
            }
        }
    }

            private static ResourceLocation bitsandbalance$legacyMalformedStepItemId(ResourceLocation stepId) {
            String path = stepId.getPath();
            if (!path.matches(".*_step_\\d+$")) return null;
            return ResourceLocation.fromNamespaceAndPath(stepId.getNamespace(), path + "_step");
            }

    private static List<Block> bitsandbalance$sortedRegistryBlocks() {
        List<Block> blocks = new ArrayList<>();
        BuiltInRegistries.BLOCK.forEach(blocks::add);
        blocks.sort(Comparator
                .comparingInt((Block block) -> bitsandbalance$isVanillaBlock(block) ? 0 : 1)
                // Reverse resource-id order preserves the released fallback assignment
                // for the netherexp/infernalexp basalt slab collisions.
                .thenComparing(block -> {
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                    return id == null ? "" : id.toString();
                }, Comparator.<String>reverseOrder()));
        return blocks;
    }

    private static boolean bitsandbalance$isVanillaBlock(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null && "minecraft".equals(id.getNamespace());
    }

    private static void bitsandbalance$registerForVerticalSlabIfNeeded(RegisterEvent event,
                                                                         ResourceLocation verticalSlabId,
                                                                         Block verticalSlabBlock) {
        try {
            if (verticalSlabBlock == null || verticalSlabId == null) return;
            if (!(verticalSlabBlock instanceof FixedVerticalSlabBlock)) return;
            if (VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlabBlock) != null) return;

            if (bitsandbalance$shouldSkipDynamicGeneration(verticalSlabId, verticalSlabBlock)) return;

            ResourceLocation stepId = VerticalStepDynamicRegistry.idForVerticalSlab(verticalSlabId, BitsAndBalance.MODID);
            if (BuiltInRegistries.BLOCK.containsKey(stepId)) {
                Block existing = BuiltInRegistries.BLOCK.get(stepId);
                if (existing != null) {
                    VerticalStepDynamicRegistry.register(verticalSlabBlock, existing);
                }
                return;
            }

            FixedVerticalStepBlock stepBlock = new FixedVerticalStepBlock(
                    verticalSlabBlock,
                    DynamicVariantMaterialHelper.copySafeDynamicProperties(verticalSlabBlock)
                        .noOcclusion()
                            
            );

            event.register(Registries.BLOCK, stepId, () -> stepBlock);
            VerticalStepDynamicRegistry.register(verticalSlabBlock, stepBlock);
        } catch (Throwable ignored) {
        }
    }

    private static boolean bitsandbalance$shouldSkipDynamicGeneration(ResourceLocation verticalSlabId,
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

            ResourceLocation candidate1 = ResourceLocation.fromNamespaceAndPath(ns, "vertical_" + base + "_step");
            if (BuiltInRegistries.BLOCK.containsKey(candidate1)) return true;

            ResourceLocation candidate2 = ResourceLocation.fromNamespaceAndPath(ns, "vertical_" + base.replace('/', '_') + "_step");
            return BuiltInRegistries.BLOCK.containsKey(candidate2);
        } catch (Throwable ignored) {
            return false;
        }
    }
}

