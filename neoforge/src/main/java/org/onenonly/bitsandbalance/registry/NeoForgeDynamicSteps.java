package org.onenonly.bitsandbalance.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.items.StepBlockItem;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;

/**
 * Registers per-slab step blocks on NeoForge.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class NeoForgeDynamicSteps {

    private NeoForgeDynamicSteps() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterBlocks(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK)) return;

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            Identifier slabId = BuiltInRegistries.BLOCK.getKey(slabBlock);
            bitsandbalance$registerForSlabIfNeeded(event, slabId, slabBlock);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterItems(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.ITEM)) return;

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            try {
                if (!(slabBlock instanceof SlabBlock)) continue;

                Block stepBlock = StepDynamicRegistry.getStepForSlab(slabBlock);
                if (!(stepBlock instanceof FixedStepBlock)) continue;

                Identifier stepId = BuiltInRegistries.BLOCK.getKey(stepBlock);
                if (stepId == null) continue;
                if (BuiltInRegistries.ITEM.containsKey(stepId)) continue;

                Item.Properties props = new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, stepId));
                event.register(Registries.ITEM, stepId, () -> new StepBlockItem(stepBlock, slabBlock, props));

                Identifier legacyAliasId = bitsandbalance$legacyMalformedStepItemId(stepId);
                if (legacyAliasId != null && !BuiltInRegistries.ITEM.containsKey(legacyAliasId)) {
                    Item.Properties aliasProps = new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, legacyAliasId));
                    event.register(Registries.ITEM, legacyAliasId,
                            () -> new StepBlockItem(stepBlock, slabBlock, aliasProps));
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

    private static void bitsandbalance$registerForSlabIfNeeded(RegisterEvent event, Identifier slabId, Block slabBlock) {
        try {
            if (slabBlock == null || slabId == null) return;
            if (!(slabBlock instanceof SlabBlock)) return;
            if (StepDynamicRegistry.getStepForSlab(slabBlock) != null) return;

            if (bitsandbalance$shouldSkipDynamicGeneration(slabId, slabBlock)) return;

            Identifier stepId = StepDynamicRegistry.idForSlab(slabId, BitsAndBalance.MODID);
            if (BuiltInRegistries.BLOCK.containsKey(stepId)) {
                Block existing = BuiltInRegistries.BLOCK.getValue(stepId);
                if (existing != null) {
                    StepDynamicRegistry.register(slabBlock, existing);
                }
                return;
            }

            FixedStepBlock stepBlock = new FixedStepBlock(
                    slabBlock,
                    BlockBehaviour.Properties.ofFullCopy(slabBlock)
                        .noOcclusion()
                            .setId(ResourceKey.create(Registries.BLOCK, stepId))
            );

            event.register(Registries.BLOCK, stepId, () -> stepBlock);
            StepDynamicRegistry.register(slabBlock, stepBlock);
        } catch (Throwable ignored) {
        }
    }

    private static boolean bitsandbalance$shouldSkipDynamicGeneration(Identifier slabId, Block slabBlock) {
        try {
            if (slabId == null || slabBlock == null) return false;

            if ("minecraft".equals(slabId.getNamespace()) && "petrified_oak_slab".equals(slabId.getPath())) {
                return true;
            }

            // Data-driven opt-out tag
            try {
                if (slabBlock.builtInRegistryHolder().is(StepDynamicRegistry.NO_DYNAMIC_STEPS)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }

            // Heuristic opt-out: if the slab's namespace already provides <slab>_step, skip
            String ns   = slabId.getNamespace();
            String path = slabId.getPath();
            String base = path.endsWith("_slab") ? path.substring(0, path.length() - "_slab".length()) : path;

            Identifier candidate1 = Identifier.fromNamespaceAndPath(ns, base + "_step");
            if (BuiltInRegistries.BLOCK.containsKey(candidate1)) return true;

            Identifier candidate2 = Identifier.fromNamespaceAndPath(ns, base.replace('/', '_') + "_step");
            return BuiltInRegistries.BLOCK.containsKey(candidate2);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
