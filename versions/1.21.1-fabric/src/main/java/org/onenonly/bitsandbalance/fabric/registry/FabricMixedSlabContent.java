package org.onenonly.bitsandbalance.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.StorageMixedSlabBlock;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;

/** Fabric-only registration for the mixed double slab block. */
public final class FabricMixedSlabContent {
    public static final ResourceLocation MIXED_SLAB_ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "mixed_slab");

    public static Block MIXED_SLAB;

    private static boolean registered;

    private FabricMixedSlabContent() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        StorageMixedSlabBlock block = new StorageMixedSlabBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0F, 6.0F)
                .sound(SoundType.STONE)
                .noOcclusion()
                .dynamicShape()
                );

        MIXED_SLAB = Registry.register(BuiltInRegistries.BLOCK, MIXED_SLAB_ID, block);
        CommonBlocks.MIXED_SLAB = block;

        // No BlockItem — mixed slabs are created only by placement merging, never in inventory.
    }
}
