package org.onenonly.bitsandbalance.fabric.registry;

import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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
import org.onenonly.bitsandbalance.common.blocks.StorageQuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepCrackProxyBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.items.VerticalStepBlockItem;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.compat.BlockEntityTypeExt;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;

/** Fabric-only registration for vertical step blocks. */
public final class FabricVerticalStepContent {

    public static final ResourceLocation VERTICAL_STEP_ID =
            ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_step");
    public static final ResourceLocation QUAD_VERTICAL_STEP_ID =
            ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "quad_vertical_step");

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
                );
        VERTICAL_STEP = Registry.register(BuiltInRegistries.BLOCK, VERTICAL_STEP_ID, vsBlock);
        CommonBlocks.VERTICAL_STEP = vsBlock;

        ResourceLocation proxyId = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_step_crack_proxy");
        VerticalStepCrackProxyBlock proxyBlock = new VerticalStepCrackProxyBlock(
            BlockBehaviour.Properties.of()
                .noCollission()
                .noOcclusion()
                .strength(-1.0F)
                );
        Registry.register(BuiltInRegistries.BLOCK, proxyId, proxyBlock);
        CommonBlocks.VERTICAL_STEP_CRACK_PROXY = proxyBlock;

        // Quad vertical step container block.
        QuadVerticalStepBlock quadBlock = new StorageQuadVerticalStepBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0F, 6.0F)
                .noOcclusion()
                .dynamicShape()
                .sound(SoundType.STONE)
                );
        QUAD_VERTICAL_STEP = Registry.register(BuiltInRegistries.BLOCK, QUAD_VERTICAL_STEP_ID, quadBlock);
        CommonBlocks.QUAD_VERTICAL_STEP = quadBlock;

        if (!FabricBuildingConfig.enhancedSlabsSteps) {
            return;
        }

        // Dynamic per-vertical-slab vertical step blocks: initial scan.
        for (Block b : BuiltInRegistries.BLOCK) {
            ResourceLocation bId = BuiltInRegistries.BLOCK.getKey(b);
            bitsandbalance$registerForVerticalSlabIfNeeded(bId, b);
        }

        // Late-registered modded vertical slabs.
        RegistryEntryAddedCallback.event(BuiltInRegistries.BLOCK).register((rawId, id, b) ->
                bitsandbalance$registerForVerticalSlabIfNeeded(id, b)
        );
    }

    private static void bitsandbalance$registerForVerticalSlabIfNeeded(ResourceLocation verticalSlabId,
                                                                         Block verticalSlabBlock) {
        if (verticalSlabBlock == null) return;
        if (!(verticalSlabBlock instanceof FixedVerticalSlabBlock)) return;
        if (VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlabBlock) != null) return;

        if (bitsandbalance$shouldSkipDynamicGeneration(verticalSlabId, verticalSlabBlock)) return;

        ResourceLocation stepId = VerticalStepDynamicRegistry.idForVerticalSlab(
                verticalSlabId, BitsAndBalanceCommon.MOD_ID);
        if (BuiltInRegistries.BLOCK.containsKey(stepId)) {
            Block existing = BuiltInRegistries.BLOCK.get(stepId);
            if (existing != null) {
                VerticalStepDynamicRegistry.register(verticalSlabBlock, existing);
                bitsandbalance$tryAddValidBlock(existing);
                bitsandbalance$tryRegisterVerticalStepItem(existing, verticalSlabBlock);
            }
            return;
        }

        FixedVerticalStepBlock fixed = bitsandbalance$createDynamicVerticalStepBlock(verticalSlabId, stepId, verticalSlabBlock);
        Block registeredBlock = bitsandbalance$registerDynamicVerticalStepBlock(verticalSlabId, stepId, fixed);
        VerticalStepDynamicRegistry.register(verticalSlabBlock, registeredBlock);
        bitsandbalance$tryAddValidBlock(registeredBlock);
        bitsandbalance$tryRegisterVerticalStepItem(registeredBlock, verticalSlabBlock);
    }

    private static FixedVerticalStepBlock bitsandbalance$createDynamicVerticalStepBlock(ResourceLocation verticalSlabId, ResourceLocation stepId, Block verticalSlabBlock) {
        try {
            return new FixedVerticalStepBlock(
                    verticalSlabBlock,
                    DynamicVariantMaterialHelper.copySafeDynamicProperties(verticalSlabBlock)
                        .noOcclusion()
            );
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to create dynamic vertical step block " + stepId + " from vertical slab " + verticalSlabId, throwable);
        }
    }

    private static Block bitsandbalance$registerDynamicVerticalStepBlock(ResourceLocation verticalSlabId, ResourceLocation stepId, FixedVerticalStepBlock fixed) {
        try {
            return Registry.register(BuiltInRegistries.BLOCK, stepId, fixed);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to register dynamic vertical step block " + stepId + " from vertical slab " + verticalSlabId, throwable);
        }
    }

    private static boolean bitsandbalance$shouldSkipDynamicGeneration(ResourceLocation verticalSlabId,
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
            ResourceLocation c1 = ResourceLocation.fromNamespaceAndPath(ns, "vertical_" + base + "_step");
            if (BuiltInRegistries.BLOCK.containsKey(c1)) return true;
            ResourceLocation c2 = ResourceLocation.fromNamespaceAndPath(ns, "vertical_" + base.replace('/', '_') + "_step");
            return BuiltInRegistries.BLOCK.containsKey(c2);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void bitsandbalance$tryRegisterVerticalStepItem(Block stepBlock, Block sourceVerticalSlab) {
        try {
            if (!(stepBlock instanceof FixedVerticalStepBlock)) return;
            ResourceLocation stepId = BuiltInRegistries.BLOCK.getKey(stepBlock);
            if (stepId == null) return;
            if (BuiltInRegistries.ITEM.containsKey(stepId)) return;
            Item.Properties props = new Item.Properties()
                    ;
            Registry.register(BuiltInRegistries.ITEM, stepId,
                    new VerticalStepBlockItem(stepBlock, sourceVerticalSlab, props));

            ResourceLocation legacyAliasId = bitsandbalance$legacyMalformedStepItemId(stepId);
            if (legacyAliasId != null && !BuiltInRegistries.ITEM.containsKey(legacyAliasId)) {
                Item.Properties aliasProps = new Item.Properties()
                        ;
                Registry.register(BuiltInRegistries.ITEM, legacyAliasId,
                        new VerticalStepBlockItem(stepBlock, sourceVerticalSlab, aliasProps));
            }
        } catch (Throwable ignored) {
        }
    }

    private static ResourceLocation bitsandbalance$legacyMalformedStepItemId(ResourceLocation stepId) {
        String path = stepId.getPath();
        if (!path.matches(".*_step_\\d+$")) return null;
        return ResourceLocation.fromNamespaceAndPath(stepId.getNamespace(), path + "_step");
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
