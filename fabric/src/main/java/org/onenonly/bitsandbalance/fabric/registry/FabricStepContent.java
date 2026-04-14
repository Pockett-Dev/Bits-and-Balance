package org.onenonly.bitsandbalance.fabric.registry;

import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepCrackProxyBlock;
import org.onenonly.bitsandbalance.common.items.StepBlockItem;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.compat.BlockEntityTypeExt;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;

/** Fabric-only registration for step blocks. */
public final class FabricStepContent {

    public static final Identifier STEP_ID    = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "step");
    public static final Identifier QUAD_STEP_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "quad_step");

    public static Block STEP;
    public static Block QUAD_STEP;

    private static boolean registered;

    private FabricStepContent() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        // Generic step block (kept for backwards compatibility and as a base type).
        StepBlock stepBlock = new StepBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0F, 6.0F)
            .noOcclusion()
                .sound(SoundType.STONE)
                .setId(ResourceKey.create(Registries.BLOCK, STEP_ID)));
        STEP = Registry.register(BuiltInRegistries.BLOCK, STEP_ID, stepBlock);
        CommonBlocks.STEP = stepBlock;

        Identifier proxyId = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "step_crack_proxy");
        StepCrackProxyBlock proxyBlock = new StepCrackProxyBlock(
            BlockBehaviour.Properties.of()
                .noCollision()
                .noOcclusion()
                .strength(-1.0F)
                .setId(ResourceKey.create(Registries.BLOCK, proxyId)));
        Registry.register(BuiltInRegistries.BLOCK, proxyId, proxyBlock);
        CommonBlocks.STEP_CRACK_PROXY = proxyBlock;

        // Quad step container block.
        QuadStepBlock quadBlock = new QuadStepBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0F, 6.0F)
            .noOcclusion()
                .sound(SoundType.STONE)
                .setId(ResourceKey.create(Registries.BLOCK, QUAD_STEP_ID)));
        QUAD_STEP = Registry.register(BuiltInRegistries.BLOCK, QUAD_STEP_ID, quadBlock);
        CommonBlocks.QUAD_STEP = quadBlock;

        if (!FabricBuildingConfig.enhancedSlabsSteps) {
            return;
        }

        // Dynamic per-slab step blocks: initial scan.
        for (Block b : BuiltInRegistries.BLOCK) {
            Identifier slabId = BuiltInRegistries.BLOCK.getKey(b);
            bitsandbalance$registerForSlabIfNeeded(slabId, b);
        }

        // Late-registered modded slabs.
        RegistryEntryAddedCallback.event(BuiltInRegistries.BLOCK).register((rawId, id, b) ->
                bitsandbalance$registerForSlabIfNeeded(id, b)
        );
    }

    private static void bitsandbalance$registerForSlabIfNeeded(Identifier slabId, Block slabBlock) {
        try {
            if (slabBlock == null) return;
            if (!(slabBlock instanceof SlabBlock)) return;
            if (StepDynamicRegistry.getStepForSlab(slabBlock) != null) return;

            if (bitsandbalance$shouldSkipDynamicGeneration(slabId, slabBlock)) return;

            Identifier stepId = StepDynamicRegistry.idForSlab(slabId, BitsAndBalanceCommon.MOD_ID);
            if (BuiltInRegistries.BLOCK.containsKey(stepId)) {
                Block existing = BuiltInRegistries.BLOCK.getValue(stepId);
                if (existing != null) {
                    StepDynamicRegistry.register(slabBlock, existing);
                    bitsandbalance$tryAddValidBlock(existing);
                    bitsandbalance$tryRegisterStepItem(existing, slabBlock);
                }
                return;
            }

            FixedStepBlock fixed = new FixedStepBlock(
                    slabBlock,
                    BlockBehaviour.Properties.ofFullCopy(slabBlock)
                        .noOcclusion()
                            .setId(ResourceKey.create(Registries.BLOCK, stepId))
            );
            Block registered = Registry.register(BuiltInRegistries.BLOCK, stepId, fixed);
            StepDynamicRegistry.register(slabBlock, registered);
            bitsandbalance$tryAddValidBlock(registered);
            bitsandbalance$tryRegisterStepItem(registered, slabBlock);
        } catch (Throwable ignored) {
        }
    }

    private static boolean bitsandbalance$shouldSkipDynamicGeneration(Identifier slabId, Block slabBlock) {
        try {
            if (slabId == null || slabBlock == null) return false;

            if ("minecraft".equals(slabId.getNamespace()) && "petrified_oak_slab".equals(slabId.getPath())) {
                return true;
            }

            try {
                if (slabBlock.builtInRegistryHolder().is(StepDynamicRegistry.NO_DYNAMIC_STEPS)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
            String ns   = slabId.getNamespace();
            String path = slabId.getPath();
            String base = path.endsWith("_slab") ? path.substring(0, path.length() - "_slab".length()) : path;
            Identifier c1 = Identifier.fromNamespaceAndPath(ns, base + "_step");
            if (BuiltInRegistries.BLOCK.containsKey(c1)) return true;
            Identifier c2 = Identifier.fromNamespaceAndPath(ns, base.replace('/', '_') + "_step");
            return BuiltInRegistries.BLOCK.containsKey(c2);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void bitsandbalance$tryRegisterStepItem(Block stepBlock, Block sourceSlab) {
        try {
            if (!(stepBlock instanceof FixedStepBlock)) return;
            Identifier stepId = BuiltInRegistries.BLOCK.getKey(stepBlock);
            if (stepId == null) return;
            if (BuiltInRegistries.ITEM.containsKey(stepId)) return;
            Item.Properties props = new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, stepId));
            Registry.register(BuiltInRegistries.ITEM, stepId, new StepBlockItem(stepBlock, sourceSlab, props));

            Identifier legacyAliasId = bitsandbalance$legacyMalformedStepItemId(stepId);
            if (legacyAliasId != null && !BuiltInRegistries.ITEM.containsKey(legacyAliasId)) {
                Item.Properties aliasProps = new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, legacyAliasId));
                Registry.register(BuiltInRegistries.ITEM, legacyAliasId,
                        new StepBlockItem(stepBlock, sourceSlab, aliasProps));
            }
        } catch (Throwable ignored) {
        }
    }

    private static Identifier bitsandbalance$legacyMalformedStepItemId(Identifier stepId) {
        String path = stepId.getPath();
        if (!path.matches(".*_step_\\d+$")) return null;
        return Identifier.fromNamespaceAndPath(stepId.getNamespace(), path + "_step");
    }

    private static void bitsandbalance$tryAddValidBlock(Block stepBlock) {
        if (CommonBlockEntities.STEP == null) return;
        try {
            ((BlockEntityTypeExt) (Object) CommonBlockEntities.STEP)
                    .bitsandbalance$addValidBlocks(stepBlock);
        } catch (Throwable ignored) {
        }
    }
}
