package org.onenonly.bitsandbalance.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagCopyingItemTagProvider;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.blocks.OreVariants;
import org.onenonly.bitsandbalance.common.content.ModKiln;
import org.onenonly.bitsandbalance.common.content.ModRawQuartz;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * 26.2 registry-key based datagen for all ore-variant tags.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class OreVariantTagsProvider {
    private static final List<DeferredHolder<Block, ? extends Block>> COAL = List.of(
            OreVariants.ANDESITE_COAL_ORE, OreVariants.DIORITE_COAL_ORE,
            OreVariants.GRANITE_COAL_ORE, OreVariants.TUFF_COAL_ORE);
    private static final List<DeferredHolder<Block, ? extends Block>> IRON = List.of(
            OreVariants.ANDESITE_IRON_ORE, OreVariants.DIORITE_IRON_ORE,
            OreVariants.GRANITE_IRON_ORE, OreVariants.TUFF_IRON_ORE);
    private static final List<DeferredHolder<Block, ? extends Block>> COPPER = List.of(
            OreVariants.ANDESITE_COPPER_ORE, OreVariants.DIORITE_COPPER_ORE,
            OreVariants.GRANITE_COPPER_ORE, OreVariants.TUFF_COPPER_ORE);
    private static final List<DeferredHolder<Block, ? extends Block>> GOLD = List.of(
            OreVariants.ANDESITE_GOLD_ORE, OreVariants.DIORITE_GOLD_ORE,
            OreVariants.GRANITE_GOLD_ORE, OreVariants.TUFF_GOLD_ORE);
    private static final List<DeferredHolder<Block, ? extends Block>> DIAMOND = List.of(
            OreVariants.ANDESITE_DIAMOND_ORE, OreVariants.DIORITE_DIAMOND_ORE,
            OreVariants.GRANITE_DIAMOND_ORE, OreVariants.TUFF_DIAMOND_ORE);
    private static final List<DeferredHolder<Block, ? extends Block>> EMERALD = List.of(
            OreVariants.ANDESITE_EMERALD_ORE, OreVariants.DIORITE_EMERALD_ORE,
            OreVariants.GRANITE_EMERALD_ORE, OreVariants.TUFF_EMERALD_ORE);
    private static final List<DeferredHolder<Block, ? extends Block>> LAPIS = List.of(
            OreVariants.ANDESITE_LAPIS_ORE, OreVariants.DIORITE_LAPIS_ORE,
            OreVariants.GRANITE_LAPIS_ORE, OreVariants.TUFF_LAPIS_ORE);
    private static final List<DeferredHolder<Block, ? extends Block>> REDSTONE = List.of(
            OreVariants.ANDESITE_REDSTONE_ORE, OreVariants.DIORITE_REDSTONE_ORE,
            OreVariants.GRANITE_REDSTONE_ORE, OreVariants.TUFF_REDSTONE_ORE);
    private static final List<DeferredHolder<Block, ? extends Block>> ZINC = List.of(
            OreVariants.ANDESITE_ZINC_ORE, OreVariants.DIORITE_ZINC_ORE,
            OreVariants.GRANITE_ZINC_ORE, OreVariants.TUFF_ZINC_ORE);
    private static final List<DeferredHolder<Block, ? extends Block>> ALL = Stream.of(
            COAL, IRON, COPPER, GOLD, DIAMOND, EMERALD, LAPIS, REDSTONE, ZINC
    ).flatMap(List::stream).toList();
    private static final List<DeferredHolder<Block, ? extends Block>> STONE_TIER = Stream.of(
            COAL, IRON, COPPER, LAPIS
    ).flatMap(List::stream).toList();
    private static final List<DeferredHolder<Block, ? extends Block>> IRON_TIER = Stream.of(
            GOLD, DIAMOND, EMERALD, REDSTONE, ZINC
    ).flatMap(List::stream).toList();

    private static final TagKey<Block> C_ORES_BLOCK = blockTag("c", "ores");
    private static final TagKey<Block> C_COAL_ORES_BLOCK = blockTag("c", "ores/coal");
    private static final TagKey<Block> C_IRON_ORES_BLOCK = blockTag("c", "ores/iron");
    private static final TagKey<Block> C_COPPER_ORES_BLOCK = blockTag("c", "ores/copper");
    private static final TagKey<Block> C_GOLD_ORES_BLOCK = blockTag("c", "ores/gold");
    private static final TagKey<Block> C_DIAMOND_ORES_BLOCK = blockTag("c", "ores/diamond");
    private static final TagKey<Block> C_EMERALD_ORES_BLOCK = blockTag("c", "ores/emerald");
    private static final TagKey<Block> C_LAPIS_ORES_BLOCK = blockTag("c", "ores/lapis");
    private static final TagKey<Block> C_REDSTONE_ORES_BLOCK = blockTag("c", "ores/redstone");
    private static final TagKey<Block> C_ZINC_ORES_BLOCK = blockTag("c", "ores/zinc");
    private static final TagKey<Block> CREATE_ZINC_ORES_BLOCK = blockTag("create", "zinc_ores");

    private static final TagKey<Item> C_ORES_ITEM = itemTag("c", "ores");
    private static final TagKey<Item> C_COAL_ORES_ITEM = itemTag("c", "ores/coal");
    private static final TagKey<Item> C_IRON_ORES_ITEM = itemTag("c", "ores/iron");
    private static final TagKey<Item> C_COPPER_ORES_ITEM = itemTag("c", "ores/copper");
    private static final TagKey<Item> C_GOLD_ORES_ITEM = itemTag("c", "ores/gold");
    private static final TagKey<Item> C_DIAMOND_ORES_ITEM = itemTag("c", "ores/diamond");
    private static final TagKey<Item> C_EMERALD_ORES_ITEM = itemTag("c", "ores/emerald");
    private static final TagKey<Item> C_LAPIS_ORES_ITEM = itemTag("c", "ores/lapis");
    private static final TagKey<Item> C_REDSTONE_ORES_ITEM = itemTag("c", "ores/redstone");
    private static final TagKey<Item> C_ZINC_ORES_ITEM = itemTag("c", "ores/zinc");
    private static final TagKey<Item> CREATE_ZINC_ORES_ITEM = itemTag("create", "zinc_ores");

    private OreVariantTagsProvider() {
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Server event) {
        PackOutput output = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        BlockTagsProvider blockTags = new BlockTagsProvider(output, lookup, BitsAndBalance.MODID) {
            @Override
            public String getName() {
                return "Ore Variant Tags for " + BitsAndBalance.MODID;
            }

            @Override
            protected void addTags(HolderLookup.Provider provider) {
                TagAppender<Block> pickaxeMineable = tag(BlockTags.MINEABLE_WITH_PICKAXE);
                add(pickaxeMineable, ALL);
                pickaxeMineable.add(
                        ResourceKey.create(Registries.BLOCK, ModRawQuartz.RAW_QUARTZ_BLOCK_ID),
                        ResourceKey.create(Registries.BLOCK, ModKiln.KILN_ID)
                );
                add(tag(BlockTags.NEEDS_STONE_TOOL), STONE_TIER);
                add(tag(BlockTags.NEEDS_IRON_TOOL), IRON_TIER);

                add(tag(BlockItemTags.COAL_ORES.block()), COAL);
                add(tag(BlockItemTags.IRON_ORES.block()), IRON);
                add(tag(BlockItemTags.COPPER_ORES.block()), COPPER);
                add(tag(BlockItemTags.GOLD_ORES.block()), GOLD);
                add(tag(BlockItemTags.DIAMOND_ORES.block()), DIAMOND);
                add(tag(BlockItemTags.EMERALD_ORES.block()), EMERALD);
                add(tag(BlockItemTags.LAPIS_ORES.block()), LAPIS);
                add(tag(BlockItemTags.REDSTONE_ORES.block()), REDSTONE);
                add(tag(BlockTags.OVERWORLD_CARVER_REPLACEABLES), ALL);

                add(tag(C_ORES_BLOCK), ALL);
                add(tag(C_COAL_ORES_BLOCK), COAL);
                add(tag(C_IRON_ORES_BLOCK), IRON);
                add(tag(C_COPPER_ORES_BLOCK), COPPER);
                add(tag(C_GOLD_ORES_BLOCK), GOLD);
                add(tag(C_DIAMOND_ORES_BLOCK), DIAMOND);
                add(tag(C_EMERALD_ORES_BLOCK), EMERALD);
                add(tag(C_LAPIS_ORES_BLOCK), LAPIS);
                add(tag(C_REDSTONE_ORES_BLOCK), REDSTONE);
                add(tag(C_ZINC_ORES_BLOCK), ZINC);
                add(tag(CREATE_ZINC_ORES_BLOCK), ZINC);
            }
        };

        BlockTagCopyingItemTagProvider itemTags = new BlockTagCopyingItemTagProvider(
                output, lookup, blockTags.contentsGetter(), BitsAndBalance.MODID
        ) {
            @Override
            public String getName() {
                return "Ore Variant Item Tags for " + BitsAndBalance.MODID;
            }

            @Override
            protected void addTags(HolderLookup.Provider provider) {
                addItems(tag(BlockItemTags.COAL_ORES.item()), COAL);
                addItems(tag(BlockItemTags.IRON_ORES.item()), IRON);
                addItems(tag(BlockItemTags.COPPER_ORES.item()), COPPER);
                addItems(tag(BlockItemTags.GOLD_ORES.item()), GOLD);
                addItems(tag(BlockItemTags.DIAMOND_ORES.item()), DIAMOND);
                addItems(tag(BlockItemTags.EMERALD_ORES.item()), EMERALD);
                addItems(tag(BlockItemTags.LAPIS_ORES.item()), LAPIS);
                addItems(tag(BlockItemTags.REDSTONE_ORES.item()), REDSTONE);

                addItems(tag(C_ORES_ITEM), ALL);
                addItems(tag(C_COAL_ORES_ITEM), COAL);
                addItems(tag(C_IRON_ORES_ITEM), IRON);
                addItems(tag(C_COPPER_ORES_ITEM), COPPER);
                addItems(tag(C_GOLD_ORES_ITEM), GOLD);
                addItems(tag(C_DIAMOND_ORES_ITEM), DIAMOND);
                addItems(tag(C_EMERALD_ORES_ITEM), EMERALD);
                addItems(tag(C_LAPIS_ORES_ITEM), LAPIS);
                addItems(tag(C_REDSTONE_ORES_ITEM), REDSTONE);
                addItems(tag(C_ZINC_ORES_ITEM), ZINC);
                addItems(tag(CREATE_ZINC_ORES_ITEM), ZINC);
            }
        };

        event.addProvider(blockTags);
        event.addProvider(itemTags);
    }

    private static void add(TagAppender<Block> tag, List<? extends DeferredHolder<Block, ? extends Block>> holders) {
        holders.forEach(holder -> tag.add(holder.getKey()));
    }

    private static void addItems(TagAppender<Item> tag, List<? extends DeferredHolder<Block, ? extends Block>> holders) {
        holders.forEach(holder -> tag.add(ResourceKey.create(Registries.ITEM, holder.getId())));
    }

    private static TagKey<Block> blockTag(String namespace, String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(namespace, path));
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, path));
    }
}
