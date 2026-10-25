package org.onenonly.bitsandbalance.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;

/**
 * Renders recovery compass information in the same location as Auto-Walking text.
 * Recovery compass has priority over Auto-Walking when both are active.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public class RecoveryCompassHudOverlay {
    
    // Static state for the recovery compass display
    public static boolean isActive = false;
    public static boolean showingCoords = false;
    public static String displayText = "";
    
    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        // Navigator compass has priority over Recovery compass
        if (NavigatorCompassHudOverlay.isActive) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        
        // Don't render HUD when any screen is open (inventory, GUI, etc.)
        if (mc.screen != null) {
            isActive = false; // Force deactivate
            return;
        }
        
        // Check if player is holding a recovery compass
        LocalPlayer player = mc.player;
        if (player == null) return;
        
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean holdingCompass = (mainHand.is(Items.RECOVERY_COMPASS)) || (offHand.is(Items.RECOVERY_COMPASS));
        
        if (!holdingCompass) {
            isActive = false;
            return;
        }
        
        GuiGraphics gg = event.getGuiGraphics();
        Font font = mc.font;
        Window window = mc.getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();
        
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
        // Center text horizontally - this should be perfectly centered
        int x = (screenWidth - textWidth) / 2;
        // Position above hotbar, similar to item names
        int baseY = screenHeight - 54;
        int y = HudPositioningHelper.getDynamicYPosition(player, baseY);
        
        // Draw in white with shadow
        gg.drawString(font, rendered, x, y, 0xFFFFFFFF, true);
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
    
    @SubscribeEvent
    public static void onRenderTooltip(RenderTooltipEvent.Pre event) {
        // Only hide tooltips that would interfere with the HUD display
        // Allow inventory tooltips to show normally
        if (isActive) {
            // Check if this is likely an item name tooltip above the hotbar
            // We can detect this by checking if the game is not in any screen (inventory, etc.)
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.screen == null) {
                // Only hide tooltips when not in any screen (inventory, crafting, etc.)
                // This prevents interference with the HUD display while allowing inventory tooltips
                event.setCanceled(true);
            }
        }
    }
}
