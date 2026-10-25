package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

/**
 * Fabric client-side handler for Improved Recovery Compass
 * Handles:
 * - HUD display when holding compass
 * - Right-click to toggle coords/distance
 * - Leaves shift+right-click particle hints to the server-side trail handler
 */
public class FabricRecoveryCompassClient {

    private static boolean showingCoords = false;

    public static void init() {
        // Register client tick event for HUD updates
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!FabricTweaksConfig.enableImprovedRecoveryCompass) return;
            if (!FabricTweaksConfig.recoveryCompassShowDistance && !FabricTweaksConfig.recoveryCompassShowCoordsOnRightClick) return;

            LocalPlayer player = client.player;
            if (player == null || player.level() == null) return;

            // Check if player is holding a recovery compass
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            boolean holdingCompass = bitsandbalance$isRecoveryCompass(mainHand) || bitsandbalance$isRecoveryCompass(offHand);

            if (holdingCompass) {
                // Update HUD overlay state
                FabricRecoveryCompassHudOverlay.isActive = true;
                FabricRecoveryCompassHudOverlay.showingCoords = showingCoords;
            } else {
                // Deactivate HUD overlay
                FabricRecoveryCompassHudOverlay.isActive = false;
                showingCoords = false;
            }
        });
        
        // Register right-click item event for compass interactions
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!FabricTweaksConfig.enableImprovedRecoveryCompass) return InteractionResult.PASS;

            ItemStack item = player.getItemInHand(hand);
            if (!bitsandbalance$isRecoveryCompass(item)) return InteractionResult.PASS;

            if (!(player instanceof LocalPlayer localPlayer)) return InteractionResult.PASS;

            // Check if shift is held down
            boolean isShiftHeld = localPlayer.isShiftKeyDown();

            if (!isShiftHeld && FabricTweaksConfig.recoveryCompassShowCoordsOnRightClick) {
                // Toggle between showing coordinates and distance
                showingCoords = !showingCoords;
                // Update HUD overlay state
                FabricRecoveryCompassHudOverlay.showingCoords = showingCoords;

                // Prevent vanilla from also handling the click
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });
    }

    private static boolean bitsandbalance$isRecoveryCompass(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == Items.RECOVERY_COMPASS;
    }
}
