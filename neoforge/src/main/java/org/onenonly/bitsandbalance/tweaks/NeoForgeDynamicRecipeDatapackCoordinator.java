package org.onenonly.bitsandbalance.tweaks;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.server.packs.repository.Pack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.recipe.RecipeRuntimeValidator;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class NeoForgeDynamicRecipeDatapackCoordinator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> PLAYERS_NEEDING_RECIPE_SYNC = ConcurrentHashMap.newKeySet();

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
            boolean legacyWorldPacksChanged = NeoForgeGeneratedServerDataPacks.removeLegacyWorldDatapacks(server);
            boolean changed = false;
            if (preparedWorld.prepared()) {
                try {
                    var selectedPackIds = server.getPackRepository().getSelectedPacks().stream()
                            .map(Pack::getId)
                            .filter(id -> id != null && !id.startsWith("bitsandbalance/generated/"))
                            .toList();
                    LOGGER.info("[{}] Dynamic recipes (NeoForge): using integrated-world datapacks prepared before load; changed={}",
                            BitsAndBalance.MODID,
                            preparedWorld.changed());
                    server.reloadResources(selectedPackIds).join();
                } catch (Exception e) {
                    LOGGER.warn("[{}] Failed reloading preloaded integrated-world datapacks: {}", BitsAndBalance.MODID, e.toString());
                }
            } else if (bootstrapState.alternativePending()) {
                changed |= legacyWorldPacksChanged;
                changed |= prepare("alternative recipes", () -> AlternativeRecipes.prepareWorldDatapack(server));
            } else {
                changed |= legacyWorldPacksChanged;
            }

            if (bootstrapState.verticalStaticPending()) {
                changed |= prepare("vertical slab static recipes", VerticalSlabStonecuttingRecipes::prepareStaticDatapack);
            }
            if (bootstrapState.verticalMirrorPending()) {
                changed |= prepare("vertical slab mirror recipes", () -> VerticalSlabStonecuttingRecipes.prepareMirrorDatapack(server));
            }
            if (bootstrapState.stepStaticPending()) {
                changed |= prepare("step static recipes", StepStonecuttingRecipes::prepareStaticDatapack);
            }
            if (bootstrapState.stepMirrorPending()) {
                changed |= prepare("step mirror recipes", () -> StepStonecuttingRecipes.prepareMirrorDatapack(server));
            }

            try {
                changed |= NeoForgeGeneratedServerDataPacks.ensureGeneratedPacksSelected(server);
                if (changed) {
                    LOGGER.info("[{}] Dynamic recipes (NeoForge): changes detected, running single final reload", BitsAndBalance.MODID);
                    server.reloadResources(server.getPackRepository().getSelectedPacks().stream().map(Pack::getId).toList()).join();
                } else {
                    LOGGER.info("[{}] Dynamic recipes (NeoForge): bootstrapped generated packs without post-start reload", BitsAndBalance.MODID);
                }
                NeoForgeGeneratedServerDataPacks.persistBootstrapState(worldRoot, NeoForgeGeneratedServerDataPacks.buildBootstrapState(worldRoot));
                RecipeRuntimeValidator.validate(server);
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    PLAYERS_NEEDING_RECIPE_SYNC.add(player.getUUID().toString());
                }
            } catch (Exception e) {
                LOGGER.warn("[{}] Failed syncing dynamic recipe datapacks with single reload: {}", BitsAndBalance.MODID, e.toString());
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PLAYERS_NEEDING_RECIPE_SYNC.add(player.getUUID().toString());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        String playerId = player.getUUID().toString();
        if (!PLAYERS_NEEDING_RECIPE_SYNC.contains(playerId) || player.tickCount <= 1) {
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

        player.connection.send(new ClientboundUpdateRecipesPacket(
                recipeManager.getSynchronizedItemProperties(),
                recipeManager.getSynchronizedStonecutterRecipes()
        ));

        var recipeBook = player.getRecipeBook();
        for (var recipe : recipes) {
            if (recipe != null) {
                recipeBook.add(recipe.id());
            }
        }
        recipeBook.sendInitialRecipeBook(player);
        LOGGER.info("[{}] Synced {} recipes into the NeoForge recipe book for {}", BitsAndBalance.MODID, recipes.size(), player.getName().getString());
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