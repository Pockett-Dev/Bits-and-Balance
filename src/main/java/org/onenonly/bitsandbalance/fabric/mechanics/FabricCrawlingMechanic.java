package org.onenonly.bitsandbalance.fabric.mechanics;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.network.CrawlTogglePayload;

/**
 * Fabric port: Crawling
 *
 * Uses a client->server packet to toggle a server-side forced swimming pose.
 */
public final class FabricCrawlingMechanic {
    private FabricCrawlingMechanic() {
    }

    public static void initServer() {
        ServerPlayNetworking.registerGlobalReceiver(CrawlTogglePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                if (!FabricMechanicsConfig.enableCrawlingMechanic) {
                    FabricCrawlingState.setCrawling(player.getUUID(), false);
                    return;
                }

                if (player.isPassenger()) {
                    FabricCrawlingState.setCrawling(player.getUUID(), false);
                    return;
                }

                boolean want = payload.crawling();
                FabricCrawlingState.setCrawling(player.getUUID(), want);

                // Apply immediately to reduce visual jitter.
                if (want) {
                    try { player.setPose(Pose.SWIMMING); } catch (Throwable ignored) {}
                    try { player.refreshDimensions(); } catch (Throwable ignored) {}
                    try { player.setShiftKeyDown(false); } catch (Throwable ignored) {}
                } else {
                    // Don't force-stand if the player is legitimately swimming.
                    try {
                        if (player.getPose() == Pose.SWIMMING
                                && !player.isInWater()
                                && !player.isFallFlying()
                                && !player.isSleeping()) {
                            player.setPose(Pose.STANDING);
                        }
                    } catch (Throwable ignored) {}
                    try { player.refreshDimensions(); } catch (Throwable ignored) {}
                }
            });
        });

        // Clear per-player crawl intent on join/disconnect (session-only behavior).
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            server.execute(() -> FabricCrawlingState.setCrawling(player.getUUID(), false));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            server.execute(() -> FabricCrawlingState.clear(player.getUUID()));
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean enabled = FabricMechanicsConfig.enableCrawlingMechanic;

            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                boolean wantCrawl = enabled && FabricCrawlingState.isCrawling(sp.getUUID());

                if (sp.isPassenger()) {
                    if (wantCrawl) {
                        FabricCrawlingState.setCrawling(sp.getUUID(), false);
                    }
                    continue;
                }

                if (wantCrawl) {
                    // Vanilla-compatible crawl emulation: force pose to swimming while desired.
                    if (sp.getPose() != Pose.SWIMMING) sp.setPose(Pose.SWIMMING);
                    try {
                        sp.setShiftKeyDown(false);
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
    }
}
