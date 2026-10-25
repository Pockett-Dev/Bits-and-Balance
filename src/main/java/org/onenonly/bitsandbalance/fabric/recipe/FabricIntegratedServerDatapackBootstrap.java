package org.onenonly.bitsandbalance.fabric.recipe;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FabricIntegratedServerDatapackBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PREPARED_WORLD_CONSUMERS = 1;
    private static final Map<Path, PreparedWorldState> PREPARED_WORLDS = new ConcurrentHashMap<>();

    private FabricIntegratedServerDatapackBootstrap() {
    }

    public static Dynamic<?> prepareBeforeWorldStemDataTag(LevelStorageSource.LevelStorageAccess levelStorage, PackRepository packRepository, Dynamic<?> dataTag) {
        Path worldRoot = normalize(levelStorage.getLevelPath(LevelResource.ROOT));
        Dynamic<?> preparedDataTag = FabricGeneratedWorldDataPacks.sanitizePersistedPackSelections(dataTag);

        try {
            FabricGeneratedWorldDataPacks.ensureRepositorySources(packRepository);
            boolean changed = timedBoolean("Fabric integrated pre-world-stem legacy datapack cleanup", () ->
                    FabricGeneratedWorldDataPacks.sanitizeLegacyWorldDatapacks(worldRoot, packRepository));
            Set<String> refreshedPacks = new LinkedHashSet<>();
            changed |= timedBoolean("Fabric integrated pre-world-stem alternative recipes", () ->
                    FabricAlternativeRecipes.prepareBeforeWorldStemDatapack(worldRoot, packRepository));
            refreshedPacks.add(FabricAlternativeRecipes.packName());
            changed |= timedBoolean("Fabric integrated pre-world-stem wood recipes", () ->
                    FabricWoodStonecuttingRecipes.prepareBeforeWorldStemDatapack(worldRoot, packRepository));
            refreshedPacks.add(FabricWoodStonecuttingRecipes.packName());
            changed |= timedBoolean("Fabric integrated pre-world-stem vertical slab static recipes", () ->
                    FabricVerticalSlabRecipes.prepareBeforeWorldStemDatapack(worldRoot, packRepository));
            refreshedPacks.add(FabricVerticalSlabRecipes.staticPackName());
            changed |= timedBoolean("Fabric integrated pre-world-stem step static recipes", () ->
                    FabricStepRecipes.prepareBeforeWorldStemDatapack(worldRoot, packRepository));
            refreshedPacks.add(FabricStepRecipes.staticPackName());
            FabricGeneratedWorldDataPacks.BootstrapState bootstrapState = timedValue("Fabric integrated pre-world-stem bootstrap state build", () ->
                    FabricGeneratedWorldDataPacks.buildBootstrapState(worldRoot, refreshedPacks));
            changed |= timedBoolean("Fabric integrated pre-world-stem bootstrap selection apply", () ->
                    FabricGeneratedWorldDataPacks.applyBootstrapSelections(packRepository, bootstrapState));
            timedValue("Fabric integrated pre-world-stem bootstrap state persist", () -> {
                FabricGeneratedWorldDataPacks.persistBootstrapState(worldRoot, bootstrapState);
                return null;
            });
            Dynamic<?> updatedDataTag = preparedDataTag;
            preparedDataTag = timedValue("Fabric integrated pre-world-stem data tag selection apply", () ->
                    FabricGeneratedWorldDataPacks.applyBootstrapSelectionsToDataTag(updatedDataTag, bootstrapState));

            if (changed) {
                LOGGER.info("[{}] Prepared current generated datapack selections before integrated world-stem load", BitsAndBalanceCommon.MOD_ID);
            }
        } catch (Exception e) {
            LOGGER.warn("[{}] Failed to prepare generated datapack selections before integrated world-stem load: {}", BitsAndBalanceCommon.MOD_ID, e.toString());
        }

        return preparedDataTag;
    }

    public static void prepare(LevelStorageSource.LevelStorageAccess levelStorage, PackRepository packRepository, WorldStem worldStem) {
        Path worldRoot = normalize(levelStorage.getLevelPath(LevelResource.ROOT));

        try {
            FabricGeneratedWorldDataPacks.ensureRepositorySources(packRepository);
            timedBoolean("Fabric integrated legacy datapack cleanup", () ->
                    FabricGeneratedWorldDataPacks.sanitizeLegacyWorldDatapacks(worldRoot, packRepository));
            FabricGeneratedWorldDataPacks.BootstrapState bootstrapState = timedValue("Fabric integrated bootstrap state build", () ->
                    FabricGeneratedWorldDataPacks.buildBootstrapState(worldRoot));
            timedBoolean("Fabric integrated bootstrap selection apply", () ->
                    FabricGeneratedWorldDataPacks.applyBootstrapSelections(packRepository, bootstrapState));

            boolean changed = false;
            Set<String> refreshedPacks = new LinkedHashSet<>();
            if (bootstrapState.hasPending()) {
                var recipeManager = worldStem.dataPackResources().getRecipeManager();
                var registryAccess = worldStem.registries().compositeAccess();

                if (bootstrapState.alternative().pending()) {
                    changed |= timedBoolean("Fabric integrated alternative recipes", () ->
                            FabricAlternativeRecipes.prepareWorldDatapack(worldRoot, packRepository, recipeManager, registryAccess));
                    refreshedPacks.add(FabricAlternativeRecipes.packName());
                }
                if (bootstrapState.wood().pending()) {
                    changed |= timedBoolean("Fabric integrated wood recipes", () ->
                            FabricWoodStonecuttingRecipes.prepareWorldDatapack(worldRoot, packRepository));
                    refreshedPacks.add(FabricWoodStonecuttingRecipes.packName());
                }
                if (bootstrapState.vertical().pending()) {
                    changed |= timedBoolean("Fabric integrated vertical slab static recipes", () ->
                            FabricVerticalSlabRecipes.prepareWorldDatapack(worldRoot, packRepository, recipeManager, registryAccess));
                    refreshedPacks.add(FabricVerticalSlabRecipes.staticPackName());
                }
                if (bootstrapState.verticalMirror().pending()) {
                    changed |= timedBoolean("Fabric integrated vertical slab mirror recipes", () ->
                            FabricVerticalSlabRecipes.prepareMirrorDatapack(worldRoot, packRepository, recipeManager, registryAccess));
                    refreshedPacks.add(FabricVerticalSlabRecipes.mirrorPackName());
                }
                if (bootstrapState.step().pending()) {
                    changed |= timedBoolean("Fabric integrated step static recipes", () ->
                            FabricStepRecipes.prepareWorldDatapack(worldRoot, packRepository, recipeManager, registryAccess));
                    refreshedPacks.add(FabricStepRecipes.staticPackName());
                }
                if (bootstrapState.stepMirror().pending()) {
                    changed |= timedBoolean("Fabric integrated step mirror recipes", () ->
                            FabricStepRecipes.prepareMirrorDatapack(worldRoot, packRepository, recipeManager, registryAccess));
                    refreshedPacks.add(FabricStepRecipes.mirrorPackName());
                }
            }

            FabricGeneratedWorldDataPacks.BootstrapState refreshedState = timedValue("Fabric integrated refreshed bootstrap state build", () ->
                    FabricGeneratedWorldDataPacks.buildBootstrapState(worldRoot, refreshedPacks));
            changed |= timedBoolean("Fabric integrated refreshed bootstrap selection apply", () ->
                    FabricGeneratedWorldDataPacks.applyBootstrapSelections(packRepository, refreshedState));
            timedValue("Fabric integrated refreshed bootstrap state persist", () -> {
                FabricGeneratedWorldDataPacks.persistBootstrapState(worldRoot, refreshedState);
                return null;
            });

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

    private static boolean timedBoolean(String label, TimedBooleanOperation operation) throws Exception {
        long started = System.nanoTime();
        boolean result = operation.run();
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        LOGGER.info("[{}] Dynamic recipes: {} completed in {} ms", BitsAndBalanceCommon.MOD_ID, label, elapsedMs);
        return result;
    }

    private static <T> T timedValue(String label, TimedOperation<T> operation) throws Exception {
        long started = System.nanoTime();
        T result = operation.run();
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        LOGGER.info("[{}] Dynamic recipes: {} completed in {} ms", BitsAndBalanceCommon.MOD_ID, label, elapsedMs);
        return result;
    }

    @FunctionalInterface
    private interface TimedBooleanOperation {
        boolean run() throws Exception;
    }

    @FunctionalInterface
    private interface TimedOperation<T> {
        T run() throws Exception;
    }
}
