package org.onenonly.bitsandbalance.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;

import java.util.concurrent.CompletableFuture;

/**
 * Data Provider for Vein Replaceable Block Tags
 * Generates tags that define which blocks can be replaced by veins.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class VeinReplaceableTagsProvider {

    // Vein replaceable tags
    private static final TagKey<Block> VEIN_REPLACEABLE_NETHER = TagKey.create(
        Registries.BLOCK, 
        Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "vein_replaceable_nether")
    );
    
    private static final TagKey<Block> VEIN_REPLACEABLE_OVERWORLD = TagKey.create(
        Registries.BLOCK, 
        Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "vein_replaceable_overworld")
    );

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Server event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        BlockTagsProvider provider = new BlockTagsProvider(packOutput, lookupProvider, BitsAndBalance.MODID) {
            @Override
            public String getName() {
                return "Vein Replaceable Tags for " + BitsAndBalance.MODID;
            }
            
            @Override
            protected void addTags(HolderLookup.Provider provider) {
                addVeinReplaceableTags();
            }

            private void addVeinReplaceableTags() {
                // Nether replaceable blocks
                tag(VEIN_REPLACEABLE_NETHER)
                    .add(Blocks.NETHERRACK)
                    .add(Blocks.BLACKSTONE);

                // Overworld replaceable blocks
                tag(VEIN_REPLACEABLE_OVERWORLD)
                    .add(Blocks.STONE)
                    .add(Blocks.DEEPSLATE)
                    .add(Blocks.ANDESITE)
                    .add(Blocks.DIORITE)
                    .add(Blocks.GRANITE)
                    .add(Blocks.TUFF);
            }
        };

        event.getGenerator().addProvider(true, provider);
    }
}
