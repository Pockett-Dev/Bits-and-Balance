package org.onenonly.bitsandbalance.common.content;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.registry.ContentRegistrar;

public final class ModRawQuartz {
    public static final Identifier RAW_QUARTZ_BLOCK_ID = id("raw_quartz_block");

    private ModRawQuartz() {
    }

    public static void register(ContentRegistrar registrar) {
        registrar.registerBlock(RAW_QUARTZ_BLOCK_ID, ModRawQuartz::createRawQuartzBlock);
        registrar.registerItem(RAW_QUARTZ_BLOCK_ID, () -> new BlockItem(rawQuartzBlock(), itemProps(RAW_QUARTZ_BLOCK_ID)));
    }

    public static Block rawQuartzBlock() {
        return BuiltInRegistries.BLOCK.get(RAW_QUARTZ_BLOCK_ID).map(holder -> holder.value()).orElse(Blocks.AIR);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, path);
    }

    private static Block createRawQuartzBlock() {
        return new Block(
            BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK)
                .sound(Blocks.NETHER_QUARTZ_ORE.defaultBlockState().getSoundType())
                .setId(ResourceKey.create(Registries.BLOCK, RAW_QUARTZ_BLOCK_ID))
        );
    }

    private static Item.Properties itemProps(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }
}
