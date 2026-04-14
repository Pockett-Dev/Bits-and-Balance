package org.onenonly.bitsandbalance.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagCopyingItemTagProvider;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.ModBlocks;
import org.onenonly.bitsandbalance.ModItems;
import java.util.concurrent.CompletableFuture;

/**
 * Data Provider for Azalea Wood Tags
 * Generates all tags for Azalea Wood blocks using datagen.
 * This replaces static JSON files with dynamic generation.
 * 
 * This approach uses the same pattern as the existing CompostableItems and FuelTweaks
 * classes, but for tag generation instead of data maps.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class FenceTagsProvider {

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Server event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Register block tags provider
        BlockTagsProvider blockTagsProvider = new BlockTagsProvider(packOutput, lookupProvider, BitsAndBalance.MODID) {
            @Override
            protected void addTags(HolderLookup.Provider provider) {
                // Add all Azalea Wood blocks to axe mineable tag
                var axeMineable = tag(BlockTags.MINEABLE_WITH_AXE)
                    .add(ModBlocks.AZALEA_LOG.get())
                    .add(ModBlocks.AZALEA_WOOD.get())
                    .add(ModBlocks.STRIPPED_AZALEA_LOG.get())
                    .add(ModBlocks.STRIPPED_AZALEA_WOOD.get())
                    .add(ModBlocks.AZALEA_PLANKS.get())
                    .add(ModBlocks.AZALEA_SLAB.get())
                    .add(ModBlocks.AZALEA_STAIRS.get())
                    .add(ModBlocks.AZALEA_BUTTON.get())
                    .add(ModBlocks.AZALEA_PRESSURE_PLATE.get())
                    .add(ModBlocks.AZALEA_FENCE.get())
                    .add(ModBlocks.AZALEA_FENCE_GATE.get())
                    .add(ModBlocks.AZALEA_DOOR.get())
                    .add(ModBlocks.AZALEA_TRAPDOOR.get())
                    .add(ModBlocks.AZALEA_SIGN.get())
                    .add(ModBlocks.AZALEA_WALL_SIGN.get());

                // Add Azalea Logs to logs tag
                var logs = tag(BlockTags.LOGS)
                    .add(ModBlocks.AZALEA_LOG.get())
                    .add(ModBlocks.AZALEA_WOOD.get())
                    .add(ModBlocks.STRIPPED_AZALEA_LOG.get())
                    .add(ModBlocks.STRIPPED_AZALEA_WOOD.get());

                var burningLogs = tag(BlockTags.LOGS_THAT_BURN)
                    .add(ModBlocks.AZALEA_LOG.get())
                    .add(ModBlocks.AZALEA_WOOD.get())
                    .add(ModBlocks.STRIPPED_AZALEA_LOG.get())
                    .add(ModBlocks.STRIPPED_AZALEA_WOOD.get());

                // Add Azalea Planks to planks tag
                tag(BlockTags.PLANKS)
                    .add(ModBlocks.AZALEA_PLANKS.get());

                // Add Azalea Wood derivatives to their respective tags
                tag(BlockTags.WOODEN_SLABS)
                    .add(ModBlocks.AZALEA_SLAB.get());

                tag(BlockTags.WOODEN_STAIRS)
                    .add(ModBlocks.AZALEA_STAIRS.get());

                tag(BlockTags.WOODEN_DOORS)
                    .add(ModBlocks.AZALEA_DOOR.get());

                tag(BlockTags.WOODEN_TRAPDOORS)
                    .add(ModBlocks.AZALEA_TRAPDOOR.get());

                tag(BlockTags.BUTTONS)
                    .add(ModBlocks.AZALEA_BUTTON.get());

                tag(BlockTags.WOODEN_BUTTONS)
                    .add(ModBlocks.AZALEA_BUTTON.get());

                tag(BlockTags.PRESSURE_PLATES)
                    .add(ModBlocks.AZALEA_PRESSURE_PLATE.get());

                tag(BlockTags.WOODEN_PRESSURE_PLATES)
                    .add(ModBlocks.AZALEA_PRESSURE_PLATE.get());

                // Add Azalea Fence to the main fences tag
                tag(BlockTags.FENCES)
                    .add(ModBlocks.AZALEA_FENCE.get());

                // Add Azalea Fence to wooden fences tag
                tag(BlockTags.WOODEN_FENCES)
                    .add(ModBlocks.AZALEA_FENCE.get());

                // Add Azalea Fence Gate to fence gates tag
                tag(BlockTags.FENCE_GATES)
                    .add(ModBlocks.AZALEA_FENCE_GATE.get());

                // WOODEN_FENCE_GATES tag doesn't exist in 1.21.1

                tag(BlockTags.STANDING_SIGNS)
                    .add(ModBlocks.AZALEA_SIGN.get());

                tag(BlockTags.WALL_SIGNS)
                    .add(ModBlocks.AZALEA_WALL_SIGN.get());

                tag(BlockTags.CEILING_HANGING_SIGNS)
                    .add(ModBlocks.AZALEA_HANGING_SIGN.get());

                tag(BlockTags.WALL_HANGING_SIGNS)
                    .add(ModBlocks.AZALEA_WALL_HANGING_SIGN.get());
            }

        };

        BlockTagCopyingItemTagProvider itemTagsProvider = new BlockTagCopyingItemTagProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), BitsAndBalance.MODID) {
            @Override
            protected void addTags(HolderLookup.Provider provider) {
                copy(BlockTags.LOGS, ItemTags.LOGS);
                copy(BlockTags.LOGS_THAT_BURN, ItemTags.LOGS_THAT_BURN);
                copy(BlockTags.PLANKS, ItemTags.PLANKS);
                copy(BlockTags.WOODEN_SLABS, ItemTags.WOODEN_SLABS);
                copy(BlockTags.WOODEN_STAIRS, ItemTags.WOODEN_STAIRS);
                copy(BlockTags.WOODEN_DOORS, ItemTags.WOODEN_DOORS);
                copy(BlockTags.WOODEN_TRAPDOORS, ItemTags.WOODEN_TRAPDOORS);
                copy(BlockTags.BUTTONS, ItemTags.BUTTONS);
                copy(BlockTags.WOODEN_BUTTONS, ItemTags.WOODEN_BUTTONS);
                // PRESSURE_PLATES item tag doesn't exist in 1.21.1
                copy(BlockTags.WOODEN_PRESSURE_PLATES, ItemTags.WOODEN_PRESSURE_PLATES);
                copy(BlockTags.FENCES, ItemTags.FENCES);
                copy(BlockTags.WOODEN_FENCES, ItemTags.WOODEN_FENCES);
                copy(BlockTags.FENCE_GATES, ItemTags.FENCE_GATES);
                // WOODEN_FENCE_GATES tag doesn't exist in 1.21.1

                tag(ItemTags.SIGNS)
                    .add(ModItems.AZALEA_SIGN.get());

                tag(ItemTags.HANGING_SIGNS)
                    .add(ModItems.AZALEA_HANGING_SIGN.get());

                tag(ItemTags.BOATS)
                    .add(ModItems.AZALEA_BOAT.get());

                tag(ItemTags.CHEST_BOATS)
                    .add(ModItems.AZALEA_CHEST_BOAT.get());
            }
        };

        event.addProvider(blockTagsProvider);
        event.addProvider(itemTagsProvider);

    }
}
