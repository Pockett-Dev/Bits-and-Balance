package org.onenonly.bitsandbalance.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.mechanics.NavigatorCompassItemData;

/**
 * Client-side handler for Navigator Compass HUD overlay.
 * Monitors when the player is holding a compass with a custom target and activates the HUD.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public class NavigatorCompassClient {
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Config.enableNavigatorCompass) return;
        if (!Config.navigatorCompassShowDistance && !Config.navigatorCompassShowCoordinates) return;
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.level() == null) return;
        
        // Don't activate HUD when any screen is open
        if (mc.screen != null) {
            NavigatorCompassHudOverlay.isActive = false;
            return;
        }
        
        // Check if player is holding a compass
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean holdingCompass = (mainHand.is(Items.COMPASS)) || (offHand.is(Items.COMPASS));
        
        if (holdingCompass) {
            // Get the compass being held
            ItemStack compass = mainHand.is(Items.COMPASS) ? mainHand : offHand;
            
            // Check if compass has a custom target
            BlockPos targetPos = getCompassTarget(compass);
            if (targetPos != null) {
                // Activate HUD overlay
                NavigatorCompassHudOverlay.isActive = true;
            } else {
                // Deactivate HUD overlay
                NavigatorCompassHudOverlay.isActive = false;
            }
        } else {
            // Deactivate HUD overlay
            NavigatorCompassHudOverlay.isActive = false;
        }
    }
    
    /**
     * Get the current compass target position from the compass item.
     */
    private static BlockPos getCompassTarget(ItemStack compass) {
        if (!compass.is(Items.COMPASS)) return null;
        return NavigatorCompassItemData.getTargetPos(compass);
    }
}
