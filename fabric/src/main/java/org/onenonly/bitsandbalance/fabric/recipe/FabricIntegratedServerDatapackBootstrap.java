package org.onenonly.bitsandbalance.fabric.recipe;

import com.mojang.logging.LogUtils;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FabricIntegratedServerDatapackBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PREPARED_WORLD_CONSUMERS = 1;
    private static final Map<Path, PreparedWorldState> PREPARED_WORLDS = new ConcurrentHashMap<>();

    private FabricIntegratedServerDatapackBootstrap() {
    }

    public static void prepare(LevelStorageSource.LevelStorageAccess levelStorage, PackRepository packRepository, WorldStem worldStem) {
        Path worldRoot = normalize(levelStorage.getLevelPath(LevelResource.ROOT));

        try {
            FabricGeneratedWorldDataPacks.ensureRepositorySources(packRepository);
            boolean changed = FabricGeneratedWorldDataPacks.sanitizeLegacyWorldDatapacks(worldRoot, packRepository);
            FabricGeneratedWorldDataPacks.BootstrapState bootstrapState = FabricGeneratedWorldDataPacks.buildBootstrapState(worldRoot);
            changed |= FabricGeneratedWorldDataPacks.enableReusablePacks(packRepository, bootstrapState);

            var recipeManager = worldStem.dataPackResources().getRecipeManager();
            var registryAccess = worldStem.registries().compositeAccess();

            changed |= FabricAlternativeRecipes.prepareWorldDatapack(worldRoot, packRepository, recipeManager, registryAccess);
            changed |= FabricWoodStonecuttingRecipes.prepareWorldDatapack(worldRoot, packRepository);
            changed |= FabricVerticalSlabRecipes.prepareWorldDatapack(worldRoot, packRepository, recipeManager, registryAccess);
            changed |= FabricStepRecipes.prepareWorldDatapack(worldRoot, packRepository, recipeManager, registryAccess);

            FabricGeneratedWorldDataPacks.persistBootstrapState(worldRoot, FabricGeneratedWorldDataPacks.buildBootstrapState(worldRoot));

            PREPARED_WORLDS.put(worldRoot, new PreparedWorldState(PREPARED_WORLD_CONSUMERS, changed));
        } catch (Exception e) {
            LOGGER.warn("[{}] Failed to prepare dynamic recipe datapacks before integrated server load: {}", BitsAndBalanceCommon.MOD_ID, e.toString());
        }
    }

    public static PreparedWorldConsumption consumePreparedWorld(Path worldRoot) {
        Path normalizedWorldRoot = normalize(worldRoot);
        AtomicBoolean consumed = new AtomicBoolean(false);
        AtomicBoolean changed = new AtomicBoolean(false);

        PREPARED_WORLDS.compute(normalizedWorldRoot, (path, state) -> {
            if (state == null) {
                return null;
            }

            consumed.set(true);
            changed.set(state.changed());
            return state.remainingConsumers() > 1 ? new PreparedWorldState(state.remainingConsumers() - 1, state.changed()) : null;
        });

        return new PreparedWorldConsumption(consumed.get(), changed.get());
    }

    public record PreparedWorldConsumption(boolean prepared, boolean changed) {
    }

    private record PreparedWorldState(int remainingConsumers, boolean changed) {
    }

    private static Path normalize(Path worldRoot) {
        return worldRoot.toAbsolutePath().normalize();
    }
}