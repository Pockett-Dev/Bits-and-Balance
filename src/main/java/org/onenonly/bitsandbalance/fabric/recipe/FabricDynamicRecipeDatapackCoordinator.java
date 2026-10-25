package org.onenonly.bitsandbalance.fabric.recipe;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.level.storage.LevelResource;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.recipe.RecipeRuntimeValidator;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricDynamicRecipeDatapackCoordinator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<UUID> PLAYERS_NEEDING_RECIPE_SYNC = ConcurrentHashMap.newKeySet();

    private FabricDynamicRecipeDatapackCoordinator() {
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(FabricDynamicRecipeDatapackCoordinator::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(FabricDynamicRecipeDatapackCoordinator::onEndServerTick);
    }

    public static void schedulePlayer(ServerPlayer player) {
        if (player != null) {
            PLAYERS_NEEDING_RECIPE_SYNC.add(player.getUUID());
        }
    }

    public static void scheduleAllPlayers(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            schedulePlayer(player);
        }
    }

    private static void onServerStarted(MinecraftServer server) {
        var preparedWorld = FabricIntegratedServerDatapackBootstrap.consumePreparedWorld(server.getWorldPath(LevelResource.ROOT));
        if (isReplayServer(server)) {
            // Flashback restores the recording's registries and intentionally suppresses
            // tag loading on reload. Generating recipes here can reference tags which
            // are not installed yet, such as bitsandbalance:map_ink.
            LOGGER.info("[{}] Preserving recorded recipe data on Flashback replay server; skipping dynamic datapack reload", BitsAndBalanceCommon.MOD_ID);
            return;
        }
        boolean preparedDuringIntegratedLoad = preparedWorld.prepared();

        server.execute(() -> {
            try {
                LOGGER.info(
                        "[{}] Dynamic recipes (Fabric {}): anyEnabled={} (config file: {})",
                        BitsAndBalanceCommon.MOD_ID,
                        preparedDuringIntegratedLoad ? "integrated" : "fallback",
                        org.onenonly.bitsandbalance.fabric.config.FabricRecipesConfig.anyEnabled(),
                        org.onenonly.bitsandbalance.fabric.config.FabricConfigPaths.resolve("bitsandbalance-content.json")
                );

                boolean changed = false;
                var worldRoot = server.getWorldPath(LevelResource.ROOT);
                var packRepository = server.getPackRepository();
                FabricGeneratedWorldDataPacks.restoreConfiguredPackSelections(worldRoot, packRepository);

                if (!preparedDuringIntegratedLoad) {
                    var recipeManager = server.getRecipeManager();
                    var registryAccess = server.registryAccess();

                    changed |= timed("Fabric fallback repository source setup", () -> {
                        FabricGeneratedWorldDataPacks.ensureRepositorySources(packRepository);
                        return FabricGeneratedWorldDataPacks.sanitizeLegacyWorldDatapacks(worldRoot, packRepository);
                    });
                    changed |= timed("Fabric fallback alternative recipes", () ->
                            FabricAlternativeRecipes.prepareWorldDatapack(worldRoot, packRepository, recipeManager, registryAccess));
                    changed |= timed("Fabric fallback wood recipes", () ->
                            FabricWoodStonecuttingRecipes.prepareWorldDatapack(worldRoot, packRepository));
                    changed |= timed("Fabric fallback vertical slab static recipes", () ->
                            FabricVerticalSlabRecipes.prepareWorldDatapack(worldRoot, packRepository, recipeManager, registryAccess));
                    changed |= timed("Fabric fallback step static recipes", () ->
                            FabricStepRecipes.prepareWorldDatapack(worldRoot, packRepository, recipeManager, registryAccess));
                    timed("Fabric fallback bootstrap state persist", () -> {
                        FabricGeneratedWorldDataPacks.persistBootstrapState(worldRoot, FabricGeneratedWorldDataPacks.buildBootstrapState(worldRoot));
                        return false;
                    });
                }

                var selectedPackIds = selectedPackIds(server);

                boolean validated = false;
                if (preparedDuringIntegratedLoad) {
                    LOGGER.info("[{}] Dynamic recipes (Fabric integrated): checking preloaded datapacks before deciding whether to reload", BitsAndBalanceCommon.MOD_ID);
                    validated = !preparedWorld.changed()
                            && timed("Fabric integrated recipe runtime validation", () -> RecipeRuntimeValidator.validate(server));
                    if (preparedWorld.changed() || !validated) {
                        LOGGER.info("[{}] Dynamic recipes (Fabric integrated): live recipe manager missed preloaded datapacks; B&B packs requiring reload: {}; Minecraft reloads full active selection: {}",
                                BitsAndBalanceCommon.MOD_ID,
                                selectedBitsAndBalancePackIds(selectedPackIds),
                                selectedPackIds);
                        timed("Fabric integrated selected datapack reload", () -> {
                            server.reloadResources(selectedPackIds).join();
                            return false;
                        });
                        validated = timed("Fabric integrated recipe runtime validation after reload", () -> RecipeRuntimeValidator.validate(server));
                        scheduleAllPlayers(server);
                    } else {
                        LOGGER.info("[{}] Dynamic recipes (Fabric integrated): generated datapacks already live, skipping post-start reload", BitsAndBalanceCommon.MOD_ID);
                    }
                } else {
                    validated = timed("Fabric fallback recipe runtime validation", () -> RecipeRuntimeValidator.validate(server));
                    if (changed || !validated) {
                        LOGGER.info("[{}] Dynamic recipes (Fabric fallback): generated datapacks {}. B&B packs requiring reload: {}; Minecraft reloads full active selection: {}",
                                BitsAndBalanceCommon.MOD_ID,
                                changed ? "changed" : "failed runtime validation",
                                selectedBitsAndBalancePackIds(selectedPackIds),
                                selectedPackIds);
                        timed("Fabric fallback selected datapack reload", () -> {
                            server.reloadResources(selectedPackIds).join();
                            return false;
                        });
                        validated = timed("Fabric fallback recipe runtime validation after reload", () -> RecipeRuntimeValidator.validate(server));
                        scheduleAllPlayers(server);
                    } else {
                        LOGGER.info("[{}] Dynamic recipes (Fabric fallback): generated datapacks already live, skipping post-start reload", BitsAndBalanceCommon.MOD_ID);
                    }
                }
                if (!validated) {
                    timed("Fabric final recipe runtime validation", () -> RecipeRuntimeValidator.validate(server));
                }
            } catch (Exception e) {
                LOGGER.warn("[{}] Failed syncing dynamic recipe datapacks with single reload: {}", BitsAndBalanceCommon.MOD_ID, e.toString());
            }
        });
    }

    private static boolean isReplayServer(MinecraftServer server) {
        // Avoid linking client-only, optional Flashback classes on dedicated servers.
        for (Class<?> type = server.getClass(); type != null; type = type.getSuperclass()) {
            if ("com.moulberry.flashback.playback.ReplayServer".equals(type.getName())) {
                return true;
            }
        }
        return false;
    }

    private static List<String> selectedPackIds(MinecraftServer server) {
        return server.getPackRepository().getSelectedPacks().stream()
                .map(Pack::getId)
                .filter(id -> id != null)
                .toList();
    }

    private static List<String> selectedBitsAndBalancePackIds(List<String> selectedPackIds) {
        return selectedPackIds.stream()
                .filter(id -> id.startsWith("bitsandbalance/generated/"))
                .toList();
    }

    private static boolean timed(String label, TimedBooleanOperation operation) throws Exception {
        long started = System.nanoTime();
        boolean result = operation.run();
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        LOGGER.info("[{}] Dynamic recipes: {} completed in {} ms", BitsAndBalanceCommon.MOD_ID, label, elapsedMs);
        return result;
    }

    @FunctionalInterface
    private interface TimedBooleanOperation {
        boolean run() throws Exception;
    }

    private static void onEndServerTick(MinecraftServer server) {
        if (PLAYERS_NEEDING_RECIPE_SYNC.isEmpty()) {
            return;
        }

        for (UUID uuid : Set.copyOf(PLAYERS_NEEDING_RECIPE_SYNC)) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                PLAYERS_NEEDING_RECIPE_SYNC.remove(uuid);
                continue;
            }
            if (player.tickCount <= 1) {
                continue;
            }

            try {
                syncRecipesToPlayer(server, player);
            } catch (Exception e) {
                LOGGER.warn("[{}] Failed to sync recipes to player {}: {}",
                        BitsAndBalanceCommon.MOD_ID,
                        player.getName().getString(),
                        e.toString());
            } finally {
                PLAYERS_NEEDING_RECIPE_SYNC.remove(uuid);
            }
        }
    }

    private static void syncRecipesToPlayer(MinecraftServer server, ServerPlayer player) {
        var recipeManager = server.getRecipeManager();
        var recipes = recipeManager.getRecipes();

        //? if mc == 1.21.1
        player.connection.send(new ClientboundUpdateRecipesPacket(recipes));
        //? else
        player.connection.send(new ClientboundUpdateRecipesPacket(
                recipeManager.getSynchronizedItemProperties(),
                recipeManager.getSynchronizedStonecutterRecipes()
        ));
        //? endif

        var recipeBook = player.getRecipeBook();
        for (var recipe : recipes) {
            if (recipe != null) {
                //? if mc == 1.21.1
                recipeBook.add(recipe);
                //? else
                recipeBook.add(recipe.id());
                //? endif
            }
        }
        recipeBook.sendInitialRecipeBook(player);
    }
}
