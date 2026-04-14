package org.onenonly.bitsandbalance.common.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.onenonly.bitsandbalance.blocks.CustomOreBlock;
import org.onenonly.bitsandbalance.blocks.CustomRedstoneOreBlock;
import org.onenonly.bitsandbalance.blocks.CustomZincOreBlock;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.registry.ContentRegistrar;

/**
 * Ore-variant blocks referenced heavily by recipes + assets.
 */
public final class ModOreVariants {
    private ModOreVariants() {
    }

    public static void register(ContentRegistrar registrar) {
        // COAL
        registerCoal(registrar, id("andesite_coal_ore"), Blocks.ANDESITE);
        registerCoal(registrar, id("diorite_coal_ore"), Blocks.DIORITE);
        registerCoal(registrar, id("granite_coal_ore"), Blocks.GRANITE);
        registerCoal(registrar, id("tuff_coal_ore"), Blocks.TUFF);

        // COPPER
        registerRawDropOre(registrar, id("andesite_copper_ore"), Blocks.COPPER_ORE, Blocks.ANDESITE, Items.RAW_COPPER);
        registerRawDropOre(registrar, id("diorite_copper_ore"), Blocks.COPPER_ORE, Blocks.DIORITE, Items.RAW_COPPER);
        registerRawDropOre(registrar, id("granite_copper_ore"), Blocks.COPPER_ORE, Blocks.GRANITE, Items.RAW_COPPER);
        registerRawDropOre(registrar, id("tuff_copper_ore"), Blocks.COPPER_ORE, Blocks.TUFF, Items.RAW_COPPER);

        // IRON
        registerRawDropOre(registrar, id("andesite_iron_ore"), Blocks.IRON_ORE, Blocks.ANDESITE, Items.RAW_IRON);
        registerRawDropOre(registrar, id("diorite_iron_ore"), Blocks.IRON_ORE, Blocks.DIORITE, Items.RAW_IRON);
        registerRawDropOre(registrar, id("granite_iron_ore"), Blocks.IRON_ORE, Blocks.GRANITE, Items.RAW_IRON);
        registerRawDropOre(registrar, id("tuff_iron_ore"), Blocks.IRON_ORE, Blocks.TUFF, Items.RAW_IRON);

        // GOLD
        registerRawDropOre(registrar, id("andesite_gold_ore"), Blocks.GOLD_ORE, Blocks.ANDESITE, Items.RAW_GOLD);
        registerRawDropOre(registrar, id("diorite_gold_ore"), Blocks.GOLD_ORE, Blocks.DIORITE, Items.RAW_GOLD);
        registerRawDropOre(registrar, id("granite_gold_ore"), Blocks.GOLD_ORE, Blocks.GRANITE, Items.RAW_GOLD);
        registerRawDropOre(registrar, id("tuff_gold_ore"), Blocks.GOLD_ORE, Blocks.TUFF, Items.RAW_GOLD);

        // DIAMOND
        registerGemOre(registrar, id("andesite_diamond_ore"), Blocks.DIAMOND_ORE, Blocks.ANDESITE, Items.DIAMOND, 3, 7);
        registerGemOre(registrar, id("diorite_diamond_ore"), Blocks.DIAMOND_ORE, Blocks.DIORITE, Items.DIAMOND, 3, 7);
        registerGemOre(registrar, id("granite_diamond_ore"), Blocks.DIAMOND_ORE, Blocks.GRANITE, Items.DIAMOND, 3, 7);
        registerGemOre(registrar, id("tuff_diamond_ore"), Blocks.DIAMOND_ORE, Blocks.TUFF, Items.DIAMOND, 3, 7);

        // EMERALD
        registerGemOre(registrar, id("andesite_emerald_ore"), Blocks.EMERALD_ORE, Blocks.ANDESITE, Items.EMERALD, 3, 7);
        registerGemOre(registrar, id("diorite_emerald_ore"), Blocks.EMERALD_ORE, Blocks.DIORITE, Items.EMERALD, 3, 7);
        registerGemOre(registrar, id("granite_emerald_ore"), Blocks.EMERALD_ORE, Blocks.GRANITE, Items.EMERALD, 3, 7);
        registerGemOre(registrar, id("tuff_emerald_ore"), Blocks.EMERALD_ORE, Blocks.TUFF, Items.EMERALD, 3, 7);

        // LAPIS (4-9 lapis, 2-5 XP)
        registerLapis(registrar, id("andesite_lapis_ore"), Blocks.ANDESITE);
        registerLapis(registrar, id("diorite_lapis_ore"), Blocks.DIORITE);
        registerLapis(registrar, id("granite_lapis_ore"), Blocks.GRANITE);
        registerLapis(registrar, id("tuff_lapis_ore"), Blocks.TUFF);

        // REDSTONE (lit property)
        registerRedstone(registrar, id("andesite_redstone_ore"), Blocks.ANDESITE);
        registerRedstone(registrar, id("diorite_redstone_ore"), Blocks.DIORITE);
        registerRedstone(registrar, id("granite_redstone_ore"), Blocks.GRANITE);
        registerRedstone(registrar, id("tuff_redstone_ore"), Blocks.TUFF);

        // ZINC (Create integration handled in block class)
        registerZinc(registrar, id("andesite_zinc_ore"), Blocks.ANDESITE);
        registerZinc(registrar, id("diorite_zinc_ore"), Blocks.DIORITE);
        registerZinc(registrar, id("granite_zinc_ore"), Blocks.GRANITE);
        registerZinc(registrar, id("tuff_zinc_ore"), Blocks.TUFF);
    }

    private static void registerCoal(ContentRegistrar registrar, Identifier id, Block hostStone) {
        Block block = new CustomOreBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_ORE)
                .setId(ResourceKey.create(Registries.BLOCK, id))
                .sound(hostStone.defaultBlockState().getSoundType())
                .requiresCorrectToolForDrops(),
            Items.COAL,
            0,
            2
        );
        registerBlockWithItem(registrar, id, block);
    }

    private static void registerRawDropOre(ContentRegistrar registrar, Identifier id, Block vanillaOre, Block hostStone, Item drop) {
        Block block = new CustomOreBlock(
            BlockBehaviour.Properties.ofFullCopy(vanillaOre)
                .setId(ResourceKey.create(Registries.BLOCK, id))
                .sound(hostStone.defaultBlockState().getSoundType())
                .requiresCorrectToolForDrops(),
            drop,
            0,
            0
        );
        registerBlockWithItem(registrar, id, block);
    }

    private static void registerGemOre(ContentRegistrar registrar, Identifier id, Block vanillaOre, Block hostStone, Item drop, int minXp, int maxXp) {
        Block block = new CustomOreBlock(
            BlockBehaviour.Properties.ofFullCopy(vanillaOre)
                .setId(ResourceKey.create(Registries.BLOCK, id))
                .sound(hostStone.defaultBlockState().getSoundType())
                .requiresCorrectToolForDrops(),
            drop,
            minXp,
            maxXp
        );
        registerBlockWithItem(registrar, id, block);
    }

    private static void registerLapis(ContentRegistrar registrar, Identifier id, Block hostStone) {
        Block block = new CustomOreBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_ORE)
                .setId(ResourceKey.create(Registries.BLOCK, id))
                .sound(hostStone.defaultBlockState().getSoundType())
                .requiresCorrectToolForDrops(),
            Items.LAPIS_LAZULI,
            2,
            5,
            4,
            9
        );
        registerBlockWithItem(registrar, id, block);
    }

    private static void registerRedstone(ContentRegistrar registrar, Identifier id, Block hostStone) {
        Block block = new CustomRedstoneOreBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_ORE)
                .setId(ResourceKey.create(Registries.BLOCK, id))
                .sound(hostStone.defaultBlockState().getSoundType())
                .requiresCorrectToolForDrops()
                .lightLevel(state -> state.getValue(CustomRedstoneOreBlock.LIT) ? 9 : 0)
                .randomTicks(),
            Items.REDSTONE,
            1,
            5,
            4,
            5
        );
        registerBlockWithItem(registrar, id, block);
    }

    private static void registerZinc(ContentRegistrar registrar, Identifier id, Block hostStone) {
        Block block = new CustomZincOreBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)
                .setId(ResourceKey.create(Registries.BLOCK, id))
                .sound(hostStone.defaultBlockState().getSoundType())
                .requiresCorrectToolForDrops()
        );
        registerBlockWithItem(registrar, id, block);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, path);
    }

    private static void registerBlockWithItem(ContentRegistrar registrar, Identifier id, Block block) {
        registrar.registerBlock(id, block);
        registrar.registerItem(id, new BlockItem(block, itemProps(id)));
    }

    private static Item.Properties itemProps(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }
}
