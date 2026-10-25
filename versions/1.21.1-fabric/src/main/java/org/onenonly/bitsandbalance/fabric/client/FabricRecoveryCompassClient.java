package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

/**
 * Fabric client-side handler for Improved Recovery Compass.
 */
public class FabricRecoveryCompassClient {

    private static boolean showingCoords = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!FabricTweaksConfig.enableImprovedRecoveryCompass) return;
            if (!FabricTweaksConfig.recoveryCompassShowDistance && !FabricTweaksConfig.recoveryCompassShowCoordsOnRightClick) return;

            LocalPlayer player = client.player;
            if (player == null || player.level() == null) return;

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            boolean holdingCompass = bitsandbalance$isRecoveryCompass(mainHand) || bitsandbalance$isRecoveryCompass(offHand);

            if (holdingCompass) {
                FabricRecoveryCompassHudOverlay.isActive = true;
                FabricRecoveryCompassHudOverlay.showingCoords = showingCoords;
            } else {
                FabricRecoveryCompassHudOverlay.isActive = false;
                showingCoords = false;
            }
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack item = player.getItemInHand(hand);
            if (!FabricTweaksConfig.enableImprovedRecoveryCompass) return InteractionResultHolder.pass(item);
            if (!bitsandbalance$isRecoveryCompass(item)) return InteractionResultHolder.pass(item);
            if (!(player instanceof LocalPlayer localPlayer)) return InteractionResultHolder.pass(item);

            boolean isShiftHeld = localPlayer.isShiftKeyDown();
            if (!isShiftHeld && FabricTweaksConfig.recoveryCompassShowCoordsOnRightClick) {
                showingCoords = !showingCoords;
                FabricRecoveryCompassHudOverlay.showingCoords = showingCoords;
                return InteractionResultHolder.success(item);
            }

            return InteractionResultHolder.pass(item);
        });
    }

    private static boolean bitsandbalance$isRecoveryCompass(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == Items.RECOVERY_COMPASS;
    }
}