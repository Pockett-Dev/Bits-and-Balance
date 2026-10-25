package org.onenonly.bitsandbalance.tweaks;

import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.recipe.RecipeRuntimeValidator;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class NeoForgeDynamicRecipeDatapackCoordinator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int POST_RELOAD_RECIPE_SYNC_DELAY_TICKS = 40;
    private static final Map<UUID, Integer> PLAYERS_NEEDING_RECIPE_SYNC = new ConcurrentHashMap<>();

    private NeoForgeDynamicRecipeDatapackCoordinator() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        var worldRoot = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        NeoForgeIntegratedServerDatapackBootstrap.PreparedWorldConsumption preparedWorld =
                NeoForgeIntegratedServerDatapackBootstrap.consumePreparedWorld(
                worldRoot
                );

        NeoForgeGeneratedServerDataPacks.BootstrapState consumedState = NeoForgeGeneratedServerDataPacks.consumeState(worldRoot);
        final NeoForgeGeneratedServerDataPacks.BootstrapState bootstrapState = consumedState != null
                ? consumedState
            : NeoForgeGeneratedServerDataPacks.buildBootstrapState(worldRoot);

        server.execute(() -> {
            NeoForgeGeneratedServerDataPacks.BootstrapState currentState = bootstrapState;
            boolean legacyWorldPacksChanged = NeoForgeGeneratedServerDataPacks.removeLegacyWorldDatapacks(server);
            boolean changed = false;
            boolean validated = false;
            if (preparedWorld.prepared()) {
                var selectedPackIds = selectedPackIds(server);
                LOGGER.info("[{}] Dynamic recipes (NeoForge): checking preloaded datapacks before deciding whether to reload", BitsAndBalance.MODID);
                validated = !preparedWorld.changed() && RecipeRuntimeValidator.validate(server);
                if (preparedWorld.changed() || !validated) {
                    try {
                        LOGGER.info("[{}] Dynamic recipes (NeoForge): live recipe manager missed preloaded datapacks; B&B packs requiring reload: {}; Minecraft reloads full active selection: {}",
                                BitsAndBalance.MODID,
                            selectedBitsAndBalancePackIds(selectedPackIds),
                                selectedPackIds);
                        server.reloadResources(selectedPackIds).join();
                        validated = RecipeRuntimeValidator.validate(server);
                    } catch (Exception e) {
                        LOGGER.warn("[{}] Failed reloading preloaded integrated-world datapacks: {}", BitsAndBalance.MODID, e.toString());
                    }
                } else {
                    LOGGER.info("[{}] Dynamic recipes (NeoForge): generated datapacks already live, skipping post-start reload", BitsAndBalance.MODID);
                }
            } else if (currentState.alternativePending()) {
                changed |= legacyWorldPacksChanged;
                changed |= prepare("alternative recipes", () -> AlternativeRecipes.prepareWorldDatapack(server));
                currentState = currentState.withPackPending(AlternativeRecipes.packName(), false);
            } else {
                changed |= legacyWorldPacksChanged;
            }

            if (currentState.verticalStaticPending()) {
                changed |= prepare("vertical slab static recipes", VerticalSlabStonecuttingRecipes::prepareStaticDatapack);
                currentState = currentState.withPackPending(VerticalSlabStonecuttingRecipes.staticPackName(), false);
            }
            if (currentState.verticalMirrorPending()) {
                changed |= prepare("vertical slab mirror recipes", () -> VerticalSlabStonecuttingRecipes.prepareMirrorDatapack(server));
                currentState = currentState.withPackPending(VerticalSlabStonecuttingRecipes.mirrorPackName(), false);
            }
            if (currentState.stepStaticPending()) {
                changed |= prepare("step static recipes", StepStonecuttingRecipes::prepareStaticDatapack);
                currentState = currentState.withPackPending(StepStonecuttingRecipes.staticPackName(), false);
            }
            if (currentState.stepMirrorPending()) {
                changed |= prepare("step mirror recipes", () -> StepStonecuttingRecipes.prepareMirrorDatapack(server));
                currentState = currentState.withPackPending(StepStonecuttingRecipes.mirrorPackName(), false);
            }

            try {
                if (preparedWorld.prepared() && validated) {
                    if (changed) {
                        LOGGER.info("[{}] Dynamic recipes (NeoForge): prepared follow-up generated datapacks for next world load; skipping post-start reload", BitsAndBalance.MODID);
                    } else {
                        LOGGER.info("[{}] Dynamic recipes (NeoForge): bootstrapped generated packs without post-start reload", BitsAndBalance.MODID);
                    }
                    NeoForgeGeneratedServerDataPacks.persistBootstrapState(worldRoot, currentState);
                    return;
                }

                changed |= NeoForgeGeneratedServerDataPacks.ensureGeneratedPacksSelected(server, currentState);
                if (changed) {
                    var selectedPackIds = selectedPackIds(server);
                    LOGGER.info("[{}] Dynamic recipes (NeoForge): generated datapacks changed; B&B packs requiring reload: {}; Minecraft reloads full active selection: {}", BitsAndBalance.MODID, selectedBitsAndBalancePackIds(selectedPackIds), selectedPackIds);
                    server.reloadResources(selectedPackIds).join();
                    validated = RecipeRuntimeValidator.validate(server);
                    scheduleAllPlayers(server);
                } else {
                    LOGGER.info("[{}] Dynamic recipes (NeoForge): bootstrapped generated packs without post-start reload", BitsAndBalance.MODID);
                }
                NeoForgeGeneratedServerDataPacks.persistBootstrapState(worldRoot, currentState);
                if (!validated) {
                    RecipeRuntimeValidator.validate(server);
                }
            } catch (Exception e) {
                LOGGER.warn("[{}] Failed syncing dynamic recipe datapacks with single reload: {}", BitsAndBalance.MODID, e.toString());
            }
        });
    }

    private static void scheduleAllPlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            schedulePlayer(player);
        }
    }

    private static void schedulePlayer(ServerPlayer player) {
        PLAYERS_NEEDING_RECIPE_SYNC.put(player.getUUID(), player.tickCount + POST_RELOAD_RECIPE_SYNC_DELAY_TICKS);
    }

    private static List<String> selectedPackIds(MinecraftServer server) {
        return server.getPackRepository().getSelectedPacks().stream()
                .map(Pack::getId)
                .filter(id -> id != null)
                .toList();
    }

    private static List<String> selectedBitsAndBalancePackIds(List<String> selectedPackIds) {
        return selectedPackIds.stream()
                .filter(id -> id.startsWith("bitsandbalance/generated/") || id.startsWith("file/bitsandbalance_"))
                .toList();
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PLAYERS_NEEDING_RECIPE_SYNC.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        UUID playerId = player.getUUID();
        Integer targetTick = PLAYERS_NEEDING_RECIPE_SYNC.get(playerId);
        if (targetTick == null || player.tickCount < targetTick) {
            return;
        }

        try {
            syncRecipeBook(player);
        } catch (Exception e) {
            LOGGER.warn("[{}] Failed recipe-book sync for player {}: {}", BitsAndBalance.MODID, player.getName().getString(), e.toString());
        } finally {
            PLAYERS_NEEDING_RECIPE_SYNC.remove(playerId);
        }
    }

    private static void syncRecipeBook(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        var recipeManager = server.getRecipeManager();
        var recipes = recipeManager.getRecipes();

        player.connection.send(new ClientboundUpdateRecipesPacket(recipes));
        player.getRecipeBook().sendInitialRecipeBook(player);
        LOGGER.info("[{}] Resynced {} recipes to the NeoForge client for {}", BitsAndBalance.MODID, recipes.size(), player.getName().getString());
    }

    private static boolean prepare(String label, ThrowingSupplier action) {
        try {
            return action.run();
        } catch (Exception e) {
            LOGGER.warn("[{}] Failed preparing {} datapack: {}", BitsAndBalance.MODID, label, e.toString());
            return false;
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        boolean run() throws Exception;
    }
}
