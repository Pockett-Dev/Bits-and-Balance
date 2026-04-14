package org.onenonly.bitsandbalance.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.CandleBundleBlock;

/** Fabric-only registration for the bundle candle container block. */
public final class FabricCandleBundleContent {
    public static final Identifier BUNDLE_CANDLE_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "bundle_candle");

    public static Block BUNDLE_CANDLE;

    private static boolean registered;

    private FabricCandleBundleContent() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        BUNDLE_CANDLE = new CandleBundleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CANDLE)
            .setId(ResourceKey.create(Registries.BLOCK, BUNDLE_CANDLE_ID)));

        Registry.register(BuiltInRegistries.BLOCK, BUNDLE_CANDLE_ID, BUNDLE_CANDLE);
        Registry.register(BuiltInRegistries.ITEM, BUNDLE_CANDLE_ID,
            new BlockItem(BUNDLE_CANDLE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, BUNDLE_CANDLE_ID))));
    }
}
