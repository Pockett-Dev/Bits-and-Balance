package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;

import java.util.concurrent.CompletableFuture;

/**
 * Tweaks: Fuel Items Data Provider
 * Registers torches as fuel for furnaces using NeoForge data maps.
 * - Torch: Configurable burn time (default 400 ticks = 20 seconds)
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class FuelTweaks {

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Server event) {
        event.getGenerator().addProvider(true, new FuelDataProvider(event.getGenerator().getPackOutput(), event.getLookupProvider()));
    }

    private static class FuelDataProvider extends DataMapProvider {
        
        protected FuelDataProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(packOutput, lookupProvider);
        }

        @Override
        public String getName() {
            return "Fuel Tweaks Data Provider";
        }

        @Override
        protected void gather(HolderLookup.Provider provider) {
            var builder = this.builder(NeoForgeDataMaps.FURNACE_FUELS);
            
            // During data generation, config may not be loaded, so we use default values
            // The actual enabling/disabling is handled at runtime through the config system
            
            // Add Torch as fuel (default burn time 400 ticks = 20 seconds)
            // This will be controlled by the config at runtime
            builder.add(BuiltInRegistries.ITEM.getResourceKey(Items.TORCH).orElseThrow(), 
                new FurnaceFuel(400), false);
        }
    }
}
