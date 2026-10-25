package org.onenonly.bitsandbalance.fabric.registry;

import net.minecraft.core.Registry;
//? if mc != 1.21.1
import net.minecraft.core.registries.Registries;
//? endif
import net.minecraft.core.registries.BuiltInRegistries;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
//? if mc != 1.21.1
import net.minecraft.resources.ResourceKey;
//? endif
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.CandleBundleBlock;

/** Fabric-only registration for the bundle candle container block. */
public final class FabricCandleBundleContent {
    //? if mc == 1.21.1
    public static final ResourceLocation BUNDLE_CANDLE_ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "bundle_candle");
    //? else
    public static final Identifier BUNDLE_CANDLE_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "bundle_candle");
    //? endif

    public static Block BUNDLE_CANDLE;

    private static boolean registered;

    private FabricCandleBundleContent() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        BUNDLE_CANDLE = new CandleBundleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CANDLE)
            //? if mc != 1.21.1
            .setId(ResourceKey.create(Registries.BLOCK, BUNDLE_CANDLE_ID))
            //? endif
            );

        Registry.register(BuiltInRegistries.BLOCK, BUNDLE_CANDLE_ID, BUNDLE_CANDLE);
        Registry.register(BuiltInRegistries.ITEM, BUNDLE_CANDLE_ID,
            new BlockItem(BUNDLE_CANDLE, new Item.Properties()
                //? if mc != 1.21.1
                .setId(ResourceKey.create(Registries.ITEM, BUNDLE_CANDLE_ID))
                //? endif
            ));
    }
}
