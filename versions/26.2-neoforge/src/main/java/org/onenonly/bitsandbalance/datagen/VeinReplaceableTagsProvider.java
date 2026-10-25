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
 * 26.2 registry-key based datagen for vein-replaceable blocks.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class VeinReplaceableTagsProvider {
    private static final TagKey<Block> VEIN_REPLACEABLE_NETHER = TagKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "vein_replaceable_nether"));
    private static final TagKey<Block> VEIN_REPLACEABLE_OVERWORLD = TagKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "vein_replaceable_overworld"));

    private VeinReplaceableTagsProvider() {
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Server event) {
        PackOutput output = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        BlockTagsProvider provider = new BlockTagsProvider(output, lookup, BitsAndBalance.MODID) {
            @Override
            public String getName() {
                return "Vein Replaceable Tags for " + BitsAndBalance.MODID;
            }

            @Override
            protected void addTags(HolderLookup.Provider registries) {
                tag(VEIN_REPLACEABLE_NETHER)
                        .add(Blocks.NETHERRACK.builtInRegistryHolder().key())
                        .add(Blocks.BLACKSTONE.builtInRegistryHolder().key());
                tag(VEIN_REPLACEABLE_OVERWORLD)
                        .add(Blocks.STONE.builtInRegistryHolder().key())
                        .add(Blocks.DEEPSLATE.builtInRegistryHolder().key())
                        .add(Blocks.ANDESITE.builtInRegistryHolder().key())
                        .add(Blocks.DIORITE.builtInRegistryHolder().key())
                        .add(Blocks.GRANITE.builtInRegistryHolder().key())
                        .add(Blocks.TUFF.builtInRegistryHolder().key());
            }
        };
        event.addProvider(provider);
    }
}
