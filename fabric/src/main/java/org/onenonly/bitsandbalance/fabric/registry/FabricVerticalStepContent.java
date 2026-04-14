package org.onenonly.bitsandbalance.fabric.registry;

import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepCrackProxyBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.items.VerticalStepBlockItem;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.compat.BlockEntityTypeExt;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;

/** Fabric-only registration for vertical step blocks. */
public final class FabricVerticalStepContent {

    public static final Identifier VERTICAL_STEP_ID =
            Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_step");
    public static final Identifier QUAD_VERTICAL_STEP_ID =
            Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "quad_vertical_step");

    public static Block VERTICAL_STEP;
    public static Block QUAD_VERTICAL_STEP;

    private static boolean registered;

    private FabricVerticalStepContent() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        // Generic vertical step block (base type).
        VerticalStepBlock vsBlock = new VerticalStepBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0F, 6.0F)
            .noOcclusion()
                .sound(SoundType.STONE)
                .setId(ResourceKey.create(Registries.BLOCK, VERTICAL_STEP_ID)));
        VERTICAL_STEP = Registry.register(BuiltInRegistries.BLOCK, VERTICAL_STEP_ID, vsBlock);
        CommonBlocks.VERTICAL_STEP = vsBlock;

        Identifier proxyId = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_step_crack_proxy");
        VerticalStepCrackProxyBlock proxyBlock = new VerticalStepCrackProxyBlock(
            BlockBehaviour.Properties.of()
                .noCollision()
                .noOcclusion()
                .strength(-1.0F)
                .setId(ResourceKey.create(Registries.BLOCK, proxyId)));
        Registry.register(BuiltInRegistries.BLOCK, proxyId, proxyBlock);
        CommonBlocks.VERTICAL_STEP_CRACK_PROXY = proxyBlock;

        // Quad vertical step container block.
        QuadVerticalStepBlock quadBlock = new QuadVerticalStepBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0F, 6.0F)
            .noOcclusion()
                .sound(SoundType.STONE)
                .setId(ResourceKey.create(Registries.BLOCK, QUAD_VERTICAL_STEP_ID)));
        QUAD_VERTICAL_STEP = Registry.register(BuiltInRegistries.BLOCK, QUAD_VERTICAL_STEP_ID, quadBlock);
        CommonBlocks.QUAD_VERTICAL_STEP = quadBlock;

        if (!FabricBuildingConfig.enhancedSlabsSteps) {
            return;
        }

        // Dynamic per-vertical-slab vertical step blocks: initial scan.
        for (Block b : BuiltInRegistries.BLOCK) {
            Identifier bId = BuiltInRegistries.BLOCK.getKey(b);
            bitsandbalance$registerForVerticalSlabIfNeeded(bId, b);
        }

        // Late-registered modded vertical slabs.
        RegistryEntryAddedCallback.event(BuiltInRegistries.BLOCK).register((rawId, id, b) ->
                bitsandbalance$registerForVerticalSlabIfNeeded(id, b)
        );
    }

    private static void bitsandbalance$registerForVerticalSlabIfNeeded(Identifier verticalSlabId,
                                                                         Block verticalSlabBlock) {
        try {
            if (verticalSlabBlock == null) return;
            if (!(verticalSlabBlock instanceof FixedVerticalSlabBlock)) return;
            if (VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlabBlock) != null) return;

            if (bitsandbalance$shouldSkipDynamicGeneration(verticalSlabId, verticalSlabBlock)) return;

            Identifier stepId = VerticalStepDynamicRegistry.idForVerticalSlab(
                    verticalSlabId, BitsAndBalanceCommon.MOD_ID);
            if (BuiltInRegistries.BLOCK.containsKey(stepId)) {
                Block existing = BuiltInRegistries.BLOCK.getValue(stepId);
                if (existing != null) {
                    VerticalStepDynamicRegistry.register(verticalSlabBlock, existing);
                    bitsandbalance$tryAddValidBlock(existing);
                    bitsandbalance$tryRegisterVerticalStepItem(existing, verticalSlabBlock);
                }
                return;
            }

            FixedVerticalStepBlock fixed = new FixedVerticalStepBlock(
                    verticalSlabBlock,
                    BlockBehaviour.Properties.ofFullCopy(verticalSlabBlock)
                        .noOcclusion()
                            .setId(ResourceKey.create(Registries.BLOCK, stepId))
            );
            Block registeredBlock = Registry.register(BuiltInRegistries.BLOCK, stepId, fixed);
            VerticalStepDynamicRegistry.register(verticalSlabBlock, registeredBlock);
            bitsandbalance$tryAddValidBlock(registeredBlock);
            bitsandbalance$tryRegisterVerticalStepItem(registeredBlock, verticalSlabBlock);
        } catch (Throwable ignored) {
        }
    }

    private static boolean bitsandbalance$shouldSkipDynamicGeneration(Identifier verticalSlabId,
                                                                        Block verticalSlabBlock) {
        try {
            if (verticalSlabId == null || verticalSlabBlock == null) return false;
            try {
                if (verticalSlabBlock.builtInRegistryHolder()
                        .is(VerticalStepDynamicRegistry.NO_DYNAMIC_VERTICAL_STEPS)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
            String ns   = verticalSlabId.getNamespace();
            String path = verticalSlabId.getPath();
            String base = path;
            if (base.startsWith("vertical_")) {
                base = base.substring("vertical_".length());
            }
            if (base.endsWith("_slab")) {
                base = base.substring(0, base.length() - "_slab".length());
            }
            Identifier c1 = Identifier.fromNamespaceAndPath(ns, "vertical_" + base + "_step");
            if (BuiltInRegistries.BLOCK.containsKey(c1)) return true;
            Identifier c2 = Identifier.fromNamespaceAndPath(ns, "vertical_" + base.replace('/', '_') + "_step");
            return BuiltInRegistries.BLOCK.containsKey(c2);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void bitsandbalance$tryRegisterVerticalStepItem(Block stepBlock, Block sourceVerticalSlab) {
        try {
            if (!(stepBlock instanceof FixedVerticalStepBlock)) return;
            Identifier stepId = BuiltInRegistries.BLOCK.getKey(stepBlock);
            if (stepId == null) return;
            if (BuiltInRegistries.ITEM.containsKey(stepId)) return;
            Item.Properties props = new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, stepId));
            Registry.register(BuiltInRegistries.ITEM, stepId,
                    new VerticalStepBlockItem(stepBlock, sourceVerticalSlab, props));

            Identifier legacyAliasId = bitsandbalance$legacyMalformedStepItemId(stepId);
            if (legacyAliasId != null && !BuiltInRegistries.ITEM.containsKey(legacyAliasId)) {
                Item.Properties aliasProps = new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, legacyAliasId));
                Registry.register(BuiltInRegistries.ITEM, legacyAliasId,
                        new VerticalStepBlockItem(stepBlock, sourceVerticalSlab, aliasProps));
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
        if (CommonBlockEntities.VERTICAL_STEP == null) return;
        try {
            ((BlockEntityTypeExt) (Object) CommonBlockEntities.VERTICAL_STEP)
                    .bitsandbalance$addValidBlocks(stepBlock);
        } catch (Throwable ignored) {
        }
    }
}
