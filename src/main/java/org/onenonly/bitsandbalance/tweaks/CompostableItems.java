package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import org.onenonly.bitsandbalance.BitsAndBalance;
import java.util.concurrent.CompletableFuture;
/**
 * Tweaks: Compostable Items Data Provider
 * Registers Rotten Flesh and Poisonous Potatoes as compostable items using NeoForge data maps.
 * - Rotten Flesh: 30% chance to increase composter level
 * - Poisonous Potato: 65% chance to increase composter level
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class CompostableItems {

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Server event) {
        event.getGenerator().addProvider(true, new CompostableDataProvider(event.getGenerator().getPackOutput(), event.getLookupProvider()));
    }

    private static class CompostableDataProvider extends DataMapProvider {
        
        protected CompostableDataProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(packOutput, lookupProvider);
        }

        @Override
        protected void gather(HolderLookup.Provider provider) {
            var builder = this.builder(NeoForgeDataMaps.COMPOSTABLES);
            
            // During data generation, config may not be loaded, so we use default values
            // The actual enabling/disabling is handled at runtime through the config system
            
            // Add Rotten Flesh (30% chance to increase composter level)
            builder.add(BuiltInRegistries.ITEM.getResourceKey(Items.ROTTEN_FLESH).orElseThrow(), new Compostable(0.3f), false);
            
            // Add Poisonous Potato (65% chance to increase composter level)
            builder.add(BuiltInRegistries.ITEM.getResourceKey(Items.POISONOUS_POTATO).orElseThrow(), new Compostable(0.65f), false);
        }
    }
}
