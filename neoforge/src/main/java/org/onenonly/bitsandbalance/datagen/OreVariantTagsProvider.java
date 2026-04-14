package org.onenonly.bitsandbalance.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagCopyingItemTagProvider;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.blocks.OreVariants;

import java.util.concurrent.CompletableFuture;

/**
 * Data Provider for Ore Variant Tags
 * Generates all tags for ore variant blocks using datagen.
 * This replaces static JSON files with dynamic generation.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class OreVariantTagsProvider {

    // Minecraft ore tags (blocks)
    private static final TagKey<Block> COAL_ORES_BLOCK = BlockTags.COAL_ORES;
    private static final TagKey<Block> IRON_ORES_BLOCK = BlockTags.IRON_ORES;
    private static final TagKey<Block> COPPER_ORES_BLOCK = BlockTags.COPPER_ORES;
    private static final TagKey<Block> GOLD_ORES_BLOCK = BlockTags.GOLD_ORES;
    private static final TagKey<Block> DIAMOND_ORES_BLOCK = BlockTags.DIAMOND_ORES;
    private static final TagKey<Block> EMERALD_ORES_BLOCK = BlockTags.EMERALD_ORES;
    private static final TagKey<Block> LAPIS_ORES_BLOCK = BlockTags.LAPIS_ORES;
    private static final TagKey<Block> REDSTONE_ORES_BLOCK = BlockTags.REDSTONE_ORES;
    private static final TagKey<Block> OVERWORLD_CARVER_REPLACEABLES_BLOCK = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("minecraft", "overworld_carver_replaceables"));

    // Minecraft ore tags (items)
    private static final TagKey<Item> COAL_ORES_ITEM = ItemTags.COAL_ORES;
    private static final TagKey<Item> IRON_ORES_ITEM = ItemTags.IRON_ORES;
    private static final TagKey<Item> COPPER_ORES_ITEM = ItemTags.COPPER_ORES;
    private static final TagKey<Item> GOLD_ORES_ITEM = ItemTags.GOLD_ORES;
    private static final TagKey<Item> DIAMOND_ORES_ITEM = ItemTags.DIAMOND_ORES;
    private static final TagKey<Item> EMERALD_ORES_ITEM = ItemTags.EMERALD_ORES;
    private static final TagKey<Item> LAPIS_ORES_ITEM = ItemTags.LAPIS_ORES;
    private static final TagKey<Item> REDSTONE_ORES_ITEM = ItemTags.REDSTONE_ORES;

    // NeoForge ore tags (blocks) - c: namespace
    private static final TagKey<Block> C_ORES_BLOCK = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));
    private static final TagKey<Block> C_COAL_ORES_BLOCK = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/coal"));
    private static final TagKey<Block> C_IRON_ORES_BLOCK = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/iron"));
    private static final TagKey<Block> C_COPPER_ORES_BLOCK = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/copper"));
    private static final TagKey<Block> C_GOLD_ORES_BLOCK = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/gold"));
    private static final TagKey<Block> C_DIAMOND_ORES_BLOCK = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/diamond"));
    private static final TagKey<Block> C_EMERALD_ORES_BLOCK = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/emerald"));
    private static final TagKey<Block> C_LAPIS_ORES_BLOCK = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/lapis"));
    private static final TagKey<Block> C_REDSTONE_ORES_BLOCK = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/redstone"));
    private static final TagKey<Block> C_ZINC_ORES_BLOCK = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/zinc"));

    // NeoForge ore tags (items) - c: namespace
    private static final TagKey<Item> C_ORES_ITEM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores"));
    private static final TagKey<Item> C_COAL_ORES_ITEM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores/coal"));
    private static final TagKey<Item> C_IRON_ORES_ITEM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores/iron"));
    private static final TagKey<Item> C_COPPER_ORES_ITEM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores/copper"));
    private static final TagKey<Item> C_GOLD_ORES_ITEM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores/gold"));
    private static final TagKey<Item> C_DIAMOND_ORES_ITEM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores/diamond"));
    private static final TagKey<Item> C_EMERALD_ORES_ITEM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores/emerald"));
    private static final TagKey<Item> C_LAPIS_ORES_ITEM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores/lapis"));
    private static final TagKey<Item> C_REDSTONE_ORES_ITEM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores/redstone"));
    private static final TagKey<Item> C_ZINC_ORES_ITEM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores/zinc"));

    // Create mod zinc ore tags
    private static final TagKey<Block> CREATE_ZINC_ORES_BLOCK = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("create", "zinc_ores"));
    private static final TagKey<Item> CREATE_ZINC_ORES_ITEM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("create", "zinc_ores"));

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Server event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Register ore variant tags provider with custom name
        BlockTagsProvider oreVariantTagsProvider = new BlockTagsProvider(packOutput, lookupProvider, BitsAndBalance.MODID) {
            @Override
            public String getName() {
                return "Ore Variant Tags for " + BitsAndBalance.MODID;
            }
            
            @Override
            protected void addTags(HolderLookup.Provider provider) {
                addOreVariantTags();
            }

            private void addOreVariantTags() {
                // Add all ore variants to pickaxe mineable tag
                tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    // Coal ore variants
                    .add(OreVariants.ANDESITE_COAL_ORE.get())
                    .add(OreVariants.DIORITE_COAL_ORE.get())
                    .add(OreVariants.GRANITE_COAL_ORE.get())
                    .add(OreVariants.TUFF_COAL_ORE.get())
                    // Iron ore variants
                    .add(OreVariants.ANDESITE_IRON_ORE.get())
                    .add(OreVariants.DIORITE_IRON_ORE.get())
                    .add(OreVariants.GRANITE_IRON_ORE.get())
                    .add(OreVariants.TUFF_IRON_ORE.get())
                    // Copper ore variants
                    .add(OreVariants.ANDESITE_COPPER_ORE.get())
                    .add(OreVariants.DIORITE_COPPER_ORE.get())
                    .add(OreVariants.GRANITE_COPPER_ORE.get())
                    .add(OreVariants.TUFF_COPPER_ORE.get())
                    // Gold ore variants
                    .add(OreVariants.ANDESITE_GOLD_ORE.get())
                    .add(OreVariants.DIORITE_GOLD_ORE.get())
                    .add(OreVariants.GRANITE_GOLD_ORE.get())
                    .add(OreVariants.TUFF_GOLD_ORE.get())
                    // Diamond ore variants
                    .add(OreVariants.ANDESITE_DIAMOND_ORE.get())
                    .add(OreVariants.DIORITE_DIAMOND_ORE.get())
                    .add(OreVariants.GRANITE_DIAMOND_ORE.get())
                    .add(OreVariants.TUFF_DIAMOND_ORE.get())
                    // Emerald ore variants
                    .add(OreVariants.ANDESITE_EMERALD_ORE.get())
                    .add(OreVariants.DIORITE_EMERALD_ORE.get())
                    .add(OreVariants.GRANITE_EMERALD_ORE.get())
                    .add(OreVariants.TUFF_EMERALD_ORE.get())
                    // Lapis ore variants
                    .add(OreVariants.ANDESITE_LAPIS_ORE.get())
                    .add(OreVariants.DIORITE_LAPIS_ORE.get())
                    .add(OreVariants.GRANITE_LAPIS_ORE.get())
                    .add(OreVariants.TUFF_LAPIS_ORE.get())
                    // Redstone ore variants
                    .add(OreVariants.ANDESITE_REDSTONE_ORE.get())
                    .add(OreVariants.DIORITE_REDSTONE_ORE.get())
                    .add(OreVariants.GRANITE_REDSTONE_ORE.get())
                    .add(OreVariants.TUFF_REDSTONE_ORE.get())
                    // Zinc ore variants
                    .add(OreVariants.ANDESITE_ZINC_ORE.get())
                    .add(OreVariants.DIORITE_ZINC_ORE.get())
                    .add(OreVariants.GRANITE_ZINC_ORE.get())
                    .add(OreVariants.TUFF_ZINC_ORE.get());

                // Add ore variants to stone tool tier (coal, iron, copper, lapis)
                tag(BlockTags.NEEDS_STONE_TOOL)
                    // Coal ore variants
                    .add(OreVariants.ANDESITE_COAL_ORE.get())
                    .add(OreVariants.DIORITE_COAL_ORE.get())
                    .add(OreVariants.GRANITE_COAL_ORE.get())
                    .add(OreVariants.TUFF_COAL_ORE.get())
                    // Iron ore variants
                    .add(OreVariants.ANDESITE_IRON_ORE.get())
                    .add(OreVariants.DIORITE_IRON_ORE.get())
                    .add(OreVariants.GRANITE_IRON_ORE.get())
                    .add(OreVariants.TUFF_IRON_ORE.get())
                    // Copper ore variants
                    .add(OreVariants.ANDESITE_COPPER_ORE.get())
                    .add(OreVariants.DIORITE_COPPER_ORE.get())
                    .add(OreVariants.GRANITE_COPPER_ORE.get())
                    .add(OreVariants.TUFF_COPPER_ORE.get())
                    // Lapis ore variants
                    .add(OreVariants.ANDESITE_LAPIS_ORE.get())
                    .add(OreVariants.DIORITE_LAPIS_ORE.get())
                    .add(OreVariants.GRANITE_LAPIS_ORE.get())
                    .add(OreVariants.TUFF_LAPIS_ORE.get());

                // Add ore variants to iron tool tier (gold, diamond, emerald, redstone, zinc)
                tag(BlockTags.NEEDS_IRON_TOOL)
                    // Gold ore variants
                    .add(OreVariants.ANDESITE_GOLD_ORE.get())
                    .add(OreVariants.DIORITE_GOLD_ORE.get())
                    .add(OreVariants.GRANITE_GOLD_ORE.get())
                    .add(OreVariants.TUFF_GOLD_ORE.get())
                    // Diamond ore variants
                    .add(OreVariants.ANDESITE_DIAMOND_ORE.get())
                    .add(OreVariants.DIORITE_DIAMOND_ORE.get())
                    .add(OreVariants.GRANITE_DIAMOND_ORE.get())
                    .add(OreVariants.TUFF_DIAMOND_ORE.get())
                    // Emerald ore variants
                    .add(OreVariants.ANDESITE_EMERALD_ORE.get())
                    .add(OreVariants.DIORITE_EMERALD_ORE.get())
                    .add(OreVariants.GRANITE_EMERALD_ORE.get())
                    .add(OreVariants.TUFF_EMERALD_ORE.get())
                    // Redstone ore variants
                    .add(OreVariants.ANDESITE_REDSTONE_ORE.get())
                    .add(OreVariants.DIORITE_REDSTONE_ORE.get())
                    .add(OreVariants.GRANITE_REDSTONE_ORE.get())
                    .add(OreVariants.TUFF_REDSTONE_ORE.get())
                    // Zinc ore variants
                    .add(OreVariants.ANDESITE_ZINC_ORE.get())
                    .add(OreVariants.DIORITE_ZINC_ORE.get())
                    .add(OreVariants.GRANITE_ZINC_ORE.get())
                    .add(OreVariants.TUFF_ZINC_ORE.get());

                // Minecraft ore tags (blocks)
                tag(COAL_ORES_BLOCK).add(OreVariants.ANDESITE_COAL_ORE.get(), OreVariants.DIORITE_COAL_ORE.get(), OreVariants.GRANITE_COAL_ORE.get(), OreVariants.TUFF_COAL_ORE.get());
                tag(IRON_ORES_BLOCK).add(OreVariants.ANDESITE_IRON_ORE.get(), OreVariants.DIORITE_IRON_ORE.get(), OreVariants.GRANITE_IRON_ORE.get(), OreVariants.TUFF_IRON_ORE.get());
                tag(COPPER_ORES_BLOCK).add(OreVariants.ANDESITE_COPPER_ORE.get(), OreVariants.DIORITE_COPPER_ORE.get(), OreVariants.GRANITE_COPPER_ORE.get(), OreVariants.TUFF_COPPER_ORE.get());
                tag(GOLD_ORES_BLOCK).add(OreVariants.ANDESITE_GOLD_ORE.get(), OreVariants.DIORITE_GOLD_ORE.get(), OreVariants.GRANITE_GOLD_ORE.get(), OreVariants.TUFF_GOLD_ORE.get());
                tag(DIAMOND_ORES_BLOCK).add(OreVariants.ANDESITE_DIAMOND_ORE.get(), OreVariants.DIORITE_DIAMOND_ORE.get(), OreVariants.GRANITE_DIAMOND_ORE.get(), OreVariants.TUFF_DIAMOND_ORE.get());
                tag(EMERALD_ORES_BLOCK).add(OreVariants.ANDESITE_EMERALD_ORE.get(), OreVariants.DIORITE_EMERALD_ORE.get(), OreVariants.GRANITE_EMERALD_ORE.get(), OreVariants.TUFF_EMERALD_ORE.get());
                tag(LAPIS_ORES_BLOCK).add(OreVariants.ANDESITE_LAPIS_ORE.get(), OreVariants.DIORITE_LAPIS_ORE.get(), OreVariants.GRANITE_LAPIS_ORE.get(), OreVariants.TUFF_LAPIS_ORE.get());
                tag(REDSTONE_ORES_BLOCK).add(OreVariants.ANDESITE_REDSTONE_ORE.get(), OreVariants.DIORITE_REDSTONE_ORE.get(), OreVariants.GRANITE_REDSTONE_ORE.get(), OreVariants.TUFF_REDSTONE_ORE.get());

                tag(OVERWORLD_CARVER_REPLACEABLES_BLOCK)
                    .add(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.COAL_BLOCK)
                    .add(OreVariants.ANDESITE_COAL_ORE.get(), OreVariants.DIORITE_COAL_ORE.get(), OreVariants.GRANITE_COAL_ORE.get(), OreVariants.TUFF_COAL_ORE.get())
                    .add(OreVariants.ANDESITE_IRON_ORE.get(), OreVariants.DIORITE_IRON_ORE.get(), OreVariants.GRANITE_IRON_ORE.get(), OreVariants.TUFF_IRON_ORE.get())
                    .add(OreVariants.ANDESITE_COPPER_ORE.get(), OreVariants.DIORITE_COPPER_ORE.get(), OreVariants.GRANITE_COPPER_ORE.get(), OreVariants.TUFF_COPPER_ORE.get())
                    .add(OreVariants.ANDESITE_GOLD_ORE.get(), OreVariants.DIORITE_GOLD_ORE.get(), OreVariants.GRANITE_GOLD_ORE.get(), OreVariants.TUFF_GOLD_ORE.get())
                    .add(OreVariants.ANDESITE_DIAMOND_ORE.get(), OreVariants.DIORITE_DIAMOND_ORE.get(), OreVariants.GRANITE_DIAMOND_ORE.get(), OreVariants.TUFF_DIAMOND_ORE.get())
                    .add(OreVariants.ANDESITE_EMERALD_ORE.get(), OreVariants.DIORITE_EMERALD_ORE.get(), OreVariants.GRANITE_EMERALD_ORE.get(), OreVariants.TUFF_EMERALD_ORE.get())
                    .add(OreVariants.ANDESITE_LAPIS_ORE.get(), OreVariants.DIORITE_LAPIS_ORE.get(), OreVariants.GRANITE_LAPIS_ORE.get(), OreVariants.TUFF_LAPIS_ORE.get())
                    .add(OreVariants.ANDESITE_REDSTONE_ORE.get(), OreVariants.DIORITE_REDSTONE_ORE.get(), OreVariants.GRANITE_REDSTONE_ORE.get(), OreVariants.TUFF_REDSTONE_ORE.get())
                    .add(OreVariants.ANDESITE_ZINC_ORE.get(), OreVariants.DIORITE_ZINC_ORE.get(), OreVariants.GRANITE_ZINC_ORE.get(), OreVariants.TUFF_ZINC_ORE.get());

                // NeoForge ore tags (blocks) - c: namespace
                tag(C_ORES_BLOCK)
                    .add(OreVariants.ANDESITE_COAL_ORE.get(), OreVariants.DIORITE_COAL_ORE.get(), OreVariants.GRANITE_COAL_ORE.get(), OreVariants.TUFF_COAL_ORE.get())
                    .add(OreVariants.ANDESITE_IRON_ORE.get(), OreVariants.DIORITE_IRON_ORE.get(), OreVariants.GRANITE_IRON_ORE.get(), OreVariants.TUFF_IRON_ORE.get())
                    .add(OreVariants.ANDESITE_COPPER_ORE.get(), OreVariants.DIORITE_COPPER_ORE.get(), OreVariants.GRANITE_COPPER_ORE.get(), OreVariants.TUFF_COPPER_ORE.get())
                    .add(OreVariants.ANDESITE_GOLD_ORE.get(), OreVariants.DIORITE_GOLD_ORE.get(), OreVariants.GRANITE_GOLD_ORE.get(), OreVariants.TUFF_GOLD_ORE.get())
                    .add(OreVariants.ANDESITE_DIAMOND_ORE.get(), OreVariants.DIORITE_DIAMOND_ORE.get(), OreVariants.GRANITE_DIAMOND_ORE.get(), OreVariants.TUFF_DIAMOND_ORE.get())
                    .add(OreVariants.ANDESITE_EMERALD_ORE.get(), OreVariants.DIORITE_EMERALD_ORE.get(), OreVariants.GRANITE_EMERALD_ORE.get(), OreVariants.TUFF_EMERALD_ORE.get())
                    .add(OreVariants.ANDESITE_LAPIS_ORE.get(), OreVariants.DIORITE_LAPIS_ORE.get(), OreVariants.GRANITE_LAPIS_ORE.get(), OreVariants.TUFF_LAPIS_ORE.get())
                    .add(OreVariants.ANDESITE_REDSTONE_ORE.get(), OreVariants.DIORITE_REDSTONE_ORE.get(), OreVariants.GRANITE_REDSTONE_ORE.get(), OreVariants.TUFF_REDSTONE_ORE.get())
                    .add(OreVariants.ANDESITE_ZINC_ORE.get(), OreVariants.DIORITE_ZINC_ORE.get(), OreVariants.GRANITE_ZINC_ORE.get(), OreVariants.TUFF_ZINC_ORE.get());

                tag(C_COAL_ORES_BLOCK).add(OreVariants.ANDESITE_COAL_ORE.get(), OreVariants.DIORITE_COAL_ORE.get(), OreVariants.GRANITE_COAL_ORE.get(), OreVariants.TUFF_COAL_ORE.get());
                tag(C_IRON_ORES_BLOCK).add(OreVariants.ANDESITE_IRON_ORE.get(), OreVariants.DIORITE_IRON_ORE.get(), OreVariants.GRANITE_IRON_ORE.get(), OreVariants.TUFF_IRON_ORE.get());
                tag(C_COPPER_ORES_BLOCK).add(OreVariants.ANDESITE_COPPER_ORE.get(), OreVariants.DIORITE_COPPER_ORE.get(), OreVariants.GRANITE_COPPER_ORE.get(), OreVariants.TUFF_COPPER_ORE.get());
                tag(C_GOLD_ORES_BLOCK).add(OreVariants.ANDESITE_GOLD_ORE.get(), OreVariants.DIORITE_GOLD_ORE.get(), OreVariants.GRANITE_GOLD_ORE.get(), OreVariants.TUFF_GOLD_ORE.get());
                tag(C_DIAMOND_ORES_BLOCK).add(OreVariants.ANDESITE_DIAMOND_ORE.get(), OreVariants.DIORITE_DIAMOND_ORE.get(), OreVariants.GRANITE_DIAMOND_ORE.get(), OreVariants.TUFF_DIAMOND_ORE.get());
                tag(C_EMERALD_ORES_BLOCK).add(OreVariants.ANDESITE_EMERALD_ORE.get(), OreVariants.DIORITE_EMERALD_ORE.get(), OreVariants.GRANITE_EMERALD_ORE.get(), OreVariants.TUFF_EMERALD_ORE.get());
                tag(C_LAPIS_ORES_BLOCK).add(OreVariants.ANDESITE_LAPIS_ORE.get(), OreVariants.DIORITE_LAPIS_ORE.get(), OreVariants.GRANITE_LAPIS_ORE.get(), OreVariants.TUFF_LAPIS_ORE.get());
                tag(C_REDSTONE_ORES_BLOCK).add(OreVariants.ANDESITE_REDSTONE_ORE.get(), OreVariants.DIORITE_REDSTONE_ORE.get(), OreVariants.GRANITE_REDSTONE_ORE.get(), OreVariants.TUFF_REDSTONE_ORE.get());
                tag(C_ZINC_ORES_BLOCK).add(OreVariants.ANDESITE_ZINC_ORE.get(), OreVariants.DIORITE_ZINC_ORE.get(), OreVariants.GRANITE_ZINC_ORE.get(), OreVariants.TUFF_ZINC_ORE.get());

                // Create mod zinc ore tag (blocks)
                tag(CREATE_ZINC_ORES_BLOCK).add(OreVariants.ANDESITE_ZINC_ORE.get(), OreVariants.DIORITE_ZINC_ORE.get(), OreVariants.GRANITE_ZINC_ORE.get(), OreVariants.TUFF_ZINC_ORE.get());
            }
        };

        // Register item tags provider
        BlockTagCopyingItemTagProvider oreVariantItemTagsProvider = new BlockTagCopyingItemTagProvider(packOutput, lookupProvider, oreVariantTagsProvider.contentsGetter(), BitsAndBalance.MODID) {
            @Override
            public String getName() {
                return "Ore Variant Item Tags for " + BitsAndBalance.MODID;
            }

            @Override
            protected void addTags(HolderLookup.Provider provider) {
                // Minecraft ore tags (items)
                tag(COAL_ORES_ITEM).add(OreVariants.ANDESITE_COAL_ORE.get().asItem(), OreVariants.DIORITE_COAL_ORE.get().asItem(), OreVariants.GRANITE_COAL_ORE.get().asItem(), OreVariants.TUFF_COAL_ORE.get().asItem());
                tag(IRON_ORES_ITEM).add(OreVariants.ANDESITE_IRON_ORE.get().asItem(), OreVariants.DIORITE_IRON_ORE.get().asItem(), OreVariants.GRANITE_IRON_ORE.get().asItem(), OreVariants.TUFF_IRON_ORE.get().asItem());
                tag(COPPER_ORES_ITEM).add(OreVariants.ANDESITE_COPPER_ORE.get().asItem(), OreVariants.DIORITE_COPPER_ORE.get().asItem(), OreVariants.GRANITE_COPPER_ORE.get().asItem(), OreVariants.TUFF_COPPER_ORE.get().asItem());
                tag(GOLD_ORES_ITEM).add(OreVariants.ANDESITE_GOLD_ORE.get().asItem(), OreVariants.DIORITE_GOLD_ORE.get().asItem(), OreVariants.GRANITE_GOLD_ORE.get().asItem(), OreVariants.TUFF_GOLD_ORE.get().asItem());
                tag(DIAMOND_ORES_ITEM).add(OreVariants.ANDESITE_DIAMOND_ORE.get().asItem(), OreVariants.DIORITE_DIAMOND_ORE.get().asItem(), OreVariants.GRANITE_DIAMOND_ORE.get().asItem(), OreVariants.TUFF_DIAMOND_ORE.get().asItem());
                tag(EMERALD_ORES_ITEM).add(OreVariants.ANDESITE_EMERALD_ORE.get().asItem(), OreVariants.DIORITE_EMERALD_ORE.get().asItem(), OreVariants.GRANITE_EMERALD_ORE.get().asItem(), OreVariants.TUFF_EMERALD_ORE.get().asItem());
                tag(LAPIS_ORES_ITEM).add(OreVariants.ANDESITE_LAPIS_ORE.get().asItem(), OreVariants.DIORITE_LAPIS_ORE.get().asItem(), OreVariants.GRANITE_LAPIS_ORE.get().asItem(), OreVariants.TUFF_LAPIS_ORE.get().asItem());
                tag(REDSTONE_ORES_ITEM).add(OreVariants.ANDESITE_REDSTONE_ORE.get().asItem(), OreVariants.DIORITE_REDSTONE_ORE.get().asItem(), OreVariants.GRANITE_REDSTONE_ORE.get().asItem(), OreVariants.TUFF_REDSTONE_ORE.get().asItem());

                // NeoForge ore tags (items) - c: namespace
                tag(C_ORES_ITEM)
                    .add(OreVariants.ANDESITE_COAL_ORE.get().asItem(), OreVariants.DIORITE_COAL_ORE.get().asItem(), OreVariants.GRANITE_COAL_ORE.get().asItem(), OreVariants.TUFF_COAL_ORE.get().asItem())
                    .add(OreVariants.ANDESITE_IRON_ORE.get().asItem(), OreVariants.DIORITE_IRON_ORE.get().asItem(), OreVariants.GRANITE_IRON_ORE.get().asItem(), OreVariants.TUFF_IRON_ORE.get().asItem())
                    .add(OreVariants.ANDESITE_COPPER_ORE.get().asItem(), OreVariants.DIORITE_COPPER_ORE.get().asItem(), OreVariants.GRANITE_COPPER_ORE.get().asItem(), OreVariants.TUFF_COPPER_ORE.get().asItem())
                    .add(OreVariants.ANDESITE_GOLD_ORE.get().asItem(), OreVariants.DIORITE_GOLD_ORE.get().asItem(), OreVariants.GRANITE_GOLD_ORE.get().asItem(), OreVariants.TUFF_GOLD_ORE.get().asItem())
                    .add(OreVariants.ANDESITE_DIAMOND_ORE.get().asItem(), OreVariants.DIORITE_DIAMOND_ORE.get().asItem(), OreVariants.GRANITE_DIAMOND_ORE.get().asItem(), OreVariants.TUFF_DIAMOND_ORE.get().asItem())
                    .add(OreVariants.ANDESITE_EMERALD_ORE.get().asItem(), OreVariants.DIORITE_EMERALD_ORE.get().asItem(), OreVariants.GRANITE_EMERALD_ORE.get().asItem(), OreVariants.TUFF_EMERALD_ORE.get().asItem())
                    .add(OreVariants.ANDESITE_LAPIS_ORE.get().asItem(), OreVariants.DIORITE_LAPIS_ORE.get().asItem(), OreVariants.GRANITE_LAPIS_ORE.get().asItem(), OreVariants.TUFF_LAPIS_ORE.get().asItem())
                    .add(OreVariants.ANDESITE_REDSTONE_ORE.get().asItem(), OreVariants.DIORITE_REDSTONE_ORE.get().asItem(), OreVariants.GRANITE_REDSTONE_ORE.get().asItem(), OreVariants.TUFF_REDSTONE_ORE.get().asItem())
                    .add(OreVariants.ANDESITE_ZINC_ORE.get().asItem(), OreVariants.DIORITE_ZINC_ORE.get().asItem(), OreVariants.GRANITE_ZINC_ORE.get().asItem(), OreVariants.TUFF_ZINC_ORE.get().asItem());

                tag(C_COAL_ORES_ITEM).add(OreVariants.ANDESITE_COAL_ORE.get().asItem(), OreVariants.DIORITE_COAL_ORE.get().asItem(), OreVariants.GRANITE_COAL_ORE.get().asItem(), OreVariants.TUFF_COAL_ORE.get().asItem());
                tag(C_IRON_ORES_ITEM).add(OreVariants.ANDESITE_IRON_ORE.get().asItem(), OreVariants.DIORITE_IRON_ORE.get().asItem(), OreVariants.GRANITE_IRON_ORE.get().asItem(), OreVariants.TUFF_IRON_ORE.get().asItem());
                tag(C_COPPER_ORES_ITEM).add(OreVariants.ANDESITE_COPPER_ORE.get().asItem(), OreVariants.DIORITE_COPPER_ORE.get().asItem(), OreVariants.GRANITE_COPPER_ORE.get().asItem(), OreVariants.TUFF_COPPER_ORE.get().asItem());
                tag(C_GOLD_ORES_ITEM).add(OreVariants.ANDESITE_GOLD_ORE.get().asItem(), OreVariants.DIORITE_GOLD_ORE.get().asItem(), OreVariants.GRANITE_GOLD_ORE.get().asItem(), OreVariants.TUFF_GOLD_ORE.get().asItem());
                tag(C_DIAMOND_ORES_ITEM).add(OreVariants.ANDESITE_DIAMOND_ORE.get().asItem(), OreVariants.DIORITE_DIAMOND_ORE.get().asItem(), OreVariants.GRANITE_DIAMOND_ORE.get().asItem(), OreVariants.TUFF_DIAMOND_ORE.get().asItem());
                tag(C_EMERALD_ORES_ITEM).add(OreVariants.ANDESITE_EMERALD_ORE.get().asItem(), OreVariants.DIORITE_EMERALD_ORE.get().asItem(), OreVariants.GRANITE_EMERALD_ORE.get().asItem(), OreVariants.TUFF_EMERALD_ORE.get().asItem());
                tag(C_LAPIS_ORES_ITEM).add(OreVariants.ANDESITE_LAPIS_ORE.get().asItem(), OreVariants.DIORITE_LAPIS_ORE.get().asItem(), OreVariants.GRANITE_LAPIS_ORE.get().asItem(), OreVariants.TUFF_LAPIS_ORE.get().asItem());
                tag(C_REDSTONE_ORES_ITEM).add(OreVariants.ANDESITE_REDSTONE_ORE.get().asItem(), OreVariants.DIORITE_REDSTONE_ORE.get().asItem(), OreVariants.GRANITE_REDSTONE_ORE.get().asItem(), OreVariants.TUFF_REDSTONE_ORE.get().asItem());
                tag(C_ZINC_ORES_ITEM).add(OreVariants.ANDESITE_ZINC_ORE.get().asItem(), OreVariants.DIORITE_ZINC_ORE.get().asItem(), OreVariants.GRANITE_ZINC_ORE.get().asItem(), OreVariants.TUFF_ZINC_ORE.get().asItem());

                // Create mod zinc ore tag (items)
                tag(CREATE_ZINC_ORES_ITEM).add(OreVariants.ANDESITE_ZINC_ORE.get().asItem(), OreVariants.DIORITE_ZINC_ORE.get().asItem(), OreVariants.GRANITE_ZINC_ORE.get().asItem(), OreVariants.TUFF_ZINC_ORE.get().asItem());
            }
        };

        event.addProvider(oreVariantTagsProvider);
        event.addProvider(oreVariantItemTagsProvider);
    }
}
