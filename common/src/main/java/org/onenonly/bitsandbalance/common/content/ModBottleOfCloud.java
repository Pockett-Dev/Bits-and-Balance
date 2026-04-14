package org.onenonly.bitsandbalance.common.content;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.CloudBlock;
import org.onenonly.bitsandbalance.common.items.BottleOfCloudItem;
import org.onenonly.bitsandbalance.common.registry.ContentRegistrar;

public final class ModBottleOfCloud {
    public static final Identifier CLOUD_BLOCK_ID = id("cloud_block");
    public static final Identifier BOTTLE_OF_CLOUD_ID = id("bottle_of_cloud");

    private ModBottleOfCloud() {
    }

    public static void register(ContentRegistrar registrar) {
        registrar.registerBlock(CLOUD_BLOCK_ID, ModBottleOfCloud::createCloudBlock);
        registrar.registerItem(CLOUD_BLOCK_ID, () -> new BlockItem(cloudBlock(), itemProps(CLOUD_BLOCK_ID)));
        registrar.registerItem(BOTTLE_OF_CLOUD_ID, () -> new BottleOfCloudItem(itemProps(BOTTLE_OF_CLOUD_ID).stacksTo(16)));
    }

    public static Block cloudBlock() {
        return BuiltInRegistries.BLOCK.get(CLOUD_BLOCK_ID).map(holder -> holder.value()).orElse(Blocks.AIR);
    }

    public static Item bottleOfCloud() {
        return BuiltInRegistries.ITEM.get(BOTTLE_OF_CLOUD_ID).map(holder -> holder.value()).orElse(null);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, path);
    }

    private static Block createCloudBlock() {
        return new CloudBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)
                .setId(ResourceKey.create(Registries.BLOCK, CLOUD_BLOCK_ID))
                .strength(0.2F)
                .noOcclusion()
                .sound(SoundType.WOOL)
        );
    }

    private static Item.Properties itemProps(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }
}
