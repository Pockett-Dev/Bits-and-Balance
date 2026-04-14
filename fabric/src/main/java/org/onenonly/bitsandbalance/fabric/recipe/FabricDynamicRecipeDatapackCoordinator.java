package org.onenonly.bitsandbalance.fabric.recipe;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.level.storage.LevelResource;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.recipe.RecipeRuntimeValidator;
import org.slf4j.Logger;

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
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> schedulePlayer(handler.player));
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
        boolean preparedDuringIntegratedLoad = preparedWorld.prepared();

        server.execute(() -> {
            try {
                LOGGER.info(
                        "[{}] Dynamic recipes (Fabric {}): anyEnabled={} (config file: config/bitsandbalance-recipes.json)",
                        BitsAndBalanceCommon.MOD_ID,
                        preparedDuringIntegratedLoad ? "integrated" : "fallback",
                        org.onenonly.bitsandbalance.fabric.config.FabricRecipesConfig.anyEnabled()
                );

                boolean changed = false;
                var worldRoot = server.getWorldPath(LevelResource.ROOT);
                var packRepository = server.getPackRepository();
                FabricGeneratedWorldDataPacks.restoreConfiguredPackSelections(worldRoot, packRepository);

                if (!preparedDuringIntegratedLoad) {
                    var recipeManager = server.getRecipeManager();
                    var registryAccess = server.registryAccess();

                    FabricGeneratedWorldDataPacks.ensureRepositorySources(packRepository);
                    FabricGeneratedWorldDataPacks.sanitizeLegacyWorldDatapacks(worldRoot, packRepository);
                    changed |= FabricAlternativeRecipes.prepareWorldDatapack(worldRoot, packRepository, recipeManager, registryAccess);
                    changed |= FabricWoodStonecuttingRecipes.prepareWorldDatapack(worldRoot, packRepository);
                    changed |= FabricVerticalSlabRecipes.prepareWorldDatapack(worldRoot, packRepository, recipeManager, registryAccess);
                    changed |= FabricStepRecipes.prepareWorldDatapack(worldRoot, packRepository, recipeManager, registryAccess);
                    FabricGeneratedWorldDataPacks.persistBootstrapState(worldRoot, FabricGeneratedWorldDataPacks.buildBootstrapState(worldRoot));
                }

                if (preparedDuringIntegratedLoad) {
                    var selectedPackIds = server.getPackRepository().getSelectedPacks().stream()
                            .map(Pack::getId)
                            .toList();
                    LOGGER.info("[{}] Dynamic recipes (Fabric integrated): reloading selected packs after bootstrap: {}", BitsAndBalanceCommon.MOD_ID, selectedPackIds);
                    server.reloadResources(selectedPackIds).join();
                } else if (changed) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
                } else {
                    LOGGER.info("[{}] Dynamic recipes (Fabric fallback): no datapack changes detected, skipping reload", BitsAndBalanceCommon.MOD_ID);
                }
                RecipeRuntimeValidator.validate(server);
                scheduleAllPlayers(server);
            } catch (Exception e) {
                LOGGER.warn("[{}] Failed syncing dynamic recipe datapacks with single reload: {}", BitsAndBalanceCommon.MOD_ID, e.toString());
            }
        });
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

        player.connection.send(new ClientboundUpdateRecipesPacket(
                recipeManager.getSynchronizedItemProperties(),
                recipeManager.getSynchronizedStonecutterRecipes()
        ));

        var recipeBook = player.getRecipeBook();
        for (var recipe : recipeManager.getRecipes()) {
            if (recipe != null) {
                recipeBook.add(recipe.id());
            }
        }
        recipeBook.sendInitialRecipeBook(player);
    }
}