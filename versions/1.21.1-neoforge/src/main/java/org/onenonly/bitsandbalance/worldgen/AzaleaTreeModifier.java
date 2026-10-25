package org.onenonly.bitsandbalance.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.ModBlocks;

/**
 * Modifies the vanilla azalea tree configured feature at runtime based on config.
 * Inspired by Quark's approach to configurable worldgen features.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class AzaleaTreeModifier {
    private AzaleaTreeModifier() {
    }

    @SubscribeEvent
    public static void onServerReload(AddReloadListenerEvent event) {
        if (!Config.enableAzaleaWoodGeneration) {
            BitsAndBalance.LOGGER.info("Bits and Balance azalea wood generation is disabled; leaving the loaded azalea tree trunk provider unchanged");
            return;
        }

        var azaleaHolder = event.getRegistryAccess()
            .lookupOrThrow(Registries.CONFIGURED_FEATURE)
            .get(TreeFeatures.AZALEA_TREE)
            .orElse(null);
        ConfiguredFeature<?, ?> azaleaFeature = azaleaHolder != null ? azaleaHolder.value() : null;

        if (azaleaFeature == null || !(azaleaFeature.config() instanceof TreeConfiguration treeConfig)) {
            BitsAndBalance.LOGGER.warn("Could not find or modify azalea tree feature");
            return;
        }

        try {
            java.lang.reflect.Field trunkProviderField = TreeConfiguration.class.getDeclaredField("trunkProvider");
            trunkProviderField.setAccessible(true);

            trunkProviderField.set(treeConfig, BlockStateProvider.simple(ModBlocks.AZALEA_LOG.get()));
            BitsAndBalance.LOGGER.info("Azalea trees will use Bits and Balance azalea logs");
        } catch (Exception e) {
            BitsAndBalance.LOGGER.error("Failed to modify azalea tree trunk provider", e);
        }
    }
}
