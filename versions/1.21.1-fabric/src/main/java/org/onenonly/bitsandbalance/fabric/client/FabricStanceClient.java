package org.onenonly.bitsandbalance.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.network.CrawlTogglePayload;

/**
 * Fabric port: Crawling + Toggle Stance
 *
 * Client-side keybind logic and packet sending.
 */
public final class FabricStanceClient {
    private FabricStanceClient() {
    }

    private static final String CATEGORY = FabricKeyCategories.MAIN;

    public static final KeyMapping TOGGLE_STANCE = new KeyMapping(
            "key.bitsandbalance.toggle_stance",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping CRAWL = new KeyMapping(
            "key.bitsandbalance.crawl",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    private static boolean lastCrawlSent = false;
    private static boolean crawlByToggle = false;

    private static boolean sneakingByToggle = false;
    private static boolean lastToggleStanceKeyDown = false;

    public static void initClient() {
        KeyBindingHelper.registerKeyBinding(TOGGLE_STANCE);
        KeyBindingHelper.registerKeyBinding(CRAWL);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;

            if (player.isPassenger()) {
                if (lastCrawlSent || crawlByToggle) {
                    if (ClientPlayNetworking.canSend(CrawlTogglePayload.TYPE)) {
                        ClientPlayNetworking.send(new CrawlTogglePayload(false));
                    }
                }
                lastCrawlSent = false;
                crawlByToggle = false;
                sneakingByToggle = false;
                return;
            }

            boolean crawlingEnabled = FabricMechanicsConfig.enableCrawlingMechanic;
            if (crawlingEnabled) {
                boolean holdCrawl = CRAWL.isDown();
                boolean wantCrawl = holdCrawl || crawlByToggle;

                if (wantCrawl != lastCrawlSent) {
                    if (ClientPlayNetworking.canSend(CrawlTogglePayload.TYPE)) {
                        ClientPlayNetworking.send(new CrawlTogglePayload(wantCrawl));
                    }
                    lastCrawlSent = wantCrawl;

                    try {
                        if (wantCrawl) {
                            player.setPose(net.minecraft.world.entity.Pose.SWIMMING);
                        } else {
                            if (player.getPose() == net.minecraft.world.entity.Pose.SWIMMING
                                    && !player.isInWater()
                                    && !player.isFallFlying()
                                    && !player.isSleeping()) {
                                player.setPose(net.minecraft.world.entity.Pose.STANDING);
                            }
                        }
                        player.refreshDimensions();
                    } catch (Throwable ignored) {
                    }
                }
            } else {
                if (lastCrawlSent || crawlByToggle) {
                    if (ClientPlayNetworking.canSend(CrawlTogglePayload.TYPE)) {
                        ClientPlayNetworking.send(new CrawlTogglePayload(false));
                    }
                }
                lastCrawlSent = false;
                crawlByToggle = false;
            }

            try {
                if (lastCrawlSent && player.getPose() != net.minecraft.world.entity.Pose.SWIMMING) {
                    player.setPose(net.minecraft.world.entity.Pose.SWIMMING);
                }
            } catch (Throwable ignored) {
            }

            boolean wantSneak;
            boolean baselineSneak;
            if (lastCrawlSent) {
                baselineSneak = false;
            } else {
                boolean toggleCrouchEnabled = false;
                try {
                    toggleCrouchEnabled = mc.options != null && mc.options.toggleCrouch().get();
                } catch (Throwable ignored) {
                }

                if (toggleCrouchEnabled) {
                    baselineSneak = player.isShiftKeyDown();
                } else {
                    baselineSneak = mc.options != null && mc.options.keyShift.isDown();
                    if (baselineSneak && sneakingByToggle) {
                        sneakingByToggle = false;
                    }
                }
            }
            wantSneak = baselineSneak || sneakingByToggle;

            try {
                boolean toggleCrouchEnabled2 = mc.options != null && mc.options.toggleCrouch().get();
                if (!toggleCrouchEnabled2 && mc.options != null) {
                    mc.options.keyShift.setDown(wantSneak);
                }
            } catch (Throwable ignored) {
            }

            try {
                if (player.isShiftKeyDown() != wantSneak) {
                    player.setShiftKeyDown(wantSneak);
                }
            } catch (Throwable ignored) {
            }

            boolean toggleStanceKeyDown = TOGGLE_STANCE.isDown();
            boolean toggleStanceJustPressed = toggleStanceKeyDown && !lastToggleStanceKeyDown;
            lastToggleStanceKeyDown = toggleStanceKeyDown;

            if (FabricMechanicsConfig.enableToggleStance && toggleStanceJustPressed) {
                boolean crawlingEnabled2 = FabricMechanicsConfig.enableCrawlingMechanic;
                boolean isCrawling = lastCrawlSent;
                boolean isSneaking = wantSneak;

                if (crawlingEnabled2) {
                    if (isCrawling) {
                        if (ClientPlayNetworking.canSend(CrawlTogglePayload.TYPE)) {
                            ClientPlayNetworking.send(new CrawlTogglePayload(false));
                        }
                        lastCrawlSent = false;
                        crawlByToggle = false;
                        sneakingByToggle = false;
                    } else if (isSneaking) {
                        sneakingByToggle = false;
                        if (ClientPlayNetworking.canSend(CrawlTogglePayload.TYPE)) {
                            ClientPlayNetworking.send(new CrawlTogglePayload(true));
                        }
                        lastCrawlSent = true;
                        crawlByToggle = true;
                    } else {
                        crawlByToggle = false;
                        sneakingByToggle = true;
                    }
                } else {
                    sneakingByToggle = !isSneaking;
                }
            }
        });
    }
}