package org.onenonly.bitsandbalance.fabric.registry;

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
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabCrackProxyBlock;
import org.onenonly.bitsandbalance.common.items.VerticalSlabBlockItem;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.compat.BlockEntityTypeExt;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;

import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;

/** Fabric-only registration for the vertical slab block. */
public final class FabricVerticalSlabContent {
    public static final Identifier VERTICAL_SLAB_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_slab");

    public static Block VERTICAL_SLAB;

    private static boolean registered;

    private FabricVerticalSlabContent() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        // Legacy generic vertical slab block (kept for backwards compatibility).
        VerticalSlabBlock block = new VerticalSlabBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0F, 6.0F)
                .sound(SoundType.STONE)
                .setId(ResourceKey.create(Registries.BLOCK, VERTICAL_SLAB_ID)));

        VERTICAL_SLAB = Registry.register(BuiltInRegistries.BLOCK, VERTICAL_SLAB_ID, block);
        CommonBlocks.VERTICAL_SLAB = block;

        // Crack proxy block — supplies correct half-prism geometry for the breaking animation.
        Identifier proxyId = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_slab_crack_proxy");
        VerticalSlabCrackProxyBlock proxyBlock = new VerticalSlabCrackProxyBlock(
                BlockBehaviour.Properties.of()
                        .noCollision()
                        .noOcclusion()
                        .strength(-1.0F)
                        .setId(ResourceKey.create(Registries.BLOCK, proxyId)));
        Registry.register(BuiltInRegistries.BLOCK, proxyId, proxyBlock);
        CommonBlocks.VERTICAL_SLAB_CRACK_PROXY = proxyBlock;

        // No BlockItem for the legacy generic vertical slab block.

        if (!FabricBuildingConfig.enhancedSlabsVerticalSlabs) {
            return;
        }

        // Dynamic per-slab vertical slab blocks.
        // - Initial scan catches blocks registered before our initializer runs.
        for (Block b : BuiltInRegistries.BLOCK) {
            Identifier slabId = BuiltInRegistries.BLOCK.getKey(b);
            bitsandbalance$registerForSlabIfNeeded(slabId, b);
        }

        // - Callback catches blocks registered after our initializer runs.
        RegistryEntryAddedCallback.event(BuiltInRegistries.BLOCK).register((rawId, id, b) ->
                bitsandbalance$registerForSlabIfNeeded(id, b)
        );
    }

    private static void bitsandbalance$registerForSlabIfNeeded(Identifier slabId, Block slabBlock) {
        try {
            if (slabBlock == null) return;
            if (!(slabBlock instanceof SlabBlock)) return;
            if (VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock) != null) return;

            // Compatibility: allow opting specific slabs out of dynamic vertical slab generation,
            // and skip generation if another mod already provides a vertical slab with a common id.
            if (bitsandbalance$shouldSkipDynamicGeneration(slabId, slabBlock)) return;

            Identifier verticalId = VerticalSlabDynamicRegistry.idForSlab(slabId, BitsAndBalanceCommon.MOD_ID);
            if (BuiltInRegistries.BLOCK.containsKey(verticalId)) {
                Block existing = BuiltInRegistries.BLOCK.getValue(verticalId);
                if (existing != null) {
                    VerticalSlabDynamicRegistry.register(slabBlock, existing);
                    bitsandbalance$tryAddValidBlock(existing);
                    bitsandbalance$tryRegisterVerticalItem(existing, slabBlock);
                }
                return;
            }

            FixedVerticalSlabBlock vertical = new FixedVerticalSlabBlock(
                    BlockBehaviour.Properties.ofFullCopy(slabBlock)
                            .setId(ResourceKey.create(Registries.BLOCK, verticalId)),
                    slabBlock
            );
            Block registeredVertical = Registry.register(BuiltInRegistries.BLOCK, verticalId, vertical);
            VerticalSlabDynamicRegistry.register(slabBlock, registeredVertical);
            bitsandbalance$tryAddValidBlock(registeredVertical);
            bitsandbalance$tryRegisterVerticalItem(registeredVertical, slabBlock);
        } catch (Throwable ignored) {
        }
    }

    private static boolean bitsandbalance$shouldSkipDynamicGeneration(Identifier slabId, Block slabBlock) {
        try {
            if (slabId == null || slabBlock == null) return false;

            if ("minecraft".equals(slabId.getNamespace()) && "petrified_oak_slab".equals(slabId.getPath())) {
                return true;
            }

            // Data-driven opt-out.
            try {
                if (slabBlock.builtInRegistryHolder().is(VerticalSlabDynamicRegistry.NO_DYNAMIC_VERTICAL_SLABS)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }

            // Heuristic opt-out: if the slab's own namespace already has a common "vertical_<slab>" id,
            // assume another mod is providing vertical slabs and avoid generating duplicates.
            String ns = slabId.getNamespace();
            String path = slabId.getPath();

            Identifier candidate1 = Identifier.fromNamespaceAndPath(ns, "vertical_" + path);
            if (BuiltInRegistries.BLOCK.containsKey(candidate1)) return true;

            Identifier candidate2 = Identifier.fromNamespaceAndPath(ns, "vertical_" + path.replace('/', '_'));
            return BuiltInRegistries.BLOCK.containsKey(candidate2);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void bitsandbalance$tryRegisterVerticalItem(Block verticalBlock, Block sourceSlab) {
        try {
            if (!(verticalBlock instanceof FixedVerticalSlabBlock)) return;

            Identifier verticalId = BuiltInRegistries.BLOCK.getKey(verticalBlock);
            if (verticalId == null) return;
            if (BuiltInRegistries.ITEM.containsKey(verticalId)) return;

            Item.Properties props = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, verticalId));
            Registry.register(BuiltInRegistries.ITEM, verticalId, new VerticalSlabBlockItem(verticalBlock, sourceSlab, props));
        } catch (Throwable ignored) {
        }
    }

    private static void bitsandbalance$tryAddValidBlock(Block verticalBlock) {
        if (CommonBlockEntities.VERTICAL_SLAB == null) return;
        try {
            ((BlockEntityTypeExt) (Object) CommonBlockEntities.VERTICAL_SLAB)
                    .bitsandbalance$addValidBlocks(verticalBlock);
        } catch (Throwable ignored) {
        }
    }
}
