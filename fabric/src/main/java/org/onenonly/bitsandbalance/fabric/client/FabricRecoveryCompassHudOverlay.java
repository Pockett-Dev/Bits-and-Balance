package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Renders recovery compass information above the hotbar.
 * Shows distance or coordinates to death location.
 */
public class FabricRecoveryCompassHudOverlay {
    private static final Identifier HUD_LAYER_ID = Identifier.parse("bitsandbalance:recovery_compass");
    
    // Static state for the recovery compass display
    public static boolean isActive = false;
    public static boolean showingCoords = false;
    public static String displayText = "";

    private static ItemStack lastHeldItem = ItemStack.EMPTY;
    private static int itemNameTicks = 0;
    private static final int ITEM_NAME_DISPLAY_TICKS = 40;
    
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;

            // Heuristic: when the held item changes, vanilla shows the item name for ~2 seconds.
            // Move our HUD up during that window to avoid overlapping the name.
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

            if (itemNameTicks > 0) {
                itemNameTicks--;
            }
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.HELD_ITEM_TOOLTIP, HUD_LAYER_ID, (guiGraphics, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            if (FabricNavigatorCompass.isActive) return;
            
            // Don't render HUD when any screen is open (inventory, GUI, etc.)
            if (mc.screen != null) {
                isActive = false; // Force deactivate
                return;
            }
            
            // Check if player is holding a recovery compass
            LocalPlayer player = mc.player;
            if (player == null) return;

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
            
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            boolean holdingCompass = bitsandbalance$isRecoveryCompass(mainHand) || bitsandbalance$isRecoveryCompass(offHand);
            
            if (!holdingCompass) {
                isActive = false;
                return;
            }
            
            Font font = mc.font;
            int screenWidth = guiGraphics.guiWidth();
            int screenHeight = guiGraphics.guiHeight();
            
            // Get death location
            BlockPos deathPos = getDeathLocationFromPlayer(player);
            if (deathPos == null) {
                displayText = "No death location found";
            } else {
                if (showingCoords) {
                    displayText = String.format("%d, %d, %d", 
                        deathPos.getX(), deathPos.getY(), deathPos.getZ());
                } else {
                    double distance = player.distanceToSqr(deathPos.getX(), deathPos.getY(), deathPos.getZ());
                    int blocksAway = (int) Math.sqrt(distance);
                    displayText = String.format("%d blocks away", blocksAway);
                }
            }
            
            Component rendered = Component.literal(displayText);
            int textWidth = font.width(rendered);
            // Center text horizontally
            int x = (screenWidth - textWidth) / 2;
            // Position above hotbar, similar to item names
            int y = screenHeight - 54;
            if (itemNameTicks > 0) {
                y -= 22;
            }
            
            // Draw in white with shadow
            guiGraphics.text(font, rendered, x, y, 0xFFFFFFFF, true);
        });
    }
    
    private static BlockPos getDeathLocationFromPlayer(LocalPlayer player) {
        try {
            // Try to get the last death location from player data
            var lastDeathLocation = player.getLastDeathLocation();
            if (lastDeathLocation.isPresent()) {
                return lastDeathLocation.get().pos();
            }
        } catch (Exception e) {
            // If we can't read the death location, return null
            return null;
        }
        
        return null;
    }

    private static boolean bitsandbalance$isRecoveryCompass(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == Items.RECOVERY_COMPASS;
    }
}
