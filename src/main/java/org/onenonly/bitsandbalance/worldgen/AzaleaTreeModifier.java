package org.onenonly.bitsandbalance.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.ModBlocks;

/**
 * Modifies the vanilla azalea tree configured feature at runtime based on config.
 * Inspired by Quark's approach to configurable worldgen features.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class AzaleaTreeModifier {
    
    @SubscribeEvent
    public static void onServerReload(AddServerReloadListenersEvent event) {
        // Get the vanilla azalea tree configured feature
        ConfiguredFeature<?, ?> azaleaFeature = event.getRegistryAccess()
            .lookup(Registries.CONFIGURED_FEATURE)
                .flatMap(reg -> reg.getOptional(TreeFeatures.AZALEA_TREE))
                .orElse(null);
        
        if (azaleaFeature == null || !(azaleaFeature.config() instanceof TreeConfiguration treeConfig)) {
            BitsAndBalance.LOGGER.warn("Could not find or modify azalea tree feature");
            return;
        }
        
        // Modify the trunk provider based on config using reflection
        try {
            java.lang.reflect.Field trunkProviderField = TreeConfiguration.class.getDeclaredField("trunkProvider");
            trunkProviderField.setAccessible(true);
            
            if (Config.enableAzaleaWoodGeneration) {
                trunkProviderField.set(treeConfig, BlockStateProvider.simple(ModBlocks.AZALEA_LOG.get()));
                BitsAndBalance.LOGGER.info("Azalea trees will use azalea logs");
            } else {
                trunkProviderField.set(treeConfig, BlockStateProvider.simple(Blocks.OAK_LOG));
                BitsAndBalance.LOGGER.info("Azalea trees will use vanilla oak logs");
            }
        } catch (Exception e) {
            BitsAndBalance.LOGGER.error("Failed to modify azalea tree trunk provider", e);
        }
    }
}
