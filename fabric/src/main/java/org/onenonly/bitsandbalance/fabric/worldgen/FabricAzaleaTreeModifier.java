package org.onenonly.bitsandbalance.fabric.worldgen;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import org.onenonly.bitsandbalance.common.content.ModAzalea;
import org.onenonly.bitsandbalance.fabric.config.FabricWorldgenConfig;
import org.onenonly.bitsandbalance.fabric.mixin.TreeConfigurationAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric equivalent of NeoForge's AzaleaTreeModifier.
 *
 * Adjusts the vanilla azalea tree configured feature trunk provider based on config.
 */
public final class FabricAzaleaTreeModifier {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-fabric-azalea-tree");

    private FabricAzaleaTreeModifier() {
    }

    public static void init() {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (!success) {
                return;
            }
            apply(server);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(FabricAzaleaTreeModifier::apply);
    }

    private static void apply(MinecraftServer server) {
        try {
            ConfiguredFeature<?, ?> azaleaFeature = server.registryAccess()
                    .lookup(Registries.CONFIGURED_FEATURE)
                    .flatMap(reg -> reg.getOptional(TreeFeatures.AZALEA_TREE))
                    .orElse(null);

            if (azaleaFeature == null || !(azaleaFeature.config() instanceof TreeConfiguration treeConfig)) {
                LOGGER.warn("Could not find or modify azalea tree feature");
                return;
            }

            if (!(treeConfig instanceof TreeConfigurationAccessor accessor)) {
                LOGGER.warn("Could not access azalea tree trunk provider (missing accessor mixin?)");
                return;
            }

            if (FabricWorldgenConfig.isAzaleaWoodGenerationEnabled()) {
                accessor.bitsandbalance$setTrunkProvider(BlockStateProvider.simple(ModAzalea.AZALEA_LOG));
                LOGGER.info("Azalea trees will use azalea logs");
            } else {
                accessor.bitsandbalance$setTrunkProvider(BlockStateProvider.simple(Blocks.OAK_LOG));
                LOGGER.info("Azalea trees will use vanilla oak logs");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to modify azalea tree trunk provider", e);
        }
    }
}
