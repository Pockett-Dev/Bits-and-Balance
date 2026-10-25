package org.onenonly.bitsandbalance.client;

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
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.mechanics.NavigatorCompassItemData;


/**
 * Renders Navigator Compass distance information when holding a compass with a custom target.
 * Similar to Recovery Compass but shows distance to custom coordinates.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public class NavigatorCompassHudOverlay {
    
    // Static state for the navigator compass display
    public static boolean isActive = false;
    public static String displayText = "";
    
    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        if (!Config.enableNavigatorCompass) return;
        if (!Config.navigatorCompassShowDistance && !Config.navigatorCompassShowCoordinates) return;
        
        // Navigator compass has priority over Auto-Walking and Recovery Compass
        if (!isActive) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        
        // Don't render HUD when any screen is open (inventory, GUI, etc.)
        if (mc.screen != null) {
            isActive = false; // Force deactivate
            return;
        }
        
        // Check if player is holding a compass with custom target
        LocalPlayer player = mc.player;
        if (player == null) return;
        
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean holdingCompass = (mainHand.is(Items.COMPASS)) || (offHand.is(Items.COMPASS));
        
        if (!holdingCompass) {
            isActive = false;
            return;
        }
        
        // Get the compass being held
        ItemStack compass = mainHand.is(Items.COMPASS) ? mainHand : offHand;
        
        // Check if compass has a custom target
        BlockPos targetPos = getCompassTarget(compass);
        if (targetPos == null) {
            isActive = false;
            return;
        }
        
        GuiGraphics gg = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        boolean showCoords = Config.navigatorCompassShowCoordinates;
        boolean showDistance = Config.navigatorCompassShowDistance;

        boolean distanceMode = NavigatorCompassItemData.isDistanceMode(compass);

        if (showCoords && showDistance) {
            if (distanceMode) {
                double distance = player.distanceToSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ());
                int blocksAway = (int) Math.sqrt(distance);
                displayText = String.format("%d blocks away", blocksAway);
            } else {
                displayText = String.format("%d, %d, %d", targetPos.getX(), targetPos.getY(), targetPos.getZ());
            }
        } else if (showCoords) {
            displayText = String.format("%d, %d, %d", targetPos.getX(), targetPos.getY(), targetPos.getZ());
        } else {
            double distance = player.distanceToSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            int blocksAway = (int) Math.sqrt(distance);
            displayText = String.format("%d blocks away", blocksAway);
        }
        
        Component rendered = Component.literal(displayText);
        int textWidth = font.width(rendered);
        // Center text horizontally
        int x = (screenWidth - textWidth) / 2;
        // Position above hotbar, similar to item names
        int baseY = screenHeight - 54;
        int y = HudPositioningHelper.getDynamicYPosition(player, baseY);
        
        // Draw in white with shadow
        gg.drawString(font, rendered, x, y, 0xFFFFFFFF, true);
    }
    
    /**
     * Get the current compass target position from the compass item.
     */
    private static BlockPos getCompassTarget(ItemStack compass) {
        if (!compass.is(Items.COMPASS)) return null;
        return NavigatorCompassItemData.getTargetPos(compass);
    }
}
