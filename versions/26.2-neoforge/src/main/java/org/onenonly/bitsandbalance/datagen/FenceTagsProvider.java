package org.onenonly.bitsandbalance.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagCopyingItemTagProvider;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.ModBlocks;
import org.onenonly.bitsandbalance.ModItems;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 26.2 tag providers use registry keys rather than live block/item instances.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class FenceTagsProvider {
    private FenceTagsProvider() {
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Server event) {
        PackOutput output = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        BlockTagsProvider blockTags = new BlockTagsProvider(output, lookup, BitsAndBalance.MODID) {
            @Override
            protected void addTags(HolderLookup.Provider provider) {
                add(tag(BlockTags.MINEABLE_WITH_AXE), List.of(
                        ModBlocks.AZALEA_LOG, ModBlocks.AZALEA_WOOD,
                        ModBlocks.STRIPPED_AZALEA_LOG, ModBlocks.STRIPPED_AZALEA_WOOD,
                        ModBlocks.AZALEA_PLANKS, ModBlocks.AZALEA_SLAB, ModBlocks.AZALEA_STAIRS,
                        ModBlocks.AZALEA_BUTTON, ModBlocks.AZALEA_PRESSURE_PLATE,
                        ModBlocks.AZALEA_FENCE, ModBlocks.AZALEA_FENCE_GATE,
                        ModBlocks.AZALEA_DOOR, ModBlocks.AZALEA_TRAPDOOR,
                        ModBlocks.AZALEA_SIGN, ModBlocks.AZALEA_WALL_SIGN
                ));

                List<DeferredHolder<Block, ? extends Block>> logs = List.of(
                        ModBlocks.AZALEA_LOG, ModBlocks.AZALEA_WOOD,
                        ModBlocks.STRIPPED_AZALEA_LOG, ModBlocks.STRIPPED_AZALEA_WOOD
                );
                add(tag(BlockTags.LOGS), logs);
                add(tag(BlockItemTags.LOGS_THAT_BURN.block()), logs);
                add(tag(BlockTags.PLANKS), ModBlocks.AZALEA_PLANKS);
                add(tag(BlockTags.WOODEN_SLABS), ModBlocks.AZALEA_SLAB);
                add(tag(BlockTags.WOODEN_STAIRS), ModBlocks.AZALEA_STAIRS);
                add(tag(BlockTags.WOODEN_DOORS), ModBlocks.AZALEA_DOOR);
                add(tag(BlockTags.WOODEN_TRAPDOORS), ModBlocks.AZALEA_TRAPDOOR);
                add(tag(BlockTags.BUTTONS), ModBlocks.AZALEA_BUTTON);
                add(tag(BlockTags.WOODEN_BUTTONS), ModBlocks.AZALEA_BUTTON);
                add(tag(BlockTags.PRESSURE_PLATES), ModBlocks.AZALEA_PRESSURE_PLATE);
                add(tag(BlockTags.WOODEN_PRESSURE_PLATES), ModBlocks.AZALEA_PRESSURE_PLATE);
                add(tag(BlockTags.FENCES), ModBlocks.AZALEA_FENCE);
                add(tag(BlockTags.WOODEN_FENCES), ModBlocks.AZALEA_FENCE);
                add(tag(BlockTags.FENCE_GATES), ModBlocks.AZALEA_FENCE_GATE);
                add(tag(BlockTags.STANDING_SIGNS), ModBlocks.AZALEA_SIGN);
                add(tag(BlockTags.WALL_SIGNS), ModBlocks.AZALEA_WALL_SIGN);
                add(tag(BlockTags.CEILING_HANGING_SIGNS), ModBlocks.AZALEA_HANGING_SIGN);
                add(tag(BlockTags.WALL_HANGING_SIGNS), ModBlocks.AZALEA_WALL_HANGING_SIGN);
            }
        };

        BlockTagCopyingItemTagProvider itemTags = new BlockTagCopyingItemTagProvider(
                output, lookup, blockTags.contentsGetter(), BitsAndBalance.MODID
        ) {
            @Override
            protected void addTags(HolderLookup.Provider provider) {
                copy(BlockTags.LOGS, ItemTags.LOGS);
                copy(BlockItemTags.LOGS_THAT_BURN.block(), BlockItemTags.LOGS_THAT_BURN.item());
                copy(BlockTags.PLANKS, ItemTags.PLANKS);
                copy(BlockTags.WOODEN_SLABS, ItemTags.WOODEN_SLABS);
                copy(BlockTags.WOODEN_STAIRS, ItemTags.WOODEN_STAIRS);
                copy(BlockTags.WOODEN_DOORS, ItemTags.WOODEN_DOORS);
                copy(BlockTags.WOODEN_TRAPDOORS, ItemTags.WOODEN_TRAPDOORS);
                copy(BlockTags.BUTTONS, BlockItemTags.BUTTONS.item());
                copy(BlockTags.WOODEN_BUTTONS, ItemTags.WOODEN_BUTTONS);
                copy(BlockTags.WOODEN_PRESSURE_PLATES, ItemTags.WOODEN_PRESSURE_PLATES);
                copy(BlockTags.FENCES, BlockItemTags.FENCES.item());
                copy(BlockTags.WOODEN_FENCES, ItemTags.WOODEN_FENCES);
                copy(BlockTags.FENCE_GATES, ItemTags.FENCE_GATES);
                addItem(tag(ItemTags.SIGNS), ModItems.AZALEA_SIGN);
                addItem(tag(ItemTags.HANGING_SIGNS), ModItems.AZALEA_HANGING_SIGN);
                addItem(tag(ItemTags.BOATS), ModItems.AZALEA_BOAT);
                addItem(tag(ItemTags.CHEST_BOATS), ModItems.AZALEA_CHEST_BOAT);
            }
        };

        event.addProvider(blockTags);
        event.addProvider(itemTags);
    }

    private static void add(TagAppender<Block> tag, DeferredHolder<Block, ? extends Block> holder) {
        tag.add(holder.getKey());
    }

    private static void add(TagAppender<Block> tag, List<? extends DeferredHolder<Block, ? extends Block>> holders) {
        holders.forEach(holder -> tag.add(holder.getKey()));
    }

    private static void addItem(TagAppender<Item> tag, DeferredHolder<Item, ? extends Item> holder) {
        tag.add(holder.getKey());
    }
}
