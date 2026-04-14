package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.mechanics.NavigatorCompassItemData;

/**
 * Fabric port: Navigator Compass
 * HUD display while holding a compass with a custom LODESTONE_TRACKER target.
 */
public final class FabricNavigatorCompass {
    private FabricNavigatorCompass() {
    }
    public static boolean isActive = false;
    public static String displayText = "";

    private static ItemStack lastHeldItem = ItemStack.EMPTY;
    private static int itemNameTicks = 0;
    private static final int ITEM_NAME_DISPLAY_TICKS = 40;

    public static void initClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!FabricMechanicsConfig.enableNavigatorCompass) return;

            LocalPlayer player = client.player;
            if (player == null) return;

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            boolean holdingCompass = mainHand.is(Items.COMPASS) || offHand.is(Items.COMPASS);

            // Heuristic: when the held item changes, vanilla shows the item name for ~2 seconds.
            // Move our HUD up during that window to avoid overlapping the name.
            try {
                if (!ItemStack.isSameItem(mainHand, lastHeldItem)) {
                    if (!mainHand.isEmpty()) {
                        itemNameTicks = ITEM_NAME_DISPLAY_TICKS;
                    }
                    lastHeldItem = mainHand.copy();
                }
            } catch (Throwable ignored) {
            }
            if (itemNameTicks > 0) {
                itemNameTicks--;
            }

            if (holdingCompass) {
                ItemStack compass = mainHand.is(Items.COMPASS) ? mainHand : offHand;
                BlockPos targetPos = getCompassTarget(compass);
                if (targetPos != null) {
                    isActive = true;
                    updateDisplay(player, compass, targetPos);
                } else {
                    isActive = false;
                }
            } else {
                isActive = false;
            }
        });

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            if (!isActive) return;
            if (!FabricMechanicsConfig.enableNavigatorCompass) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            if (mc.screen != null) return;

            // If HUD render happens before our tick handler runs (rare), avoid a one-frame overlap
            // by detecting held-item changes here too.
            try {
                ItemStack currentItem = mc.player.getMainHandItem();
                if (!ItemStack.isSameItem(currentItem, lastHeldItem)) {
                    if (!currentItem.isEmpty()) {
                        itemNameTicks = ITEM_NAME_DISPLAY_TICKS;
                    }
                    lastHeldItem = currentItem.copy();
                }
            } catch (Throwable ignored) {
            }

            // Priority: Navigator > Recovery > Auto-Walk
            if (FabricRecoveryCompassHudOverlay.isActive) return;

            Font font = mc.font;
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            Component rendered = Component.literal(displayText);
            int textWidth = font.width(rendered);
            int x = (screenWidth - textWidth) / 2;
            int y = screenHeight - 54;
            if (itemNameTicks > 0) {
                y -= 22;
            }

            guiGraphics.drawString(font, rendered, x, y, 0xFFFFFFFF, true);
        });
    }

    private static BlockPos getCompassTarget(ItemStack compass) {
        if (!compass.is(Items.COMPASS)) return null;

        return NavigatorCompassItemData.getTargetPos(compass);
    }

    private static void updateDisplay(LocalPlayer player, ItemStack compass, BlockPos targetPos) {
        boolean showCoords = FabricMechanicsConfig.navigatorCompassShowCoordinates;
        boolean showDistance = FabricMechanicsConfig.navigatorCompassShowDistance;

        boolean distanceMode = NavigatorCompassItemData.isDistanceMode(compass);

        if (showCoords && showDistance) {
            if (distanceMode) {
                double distance = player.distanceToSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ());
                int blocksAway = (int) Math.sqrt(distance);
                displayText = String.format("%d blocks away", blocksAway);
            } else {
                displayText = String.format("%d, %d, %d", targetPos.getX(), targetPos.getY(), targetPos.getZ());
            }
            return;
        }

        if (showCoords) {
            displayText = String.format("%d, %d, %d", targetPos.getX(), targetPos.getY(), targetPos.getZ());
            return;
        }

        if (showDistance) {
            double distance = player.distanceToSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            int blocksAway = (int) Math.sqrt(distance);
            displayText = String.format("%d blocks away", blocksAway);
            return;
        }

        displayText = "Target set";
    }
}
